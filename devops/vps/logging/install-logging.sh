#!/usr/bin/env bash
# Installs a centralized logging stack — Grafana Loki + Promtail + Grafana — as
# bare-metal systemd services on the VPS (no Docker, same as everything else
# under devops/vps/). Run ONCE.
#
# Usage: sudo ./install-logging.sh
#
# What this does NOT require: no change to any of the 13 backend services'
# code, config, or logging setup. Promtail reads directly from journald (the
# same `journalctl -u aerospace-<name>` stream every service already writes
# to today) — this is purely a deployment-layer addition that fans those 13
# streams out into one searchable place, on top of what already exists.
#
# Components:
#   - Loki      — log storage + query engine. Bound to 127.0.0.1:3100 only —
#                 it is never meant to be reached from outside this machine;
#                 only Promtail and Grafana (both on this same host) talk to it.
#   - Promtail  — tails journald, keeps only units matching `aerospace-*.service`
#                 (via relabel_config below), ships them to Loki.
#   - Grafana   — the dashboard/query UI, installed from Grafana's own official
#                 apt repo (their recommended bare-metal install path — unlike
#                 Loki/Promtail it doesn't ship a bare Linux binary as the
#                 primary distribution, and its .deb package already includes
#                 a systemd unit + postinst setup, so we don't hand-roll one).
#
# Reachability: Grafana is bound to 127.0.0.1:3000, NOT exposed on 0.0.0.0.
# This is a small internal ERP — the simplest and safest way to view
# dashboards is an SSH tunnel, not another public Nginx-fronted login surface
# to maintain. See devops/vps/README.md for the tunnel command. If you later
# decide you genuinely need it reachable without SSH, add an Nginx location
# block + basic auth (or Grafana's own auth in front of a public listener) —
# don't just flip http_addr to 0.0.0.0 without one of those in place.
#
# Grafana's default admin credentials (admin/admin) are changed automatically
# by this script — the real password is generated the SAME way every other
# secret in this repo works: read from GRAFANA_ADMIN_PASSWORD in
# /opt/aerospace-erp/.env (see devops/vps/README.md step 4 / .env.example).
# Even though Grafana sits behind an SSH tunnel here, it still gets a real
# password rather than shipping with admin/admin left in place.

set -euo pipefail

ENV_FILE="/opt/aerospace-erp/.env"
if [ ! -f "$ENV_FILE" ]; then
    echo "Missing $ENV_FILE — set up .env (see devops/vps/README.md step 4) before running this." >&2
    exit 1
fi
GRAFANA_ADMIN_PASSWORD="$(grep -E '^GRAFANA_ADMIN_PASSWORD=' "$ENV_FILE" | cut -d'=' -f2-)"
if [ -z "$GRAFANA_ADMIN_PASSWORD" ]; then
    echo "GRAFANA_ADMIN_PASSWORD must be set in $ENV_FILE before running this (see .env.example)." >&2
    exit 1
fi

# Loki and Promtail are released together from the same grafana/loki GitHub repo
# tag — pin one version for both, matching how install-kafka.sh pins
# KAFKA_VERSION. 3.4.3 is the newest stable tag that still ships a
# promtail-linux-amd64.zip asset (Promtail was removed from the Loki release
# starting with 3.7.3 in favor of Grafana Alloy — not used here, since journald
# scraping via Promtail is simpler and is exactly what this stack needs).
LOKI_VERSION="3.4.3"
# Grafana, installed via its official apt repo (see below) rather than a raw
# binary — pinned to the exact package version available there at the time
# this script was written. `apt-cache madison grafana` on the box will show
# what's currently available if this version has aged out of the repo.
GRAFANA_VERSION="13.2.1"

LOKI_INSTALL_DIR="/opt/loki"
LOKI_DATA_DIR="/var/lib/loki"
LOKI_USER="loki"

PROMTAIL_INSTALL_DIR="/opt/promtail"
PROMTAIL_DATA_DIR="/var/lib/promtail"
PROMTAIL_USER="promtail"

echo ">> Installing prerequisites (unzip, curl, gnupg — needed to fetch/verify the Grafana apt repo)..."
apt-get update -qq
apt-get install -y -qq unzip curl gnupg ca-certificates

for u in "$LOKI_USER" "$PROMTAIL_USER"; do
    if ! id "$u" &>/dev/null; then
        useradd --system --no-create-home --shell /usr/sbin/nologin "$u"
    fi
done

# --- journald: switch to persistent storage -------------------------------
# By default journald keeps logs only in /run/log/journal (RAM-backed,
# wiped on reboot). Centralizing logs that vanish on every reboot defeats the
# point, so make journald persist to disk under /var/log/journal — this is a
# host-level journald setting, not a change to any of the 13 services
# themselves. Idempotent: safe to run every time.
mkdir -p /var/log/journal
systemd-tmpfiles --create --prefix /var/log/journal
systemctl restart systemd-journald

# --- Loki -------------------------------------------------------------------
echo ">> Downloading Loki ${LOKI_VERSION}..."
cd /tmp
rm -rf loki-extract loki.zip
curl -fsSL -o loki.zip "https://github.com/grafana/loki/releases/download/v${LOKI_VERSION}/loki-linux-amd64.zip"
unzip -q -o loki.zip -d loki-extract
mkdir -p "$LOKI_INSTALL_DIR"
mv -f loki-extract/loki-linux-amd64 "$LOKI_INSTALL_DIR/loki"
chmod +x "$LOKI_INSTALL_DIR/loki"
rm -rf loki.zip loki-extract

mkdir -p "$LOKI_DATA_DIR/chunks" "$LOKI_DATA_DIR/rules" "$LOKI_DATA_DIR/compactor"
mkdir -p /etc/loki
chown -R "$LOKI_USER:$LOKI_USER" "$LOKI_INSTALL_DIR" "$LOKI_DATA_DIR"

cat > /etc/loki/config.yaml <<EOF
auth_enabled: false

server:
  http_listen_address: 127.0.0.1
  http_listen_port: 3100
  grpc_listen_address: 127.0.0.1
  grpc_listen_port: 9096

common:
  path_prefix: ${LOKI_DATA_DIR}
  storage:
    filesystem:
      chunks_directory: ${LOKI_DATA_DIR}/chunks
      rules_directory: ${LOKI_DATA_DIR}/rules
  replication_factor: 1
  ring:
    kvstore:
      store: inmemory

schema_config:
  configs:
    - from: 2024-01-01
      store: tsdb
      object_store: filesystem
      schema: v13
      index:
        prefix: index_
        period: 24h

compactor:
  working_directory: ${LOKI_DATA_DIR}/compactor
  retention_enabled: true
  delete_request_store: filesystem

limits_config:
  # 31 days of retention — plenty for a small internal ERP's debugging use
  # case, adjust if disk space becomes a concern.
  retention_period: 744h

ruler:
  alertmanager_url: http://localhost:9093
EOF
chown "$LOKI_USER:$LOKI_USER" /etc/loki/config.yaml

cat > /etc/systemd/system/loki.service <<EOF
[Unit]
Description=Grafana Loki (log storage — Aerospace-ERP centralized logging)
After=network.target

[Service]
Type=simple
User=${LOKI_USER}
Group=${LOKI_USER}
ExecStart=${LOKI_INSTALL_DIR}/loki -config.file=/etc/loki/config.yaml
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

# --- Promtail -----------------------------------------------------------
echo ">> Downloading Promtail ${LOKI_VERSION}..."
cd /tmp
rm -rf promtail-extract promtail.zip
curl -fsSL -o promtail.zip "https://github.com/grafana/loki/releases/download/v${LOKI_VERSION}/promtail-linux-amd64.zip"
unzip -q -o promtail.zip -d promtail-extract
mkdir -p "$PROMTAIL_INSTALL_DIR"
mv -f promtail-extract/promtail-linux-amd64 "$PROMTAIL_INSTALL_DIR/promtail"
chmod +x "$PROMTAIL_INSTALL_DIR/promtail"
rm -rf promtail.zip promtail-extract

mkdir -p "$PROMTAIL_DATA_DIR"
mkdir -p /etc/promtail
chown -R "$PROMTAIL_USER:$PROMTAIL_USER" "$PROMTAIL_INSTALL_DIR" "$PROMTAIL_DATA_DIR"

cat > /etc/promtail/config.yaml <<EOF
server:
  http_listen_address: 127.0.0.1
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: ${PROMTAIL_DATA_DIR}/positions.yaml

clients:
  - url: http://127.0.0.1:3100/loki/api/v1/push

scrape_configs:
  - job_name: journal
    journal:
      max_age: 12h
      path: /var/log/journal
      labels:
        job: systemd-journal
    relabel_configs:
      # Pull the systemd unit name out of journald's own field and only keep
      # entries from this repo's 13 aerospace-<name>.service units — matching
      # the exact naming convention from devops/vps/systemd/generate-units.sh.
      - source_labels: ['__journal__systemd_unit']
        target_label: 'unit'
      - source_labels: ['unit']
        regex: 'aerospace-.+\.service'
        action: keep
      - source_labels: ['__journal__hostname']
        target_label: 'host'
EOF
chown "$PROMTAIL_USER:$PROMTAIL_USER" /etc/promtail/config.yaml

cat > /etc/systemd/system/promtail.service <<EOF
[Unit]
Description=Promtail (ships aerospace-*.service journald logs to Loki)
After=network.target loki.service
Wants=loki.service

[Service]
Type=simple
User=${PROMTAIL_USER}
Group=${PROMTAIL_USER}
# Grants read access to /var/log/journal without adding promtail to any more
# privileged group — this is the standard way to let a non-root user read
# journald's persisted logs.
SupplementaryGroups=systemd-journal
ExecStart=${PROMTAIL_INSTALL_DIR}/promtail -config.file=/etc/promtail/config.yaml
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

# --- Grafana (via official apt repo — see header comment for why) ----------
echo ">> Adding Grafana's official apt repo..."
mkdir -p /etc/apt/keyrings
curl -fsSL https://apt.grafana.com/gpg.key | gpg --dearmor -o /etc/apt/keyrings/grafana.gpg
echo "deb [signed-by=/etc/apt/keyrings/grafana.gpg] https://apt.grafana.com stable main" \
    > /etc/apt/sources.list.d/grafana.list

echo ">> Installing Grafana ${GRAFANA_VERSION}..."
apt-get update -qq
apt-get install -y -qq "grafana=${GRAFANA_VERSION}"

# Bind to 127.0.0.1 only (SSH-tunnel-only access — see header comment) and set
# a real admin password from .env instead of leaving admin/admin in place.
# These sed patterns target grafana.ini's shipped default lines (commented-out
# placeholders); the `;\?` makes them match whether or not a previous run of
# this script already uncommented them, so re-running is safe.
sed -i "s/^;\?http_addr[[:space:]]*=.*/http_addr = 127.0.0.1/" /etc/grafana/grafana.ini
sed -i "s/^;\?admin_password[[:space:]]*=.*/admin_password = ${GRAFANA_ADMIN_PASSWORD}/" /etc/grafana/grafana.ini

# Provision the Loki datasource automatically — ready to query the moment you
# log in, no manual "Add data source" step.
mkdir -p /etc/grafana/provisioning/datasources
cat > /etc/grafana/provisioning/datasources/loki.yaml <<EOF
apiVersion: 1

datasources:
  - name: Loki
    type: loki
    access: proxy
    url: http://127.0.0.1:3100
    isDefault: true
    editable: true
EOF
chown -R root:grafana /etc/grafana/provisioning/datasources
chmod 640 /etc/grafana/provisioning/datasources/loki.yaml

echo ">> Starting services..."
systemctl daemon-reload
systemctl enable loki promtail grafana-server
systemctl restart loki
sleep 3
systemctl restart promtail
systemctl restart grafana-server

echo ""
echo "Done."
echo "  Loki:     127.0.0.1:3100 (localhost-only, check: systemctl status loki)"
echo "  Promtail: shipping aerospace-*.service journald logs to Loki"
echo "            (check: systemctl status promtail)"
echo "  Grafana:  127.0.0.1:3000 (localhost-only, check: systemctl status grafana-server)"
echo "            Log in as admin / the GRAFANA_ADMIN_PASSWORD value from .env."
echo "            See devops/vps/README.md for the SSH tunnel command to reach it."

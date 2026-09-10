#!/usr/bin/env bash
# Generates one systemd unit file per Aerospace-ERP backend service and installs
# them to /etc/systemd/system/. Run this ONCE on the VPS after the first deploy
# (and again only if you add/remove a service, not on every code update).
#
# Usage: sudo ./generate-units.sh
#
# Assumes:
#   - The repo is checked out at /opt/aerospace-erp (APP_ROOT below)
#   - Each service has already been built (target/*.jar exists) via deploy.sh
#   - Secrets live in /opt/aerospace-erp/.env (systemd reads this natively via
#     EnvironmentFile= — same KEY=VALUE format as everywhere else in this repo)
#   - A dedicated, unprivileged `erp` system user/group runs everything (created
#     below if missing) — never run application services as root

set -euo pipefail

APP_ROOT="/opt/aerospace-erp"
ENV_FILE="$APP_ROOT/.env"
RUN_USER="erp"

if ! id "$RUN_USER" &>/dev/null; then
    echo "Creating dedicated service user '$RUN_USER' (no login, no home dir needed)..."
    useradd --system --no-create-home --shell /usr/sbin/nologin "$RUN_USER"
fi

# name:port:dir:after  — "after" is a systemd unit name this one should start
# after (dependency ordering only, not a hard requirement — each service already
# tolerates a downstream being briefly unavailable at boot via the retry/circuit
# breaker work, and Eureka registration is itself retried).
SERVICES=(
    "service-registry:8761:service-registry:"
    "auth-service:8087:auth-service:aerospace-service-registry.service"
    "inventory-service:8081:inventory-service:aerospace-auth-service.service"
    "qa-service:8082:qa-service:aerospace-auth-service.service"
    "notification-service:8083:notification-service:aerospace-auth-service.service"
    "order-service:8084:order-service:aerospace-auth-service.service"
    "production-service:8085:production-service:aerospace-auth-service.service"
    "procurement-service:8086:procurement-service:aerospace-auth-service.service"
    "reporting-service:8088:reporting-service:aerospace-auth-service.service"
    "production-resource-service:8090:production-resource-service:aerospace-auth-service.service"
    "sales-service:8091:sales-service:aerospace-auth-service.service"
    "finance-service:8092:finance-service:aerospace-auth-service.service"
    "api-gateway:8089:api-gateway:aerospace-auth-service.service"
)

for entry in "${SERVICES[@]}"; do
    IFS=":" read -r name port dir after <<< "$entry"
    svc_dir="$APP_ROOT/backend/$dir"
    unit_file="/etc/systemd/system/aerospace-${name}.service"

    after_line="After=network.target postgresql.service"
    wants_line=""
    if [ -n "$after" ]; then
        after_line="$after_line $after"
        wants_line="Wants=$after"
    fi

    cat > "$unit_file" <<EOF
[Unit]
Description=Aerospace-ERP - ${name} (port ${port})
${after_line}
${wants_line}

[Service]
Type=simple
User=${RUN_USER}
Group=${RUN_USER}
WorkingDirectory=${svc_dir}
EnvironmentFile=${ENV_FILE}
# Runs the pre-built jar directly (produced by deploy.sh's `mvn package`) — NOT
# `mvn spring-boot:run`, which recompiles/re-resolves on every start and expects
# full source + a local Maven cache, neither appropriate for a running service.
ExecStart=/usr/bin/java -jar ${svc_dir}/target/*.jar
Restart=on-failure
RestartSec=5
# A crash-looping service still gets 5 tries before systemd gives up entirely —
# tune down if you want faster failure visibility instead.
StartLimitBurst=5
StartLimitIntervalSec=120

[Install]
WantedBy=multi-user.target
EOF

    echo "Wrote $unit_file"
done

echo ""
echo "Reloading systemd and enabling all units (won't start them yet)..."
systemctl daemon-reload
for entry in "${SERVICES[@]}"; do
    IFS=":" read -r name _ _ _ <<< "$entry"
    systemctl enable "aerospace-${name}.service"
done

echo ""
echo "Done. Services are enabled (will start on boot) but not yet running."
echo "Start them in order (matches the ordering start-all.ps1 already uses for local dev):"
echo "  sudo systemctl start aerospace-service-registry.service aerospace-auth-service.service"
echo "  sleep 15"
echo "  sudo systemctl start aerospace-inventory-service.service aerospace-qa-service.service \\"
echo "    aerospace-notification-service.service aerospace-order-service.service \\"
echo "    aerospace-production-service.service aerospace-procurement-service.service \\"
echo "    aerospace-reporting-service.service aerospace-production-resource-service.service \\"
echo "    aerospace-sales-service.service aerospace-finance-service.service"
echo "  sleep 15"
echo "  sudo systemctl start aerospace-api-gateway.service   # last — depends on everything else"

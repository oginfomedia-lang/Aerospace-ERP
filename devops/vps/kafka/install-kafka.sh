#!/usr/bin/env bash
# Installs Apache Kafka (with its bundled ZooKeeper) as two systemd services on
# the VPS — bare-metal, since this VPS doesn't use Docker. Run ONCE.
#
# Usage: sudo ./install-kafka.sh
#
# Security: authentication is now enabled (SASL_PLAINTEXT / PLAIN mechanism) —
# a client must present KAFKA_USERNAME/KAFKA_PASSWORD (from /opt/aerospace-erp/.env)
# to connect at all. Encryption-in-transit (TLS) is still NOT set up — traffic
# stays plaintext, mitigated by Kafka remaining bound to 127.0.0.1 only, so it
# never leaves this machine's loopback interface regardless. PLAIN was chosen
# over SCRAM deliberately: SCRAM credentials live in ZooKeeper and need the
# broker already running to bootstrap via `kafka-configs.sh`, which is more
# moving parts for a single-broker, single-tenant setup than it's worth here —
# PLAIN's static JAAS file (generated below FROM .env, same pattern as every
# other secret in this repo) gets the same "you need real credentials to
# connect" outcome more simply.

set -euo pipefail

ENV_FILE="/opt/aerospace-erp/.env"
if [ ! -f "$ENV_FILE" ]; then
    echo "Missing $ENV_FILE — set up .env (see devops/vps/README.md step 4) before running this." >&2
    exit 1
fi
KAFKA_USERNAME="$(grep -E '^KAFKA_USERNAME=' "$ENV_FILE" | cut -d'=' -f2-)"
KAFKA_PASSWORD="$(grep -E '^KAFKA_PASSWORD=' "$ENV_FILE" | cut -d'=' -f2-)"
if [ -z "$KAFKA_USERNAME" ] || [ -z "$KAFKA_PASSWORD" ]; then
    echo "KAFKA_USERNAME / KAFKA_PASSWORD must be set in $ENV_FILE before running this." >&2
    exit 1
fi

KAFKA_VERSION="3.9.0"
SCALA_VERSION="2.13"
INSTALL_DIR="/opt/kafka"
DATA_DIR="/var/lib/kafka"
RUN_USER="kafka"

if ! id "$RUN_USER" &>/dev/null; then
    useradd --system --no-create-home --shell /usr/sbin/nologin "$RUN_USER"
fi

echo ">> Downloading Kafka ${KAFKA_VERSION}..."
cd /tmp
curl -fsSL -o kafka.tgz "https://downloads.apache.org/kafka/${KAFKA_VERSION}/kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz"
tar -xzf kafka.tgz
rm -rf "$INSTALL_DIR"
mv "kafka_${SCALA_VERSION}-${KAFKA_VERSION}" "$INSTALL_DIR"
rm kafka.tgz

mkdir -p "$DATA_DIR/zookeeper" "$DATA_DIR/kafka-logs"
chown -R "$RUN_USER:$RUN_USER" "$INSTALL_DIR" "$DATA_DIR"

# Bind both to localhost only — see security note above.
sed -i "s|^dataDir=.*|dataDir=${DATA_DIR}/zookeeper|" "$INSTALL_DIR/config/zookeeper.properties"
grep -q "^clientPortAddress" "$INSTALL_DIR/config/zookeeper.properties" || \
    echo "clientPortAddress=127.0.0.1" >> "$INSTALL_DIR/config/zookeeper.properties"

sed -i "s|^log.dirs=.*|log.dirs=${DATA_DIR}/kafka-logs|" "$INSTALL_DIR/config/server.properties"

# Replace the plain listener setup with an authenticated SASL_PLAINTEXT one —
# comment out whatever plaintext listener lines shipped in the default config
# first so they don't conflict, then append the real config.
sed -i "s|^listeners=PLAINTEXT|#listeners=PLAINTEXT|; s|^advertised.listeners=PLAINTEXT|#advertised.listeners=PLAINTEXT|; s|^#listeners=PLAINTEXT://:9092\$|#listeners=PLAINTEXT://:9092|" "$INSTALL_DIR/config/server.properties"
cat >> "$INSTALL_DIR/config/server.properties" <<EOF

# --- Aerospace-ERP: SASL_PLAINTEXT auth (added by install-kafka.sh) ---
listeners=SASL_PLAINTEXT://127.0.0.1:9092
advertised.listeners=SASL_PLAINTEXT://127.0.0.1:9092
security.inter.broker.protocol=SASL_PLAINTEXT
sasl.enabled.mechanisms=PLAIN
sasl.mechanism.inter.broker.protocol=PLAIN
EOF

# JAAS config: one static broker-to-broker user (admin) and the one client
# user every Spring service authenticates as, both sourced from .env — not
# hardcoded here. Referenced via KAFKA_OPTS in the systemd unit below.
JAAS_FILE="${INSTALL_DIR}/config/kafka_server_jaas.conf"
cat > "$JAAS_FILE" <<EOF
KafkaServer {
    org.apache.kafka.common.security.plain.PlainLoginModule required
    username="admin"
    password="${KAFKA_PASSWORD}"
    user_admin="${KAFKA_PASSWORD}"
    user_${KAFKA_USERNAME}="${KAFKA_PASSWORD}";
};
EOF
chown "$RUN_USER:$RUN_USER" "$JAAS_FILE"
chmod 600 "$JAAS_FILE"

cat > /etc/systemd/system/zookeeper.service <<EOF
[Unit]
Description=ZooKeeper (for Kafka)
After=network.target

[Service]
Type=simple
User=${RUN_USER}
ExecStart=${INSTALL_DIR}/bin/zookeeper-server-start.sh ${INSTALL_DIR}/config/zookeeper.properties
ExecStop=${INSTALL_DIR}/bin/zookeeper-server-stop.sh
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

cat > /etc/systemd/system/kafka.service <<EOF
[Unit]
Description=Apache Kafka
Requires=zookeeper.service
After=zookeeper.service network.target

[Service]
Type=simple
User=${RUN_USER}
Environment="KAFKA_HEAP_OPTS=-Xmx512M -Xms512M"
Environment="KAFKA_OPTS=-Djava.security.auth.login.config=${JAAS_FILE}"
ExecStart=${INSTALL_DIR}/bin/kafka-server-start.sh ${INSTALL_DIR}/config/server.properties
ExecStop=${INSTALL_DIR}/bin/kafka-server-stop.sh
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable zookeeper kafka
systemctl start zookeeper
sleep 10
systemctl start kafka

echo ""
echo "Done. Kafka is running on 127.0.0.1:9092 (localhost-only)."
echo "Check status with: systemctl status kafka zookeeper"
echo "Topics are auto-created by the Spring Kafka producers on first publish —"
echo "no manual topic setup needed, matching local dev behavior."

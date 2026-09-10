# Hosting Aerospace-ERP on your VPS

Target: a Linux VPS (Ubuntu/Debian assumed — adjust package-manager commands if
different) that already has PostgreSQL and Nginx running for other sites, no
Docker. Everything runs as bare systemd services.

Architecture in one line: **Nginx (443) → api-gateway (127.0.0.1:8089) → the 12
backend services (127.0.0.1-only, never reached directly from the internet)**,
with the React build served as static files by that same Nginx block. Only
port 443 (and 80, for the Let's Encrypt challenge/redirect) is ever exposed
publicly.

Every file under `devops/vps/` and `frontend/web-app/.env.production` already
has the real domain baked in: `ilitecode.com`. Nothing left to substitute —
just double check with `grep -r ilitecode.com devops/vps frontend/web-app/.env.production`
if you ever move to a different domain later.

---

## 0. One-time server prerequisites

```bash
# Java 21 (matches what this project is built/tested against locally)
sudo apt update
sudo apt install -y openjdk-21-jdk maven git

java -version   # confirm 21.x
mvn -version
```
PostgreSQL and Nginx are already installed per your setup — nothing to do here
for those beyond what's below.

## 1. Get the code onto the server

```bash
sudo mkdir -p /opt/aerospace-erp
sudo chown $USER:$USER /opt/aerospace-erp
git clone <your-repo-url> /opt/aerospace-erp
cd /opt/aerospace-erp
```

## 2. Databases

```bash
sudo -u postgres psql -f devops/vps/postgres/setup-databases.sql
```
Creates the 11 databases every service expects (`auth_db`, `inventory_db`,
`qa_db`, `notification_db`, `workorder_db`, `production_db`,
`procurement_db`, `reporting_db`, `production_resource_db`, `sales_db`,
`finance_db`), all owned by the existing `postgres` user — matching what
every service's `application.yml` already expects.

## 3. Kafka (bare-metal — no Docker)

```bash
sudo devops/vps/kafka/install-kafka.sh
```
Installs Kafka + its bundled ZooKeeper as two systemd services, bound to
`127.0.0.1:9092` — only reachable from this same VPS. See the script's own
comments for the (deliberate, already-flagged) plaintext/no-SASL caveat.

## 4. Centralized logging (Loki + Promtail + Grafana — bare-metal, no Docker)

```bash
sudo devops/vps/logging/install-logging.sh
```
Installs Grafana Loki + Promtail as systemd services (`loki.service`,
`promtail.service`) plus Grafana itself via its official apt repo
(`grafana-server.service`). Promtail reads straight from journald and ships
only the 13 `aerospace-*.service` streams to Loki — no change to how any
service logs today, and no more checking 13 separate `journalctl` streams one
at a time.

Both Loki (`127.0.0.1:3100`) and Grafana (`127.0.0.1:3000`) are bound to
localhost only — Grafana is reached over an SSH tunnel, not exposed publicly:
```bash
ssh -L 3000:localhost:3000 user@your-vps-host
```
then browse `http://localhost:3000` and log in as `admin` / the
`GRAFANA_ADMIN_PASSWORD` value from `.env` (see step 5 below — the script
requires that to already be set, refusing to leave the default admin/admin
login in place). The Loki datasource is already provisioned — open the
**Explore** view and query e.g. `{unit=~"aerospace-.+"}` to start reading
logs across all 13 services immediately, no datasource setup needed.

## 5. Secrets — `.env`

```bash
cp .env.example .env
nano .env       # fill in real JWT_SECRET / DB_PASSWORD / MAIL_USERNAME / MAIL_PASSWORD
```
Use the SAME generation approach documented in `.env.example` — a fresh
`JWT_SECRET` and `DB_PASSWORD` for this server, not copy-pasted from your local
machine's `.env`. Then apply the DB password for real:

Also set the Kafka SASL credentials — required here, since `install-kafka.sh`
(step 3, above) configures the broker to reject unauthenticated clients:
```
KAFKA_USERNAME=<pick a username>
KAFKA_PASSWORD=<generate one, same approach as JWT_SECRET>
KAFKA_SECURITY_PROTOCOL=SASL_PLAINTEXT
```
`KAFKA_SECURITY_PROTOCOL` is what switches every service's Kafka client from
this VPS's SASL_PLAINTEXT setup to plain PLAINTEXT — it must be set here, but
leave it unset on your local machine (local dev's docker-compose Kafka has no
SASL and needs no credentials).
```bash
sudo -u postgres psql -c "ALTER USER postgres WITH PASSWORD 'the-same-value-you-put-in-.env';"
```

Also add one production-specific line to `.env` that doesn't exist in your
local one — the real CORS origin (every backend service's `CorsConfigurationSource`
already reads this, it just has never had a real value before now):
```
CORS_ALLOWED_ORIGINS=https://ilitecode.com
```

## 6. Frontend production env

`frontend/web-app/.env.production` already has all ten `REACT_APP_*_API_URL`
values (plus the websocket URL) pointing at `https://ilitecode.com` — nothing
to edit here. (`deploy.sh`, below, builds the frontend using whatever's in this
file — CRA only bakes `REACT_APP_*` vars in at build time, so this has to be
correct *before* the first `npm run build`, not set as a runtime env var —
which is exactly why it's already committed with the real value rather than
left as a step you do by hand.)

## 7. Build everything and set up systemd

```bash
sudo devops/vps/systemd/generate-units.sh      # creates the erp system user +
                                                 # 13 systemd unit files, ONCE
sudo devops/vps/deploy.sh                       # builds every jar + the
                                                 # frontend, then starts everything
```
`generate-units.sh` only needs to run once (or again if you add/remove a
service). `deploy.sh` is what you run on every future code update.

## 8. Nginx + TLS

```bash
sudo cp devops/vps/nginx/erp.conf /etc/nginx/sites-available/ilitecode.com
sudo ln -s /etc/nginx/sites-available/ilitecode.com /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx

sudo certbot --nginx -d ilitecode.com
```
`certbot --nginx` issues the certificate AND rewrites the config to add the
HTTPS server block + HTTP→HTTPS redirect automatically — don't hand-write the
`listen 443 ssl` block yourself.

## 9. Verify

```bash
systemctl status "aerospace-*" --no-pager
journalctl -u aerospace-api-gateway -f     # watch it come up
curl -I https://ilitecode.com               # frontend
curl -I https://ilitecode.com/api/auth/health   # or whatever health path exists
```
Then open the domain in a browser and log in for real.

---

## Every future deployment (after the first)

```bash
cd /opt/aerospace-erp
git pull
sudo devops/vps/deploy.sh
```
That's the whole update flow — build + restart, in the right order.

## What this does NOT cover

Carried over from the earlier audit/fix work, still genuinely open, not part
of this hosting pass:
- **Flyway/Liquibase** — every service still runs `ddl-auto: update` against
  these new production databases too. Fine to start with, but plan to move
  off it before this holds data you can't afford to lose to a bad auto-migration.
- **Postgres backups** — nothing here sets up `pg_dump` cron jobs or WAL
  archiving. Set up a real backup schedule before this holds real data.
- **Metrics** — centralized *logging* is now set up (step 4, Loki + Promtail +
  Grafana), but there's still no Prometheus/metrics scraping or alerting.
- **Kafka SASL/TLS** — still plaintext, mitigated only by binding to
  `127.0.0.1`, as noted in `kafka/install-kafka.sh`.
- **CI/CD** — `deploy.sh` is a manual script you run over SSH, not an
  automated pipeline triggered by a push.

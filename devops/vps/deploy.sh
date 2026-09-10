#!/usr/bin/env bash
# Aerospace-ERP — build everything and restart all services on the VPS.
# Run this on every code update (after `git pull`), not just the first time.
#
# Usage: sudo ./deploy.sh
#
# What it does, in order:
#   1. Installs common-lib to the local Maven repo (every backend service depends
#      on it as a versioned jar, not a live multi-module build — see
#      backend/common-lib's own README/pom for why — so it must be installed
#      BEFORE any service that depends on it is built, every single deploy).
#   2. Builds every backend service's jar.
#   3. Builds the frontend's production bundle.
#   4. Restarts every systemd service in the same order start-all.ps1 uses.

set -euo pipefail

APP_ROOT="/opt/aerospace-erp"
cd "$APP_ROOT"

echo ">> Installing common-lib..."
(cd backend/common-lib && mvn -q -DskipTests clean install)

BACKEND_SERVICES=(
    service-registry auth-service inventory-service qa-service notification-service
    order-service production-service procurement-service reporting-service
    production-resource-service sales-service finance-service api-gateway
)

for svc in "${BACKEND_SERVICES[@]}"; do
    echo ">> Building $svc..."
    (cd "backend/$svc" && mvn -q -DskipTests clean package)
done

echo ">> Building frontend (production bundle)..."
(cd frontend/web-app && npm ci && npm run build)
# Nginx serves straight from this build/ directory — see devops/vps/nginx/erp.conf.

echo ">> Restarting services..."
systemctl restart aerospace-service-registry.service aerospace-auth-service.service
sleep 15
systemctl restart aerospace-inventory-service.service aerospace-qa-service.service \
    aerospace-notification-service.service aerospace-order-service.service \
    aerospace-production-service.service aerospace-procurement-service.service \
    aerospace-reporting-service.service aerospace-production-resource-service.service \
    aerospace-sales-service.service aerospace-finance-service.service
sleep 15
systemctl restart aerospace-api-gateway.service

echo ""
echo ">> Deploy complete. Checking status..."
systemctl status "aerospace-*.service" --no-pager -l | grep -E "^\s*aerospace-|Active:" || true
echo ""
echo "Tail any service's logs with: journalctl -u aerospace-<service-name> -f"

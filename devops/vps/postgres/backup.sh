#!/usr/bin/env bash
# Dumps all 11 Aerospace-ERP databases to a dated directory and prunes backups
# older than RETENTION_DAYS. Installed to run daily via systemd timer (see
# backup.timer/backup.service in this same directory) — not meant to be run
# manually except to test it.

set -euo pipefail

BACKUP_ROOT="/var/backups/aerospace-erp/postgres"
RETENTION_DAYS=14
STAMP="$(date +%Y-%m-%d_%H%M%S)"
OUT_DIR="${BACKUP_ROOT}/${STAMP}"

DATABASES=(
    auth_db inventory_db qa_db notification_db workorder_db production_db
    procurement_db reporting_db production_resource_db sales_db finance_db
)

mkdir -p "$OUT_DIR"

echo ">> Backing up ${#DATABASES[@]} databases to $OUT_DIR"
for db in "${DATABASES[@]}"; do
    # -Fc = custom format: compressed, and restorable with pg_restore (including
    # selectively restoring a single table if that's ever all you need) rather
    # than a single giant plain-SQL file.
    sudo -u postgres pg_dump -Fc "$db" > "${OUT_DIR}/${db}.dump"
done

# Integrity check — a truncated/corrupt dump is worse than no backup, because it
# LOOKS like you're covered. Fail loudly if any file is empty or pg_restore
# can't even list its contents.
for db in "${DATABASES[@]}"; do
    f="${OUT_DIR}/${db}.dump"
    if [ ! -s "$f" ]; then
        echo "!! ${db}.dump is empty — backup failed" >&2
        exit 1
    fi
    sudo -u postgres pg_restore --list "$f" > /dev/null
done
echo ">> All dumps verified readable."

echo ">> Pruning backups older than ${RETENTION_DAYS} days..."
find "$BACKUP_ROOT" -maxdepth 1 -type d -mtime "+${RETENTION_DAYS}" -exec rm -rf {} \;

echo ">> Backup complete: $OUT_DIR ($(du -sh "$OUT_DIR" | cut -f1))"

#!/usr/bin/env bash
# Restores ONE database from a backup produced by backup.sh. Never restores all
# 11 automatically — a restore is a deliberate, one-database-at-a-time action.
#
# Usage: sudo ./restore.sh <database_name> <path-to-dump-file>
# Example: sudo ./restore.sh finance_db /var/backups/aerospace-erp/postgres/2026-09-08_023000/finance_db.dump

set -euo pipefail

DB_NAME="${1:?Usage: restore.sh <database_name> <path-to-dump-file>}"
DUMP_FILE="${2:?Usage: restore.sh <database_name> <path-to-dump-file>}"

if [ ! -f "$DUMP_FILE" ]; then
    echo "Dump file not found: $DUMP_FILE" >&2
    exit 1
fi

echo "!! This will DROP and recreate '$DB_NAME' from $DUMP_FILE."
echo "!! Stop the corresponding aerospace-* service first, or it'll be writing"
echo "!! to the database mid-restore."
read -r -p "Type the database name to confirm ('$DB_NAME'): " CONFIRM
if [ "$CONFIRM" != "$DB_NAME" ]; then
    echo "Confirmation didn't match — aborted, nothing was touched."
    exit 1
fi

sudo -u postgres dropdb --if-exists "$DB_NAME"
sudo -u postgres createdb "$DB_NAME"
sudo -u postgres pg_restore -d "$DB_NAME" "$DUMP_FILE"

echo ">> Restored $DB_NAME from $DUMP_FILE. Restart its service now."

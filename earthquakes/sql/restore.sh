#!/usr/bin/env bash
#
# Restores the earthquakeDB database from a dump file produced by backup.sh.
# Drops and recreates the target database before restoring, so this is destructive
# to whatever currently exists in PGDATABASE.
#
# Usage:
#   ./sql/restore.sh path/to/earthquakeDB_20260729_120000.dump
#
# Env vars (override as needed, defaults match application-local.yml):
#   PGHOST     (default: localhost)
#   PGPORT     (default: 5432)
#   PGUSER     (default: postgres)
#   PGDATABASE (default: earthquakeDB)
#   PGPASSWORD (default: scurtis)
#   PG_BIN     (default: /Library/PostgreSQL/16/bin - where these tools live on this machine's
#               desktop Postgres install, since it isn't on PATH)

set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <dump-file>" >&2
  exit 1
fi

DUMP_FILE="$1"
if [[ ! -f "$DUMP_FILE" ]]; then
  echo "Dump file not found: ${DUMP_FILE}" >&2
  exit 1
fi

PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5432}"
PGUSER="${PGUSER:-postgres}"
PGDATABASE="${PGDATABASE:-earthquakeDB}"
export PGPASSWORD="${PGPASSWORD:-scurtis}"
PG_BIN="${PG_BIN:-/Library/PostgreSQL/16/bin}"

read -r -p "This will DROP and recreate '${PGDATABASE}' on ${PGHOST}:${PGPORT} before restoring. Continue? [y/N] " CONFIRM
if [[ "$CONFIRM" != "y" && "$CONFIRM" != "Y" ]]; then
  echo "Aborted."
  exit 1
fi

echo "Dropping and recreating ${PGDATABASE}..."
"${PG_BIN}/dropdb" --host="$PGHOST" --port="$PGPORT" --username="$PGUSER" --if-exists "$PGDATABASE"
"${PG_BIN}/createdb" --host="$PGHOST" --port="$PGPORT" --username="$PGUSER" "$PGDATABASE"

echo "Restoring from ${DUMP_FILE}..."
"${PG_BIN}/pg_restore" \
  --host="$PGHOST" \
  --port="$PGPORT" \
  --username="$PGUSER" \
  --dbname="$PGDATABASE" \
  --no-owner \
  "$DUMP_FILE"

echo "Restore complete."

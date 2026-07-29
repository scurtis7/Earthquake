#!/usr/bin/env bash
#
# Backs up the earthquakeDB database to a timestamped custom-format dump file.
#
# Usage:
#   ./sql/backup.sh [output-dir]
#
# Env vars (override as needed, defaults match application-local.yml):
#   PGHOST     (default: localhost)
#   PGPORT     (default: 5432)
#   PGUSER     (default: postgres)
#   PGDATABASE (default: earthquakeDB)
#   PGPASSWORD (default: scurtis)
#   PG_BIN     (default: /Library/PostgreSQL/16/bin - where pg_dump lives on this machine's
#               desktop Postgres install, since it isn't on PATH)

set -euo pipefail

PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5432}"
PGUSER="${PGUSER:-postgres}"
PGDATABASE="${PGDATABASE:-earthquakeDB}"
export PGPASSWORD="${PGPASSWORD:-scurtis}"
PG_BIN="${PG_BIN:-/Library/PostgreSQL/16/bin}"

OUTPUT_DIR="${1:-./backups}"
mkdir -p "$OUTPUT_DIR"

TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
OUTPUT_FILE="${OUTPUT_DIR}/${PGDATABASE}_${TIMESTAMP}.dump"

echo "Backing up ${PGDATABASE} from ${PGHOST}:${PGPORT} to ${OUTPUT_FILE}..."

"${PG_BIN}/pg_dump" \
  --host="$PGHOST" \
  --port="$PGPORT" \
  --username="$PGUSER" \
  --dbname="$PGDATABASE" \
  --format=custom \
  --file="$OUTPUT_FILE"

echo "Backup complete: ${OUTPUT_FILE}"

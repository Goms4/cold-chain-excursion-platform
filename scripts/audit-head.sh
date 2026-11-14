#!/usr/bin/env bash
set -euo pipefail
db_name="${DB_NAME:-coldchain_demo_local}"
: "${COLDCHAIN_APP_PASSWORD:?Set COLDCHAIN_APP_PASSWORD}"
export PGPASSWORD="$COLDCHAIN_APP_PASSWORD"
psql -X -h "${DB_HOST:-127.0.0.1}" -p "${DB_PORT:-5432}" -U coldchain_app -d "$db_name" -P pager=off -c 'SELECT seq, timestamp AT TIME ZONE '\''UTC'\'' AS timestamp_utc, row_hash FROM audit_history ORDER BY seq DESC LIMIT 1'

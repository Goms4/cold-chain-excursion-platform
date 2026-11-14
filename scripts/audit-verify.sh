#!/usr/bin/env bash
set -euo pipefail
db_name="${DB_NAME:-coldchain_demo_local}"
: "${COLDCHAIN_APP_PASSWORD:?Set COLDCHAIN_APP_PASSWORD}"
export PGPASSWORD="$COLDCHAIN_APP_PASSWORD"
result="$(psql -X -h "${DB_HOST:-127.0.0.1}" -p "${DB_PORT:-5432}" -U coldchain_app -d "$db_name" -At -F '|' -c 'SELECT ok, first_broken_seq, rows_checked FROM verify_audit_chain()')"
IFS='|' read -r ok broken count <<<"$result"
if [[ "$ok" == t ]]; then echo "Chain verified: $count records"; else echo "Chain broken at record $broken (checked $count)" >&2; exit 1; fi

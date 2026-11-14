#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ -f "$root/docker/.env" ]]; then set -a; source "$root/docker/.env"; set +a; fi
db_host="${DB_HOST:-127.0.0.1}" db_port="${DB_PORT:-5432}" db_name="${DB_NAME:-${POSTGRES_DB:-cold_chain}}"
: "${COLDCHAIN_ADMIN_PASSWORD:?Set COLDCHAIN_ADMIN_PASSWORD in docker/.env}"
: "${COLDLATCH_OPERATIONS_PASSWORD:?Set COLDLATCH_OPERATIONS_PASSWORD in docker/.env}"
: "${COLDLATCH_ANALYST_PASSWORD:?Set COLDLATCH_ANALYST_PASSWORD in docker/.env}"
pg_isready -h "$db_host" -p "$db_port" >/dev/null || { echo "PostgreSQL is not reachable at $db_host:$db_port." >&2; exit 1; }
export PGPASSWORD="$COLDCHAIN_ADMIN_PASSWORD"
roles="$(psql -X -h "$db_host" -p "$db_port" -U coldchain_admin -d "$db_name" -Atqc "SELECT count(*) FROM pg_roles WHERE rolname IN ('coldchain_app','coldchain_migrator')")"
[[ "$roles" == 2 ]] || { echo "Required DB roles are missing. Run scripts/provision-db-roles.sh first." >&2; exit 1; }
crypto="$(psql -X -h "$db_host" -p "$db_port" -U coldchain_admin -d "$db_name" -Atqc "SELECT count(*) FROM pg_extension WHERE extname='pgcrypto'")"
[[ "$crypto" == 1 ]] || { echo "pgcrypto is missing. Run: scripts/fix-pgcrypto.sh $db_name" >&2; exit 1; }
echo "Frontend: http://localhost:3000 | Backend API: http://localhost:8080"
echo "Demo logins: Operations / ${COLDLATCH_OPERATIONS_PASSWORD}; Analyst / ${COLDLATCH_ANALYST_PASSWORD}"
cd "$root/frontend"
exec npm run dev -- --host 0.0.0.0

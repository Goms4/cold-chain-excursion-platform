#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ -f "$root/docker/.env" ]]; then set -a; source "$root/docker/.env"; set +a; fi
export DB_HOST="${DB_HOST:-127.0.0.1}" DB_PORT="${DB_PORT:-5432}" DB_NAME="${DB_NAME:-${POSTGRES_DB:-cold_chain}}"
export DB_USER="${DB_USER:-coldchain_app}" DB_PASSWORD="${DB_PASSWORD:-${COLDCHAIN_APP_PASSWORD:-}}"
export FLYWAY_USER="${FLYWAY_USER:-coldchain_migrator}" FLYWAY_PASSWORD="${FLYWAY_PASSWORD:-${COLDCHAIN_MIGRATOR_PASSWORD:-}}"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-local}"
: "${COLDCHAIN_ADMIN_PASSWORD:?Set COLDCHAIN_ADMIN_PASSWORD in docker/.env}"
: "${COLDCHAIN_APP_PASSWORD:?Set COLDCHAIN_APP_PASSWORD in docker/.env}"
: "${COLDLATCH_OPERATIONS_PASSWORD:?Set COLDLATCH_OPERATIONS_PASSWORD in docker/.env}"
: "${COLDLATCH_ANALYST_PASSWORD:?Set COLDLATCH_ANALYST_PASSWORD in docker/.env}"
pg_isready -h "$DB_HOST" -p "$DB_PORT" >/dev/null || { echo "PostgreSQL is not reachable at $DB_HOST:$DB_PORT." >&2; exit 1; }
export PGPASSWORD="$COLDCHAIN_ADMIN_PASSWORD"
roles="$(psql -X -h "$DB_HOST" -p "$DB_PORT" -U coldchain_admin -d "$DB_NAME" -Atqc "SELECT count(*) FROM pg_roles WHERE rolname IN ('coldchain_app','coldchain_migrator')")"
[[ "$roles" == 2 ]] || { echo "Required DB roles are missing. Run scripts/provision-db-roles.sh first." >&2; exit 1; }
crypto="$(psql -X -h "$DB_HOST" -p "$DB_PORT" -U coldchain_admin -d "$DB_NAME" -Atqc "SELECT count(*) FROM pg_extension WHERE extname='pgcrypto'")"
[[ "$crypto" == 1 ]] || { echo "pgcrypto is missing. Run: scripts/fix-pgcrypto.sh $DB_NAME" >&2; exit 1; }
echo "Backend: http://localhost:8080 | Frontend: http://localhost:3000"
echo "Login credentials are loaded from the configured environment and are not printed."
cd "$root/backend"
exec mvn spring-boot:run -Dspring-boot.run.profiles="$SPRING_PROFILES_ACTIVE"

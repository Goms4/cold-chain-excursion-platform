#!/usr/bin/env bash
set -euo pipefail
profile="${COLDLATCH_ENV:-${SPRING_PROFILES_ACTIVE:-}}"
if [[ "$profile" != local && "$profile" != demo ]]; then
  echo "Refusing reset: set COLDLATCH_ENV or SPRING_PROFILES_ACTIVE to local or demo." >&2; exit 2
fi
db_host="${DB_HOST:-127.0.0.1}"
db_port="${DB_PORT:-5432}"
db_name="${DB_NAME:-coldchain_demo_local}"
if [[ ! "$db_name" =~ ^[a-zA-Z0-9_]+(_demo|_local)$ ]]; then
  echo "Refusing reset: database name must end in _demo or _local." >&2; exit 2
fi
if [[ "$db_host" != localhost && "$db_host" != 127.0.0.1 && "${COLDLATCH_ALLOW_REMOTE_RESET:-false}" != true ]]; then
  echo "Refusing reset: host is not localhost; explicitly set COLDLATCH_ALLOW_REMOTE_RESET=true to override." >&2; exit 2
fi
if [[ "${1:-}" != --yes ]]; then
  echo "Target: $db_host:$db_port/$db_name. Re-run with --yes to drop and recreate this database." >&2; exit 2
fi
: "${COLDCHAIN_ADMIN_PASSWORD:?Set COLDCHAIN_ADMIN_PASSWORD}"
: "${COLDCHAIN_MIGRATOR_PASSWORD:?Set COLDCHAIN_MIGRATOR_PASSWORD}"
: "${COLDCHAIN_APP_PASSWORD:?Set COLDCHAIN_APP_PASSWORD}"
echo "Reset target: $db_host:$db_port/$db_name"
export PGPASSWORD="$COLDCHAIN_ADMIN_PASSWORD"
exists="$(psql -X -h "$db_host" -p "$db_port" -U coldchain_admin -d postgres -At -c "SELECT 1 FROM pg_database WHERE datname='$db_name'")"
if [[ "$exists" == 1 ]]; then
  backup_dir="${DEMO_BACKUP_DIR:-./backups/coldlatch-demo}"
  mkdir -p "$backup_dir"
  backup_file="$backup_dir/$db_name-$(date +%Y%m%d-%H%M%S).dump"
  pg_dump -Fc -h "$db_host" -p "$db_port" -U coldchain_admin -d "$db_name" -f "$backup_file"
  echo "Existing demo DB backup: $backup_file"
fi
dropdb --if-exists -h "$db_host" -p "$db_port" -U coldchain_admin "$db_name"
createdb -h "$db_host" -p "$db_port" -U coldchain_admin -O coldchain_admin "$db_name"
PGPASSWORD="$COLDCHAIN_ADMIN_PASSWORD" psql -X -h "$db_host" -p "$db_port" -U coldchain_admin -d "$db_name" -v ON_ERROR_STOP=1 <<'SQL'
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
ALTER SCHEMA public OWNER TO coldchain_migrator;
SQL
PGPASSWORD="$COLDCHAIN_ADMIN_PASSWORD" psql -X -h "$db_host" -p "$db_port" -U coldchain_admin -d postgres -v ON_ERROR_STOP=1 -c "GRANT CREATE ON DATABASE \"$db_name\" TO coldchain_migrator"
export DB_HOST="$db_host" DB_PORT="$db_port" DB_NAME="$db_name"
export DB_USER=coldchain_app DB_PASSWORD="$COLDCHAIN_APP_PASSWORD"
export FLYWAY_USER=coldchain_migrator FLYWAY_PASSWORD="$COLDCHAIN_MIGRATOR_PASSWORD"
mvn -q -f backend/pom.xml -DskipTests spring-boot:run -Dspring-boot.run.profiles=local >"${TMPDIR:-/tmp}/coldlatch-seed-app.log" 2>&1 &
app_pid=$!
cleanup() { kill "$app_pid" 2>/dev/null || true; wait "$app_pid" 2>/dev/null || true; }
trap cleanup EXIT
ready=false
for _ in $(seq 1 90); do
  if curl -fsS "http://127.0.0.1:8080/actuator/health" >/dev/null 2>&1; then ready=true; break; fi
  if ! kill -0 "$app_pid" 2>/dev/null; then cat "${TMPDIR:-/tmp}/coldlatch-seed-app.log" >&2; exit 1; fi
  sleep 1
done
if [[ "$ready" != true ]]; then cat "${TMPDIR:-/tmp}/coldlatch-seed-app.log" >&2; echo "Spring Boot did not start." >&2; exit 1; fi
PGPASSWORD="$COLDCHAIN_ADMIN_PASSWORD" psql -X -h "$db_host" -p "$db_port" -U coldchain_admin -d "$db_name" -v ON_ERROR_STOP=1 -v db_name="$db_name" -f database/grant-app-permissions.sql
DB_HOST="$db_host" DB_PORT="$db_port" DB_NAME="$db_name" \
  COLDCHAIN_PROVISIONER_USER=coldchain_admin \
  COLDCHAIN_PROVISIONER_PASSWORD="$COLDCHAIN_ADMIN_PASSWORD" \
  scripts/provision-analytics-role.sh
scripts/demo-seed.sh
scripts/demo-verify.sh
PGPASSWORD="$COLDCHAIN_ADMIN_PASSWORD" psql -X -h "$db_host" -p "$db_port" -U coldchain_admin -d "$db_name" -v ON_ERROR_STOP=1 -c "select 'shipments='||count(*) from shipments; select 'lots='||count(*) from lots; select 'events='||count(*) from sensor_events; select 'incidents='||count(*) from excursion_incidents; select 'investigations='||count(*) from investigations; select 'decisions='||count(*) from disposition_decisions;"
echo "Reset, migrations, grants, service-backed REST seed, and SQL count verification completed."

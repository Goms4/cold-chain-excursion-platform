#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
db_host="${DB_HOST:-localhost}" db_port="${DB_PORT:-5432}" db_name="${DB_NAME:-cold_chain}"
admin_database="${COLDCHAIN_PROVISIONER_DATABASE:-postgres}"
admin_user="${COLDCHAIN_PROVISIONER_USER:-${PGUSER:-${USER}}}"
if [[ -z "${ANALYTICS_DB_PASSWORD:-}" && -f "$root/data-pipeline/.env" ]]; then
  set -a
  source "$root/data-pipeline/.env"
  set +a
fi
if [[ -z "${ANALYTICS_DB_PASSWORD:-}" ]]; then
  ANALYTICS_DB_PASSWORD="$(openssl rand -hex 32)"
  export ANALYTICS_DB_PASSWORD
  umask 077
  python3 - "$root/data-pipeline/.env" <<'PY'
import os, pathlib, sys
path = pathlib.Path(sys.argv[1])
lines = path.read_text().splitlines() if path.exists() else []
lines = [line for line in lines if not line.startswith("ANALYTICS_DB_PASSWORD=")]
lines.append("ANALYTICS_DB_PASSWORD=" + os.environ["ANALYTICS_DB_PASSWORD"])
path.write_text("\n".join(lines) + "\n")
path.chmod(0o600)
PY
fi
export PGPASSWORD="${COLDCHAIN_PROVISIONER_PASSWORD:-${PGPASSWORD:-}}"
psql -X -h "$db_host" -p "$db_port" -U "$admin_user" -d "$admin_database" \
  -v ON_ERROR_STOP=1 -v analytics_password="$ANALYTICS_DB_PASSWORD" <<'SQL'
SELECT format('CREATE ROLE coldchain_analytics WITH LOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS CONNECTION LIMIT 5 PASSWORD %L', :'analytics_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='coldchain_analytics')
\gexec
SELECT format('ALTER ROLE coldchain_analytics WITH LOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS CONNECTION LIMIT 5 PASSWORD %L', :'analytics_password')
\gexec
ALTER ROLE coldchain_analytics SET default_transaction_read_only = on;
DO $$ DECLARE membership record; BEGIN
  FOR membership IN SELECT r.rolname FROM pg_auth_members m JOIN pg_roles r ON r.oid=m.roleid
    WHERE m.member=(SELECT oid FROM pg_roles WHERE rolname='coldchain_analytics')
  LOOP EXECUTE format('REVOKE %I FROM coldchain_analytics', membership.rolname); END LOOP;
END $$;
SQL
PGPASSWORD="${COLDCHAIN_PROVISIONER_PASSWORD:-${PGPASSWORD:-}}" psql -X -h "$db_host" -p "$db_port" -U "$admin_user" -d "$admin_database" \
  -v ON_ERROR_STOP=1 -v db_name="$db_name" <<'SQL'
SELECT format('REVOKE ALL ON DATABASE %I FROM coldchain_analytics', :'db_name')
\gexec
SELECT format('GRANT CONNECT ON DATABASE %I TO coldchain_analytics', :'db_name')
\gexec
SQL
PGPASSWORD="${COLDCHAIN_PROVISIONER_PASSWORD:-${PGPASSWORD:-}}" psql -X -h "$db_host" -p "$db_port" -U "$admin_user" -d "$db_name" -v ON_ERROR_STOP=1 <<'SQL'
REVOKE ALL ON SCHEMA public FROM coldchain_analytics;
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM coldchain_analytics;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM coldchain_analytics;
REVOKE ALL ON ALL FUNCTIONS IN SCHEMA public FROM coldchain_analytics;
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name='analytics') THEN
    GRANT USAGE ON SCHEMA analytics TO coldchain_analytics;
    GRANT SELECT ON ALL TABLES IN SCHEMA analytics TO coldchain_analytics;
  END IF;
END $$;
SQL
echo "coldchain_analytics provisioned for $db_name; credentials were not printed."

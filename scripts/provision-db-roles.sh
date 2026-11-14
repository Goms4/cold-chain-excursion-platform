#!/usr/bin/env bash
set -euo pipefail
db_host="${DB_HOST:-127.0.0.1}"
db_port="${DB_PORT:-5432}"
db_name="${DB_NAME:-cold_chain}"
admin_database="${COLDCHAIN_PROVISIONER_DATABASE:-postgres}"
admin_user="${COLDCHAIN_PROVISIONER_USER:-${PGUSER:-$USER}}"
: "${COLDCHAIN_ADMIN_PASSWORD:?Set COLDCHAIN_ADMIN_PASSWORD}"
: "${COLDCHAIN_MIGRATOR_PASSWORD:?Set COLDCHAIN_MIGRATOR_PASSWORD}"
: "${COLDCHAIN_APP_PASSWORD:?Set COLDCHAIN_APP_PASSWORD}"
export PGPASSWORD="${COLDCHAIN_PROVISIONER_PASSWORD:-${PGPASSWORD:-}}"
psql -X -h "$db_host" -p "$db_port" -U "$admin_user" -d "$admin_database" -v ON_ERROR_STOP=1 \
  -v admin_password="$COLDCHAIN_ADMIN_PASSWORD" -v migrator_password="$COLDCHAIN_MIGRATOR_PASSWORD" \
  -v app_password="$COLDCHAIN_APP_PASSWORD" -f database/provision-roles.sql
if [[ "${CREATE_DB_IF_MISSING:-false}" == true ]]; then
  PGPASSWORD="${COLDCHAIN_PROVISIONER_PASSWORD:-${PGPASSWORD:-}}" psql -X -h "$db_host" -p "$db_port" -U "$admin_user" -d "$admin_database" \
    -v ON_ERROR_STOP=1 -v db_name="$db_name" <<'SQL'
SELECT format('CREATE DATABASE %I OWNER coldchain_admin', :'db_name')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'db_name')
\gexec
SQL
fi
PGPASSWORD="$COLDCHAIN_ADMIN_PASSWORD" psql -X -h "$db_host" -p "$db_port" -U coldchain_admin \
  -d "$db_name" -v ON_ERROR_STOP=1 -c 'CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public'
PGPASSWORD="$COLDCHAIN_ADMIN_PASSWORD" psql -X -h "$db_host" -p "$db_port" -U coldchain_admin \
  -d "$db_name" -v ON_ERROR_STOP=1 <<'SQL'
REVOKE CREATE ON SCHEMA public FROM PUBLIC, coldchain_app;
ALTER SCHEMA public OWNER TO coldchain_migrator;
DO $$ DECLARE obj record; BEGIN
  FOR obj IN SELECT tablename FROM pg_tables WHERE schemaname='public' LOOP
    EXECUTE format('ALTER TABLE public.%I OWNER TO coldchain_migrator', obj.tablename);
  END LOOP;
  FOR obj IN SELECT sequencename FROM pg_sequences WHERE schemaname='public' LOOP
    EXECUTE format('ALTER SEQUENCE public.%I OWNER TO coldchain_migrator', obj.sequencename);
  END LOOP;
END $$;
SQL
if [[ "${PROVISION_APP_PERMISSIONS:-true}" == true ]]; then
  PGPASSWORD="$COLDCHAIN_ADMIN_PASSWORD" psql -X -h "$db_host" -p "$db_port" -U coldchain_admin \
    -d "$db_name" -v ON_ERROR_STOP=1 -v db_name="$db_name" -f database/grant-app-permissions.sql
fi
PGPASSWORD="$COLDCHAIN_ADMIN_PASSWORD" psql -X -h "$db_host" -p "$db_port" -U coldchain_admin -d "$admin_database" \
  -v ON_ERROR_STOP=1 -c "GRANT CREATE ON DATABASE \"$db_name\" TO coldchain_migrator"
echo "Roles provisioned and schema ownership/grants applied to $db_name on $db_host."
if [[ "${PROVISION_ANALYTICS_ROLE:-true}" == true ]]; then
  DB_HOST="$db_host" DB_PORT="$db_port" DB_NAME="$db_name" COLDCHAIN_PROVISIONER_DATABASE="$admin_database" scripts/provision-analytics-role.sh
fi

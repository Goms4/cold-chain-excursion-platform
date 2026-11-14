#!/usr/bin/env bash
set -euo pipefail
psql -X -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  -v admin_password="$COLDCHAIN_ADMIN_PASSWORD" \
  -v migrator_password="$COLDCHAIN_MIGRATOR_PASSWORD" \
  -v app_password="$COLDCHAIN_APP_PASSWORD" -f /docker-entrypoint-initdb.d/provision-roles.txt
psql -X -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  -c 'CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public'
psql -X -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  -c 'REVOKE CREATE ON SCHEMA public FROM PUBLIC; ALTER SCHEMA public OWNER TO coldchain_migrator;'
psql -X -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
  -c "GRANT CREATE ON DATABASE \"$POSTGRES_DB\" TO coldchain_migrator"

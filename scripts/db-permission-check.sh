#!/usr/bin/env bash
set -euo pipefail
db_host="${DB_HOST:-127.0.0.1}"; db_port="${DB_PORT:-5432}"; db_name="${DB_NAME:-cold_chain}"
: "${COLDCHAIN_APP_PASSWORD:?Set COLDCHAIN_APP_PASSWORD}"
if [[ -f "$(dirname "$0")/../data-pipeline/.env" ]]; then
  set -a; source "$(dirname "$0")/../data-pipeline/.env"; set +a
fi
: "${ANALYTICS_DB_PASSWORD:?Run scripts/provision-analytics-role.sh or set ANALYTICS_DB_PASSWORD}"
export PGPASSWORD="$COLDCHAIN_APP_PASSWORD"
psql_app() { psql -X -h "$db_host" -p "$db_port" -U coldchain_app -d "$db_name" -v ON_ERROR_STOP=1 "$@"; }
role="$(psql_app -Atc "select current_user || ':' || r.rolsuper || ':' || r.rolbypassrls || ':' || pg_get_userbyid(c.relowner) from pg_roles r cross join pg_class c where r.rolname=current_user and c.oid='public.audit_history'::regclass")"
[[ "$role" == "coldchain_app:false:false:coldchain_migrator" ]] || { echo "FAIL app role/owner: $role"; exit 1; }
echo "PASS coldchain_app is non-superuser, no BYPASSRLS, audit owner coldchain_migrator"
expect_denied() {
  local label="$1" sql="$2" output
  if output="$(psql_app -c "$sql" 2>&1)"; then echo "FAIL $label unexpectedly succeeded"; exit 1; fi
  printf 'PASS %s: %s\n' "$label" "$(printf '%s' "$output" | sed -n '1p')"
}
expect_denied 'audit UPDATE denied' 'UPDATE audit_history SET actor=actor WHERE false'
expect_denied 'audit DELETE denied' 'DELETE FROM audit_history WHERE false'
expect_denied 'audit TRUNCATE denied' 'TRUNCATE audit_history'
expect_denied 'trigger disable denied' 'ALTER TABLE audit_history DISABLE TRIGGER ALL'
expect_denied 'public CREATE denied' 'CREATE TABLE public.permission_probe(id int)'
psql_app -c "BEGIN; INSERT INTO audit_history(entity_type,entity_id,action,actor,payload_after,rationale,category) VALUES ('SECURITY_CHECK','SEC-ROLLBACK','INSERTED','System','permission test','rolled back','TECHNICAL'); ROLLBACK" >/dev/null
echo 'PASS audit INSERT succeeds (rolled back after permission check)'

analytics_sql() { PGPASSWORD="$ANALYTICS_DB_PASSWORD" psql -X -h "$db_host" -p "$db_port" -U coldchain_analytics -d "$db_name" -v ON_ERROR_STOP=1 "$@"; }
for view in v_shipments v_lots v_sensor_events v_excursions v_investigations v_decisions v_missing_intervals; do
  analytics_sql -c "SELECT * FROM analytics.$view LIMIT 0" >/dev/null
  echo "PASS analytics SELECT analytics.$view"
done
for table in shipments lots audit_history role_credential; do
  if output="$(analytics_sql -c "SELECT * FROM public.$table LIMIT 1" 2>&1)"; then echo "FAIL analytics can SELECT public.$table"; exit 1; fi
  [[ "$output" == *"permission denied"* ]] || { printf '%s\n' "$output" | sed -n '1p'; exit 1; }
  echo "PASS analytics SELECT public.$table denied"
done
expect_analytics_denied() {
  local label="$1" sql="$2" output
  if output="$(analytics_sql -c "$sql" 2>&1)"; then echo "FAIL analytics $label unexpectedly succeeded"; exit 1; fi
  [[ "$output" == *"permission denied"* || "$output" == *"read-only transaction"* ]] || { printf '%s\n' "$output" | sed -n '1p'; exit 1; }
  echo "PASS analytics $label denied"
}
expect_analytics_denied INSERT 'INSERT INTO public.shipments SELECT * FROM public.shipments WHERE false'
expect_analytics_denied UPDATE 'UPDATE public.shipments SET status=status WHERE false'
expect_analytics_denied DELETE 'DELETE FROM public.shipments WHERE false'
expect_analytics_denied TRUNCATE 'TRUNCATE public.shipments'
expect_analytics_denied 'CREATE TABLE' 'CREATE TABLE public.analytics_permission_probe(id integer)'
expect_analytics_denied 'CREATE VIEW' 'CREATE VIEW analytics.analytics_permission_probe AS SELECT 1 AS id'
role_flags="$(analytics_sql -Atc "SELECT rolsuper||'|'||rolcreatedb||'|'||rolcreaterole||'|'||rolreplication||'|'||rolbypassrls||'|'||rolconnlimit FROM pg_roles WHERE rolname=current_user")"
[[ "$role_flags" == "false|false|false|false|false|5" ]] || { echo "FAIL analytics role attributes"; exit 1; }
echo 'PASS analytics role is NOSUPERUSER/NOCREATEDB/NOCREATEROLE/NOREPLICATION/NOBYPASSRLS with limit 5'
readonly_flag="$(analytics_sql -Atc "SHOW default_transaction_read_only")"
[[ "$readonly_flag" == on ]] || { echo 'FAIL analytics role default is not read-only'; exit 1; }
echo 'PASS analytics role default_transaction_read_only=on'

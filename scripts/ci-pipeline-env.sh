#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
host="${PGHOST:-127.0.0.1}"
port="${PGPORT:-5432}"
db="${PGDATABASE:-coldlatch_ci_${GITHUB_RUN_ID:-local}}"
bootstrap_user="${COLDCHAIN_PROVISIONER_USER:-${PGUSER:-}}"
bootstrap_password="${COLDCHAIN_PROVISIONER_PASSWORD:-${PGPASSWORD:-}}"
admin_db="${COLDCHAIN_PROVISIONER_DATABASE:-coldlatch_ci_bootstrap}"
server_port="${SERVER_PORT:-8080}"
api="${COLDLATCH_API_URL:-http://127.0.0.1:$server_port}"
log="${CI_BACKEND_LOG:-${TMPDIR:-/tmp}/coldlatch-ci-backend.log}"
pid_file="${CI_BACKEND_PID_FILE:-${TMPDIR:-/tmp}/coldlatch-ci-backend.pid}"
timeout_seconds="${CI_WAIT_TIMEOUT_SECONDS:-120}"

: "${bootstrap_user:?Set COLDCHAIN_PROVISIONER_USER to the temporary PostgreSQL bootstrap role}"
: "${bootstrap_password:?Set COLDCHAIN_PROVISIONER_PASSWORD}"
: "${COLDCHAIN_ADMIN_PASSWORD:?Set COLDCHAIN_ADMIN_PASSWORD}"
: "${COLDCHAIN_MIGRATOR_PASSWORD:?Set COLDCHAIN_MIGRATOR_PASSWORD}"
: "${COLDCHAIN_APP_PASSWORD:?Set COLDCHAIN_APP_PASSWORD}"
: "${ANALYTICS_DB_PASSWORD:?Set ANALYTICS_DB_PASSWORD}"
: "${COLDLATCH_OPERATIONS_PASSWORD:?Set COLDLATCH_OPERATIONS_PASSWORD}"
: "${COLDLATCH_ANALYST_PASSWORD:?Set COLDLATCH_ANALYST_PASSWORD}"
[[ "$db" =~ ^[a-zA-Z0-9_]+$ ]] || { echo 'PGDATABASE must contain only letters, digits, and underscores' >&2; exit 2; }

backend_pid=""
redact_log() {
  [[ -f "$log" ]] || return 0
  python3 - "$log" <<'PY'
import os, pathlib, sys
p = pathlib.Path(sys.argv[1])
data = p.read_text(errors="replace")
for key in ("COLDCHAIN_PROVISIONER_PASSWORD", "COLDCHAIN_ADMIN_PASSWORD", "COLDCHAIN_MIGRATOR_PASSWORD", "COLDCHAIN_APP_PASSWORD", "ANALYTICS_DB_PASSWORD", "COLDLATCH_OPERATIONS_PASSWORD", "COLDLATCH_ANALYST_PASSWORD"):
    secret = os.environ.get(key, "")
    if secret:
        data = data.replace(secret, "[REDACTED]")
p.write_text(data)
PY
}
stop_backend() {
  if [[ -n "$backend_pid" ]] && kill -0 "$backend_pid" 2>/dev/null; then
    kill "$backend_pid" 2>/dev/null || true
    wait "$backend_pid" 2>/dev/null || true
  fi
  redact_log
  rm -f "$pid_file"
}
trap stop_backend EXIT

deadline=$((SECONDS + timeout_seconds))
until PGPASSWORD="$bootstrap_password" pg_isready -h "$host" -p "$port" -U "$bootstrap_user" -d "$admin_db" >/dev/null 2>&1; do
  if (( SECONDS >= deadline )); then echo "PostgreSQL did not become ready within ${timeout_seconds}s" >&2; exit 1; fi
  sleep 2
done

export DB_HOST="$host" DB_PORT="$port" DB_NAME="$db"
export COLDCHAIN_PROVISIONER_DATABASE="$admin_db"
export PROVISION_ANALYTICS_ROLE=false PROVISION_APP_PERMISSIONS=false CREATE_DB_IF_MISSING=true
export COLDCHAIN_PROVISIONER_USER="$bootstrap_user" COLDCHAIN_PROVISIONER_PASSWORD="$bootstrap_password"
scripts/provision-db-roles.sh
PGPASSWORD="$COLDCHAIN_ADMIN_PASSWORD" psql -X -h "$host" -p "$port" -U coldchain_admin -d "$db" -v ON_ERROR_STOP=1 \
  -c 'CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public'

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-local}"
export SERVER_PORT="$server_port"
export DB_USER=coldchain_app DB_PASSWORD="$COLDCHAIN_APP_PASSWORD"
export FLYWAY_USER=coldchain_migrator FLYWAY_PASSWORD="$COLDCHAIN_MIGRATOR_PASSWORD"
export COLDLATCH_API_URL="$api"
python3 - "$server_port" <<'PY'
import socket, sys
with socket.socket() as probe:
    probe.settimeout(0.25)
    if probe.connect_ex(("127.0.0.1", int(sys.argv[1]))) == 0:
        raise SystemExit(f"TCP port {sys.argv[1]} is already in use; choose an unused SERVER_PORT")
PY
mkdir -p "$(dirname "$log")"
: > "$log"
java -jar "$root/backend/target/cold-chain-platform-1.0.0.jar" >>"$log" 2>&1 &
backend_pid=$!
printf '%s\n' "$backend_pid" > "$pid_file"
deadline=$((SECONDS + timeout_seconds))
until curl -fsS "$api/actuator/health" >/dev/null 2>&1 && kill -0 "$backend_pid" 2>/dev/null; do
  if ! kill -0 "$backend_pid" 2>/dev/null; then
    redact_log; tail -n 100 "$log" >&2
    echo 'Backend exited before health became available' >&2; exit 1
  fi
  if (( SECONDS >= deadline )); then
    redact_log; tail -n 100 "$log" >&2
    echo "Backend health endpoint did not become ready within ${timeout_seconds}s" >&2; exit 1
  fi
  sleep 2
done
echo 'Backend health: UP'
migration_count="$(PGPASSWORD="$COLDCHAIN_ADMIN_PASSWORD" psql -X -h "$host" -p "$port" -U coldchain_admin -d "$db" -v ON_ERROR_STOP=1 -Atc "SELECT count(*) FROM flyway_schema_history WHERE success AND version ~ '^[0-9]+$' AND version::integer BETWEEN 1 AND 12")"
[[ "$migration_count" == 12 ]] || { echo "Expected successful Flyway V1–V12 migrations; found $migration_count" >&2; exit 1; }
echo "Flyway migrations applied: V1–V12 ($migration_count successful)"
PGPASSWORD="$COLDCHAIN_ADMIN_PASSWORD" psql -X -h "$host" -p "$port" -U coldchain_admin -d "$db" \
  -v ON_ERROR_STOP=1 -v db_name="$db" -f "$root/database/grant-app-permissions.sql"
PGPASSWORD="$bootstrap_password" scripts/provision-analytics-role.sh
scripts/demo-seed.sh
if [[ -x scripts/demo-verify.sh ]]; then scripts/demo-verify.sh; fi

# Export through the existing pipeline entry point. Its verifier uses the migrator
# credentials while extraction itself connects only as coldchain_analytics.
export COLDLATCH_SKIP_PIPELINE_DOTENV=true
export PGHOST="$host" PGPORT="$port" PGDATABASE="$db" PGUSER=coldchain_analytics PGPASSWORD="$ANALYTICS_DB_PASSWORD"
export COLDCHAIN_PROVISIONER_USER=coldchain_migrator COLDCHAIN_PROVISIONER_PASSWORD="$COLDCHAIN_MIGRATOR_PASSWORD"
scripts/run-pipeline.sh

output="$root/data-pipeline/output"
[[ -s "$output/LATEST" ]] || { echo 'Pipeline did not create data-pipeline/output/LATEST' >&2; exit 1; }
run_id="$(tr -d '\r\n' < "$output/LATEST")"
snapshot="$output/$run_id"
[[ -d "$snapshot" ]] || { echo "LATEST points to missing snapshot directory: $run_id" >&2; exit 1; }
for file in dim_shipment.csv dim_lot.csv fact_sensor_event.csv fact_excursion.csv fact_investigation.csv fact_decision.csv telemetry_quality_report.csv kpi_summary.csv manifest.csv; do
  [[ -s "$snapshot/$file" ]] || { echo "Required pipeline file missing or empty: $snapshot/$file" >&2; exit 1; }
done
if ! python3 - "$snapshot/manifest.csv" <<'PY'
import csv, sys
with open(sys.argv[1], newline="", encoding="utf-8") as f:
    rows = list(csv.DictReader(f))
if not rows or any((row.get("status") or "").upper() == "FAIL" for row in rows):
    raise SystemExit("manifest is empty or contains FAIL")
PY
then echo 'Pipeline manifest validation failed' >&2; exit 1; fi
echo "Pipeline snapshot validated: $snapshot"

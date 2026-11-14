#!/usr/bin/env bash
set -euo pipefail
db_name="${DB_NAME:-coldchain_demo_local}"
host="${DB_HOST:-127.0.0.1}" port="${DB_PORT:-5432}"
: "${COLDCHAIN_APP_PASSWORD:?Set COLDCHAIN_APP_PASSWORD}"
: "${COLDCHAIN_ADMIN_PASSWORD:?Set COLDCHAIN_ADMIN_PASSWORD}"
psql_as() { local role="$1" password="$2"; shift 2; PGPASSWORD="$password" psql -X -h "$host" -p "$port" -U "$role" -d "$db_name" -v ON_ERROR_STOP=1 "$@"; }
prefix="CHAIN-CHECK-$(date +%s)-$$"
before="$(psql_as coldchain_app "$COLDCHAIN_APP_PASSWORD" -At -c 'SELECT count(*) FROM audit_history')"
pids=()
for worker in $(seq 1 6); do
  (
    values=""
    for n in $(seq 1 50); do
      id="$prefix-$worker-$n"
      values+="('CHAIN_CHECK','$id','INSERTED','Chain verifier',NULL,NULL,'chain concurrency probe','TECHNICAL'),"
    done
    values="${values%,}"
    psql_as coldchain_app "$COLDCHAIN_APP_PASSWORD" -c "INSERT INTO audit_history(entity_type,entity_id,action,actor,payload_before,payload_after,rationale,category) VALUES $values" >/dev/null
  ) & pids+=("$!")
done
for pid in "${pids[@]}"; do wait "$pid"; done
inserted="$(psql_as coldchain_app "$COLDCHAIN_APP_PASSWORD" -At -c "SELECT count(*) FROM audit_history WHERE entity_type='CHAIN_CHECK' AND entity_id LIKE '$prefix-%'")"
[[ "$inserted" == 300 ]] || { echo "FAIL expected 300 concurrent audit rows, found $inserted" >&2; exit 1; }
result="$(psql_as coldchain_app "$COLDCHAIN_APP_PASSWORD" -At -F '|' -c "SELECT ok, rows_checked FROM verify_audit_chain(); SELECT count(*)=count(DISTINCT prev_hash) FROM audit_history; SELECT count(*)=max(seq) AND min(seq)=1 FROM audit_history;")"
printf 'CONCURRENT_INSERTS workers=6 rows_per_worker=50\nINSERTED=%s (previous=%s)\nCHAIN_OK_ROWS=%s\nDISTINCT_PREV_HASH=%s\nCONTIGUOUS_SEQ=%s\n' "$inserted" "$before" "$(printf '%s\n' "$result" | head -1)" "$(printf '%s\n' "$result" | sed -n '2p')" "$(printf '%s\n' "$result" | tail -1)"
[[ "$(printf '%s\n' "$result" | head -1 | cut -d'|' -f1)" == t ]]
[[ "$(printf '%s\n' "$result" | sed -n '2p')" == t ]]
[[ "$(printf '%s\n' "$result" | tail -1)" == t ]]

PGPASSWORD="$COLDCHAIN_ADMIN_PASSWORD" psql -X -h "$host" -p "$port" -U coldchain_admin -d "$db_name" -v ON_ERROR_STOP=1 <<SQL
BEGIN;
ALTER TABLE audit_history DISABLE TRIGGER ALL;
UPDATE audit_history SET rationale = COALESCE(rationale,'') || ' tampered' WHERE seq=2;
ALTER TABLE audit_history ENABLE TRIGGER ALL;
DO \$\$
DECLARE result RECORD;
BEGIN
  SELECT * INTO result FROM verify_audit_chain();
  IF result.ok OR result.first_broken_seq <> 2 THEN RAISE EXCEPTION 'reason tamper was not detected at row 2: %', result; END IF;
  RAISE NOTICE 'TAMPER_UPDATE_DETECTED first_broken_seq=%', result.first_broken_seq;
END \$\$;
ROLLBACK;

BEGIN;
ALTER TABLE audit_history DISABLE TRIGGER ALL;
DELETE FROM audit_history WHERE seq=(SELECT max(seq)/2 FROM audit_history);
ALTER TABLE audit_history ENABLE TRIGGER ALL;
DO \$\$
DECLARE result RECORD;
BEGIN
  SELECT * INTO result FROM verify_audit_chain();
  IF result.ok THEN RAISE EXCEPTION 'middle-row deletion was not detected'; END IF;
  RAISE NOTICE 'TAMPER_DELETE_DETECTED first_broken_seq=%', result.first_broken_seq;
END \$\$;
ROLLBACK;
SQL
echo 'PASS audit concurrency and both rollback-isolated tamper checks'

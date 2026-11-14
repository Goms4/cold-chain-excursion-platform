#!/usr/bin/env bash
set -euo pipefail
base="${COLDLATCH_API_URL:-http://127.0.0.1:8080}"
db_name="${DB_NAME:-coldchain_demo_local}"
check_no=0; failures=0; body=''
call() {
  local expected="$1" method="$2" path="$3" token="${4:-}" payload="${5:-}" temp status jar csrf
  temp="$(mktemp)"; local args=(-sS -o "$temp" -w '%{http_code}' -X "$method" "$base$path")
  jar="$(mktemp)"; [[ "$token" == ops ]] && jar="${TMPDIR:-/tmp}/coldlatch-operations.cookies"; [[ "$token" == analyst ]] && jar="${TMPDIR:-/tmp}/coldlatch-analyst.cookies"
  [[ "$token" == none || -z "$token" ]] || args+=(-b "$jar" -c "$jar")
  if [[ "$method" != GET && "$path" != /api/auth/login && "$path" != /api/auth/csrf ]]; then
    curl -sS -o /dev/null -b "$jar" -c "$jar" "$base/api/auth/csrf"
    csrf="$(awk '$6=="XSRF-TOKEN" {v=$7} END {print v}' "$jar")"
    [[ -z "$csrf" ]] || args+=(-H "X-XSRF-TOKEN: $csrf")
  fi
  [[ -z "$payload" ]] || args+=(-H 'Content-Type: application/json' --data "$payload")
  status="$(curl "${args[@]}")" || { echo "FAIL curl $method $path"; failures=$((failures+1)); rm -f "$temp"; body=''; return; }
  body="$(cat "$temp")"; rm -f "$temp"; check_no=$((check_no+1))
  if [[ "$status" == "$expected" ]]; then printf 'PASS %02d HTTP %s %s\n' "$check_no" "$method" "$path";
  else printf 'FAIL %02d %s %s expected %s got %s: %s\n' "$check_no" "$method" "$path" "$expected" "$status" "$body"; failures=$((failures+1)); fi
}
assert_json() {
  local label="$1" expr="$2"; check_no=$((check_no+1))
  if jq -e "$expr" <<<"$body" >/dev/null; then printf 'PASS %02d %s\n' "$check_no" "$label";
  else printf 'FAIL %02d %s: %s\n' "$check_no" "$label" "$body"; failures=$((failures+1)); fi
}
offset_time() { node -e 'process.stdout.write(new Date(Date.now()+Number(process.argv[1])*60000).toISOString())' -- "$1"; }
read_temp() {
  local shipment="$1" lot="$2" sensor="$3" temp="$4" time="$5" payload
  payload="$(jq -cn --argjson c "$temp" --arg s "$sensor" --arg t "$time" '{temperatureC:$c,sensorId:$s,eventTime:$t}')"
  call 201 POST "/api/shipments/$shipment/lots/$lot/temperature" "$ops" "$payload"
}

ops=ops; analyst=analyst
bad_jar="${TMPDIR:-/tmp}/coldlatch-bad-login.cookies"; rm -f "$bad_jar"
curl -sS -o /dev/null -c "$bad_jar" "$base/api/auth/csrf"
bad_csrf="$(awk '$6=="XSRF-TOKEN" {v=$7} END {print v}' "$bad_jar")"
for bad_role in OPERATIONS ANALYST; do
  bad_password="incorrect-password"; [[ "$bad_role" != ANALYST ]] || bad_password="${COLDLATCH_OPERATIONS_PASSWORD:-incorrect-password}"
  response="$(mktemp)"; status="$(curl -sS -o "$response" -w '%{http_code}' -b "$bad_jar" -c "$bad_jar" -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $bad_csrf" --data "$(jq -cn --arg r "$bad_role" --arg p "$bad_password" '{role:$r,companyName:"ColdLatch",password:$p}')" "$base/api/auth/login")"
  check_no=$((check_no+1)); if [[ "$status" == 401 ]] && jq -e '.message=="Incorrect role or password" and (has("password")|not) and (has("passwordHash")|not)' "$response" >/dev/null; then printf 'PASS %02d generic failure for incorrect %s credential\n' "$check_no" "$bad_role"; else printf 'FAIL %02d generic failure %s: %s\n' "$check_no" "$bad_role" "$(cat "$response")"; failures=$((failures+1)); fi
  rm -f "$response"
done
response="$(mktemp)"; status="$(curl -sS -o "$response" -w '%{http_code}' "$base/api/shipments")"; check_no=$((check_no+1))
if [[ "$status" == 401 ]]; then printf 'PASS %02d unauthenticated request is rejected\n' "$check_no"; else printf 'FAIL %02d unauthenticated request expected 401 got %s\n' "$check_no" "$status"; failures=$((failures+1)); fi; rm -f "$response"
for auth_role in OPERATIONS ANALYST; do
  auth_role_lower="$(printf '%s' "$auth_role" | tr '[:upper:]' '[:lower:]')"
  auth_jar="${TMPDIR:-/tmp}/coldlatch-${auth_role_lower}.cookies"; rm -f "$auth_jar"
  curl -sS -o /dev/null -c "$auth_jar" "$base/api/auth/csrf"
  auth_csrf="$(awk '$6=="XSRF-TOKEN" {v=$7} END {print v}' "$auth_jar")"
  auth_password="${COLDLATCH_OPERATIONS_PASSWORD:-}"; [[ "$auth_role" != ANALYST ]] || auth_password="${COLDLATCH_ANALYST_PASSWORD:-}"
  [[ -n "$auth_password" ]] || { echo "Set COLDLATCH_${auth_role}_PASSWORD" >&2; exit 2; }
  auth_response="$(mktemp)"; status="$(curl -sS -o "$auth_response" -w '%{http_code}' -b "$auth_jar" -c "$auth_jar" -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $auth_csrf" --data "$(jq -cn --arg r "$auth_role" --arg p "$auth_password" '{role:$r,companyName:"ColdLatch",password:$p}')" "$base/api/auth/login")"
  check_no=$((check_no+1)); if [[ "$status" == 200 ]] && jq -e --arg r "$auth_role" '.role==$r and (has("token")|not) and (has("password")|not) and (has("passwordHash")|not)' "$auth_response" >/dev/null; then printf 'PASS %02d role/password login %s with cookie session and no credential fields\n' "$check_no" "$auth_role"; else printf 'FAIL %02d login %s: %s\n' "$check_no" "$auth_role" "$(cat "$auth_response")"; failures=$((failures+1)); fi
  rm -f "$auth_response"
done
call 200 GET /api/audit/verify "$ops"; assert_json 'audit-chain API verifies the current rows' '.ok==true and .rowsChecked>=0'

call 200 GET /api/shipments/SHP-10001/sensors "$ops"; sensor1="$(jq -r '.[0].id' <<<"$body")"
read_temp SHP-10001 LOT-10001 "$sensor1" 5.8 "$(offset_time 0)"; assert_json 'normal reading accepted' '.condition=="NORMAL"'
call 200 GET /api/shipments/SHP-10001/excursions "$ops"; before="$(jq length <<<"$body")"
high_time="$(offset_time 0)"; read_temp SHP-10001 LOT-10001 "$sensor1" 25 "$high_time"; assert_json 'high reading accepted' '.dataQuality!="QUARANTINED"'
call 200 GET /api/shipments/SHP-10001/excursions "$ops"; after="$(jq length <<<"$body")"
assert_json 'high reading created exactly one incident' "length==$(($before+1))"
read_temp SHP-10001 LOT-10001 "$sensor1" 25 "$high_time"; assert_json 'duplicate replay reported' '.message|ascii_downcase|contains("already")'
read_temp SHP-10001 LOT-10001 "$sensor1" 5.6 "$(offset_time -60)"; assert_json 'late event within window accepted' '.dataQuality!="QUARANTINED"'
read_temp SHP-10009 LOT-10010 SEN-009 5.7 "$(offset_time -150000)"; assert_json 'late event beyond window quarantined' '.dataQuality=="QUARANTINED"'
read_temp SHP-10001 LOT-10001 "$sensor1" 5.5 "$(offset_time 120)"; assert_json 'future timestamp quarantined for review' '.dataQuality=="QUARANTINED"'

bad="$(jq -cn --arg s "$sensor1" --arg t "$(offset_time 0)" '{temperatureC:-999,sensorId:$s,eventTime:$t}')"
call 400 POST /api/shipments/SHP-10001/lots/LOT-10001/temperature "$ops" "$bad"
call 400 POST /api/shipments/SHP-10001/lots/LOT-10001/temperature "$ops" '{"temperatureC":"not-a-number","sensorId":"SEN-001"}'
call 404 POST /api/shipments/SHP-99999/lots/LOT-10001/temperature "$ops" '{"temperatureC":5.5}'
call 400 POST /api/shipments/SHP-10001/lots/LOT-10002/temperature "$ops" '{"temperatureC":5.5}'
call 403 POST /api/shipments "$analyst" '{"trackingNumber":"TRK-CHECK-RBAC","shipmentReference":"Routine shipment","origin":"Berlin","destination":"Munich","storageProfileId":"PROFILE-REF"}'

call 200 GET /api/shipments/SHP-10002/excursions "$ops"; excursion2="$(jq -r '.[0].id' <<<"$body")"
call 200 GET "/api/investigations/excursion/$excursion2" "$ops"; inv2="$(jq -r .id <<<"$body")"
registration="$(jq -cn --arg e "$excursion2" '{shipmentId:"SHP-10002",lotId:"LOT-10002",excursionId:$e,investigator:"Operations reviewer"}')"
call 201 POST /api/investigations "$ops" "$registration"; registered_inv="$(jq -r .id <<<"$body")"
assert_json 'Operations registered the existing excursion investigation' '.shipmentId=="SHP-10002" and (.investigationId|startswith("INV-"))'
call 200 GET "/api/investigations/$registered_inv" "$analyst"
assert_json 'Analyst can retrieve Operations-registered investigation' '.shipmentId=="SHP-10002"'
call 403 POST "/api/investigations/$inv2/decision" "$ops" '{"decision":"HOLD","reason":"Not authorized by role."}'
call 200 PATCH "/api/investigations/$inv2/assignee" "$ops" '{"investigator":"Analyst"}'
call 201 POST "/api/investigations/$inv2/manual-inspection" "$analyst" "$(jq -cn --arg t "$(offset_time 0)" '{inspectionTime:$t,observedTemperatureC:5.5,packageIntegrityStatus:"INTACT",visualCondition:"Packaging intact",notes:"Reviewed for validation"}')"

call 200 GET /api/shipments/SHP-10001/excursions "$ops"; excursion1="$(jq -r '.[0].id' <<<"$body")"
call 200 GET "/api/investigations/excursion/$excursion1" "$ops"; inv1="$(jq -r .id <<<"$body")"
release="$(jq -cn --arg l LOT-10001 '{decision:"RELEASE",reason:"Evidence supports a safe release decision.",investigatorName:"Forged investigator",ruleVersion:"forged-v999",affectedLotIds:[$l]}')"
call 201 POST "/api/investigations/$inv1/decision" "$analyst" "$release"
assert_json 'forged investigator name ignored' '.investigatorName=="Analyst · ColdLatch"'
assert_json 'forged rule version ignored' '.ruleVersion=="v1.0.0"'
hold='{"decision":"HOLD","reason":"Keep this lot quarantined after review."}'
call 201 POST "/api/investigations/$inv2/decision" "$analyst" "$hold"

read_temp SHP-10004 LOT-10005 SEN-004 25 "$(offset_time 0)"
call 200 GET /api/shipments/SHP-10004/excursions "$ops"; excursion4="$(jq -r '.[0].id' <<<"$body")"
call 200 GET "/api/investigations/excursion/$excursion4" "$ops"; inv4="$(jq -r .id <<<"$body")"
reject='{"decision":"REJECT","reason":"Exposure exceeds the validated limit."}'
call 201 POST "/api/investigations/$inv4/decision" "$analyst" "$reject"
blank='{"decision":"HOLD","reason":""}'
call 400 POST /api/investigations/INV-10005/decision "$analyst" "$blank"; assert_json 'blank reason has a friendly validation message' '.message|ascii_downcase|contains("reason")'

call 409 DELETE /api/shipments/SHP-10009 "$ops" '{"reason":"Cannot remove during an open investigation."}'
call 409 DELETE /api/shipments/SHP-10003 "$ops" '{"reason":"Cannot remove recorded quality decision."}'
delete_tracking="TRK-CHECK-DELETE-$(date +%s)"
new_shipment="$(jq -cn --arg t "$delete_tracking" '{trackingNumber:$t,shipmentReference:"Hamburg to Copenhagen, Samples",origin:"Hamburg",destination:"Copenhagen",storageProfileId:"PROFILE-REF",status:"PLANNED"}')"
call 201 POST /api/shipments "$ops" "$new_shipment"; deleted_id="$(jq -r .id <<<"$body")"
call 204 DELETE "/api/shipments/$deleted_id" "$ops" '{"reason":"Duplicate registration withdrawn before dispatch."}'
call 404 GET "/api/shipments/$deleted_id" "$ops"
call 200 GET /api/shipments "$ops"; assert_json 'deleted shipment is absent from active shipment list' "all(.[];.id!=\"$deleted_id\")"
call 200 GET "/api/audit/SHIPMENT/$deleted_id" "$ops"; assert_json 'audit remains after soft delete' 'any(.[];.action=="DELETED") and any(.[];.action=="CREATED")'
empty_tracking="TRK-CHECK-LOT-$(date +%s)"
empty_shipment="$(jq -cn --arg t "$empty_tracking" '{trackingNumber:$t,shipmentReference:"Aarhus to Odense, Samples",origin:"Aarhus",destination:"Odense",storageProfileId:"PROFILE-REF",status:"PLANNED"}')"
call 201 POST /api/shipments "$ops" "$empty_shipment"; empty_id="$(jq -r .id <<<"$body")"
empty_lot="$(jq -cn --arg s "$empty_id" '{shipmentId:$s,productName:"Stability samples",batchNumber:"BATCH-DELETE-LOT",quantity:10}')"
call 201 POST /api/lots "$ops" "$empty_lot"; empty_lot_id="$(jq -r .id <<<"$body")"
call 204 DELETE "/api/lots/$empty_lot_id" "$ops" '{"reason":"Lot withdrawn before dispatch."}'
call 200 GET "/api/shipments/$empty_id" "$ops"; assert_json 'deleting an unused lot leaves shipment condition Unknown' '.condition=="UNKNOWN"'

read_temp SHP-10010 LOT-10011 SEN-010 45 "$(offset_time 0)"
call 200 GET /api/shipments/SHP-10010/excursions "$ops"; excursion10="$(jq -r '.[0].id' <<<"$body")"
call 200 GET "/api/investigations/excursion/$excursion10" "$ops"; inv10="$(jq -r .id <<<"$body")"
call 201 POST "/api/investigations/$inv10/decision" "$analyst" '{"decision":"RELEASE","reason":"Reviewed stability evidence supports release."}'

# Logout must invalidate the server-side session; then exercise the five-in-five-minute limiter.
jar="${TMPDIR:-/tmp}/coldlatch-operations.cookies"; curl -sS -o /dev/null -b "$jar" -c "$jar" "$base/api/auth/csrf"; csrf="$(awk '$6=="XSRF-TOKEN" {v=$7} END {print v}' "$jar")"
call 204 POST /api/auth/logout "$ops"
response="$(mktemp)"; status="$(curl -sS -o "$response" -w '%{http_code}' -b "$jar" "$base/api/auth/me")"; check_no=$((check_no+1))
if [[ "$status" == 401 ]]; then printf 'PASS %02d logout invalidates the session\n' "$check_no"; else printf 'FAIL %02d logout invalidation expected 401 got %s\n' "$check_no" "$status"; failures=$((failures+1)); fi; rm -f "$response"
bad_jar="${TMPDIR:-/tmp}/coldlatch-rate-limit.cookies"; rm -f "$bad_jar"; curl -sS -o /dev/null -c "$bad_jar" "$base/api/auth/csrf"; bad_csrf="$(awk '$6=="XSRF-TOKEN" {v=$7} END {print v}' "$bad_jar")"
for attempt in 1 2 3 4 5 6; do
  response="$(mktemp)"; status="$(curl -sS -o "$response" -w '%{http_code}' -b "$bad_jar" -c "$bad_jar" -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $bad_csrf" --data '{"role":"OPERATIONS","companyName":"ColdLatch","password":"invalid-password"}' "$base/api/auth/login")"; check_no=$((check_no+1))
  expected=401; [[ "$attempt" != 6 ]] || expected=429
  if [[ "$status" == "$expected" ]]; then printf 'PASS %02d login rate-limit attempt %s/%s returned %s\n' "$check_no" "$attempt" 6 "$status"; else printf 'FAIL %02d login rate-limit expected %s got %s\n' "$check_no" "$expected" "$status"; failures=$((failures+1)); fi; rm -f "$response"
done

: "${COLDCHAIN_APP_PASSWORD:?Set COLDCHAIN_APP_PASSWORD for database permission checks}"
export PGPASSWORD="$COLDCHAIN_APP_PASSWORD"
psql_app() { psql -X -h "${DB_HOST:-127.0.0.1}" -p "${DB_PORT:-5432}" -U coldchain_app -d "$db_name" -v ON_ERROR_STOP=1 "$@"; }
denied_sql() {
  local label="$1" statement="$2" output; check_no=$((check_no+1))
  if output="$(psql_app -c "$statement" 2>&1)"; then printf 'FAIL %02d %s unexpectedly succeeded\n' "$check_no" "$label"; failures=$((failures+1));
  else printf 'PASS %02d %s: %s\n' "$check_no" "$label" "$(printf '%s' "$output" | sed -n '1p')"; fi
}
denied_sql 'audit UPDATE denied as app' 'UPDATE audit_history SET actor=actor WHERE false'
denied_sql 'audit DELETE denied as app' 'DELETE FROM audit_history WHERE false'
denied_sql 'audit TRUNCATE denied as app' 'TRUNCATE audit_history'
denied_sql 'trigger disable denied as app' 'ALTER TABLE audit_history DISABLE TRIGGER ALL'
denied_sql 'CREATE in public denied as app' 'CREATE TABLE public.permission_probe(id int)'
check_no=$((check_no+1)); psql_app -c "BEGIN; INSERT INTO audit_history(entity_type,entity_id,action,actor,payload_after,rationale,category) VALUES ('SECURITY_CHECK','SEC-ROLLBACK','INSERTED','System','permission test','rolled back','TECHNICAL'); ROLLBACK" >/dev/null && printf 'PASS %02d audit INSERT succeeds then rolls back\n' "$check_no"
if (( failures > 0 )); then echo "FAILURES=$failures CHECKS=$check_no"; exit 1; fi
echo "PASS all $check_no REST/database checks"

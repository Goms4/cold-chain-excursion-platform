#!/usr/bin/env bash
set -euo pipefail
db_name="${DB_NAME:-coldchain_demo_local}"
base="${COLDLATCH_API_URL:-http://127.0.0.1:8080}"
: "${COLDCHAIN_ADMIN_PASSWORD:?Set COLDCHAIN_ADMIN_PASSWORD}"
export PGPASSWORD="$COLDCHAIN_ADMIN_PASSWORD"
psql_demo() { psql -X -h "${DB_HOST:-127.0.0.1}" -p "${DB_PORT:-5432}" -U coldchain_admin -d "$db_name" -v ON_ERROR_STOP=1 -At "$@"; }
expect() { local label="$1" value="$2" expected="$3"; [[ "$value" == "$expected" ]] || { echo "FAIL $label expected=$expected actual=$value" >&2; exit 1; }; echo "PASS $label=$value"; }
expect_positive() { local label="$1" value="$2"; [[ "$value" =~ ^[0-9]+$ ]] && (( value > 0 )) || { echo "FAIL $label expected positive count actual=$value" >&2; exit 1; }; echo "PASS $label=$value"; }
api_jar="${TMPDIR:-/tmp}/coldlatch-demo-verify.cookies"; rm -f "$api_jar"
curl -fsS -o /dev/null -c "$api_jar" "$base/api/auth/csrf"
api_csrf="$(awk '$6=="XSRF-TOKEN" {v=$7} END {print v}' "$api_jar")"
: "${COLDLATCH_ANALYST_PASSWORD:?Set COLDLATCH_ANALYST_PASSWORD}"
curl -fsS -o /dev/null -b "$api_jar" -c "$api_jar" -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $api_csrf" --data "$(jq -cn --arg p "$COLDLATCH_ANALYST_PASSWORD" '{role:"ANALYST",companyName:"ColdLatch",password:$p}')" "$base/api/auth/login"
api_get() { curl -fsS -b "$api_jar" "$base$1"; }

expect shipments "$(psql_demo -c 'select count(*) from shipments where deleted_at is null')" 10
expect lots "$(psql_demo -c 'select count(*) from lots where deleted_at is null')" 11
expect orphan_lots "$(psql_demo -c 'select count(*) from lots l left join shipments s on s.id=l.shipment_id where s.id is null')" 0
leaks="$(psql_demo -c "SELECT count(*) FROM (SELECT to_jsonb(x)::text AS data FROM shipments x UNION ALL SELECT to_jsonb(x)::text FROM lots x UNION ALL SELECT to_jsonb(x)::text FROM sensors x UNION ALL SELECT to_jsonb(x)::text FROM sensor_events x UNION ALL SELECT to_jsonb(x)::text FROM raw_sensor_events x UNION ALL SELECT to_jsonb(x)::text FROM excursion_incidents x UNION ALL SELECT to_jsonb(x)::text FROM investigations x UNION ALL SELECT to_jsonb(x)::text FROM disposition_decisions x UNION ALL SELECT to_jsonb(x)::text FROM audit_history x) q WHERE data ILIKE ANY(ARRAY['%SCENARIO%','%SCEN-%','%TEST%','%RBAC%','%RUNNER%','%SHP-624%'])")"
expect forbidden_business_markers "$leaks" 0
normal="$(psql_demo -c "select count(*) from sensor_events where status='NORMAL' and not is_duplicate and quality_flag<>'QUARANTINED'")"; expect_positive normal_readings "$normal"
expect_positive minor_incidents "$(psql_demo -c "select count(*) from excursion_incidents where severity='MINOR'")"
expect pre_excursion_lot_excluded "$(psql_demo -c "select count(*) from excursion_affected_lots a join lots l on l.id=a.lot_id where l.batch_number='BATCH-26-RESERVE'")" 0
expect pre_excursion_lot_stays_low_risk "$(psql_demo -c "select count(*) from lots where batch_number='BATCH-26-RESERVE' and risk_level='LOW'")" 1
expect_positive critical_incidents "$(psql_demo -c "select count(*) from excursion_incidents where severity='CRITICAL'")"
expect duplicate_replays "$(psql_demo -c "select count(*) from raw_sensor_events where processing_status='DUPLICATE'")" 3
expect_positive accepted_late_readings "$(psql_demo -c "select count(*) from sensor_events where quality_flag in ('LATE','OUT_OF_ORDER') and quality_flag<>'QUARANTINED'")"
expect quarantined_events "$(psql_demo -c "select count(*) from raw_sensor_events where processing_status='QUARANTINED'")" 2
expect_positive missing_intervals "$(psql_demo -c 'select count(*) from missing_intervals')"
expect_positive contradictory_inspections "$(psql_demo -c 'select count(*) from manual_inspections where contradicts_sensor')"
expect late_within_72h_SHP10005 "$(psql_demo -c "select count(*) from sensor_events where shipment_id='SHP-10005' and quality_flag='OUT_OF_ORDER' and event_time>now()-interval '72 hours'")" 1
expect quarantined_beyond_72h_SHP10009 "$(psql_demo -c "select count(*) from raw_sensor_events where shipment_id='SHP-10009' and processing_status='QUARANTINED' and event_time<now()-interval '72 hours'")" 1
expect duplicate_shipment_has_one_incident "$(psql_demo -c "select count(*) from excursion_incidents where shipment_id='SHP-10008'")" 1
expect missing_interval_has_no_incident "$(psql_demo -c "select count(*) from excursion_incidents where shipment_id='SHP-10004'")" 0
expect slow_drift_readings "$(psql_demo -c "select count(*) from sensor_events where shipment_id='SHP-10007' and temperature_c between 15 and 27 and not is_duplicate")" 4
expect_positive audit_ops_actor "$(psql_demo -c "select count(*) from audit_history where actor='Operations officer · ColdLatch'")"
expect_positive audit_analyst_actor "$(psql_demo -c "select count(*) from audit_history where actor='Analyst · ColdLatch'")"
expect_positive audit_system_actor "$(psql_demo -c "select count(*) from audit_history where actor='System'")"
raw="$(psql_demo -c 'select count(*) from raw_sensor_events')"; clean="$(psql_demo -c "select count(*) from raw_sensor_events where processing_status='VALIDATED'")"; duplicates="$(psql_demo -c "select count(*) from raw_sensor_events where processing_status='DUPLICATE'")"; quarantined="$(psql_demo -c "select count(*) from raw_sensor_events where processing_status='QUARANTINED'")"
expect 'raw = clean + duplicate + quarantined' "$((clean+duplicates+quarantined))" "$raw"
echo "RAW_EVENT_COUNTS raw=$raw clean=$clean duplicates=$duplicates quarantined=$quarantined"

# SQL mirrors ShipmentConditionService's precedence and profile-specific bounds.
sql_summary="$(psql_demo -F, -c "WITH active AS (SELECT s.*, p.target_min_c, p.target_max_c FROM shipments s JOIN storage_profiles p ON p.id=s.storage_profile_id WHERE s.archived_at IS NULL AND s.deleted_at IS NULL), evaluated AS (SELECT s.id, CASE WHEN EXISTS (SELECT 1 FROM excursion_incidents i JOIN investigations v ON v.excursion_id=i.id WHERE i.shipment_id=s.id AND i.status IN ('OPEN','UNDER_INVESTIGATION') AND v.status<>'COMPLETED') THEN 'UNDER_REVIEW' WHEN EXISTS (SELECT 1 FROM excursion_incidents i WHERE i.shipment_id=s.id AND i.status IN ('OPEN','UNDER_INVESTIGATION')) THEN 'TEMPERATURE_ISSUE' WHEN e.id IS NULL THEN CASE WHEN EXISTS (SELECT 1 FROM sensor_events x WHERE x.shipment_id=s.id) THEN 'AT_RISK' ELSE 'UNKNOWN' END WHEN e.event_time + ((s.expected_cadence_minutes+1)||' minutes')::interval < now() OR EXISTS (SELECT 1 FROM missing_intervals m WHERE m.shipment_id=s.id AND upper(m.status)='UNKNOWN' AND m.gap_end_time>e.event_time) THEN 'AT_RISK' WHEN e.temperature_c<s.target_min_c OR e.temperature_c>s.target_max_c THEN 'AT_RISK' ELSE 'NORMAL' END condition FROM active s LEFT JOIN LATERAL (SELECT x.id,x.event_time,x.temperature_c FROM sensor_events x WHERE x.shipment_id=s.id AND NOT x.is_duplicate AND x.quality_flag<>'QUARANTINED' AND x.temperature_c IS NOT NULL ORDER BY x.event_time DESC LIMIT 1) e ON true) SELECT (SELECT count(*) FROM active)||','||(SELECT count(*) FROM evaluated WHERE condition IN ('TEMPERATURE_ISSUE','AT_RISK'))||(SELECT ','||count(*) FROM excursion_incidents WHERE status IN ('OPEN','UNDER_INVESTIGATION'))||(SELECT ','||count(*) FROM investigations WHERE status<>'COMPLETED')")"
api_summary="$(api_get /api/dashboard/summary | jq -r '[.activeShipments,.needAttention,.activeIssues,.openInvestigations]|join(",")')"
expect dashboard_summary_api_equals_sql "$api_summary" "$sql_summary"
echo "DASHBOARD_SUMMARY=$api_summary"

ids=(SHP-10001 SHP-10002 SHP-10003 SHP-10004 SHP-10005 SHP-10006 SHP-10007 SHP-10008 SHP-10009 SHP-10010)
conditions=(NORMAL UNDER_REVIEW AT_RISK AT_RISK NORMAL NORMAL AT_RISK NORMAL UNDER_REVIEW UNKNOWN)
for i in "${!ids[@]}"; do
  id="${ids[$i]}"
  actual="$(api_get "/api/shipments/$id" | jq -r .condition)"
  expect "condition_$id" "$actual" "${conditions[$i]}"
done
echo 'Condition explanation: OPEN investigations take precedence over the minor/critical issue label; resolved HOLD/RELEASE/REJECT cases return AT_RISK when the latest temperature remains outside range or telemetry is overdue. The second active sensor on SHP-10008 supplies a recent in-range reading.'
echo 'PASS demo seed verification'

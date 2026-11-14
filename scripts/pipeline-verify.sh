#!/usr/bin/env bash
set -euo pipefail
db_host="${DB_HOST:-localhost}" db_port="${DB_PORT:-5432}" db_name="${DB_NAME:-cold_chain}"
[[ "$db_name" =~ ^[a-zA-Z0-9_]+$ ]] || { echo "Invalid DB_NAME" >&2; exit 2; }
verify_user="${COLDCHAIN_PROVISIONER_USER:-${COLDCHAIN_MIGRATOR_USER:-${PGUSER:-${USER}}}}"
export PGPASSWORD="${COLDCHAIN_PROVISIONER_PASSWORD:-${COLDCHAIN_MIGRATOR_PASSWORD:-${PGPASSWORD:-}}}"
rows="$(psql -X -h "$db_host" -p "$db_port" -U "$verify_user" -d "$db_name" -v ON_ERROR_STOP=1 -At -F '|' <<'SQL'
WITH counts(dataset, view_count, base_count) AS (
  SELECT 'dim_shipment', (SELECT count(*) FROM analytics.v_shipments), (SELECT count(*) FROM public.shipments WHERE deleted_at IS NULL)
  UNION ALL SELECT 'dim_lot', (SELECT count(*) FROM analytics.v_lots),
    (SELECT count(*) FROM public.lots l JOIN public.shipments s ON s.id=l.shipment_id WHERE l.deleted_at IS NULL AND s.deleted_at IS NULL)
  UNION ALL SELECT 'fact_sensor_event', (SELECT count(*) FROM analytics.v_sensor_events),
    (SELECT count(*) FROM public.raw_sensor_events r JOIN public.shipments s ON s.id=r.shipment_id AND s.deleted_at IS NULL LEFT JOIN public.sensor_events e ON e.event_id=r.event_id LEFT JOIN public.lots l ON l.id=e.lot_id WHERE e.id IS NULL OR l.id IS NULL OR l.deleted_at IS NULL)
  UNION ALL SELECT 'fact_excursion', (SELECT count(*) FROM analytics.v_excursions),
    (SELECT count(*) FROM public.excursion_incidents i JOIN public.shipments s ON s.id=i.shipment_id WHERE s.deleted_at IS NULL)
  UNION ALL SELECT 'fact_investigation', (SELECT count(*) FROM analytics.v_investigations),
    (SELECT count(*) FROM public.investigations i JOIN public.excursion_incidents x ON x.id=i.excursion_id JOIN public.shipments s ON s.id=x.shipment_id WHERE s.deleted_at IS NULL)
  UNION ALL SELECT 'fact_decision', (SELECT count(*) FROM analytics.v_decisions),
    (SELECT count(*) FROM public.disposition_decisions d JOIN public.investigations i ON i.id=d.investigation_id JOIN public.excursion_incidents x ON x.id=d.excursion_id JOIN public.shipments s ON s.id=x.shipment_id WHERE s.deleted_at IS NULL)
  UNION ALL SELECT 'helper_missing_intervals', (SELECT count(*) FROM analytics.v_missing_intervals),
    (SELECT count(*) FROM public.missing_intervals m JOIN public.shipments s ON s.id=m.shipment_id WHERE s.deleted_at IS NULL)
)
SELECT dataset, view_count, base_count FROM counts ORDER BY dataset;
SQL
)"
printf 'dataset|view_count|base_count|difference|status\n'
failed=0
while IFS='|' read -r dataset view_count base_count; do
  [[ -n "$dataset" ]] || continue
  difference=$((view_count - base_count))
  status=PASS
  if (( difference != 0 )); then status=FAIL; failed=1; fi
  printf '%s|%s|%s|%s|%s\n' "$dataset" "$view_count" "$base_count" "$difference" "$status"
done <<< "$rows"
identity="$(psql -X -h "$db_host" -p "$db_port" -U "$verify_user" -d "$db_name" -v ON_ERROR_STOP=1 -Atc "SELECT count(*),count(*) FILTER (WHERE validation_status='CLEAN'),count(*) FILTER (WHERE validation_status='DUPLICATE'),count(*) FILTER (WHERE validation_status='QUARANTINED') FROM analytics.v_sensor_events")"
IFS='|' read -r raw clean duplicate quarantined <<< "$identity"
if (( raw != clean + duplicate + quarantined )); then echo "event_identity|$raw|$((clean+duplicate+quarantined))|$((raw-clean-duplicate-quarantined))|FAIL"; failed=1; else echo "event_identity|$raw|$((clean+duplicate+quarantined))|0|PASS"; fi
(( failed == 0 ))

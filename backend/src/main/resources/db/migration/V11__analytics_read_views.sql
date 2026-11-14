-- Read-only analytical boundary. Views are owned by the Flyway migrator.
CREATE SCHEMA IF NOT EXISTS analytics AUTHORIZATION coldchain_migrator;
ALTER SCHEMA analytics OWNER TO coldchain_migrator;
REVOKE ALL ON SCHEMA analytics FROM PUBLIC;

CREATE OR REPLACE VIEW analytics.v_shipments AS
SELECT s.id AS shipment_id, s.tracking_number, s.shipment_reference,
       s.origin, s.destination, s.status, p.id AS storage_profile_id,
       p.name AS storage_profile, p.target_min_c, p.target_max_c,
       s.expected_cadence_minutes, s.allowed_lateness_minutes,
       s.created_at, s.updated_at, s.expected_delivery_at, s.archived_at
FROM public.shipments s
JOIN public.storage_profiles p ON p.id = s.storage_profile_id
WHERE s.deleted_at IS NULL;

CREATE OR REPLACE VIEW analytics.v_lots AS
SELECT l.id AS lot_id, l.shipment_id, l.product_name, l.batch_number,
       l.quantity, l.risk_level, l.disposition_status, l.created_at,
       l.expiry_date, l.archived_at, l.unloaded_at
FROM public.lots l
JOIN public.shipments s ON s.id = l.shipment_id AND s.deleted_at IS NULL
WHERE l.deleted_at IS NULL;

-- One row per raw ledger record. A duplicate raw event can point at the same
-- normalized event as its original; raw processing_status retains that fact.
CREATE OR REPLACE VIEW analytics.v_sensor_events AS
SELECT r.id AS raw_record_id, r.event_id, r.sensor_id,
       r.shipment_id, e.lot_id, r.sequence_no, r.event_time,
       r.ingestion_time AS ingest_time, e.temperature_c, e.source,
       e.status AS condition, e.quality_flag AS data_quality,
       CASE r.processing_status
         WHEN 'VALIDATED' THEN 'CLEAN'
         WHEN 'DUPLICATE' THEN 'DUPLICATE'
         WHEN 'QUARANTINED' THEN 'QUARANTINED'
         ELSE 'QUARANTINED'
       END AS validation_status,
       e.duplicate_of_event_id,
       NULL::text AS quarantine_reason
FROM public.raw_sensor_events r
JOIN public.shipments s ON s.id = r.shipment_id AND s.deleted_at IS NULL
LEFT JOIN public.sensor_events e ON e.event_id = r.event_id
LEFT JOIN public.lots l ON l.id = e.lot_id
WHERE e.id IS NULL OR l.id IS NULL OR l.deleted_at IS NULL;

CREATE OR REPLACE VIEW analytics.v_excursions AS
SELECT i.id AS excursion_id, i.shipment_id, i.severity, i.peak_temperature_c,
       i.start_event_time, i.end_event_time, i.duration_minutes,
       (SELECT count(*)::integer FROM public.sensor_events e
        WHERE e.excursion_incident_id = i.id AND NOT e.is_duplicate
          AND e.quality_flag <> 'QUARANTINED') AS reading_count,
       i.rule_version, i.status, i.created_at AS flagged_at,
       COALESCE((SELECT array_agg(al.lot_id ORDER BY al.lot_id)
                 FROM public.excursion_affected_lots al
                 JOIN public.lots l ON l.id = al.lot_id AND l.deleted_at IS NULL
                 WHERE al.excursion_id = i.id), ARRAY[]::varchar[]) AS affected_lot_ids
FROM public.excursion_incidents i
JOIN public.shipments s ON s.id = i.shipment_id AND s.deleted_at IS NULL;

CREATE OR REPLACE VIEW analytics.v_investigations AS
SELECT inv.id AS investigation_id, inv.excursion_id, ex.shipment_id,
       inv.status, inv.opened_at, inv.sla_due_at, inv.closed_at,
       (count(mi.id) > 0) AS has_evidence,
       COALESCE(bool_or(mi.contradicts_sensor), false) AS contradictory_evidence
FROM public.investigations inv
JOIN analytics.v_excursions ex ON ex.excursion_id = inv.excursion_id
LEFT JOIN public.manual_inspections mi ON mi.investigation_id = inv.id
GROUP BY inv.id, inv.excursion_id, ex.shipment_id, inv.status,
         inv.opened_at, inv.sla_due_at, inv.closed_at;

CREATE OR REPLACE VIEW analytics.v_decisions AS
SELECT d.id AS decision_record_id, d.investigation_id, d.excursion_id,
       ex.shipment_id, d.decision AS decision_value, d.decided_at,
       d.rule_version, 'ANALYST'::text AS actor_role,
       ex.affected_lot_ids
FROM public.disposition_decisions d
JOIN analytics.v_excursions ex ON ex.excursion_id = d.excursion_id
JOIN analytics.v_investigations inv ON inv.investigation_id = d.investigation_id;

CREATE OR REPLACE VIEW analytics.v_missing_intervals AS
SELECT m.id AS missing_interval_id, m.shipment_id, m.sensor_id,
       m.gap_start_time, m.gap_end_time, m.expected_cadence_minutes,
       m.status, m.detected_at
FROM public.missing_intervals m
JOIN public.shipments s ON s.id = m.shipment_id AND s.deleted_at IS NULL;

DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'coldchain_analytics') THEN
    GRANT USAGE ON SCHEMA analytics TO coldchain_analytics;
    GRANT SELECT ON analytics.v_shipments, analytics.v_lots,
      analytics.v_sensor_events, analytics.v_excursions,
      analytics.v_investigations, analytics.v_decisions,
      analytics.v_missing_intervals TO coldchain_analytics;
  END IF;
END $$;

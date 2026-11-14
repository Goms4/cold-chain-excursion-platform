# Analytics data contract

The export is a PostgreSQL 17 `REPEATABLE READ`, `READ ONLY` snapshot over
owner-executed views in schema `analytics`. All views are owned by
`coldchain_migrator` and do not use `security_invoker`; `coldchain_analytics`
has no base-table privileges. NULL becomes an empty CSV field. Timestamps are
UTC ISO-8601 with `Z`, temperatures have two decimals, booleans are lowercase.
Snowflake targets below are logical types; the loader is outside this task.

| Dataset / column | CSV / source type | Meaning and source rule | Snowflake type |
|---|---|---|---|
| dim_shipment.shipment_id | VARCHAR(64) | Shipment primary key; excludes `deleted_at` rows. | VARCHAR(64) |
| dim_shipment.tracking_number | VARCHAR(128) | Carrier tracking reference. | VARCHAR(128) |
| dim_shipment.shipment_reference | VARCHAR(128) | Business reference. | VARCHAR(128) |
| dim_shipment.origin, destination | VARCHAR(128) | Route endpoints. | VARCHAR(128) each |
| dim_shipment.status | VARCHAR(32) | Shipment lifecycle status. | VARCHAR(32) |
| dim_shipment.storage_profile_id | VARCHAR(64) | Joined profile key. | VARCHAR(64) |
| dim_shipment.storage_profile | VARCHAR(64) | Profile name joined from storage_profiles. | VARCHAR(64) |
| dim_shipment.target_min_c, target_max_c | NUMERIC(5,2) | Shipment's own profile bounds. | NUMBER(5,2) each |
| dim_shipment.expected_cadence_minutes | INTEGER | Per-shipment sampling cadence. | NUMBER(10,0) |
| dim_shipment.allowed_lateness_minutes | INTEGER | Per-shipment accepted lateness threshold. | NUMBER(10,0) |
| dim_shipment.created_at, updated_at | TIMESTAMPTZ | Source lifecycle timestamps. | TIMESTAMP_TZ |
| dim_shipment.expected_delivery_at, archived_at | nullable TIMESTAMPTZ | Expected delivery and archive time. | TIMESTAMP_TZ nullable |
| dim_lot.lot_id | VARCHAR(64) | Lot primary key; excludes deleted lot/shipment. | VARCHAR(64) |
| dim_lot.shipment_id | VARCHAR(64) | Parent shipment key. | VARCHAR(64) |
| dim_lot.product_name | VARCHAR(128) | Product display name (not free-text notes). | VARCHAR(128) |
| dim_lot.batch_number | VARCHAR(64) | Product batch identifier. | VARCHAR(64) |
| dim_lot.quantity | INTEGER | Registered quantity. | NUMBER(10,0) |
| dim_lot.risk_level, disposition_status | VARCHAR(32) | Current lot risk and disposition codes. | VARCHAR(32) each |
| dim_lot.created_at, expiry_date, archived_at, unloaded_at | TIMESTAMPTZ/DATE | Lot lifecycle dates; optional values empty in CSV. | TIMESTAMP_TZ / DATE |
| fact_sensor_event.raw_record_id | BIGINT | Raw ledger row key; preserves repeated raw IDs. | NUMBER(19,0) |
| fact_sensor_event.event_id | VARCHAR(64) | Source event identifier; duplicate raw rows can repeat it. | VARCHAR(64) |
| fact_sensor_event.sensor_id, shipment_id, lot_id | VARCHAR(64) | Sensor, shipment, and linked active lot keys. | VARCHAR(64) |
| fact_sensor_event.sequence_no | BIGINT | Optional device sequence. | NUMBER(19,0) |
| fact_sensor_event.event_time | TIMESTAMPTZ | Measurement time; authoritative chronology. | TIMESTAMP_TZ |
| fact_sensor_event.ingest_time | TIMESTAMPTZ | Backend ingestion time; retained separately. | TIMESTAMP_TZ |
| fact_sensor_event.temperature_c | NUMERIC(5,2) | Normalized measurement; NULL where no normalized event exists. | NUMBER(5,2) |
| fact_sensor_event.source | VARCHAR(32) | SENSOR or MANUAL from normalized event. | VARCHAR(32) |
| fact_sensor_event.condition | VARCHAR(32) | Stored event status; separate from data quality. | VARCHAR(32) |
| fact_sensor_event.data_quality | VARCHAR(32) | Stored quality flag. | VARCHAR(32) |
| fact_sensor_event.validation_status | VARCHAR(16) | Raw processing status mapped to CLEAN, DUPLICATE, QUARANTINED. | VARCHAR(16) |
| fact_sensor_event.duplicate_of_event_id | VARCHAR(64) | Normalized duplicate reference, if stored. | VARCHAR(64) |
| fact_sensor_event.quarantine_reason | VARCHAR(1) | Always NULL: the actual schema stores no reason column; raw payload is intentionally excluded. | VARCHAR(1) |
| fact_sensor_event.previous_event_time | TIMESTAMPTZ | Previous CLEAN event time per shipment and sensor. | TIMESTAMP_TZ |
| fact_sensor_event.gap_minutes | FLOAT | Difference from previous CLEAN event. | FLOAT |
| fact_sensor_event.missing_interval | BOOLEAN | Gap exceeds that shipment's own cadence; unknown, not safe. | BOOLEAN |
| fact_sensor_event.lateness_minutes | FLOAT | `ingest_time - event_time`; negative values retained. | FLOAT |
| fact_sensor_event.is_late | BOOLEAN | Lateness exceeds that shipment's own allowed threshold. | BOOLEAN |
| fact_excursion.excursion_id, shipment_id | VARCHAR(64) | Incident key and parent shipment. | VARCHAR(64) |
| fact_excursion.severity, status | VARCHAR(32) | Persisted incident codes. | VARCHAR(32) each |
| fact_excursion.peak_temperature_c | NUMERIC | Persisted peak. | NUMBER(10,2) |
| fact_excursion.start_event_time, end_event_time, flagged_at | TIMESTAMPTZ | Event-time bounds and incident `created_at`. | TIMESTAMP_TZ |
| fact_excursion.duration_minutes, reading_count | INTEGER | Persisted duration; count of linked nonduplicate, nonquarantined sensor rows. | NUMBER(10,0) |
| fact_excursion.rule_version | VARCHAR(32) | Applied rule version. | VARCHAR(32) |
| fact_excursion.affected_lot_ids | VARCHAR array | Ordered active lot IDs from the incident-lot link table. | ARRAY |
| fact_investigation.investigation_id, excursion_id, shipment_id | VARCHAR(64) | Investigation and parent keys. | VARCHAR(64) |
| fact_investigation.status | VARCHAR(32) | Persisted investigation status; assignee omitted as personal data. | VARCHAR(32) |
| fact_investigation.opened_at, sla_due_at, closed_at | TIMESTAMPTZ | Investigation lifecycle and stored deadline. | TIMESTAMP_TZ |
| fact_investigation.has_evidence | BOOLEAN | At least one manual inspection row exists. | BOOLEAN |
| fact_investigation.contradictory_evidence | BOOLEAN | Any linked inspection contradicts sensor evidence. | BOOLEAN |
| fact_decision.decision_record_id | BIGINT | Append-only decision row key. | NUMBER(19,0) |
| fact_decision.investigation_id, excursion_id, shipment_id | VARCHAR(64) | Parent keys. | VARCHAR(64) |
| fact_decision.decision_value | VARCHAR(32) | RELEASE, HOLD, or REJECT. | VARCHAR(32) |
| fact_decision.decided_at | TIMESTAMPTZ | Persisted decision timestamp. | TIMESTAMP_TZ |
| fact_decision.rule_version | VARCHAR(32) | Applied rule version. | VARCHAR(32) |
| fact_decision.actor_role | VARCHAR(16) | `ANALYST`, inferred from the existing decision endpoint RBAC; actor identity is not stored in this table. | VARCHAR(16) |
| fact_decision.affected_lot_ids | VARCHAR array | Active incident lots; source list is not exported from free-text decision field. | ARRAY |
| fact_decision.investigation_status | VARCHAR(32) | Parent investigation's current stored status. | VARCHAR(32) |
| fact_decision.investigation_opened_at | TIMESTAMPTZ | Parent investigation opened timestamp. | TIMESTAMP_TZ |
| fact_decision.contradictory_evidence | BOOLEAN | Whether any linked inspection contradicts sensor evidence. | BOOLEAN |

`fact_sensor_event.condition` is the stored event status, not the Java-derived
current shipment condition. `telemetry_quality_report.csv` is one row per active shipment plus `TOTAL`.
`raw`, `clean`, `duplicate`, and `quarantined` are counted from the raw-row
view and partition it exactly. `late_accepted` counts CLEAN events with stored
quality `LATE`. `expected_readings` sums expected cadence slots separately for
each shipment sensor over its first-to-last clean event span, extended to
`expected_delivery_at` when that value is later; when delivery is absent, the
latest clean event is the end. Multi-sensor shipment spans are summed. No wall
clock is used.
An interval longer than cadence contributes `gap - cadence` unknown minutes;
the same applies to a trailing delivery gap. Missing intervals are never
counted as in-range. In-range/out-of-range counts only CLEAN readings whose
stored condition is NORMAL/EXCURSION. Shipment condition is omitted because
the application derives it in Java and parity has not been established.

The source schema has no stored quarantine reason, so this field is empty. It
has no stored decision actor role; `ANALYST` is inferred from the secured
decision endpoint. Neither raw payload nor free-text notes/reasons, assignees,
inspector names, request bodies, audit rows, sessions, role credentials, IP
addresses, or hashes are exported. The migration defines an additional
`analytics.v_missing_intervals` helper view, granted read-only for future use.

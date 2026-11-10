-- V1__create_schema.sql
-- Cold-Chain Excursion Platform Schema

-- 1. Storage Profiles
CREATE TABLE storage_profiles (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    target_min_c NUMERIC(5,2) NOT NULL,
    target_max_c NUMERIC(5,2) NOT NULL,
    minor_min_c NUMERIC(5,2),
    minor_max_c NUMERIC(5,2),
    major_min_c NUMERIC(5,2),
    major_max_c NUMERIC(5,2),
    critical_min_c NUMERIC(5,2),
    critical_max_c NUMERIC(5,2),
    max_minor_duration_minutes INT DEFAULT 240,
    rule_version VARCHAR(32) NOT NULL DEFAULT 'v1.0.0',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. Shipments
CREATE TABLE shipments (
    id VARCHAR(64) PRIMARY KEY,
    tracking_number VARCHAR(128) NOT NULL UNIQUE,
    origin VARCHAR(128) NOT NULL,
    destination VARCHAR(128) NOT NULL,
    storage_profile_id VARCHAR(64) NOT NULL REFERENCES storage_profiles(id),
    status VARCHAR(32) NOT NULL DEFAULT 'PLANNED',
    expected_cadence_minutes INT NOT NULL DEFAULT 5,
    allowed_lateness_minutes INT NOT NULL DEFAULT 120,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. Lots
CREATE TABLE lots (
    id VARCHAR(64) PRIMARY KEY,
    shipment_id VARCHAR(64) NOT NULL REFERENCES shipments(id),
    product_name VARCHAR(128) NOT NULL,
    batch_number VARCHAR(64) NOT NULL,
    quantity INT NOT NULL,
    risk_level VARCHAR(32) NOT NULL DEFAULT 'LOW',
    disposition_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. Sensors
CREATE TABLE sensors (
    id VARCHAR(64) PRIMARY KEY,
    shipment_id VARCHAR(64) REFERENCES shipments(id),
    model VARCHAR(64) NOT NULL,
    battery_level INT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    registered_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 5. Raw Sensor Events (Append-Only Payload Retention)
CREATE TABLE raw_sensor_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    sensor_id VARCHAR(64) NOT NULL,
    shipment_id VARCHAR(64) NOT NULL,
    sequence_no BIGINT,
    event_time TIMESTAMPTZ NOT NULL,
    ingestion_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    raw_payload TEXT NOT NULL,
    processing_status VARCHAR(32) NOT NULL, -- RECEIVED, VALIDATED, DUPLICATE, QUARANTINED, REJECTED
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_raw_events_event_id ON raw_sensor_events(event_id);
CREATE INDEX idx_raw_events_shipment_time ON raw_sensor_events(shipment_id, event_time);

-- 6. Processed / Normalized Sensor Events
CREATE TABLE sensor_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    sensor_id VARCHAR(64) NOT NULL REFERENCES sensors(id),
    shipment_id VARCHAR(64) NOT NULL REFERENCES shipments(id),
    sequence_no BIGINT,
    event_time TIMESTAMPTZ NOT NULL,
    ingestion_time TIMESTAMPTZ NOT NULL,
    temperature_c NUMERIC(5,2),
    status VARCHAR(32) NOT NULL,           -- NORMAL, EXCURSION, UNKNOWN
    quality_flag VARCHAR(32) NOT NULL,     -- VALID, LATE, OUT_OF_ORDER, QUARANTINED
    is_duplicate BOOLEAN NOT NULL DEFAULT FALSE,
    duplicate_of_event_id VARCHAR(64),
    excursion_incident_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_events_shipment_event_time ON sensor_events(shipment_id, event_time);
CREATE INDEX idx_events_event_id ON sensor_events(event_id);

-- 7. Missing Intervals (UNKNOWN, Never SAFE)
CREATE TABLE missing_intervals (
    id BIGSERIAL PRIMARY KEY,
    shipment_id VARCHAR(64) NOT NULL REFERENCES shipments(id),
    sensor_id VARCHAR(64) NOT NULL REFERENCES sensors(id),
    gap_start_time TIMESTAMPTZ NOT NULL,
    gap_end_time TIMESTAMPTZ NOT NULL,
    expected_cadence_minutes INT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',
    detected_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 8. Excursion Incidents
CREATE TABLE excursion_incidents (
    id VARCHAR(64) PRIMARY KEY,
    shipment_id VARCHAR(64) NOT NULL REFERENCES shipments(id),
    rule_version VARCHAR(32) NOT NULL,
    severity VARCHAR(32) NOT NULL,         -- MINOR, MAJOR, CRITICAL
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN', -- OPEN, UNDER_INVESTIGATION, DISPOSITIONED, CLOSED
    start_event_time TIMESTAMPTZ NOT NULL,
    end_event_time TIMESTAMPTZ,
    peak_temperature_c NUMERIC(5,2) NOT NULL,
    duration_minutes INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 9. Excursion Affected Lots
CREATE TABLE excursion_affected_lots (
    id BIGSERIAL PRIMARY KEY,
    excursion_id VARCHAR(64) NOT NULL REFERENCES excursion_incidents(id),
    lot_id VARCHAR(64) NOT NULL REFERENCES lots(id),
    initial_risk_level VARCHAR(32) NOT NULL,
    disposition_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 10. Investigations
CREATE TABLE investigations (
    id VARCHAR(64) PRIMARY KEY,
    excursion_id VARCHAR(64) NOT NULL UNIQUE REFERENCES excursion_incidents(id),
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN', -- OPEN, UNDER_REVIEW, CONTRADICTORY_EVIDENCE, COMPLETED
    assigned_investigator VARCHAR(128),
    sla_due_at TIMESTAMPTZ NOT NULL,
    opened_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    closed_at TIMESTAMPTZ,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 11. Manual Inspection Records (Preserves conflicting evidence independently)
CREATE TABLE manual_inspections (
    id BIGSERIAL PRIMARY KEY,
    investigation_id VARCHAR(64) NOT NULL REFERENCES investigations(id),
    shipment_id VARCHAR(64) NOT NULL REFERENCES shipments(id),
    inspector_name VARCHAR(128) NOT NULL,
    inspection_time TIMESTAMPTZ NOT NULL,
    observed_temperature_c NUMERIC(5,2) NOT NULL,
    package_integrity_status VARCHAR(64) NOT NULL,
    visual_condition TEXT,
    notes TEXT,
    contradicts_sensor BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 12. Disposition Decisions (Append-Only Decision Log)
CREATE TABLE disposition_decisions (
    id BIGSERIAL PRIMARY KEY,
    investigation_id VARCHAR(64) NOT NULL REFERENCES investigations(id),
    excursion_id VARCHAR(64) NOT NULL REFERENCES excursion_incidents(id),
    decision VARCHAR(32) NOT NULL,         -- RELEASE, HOLD, REJECT
    investigator_name VARCHAR(128) NOT NULL,
    reason TEXT NOT NULL,
    rule_version VARCHAR(32) NOT NULL,
    affected_lot_ids TEXT NOT NULL,        -- Comma-separated or JSON list of lot IDs
    decided_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 13. Audit History (Append-Only Audit Ledger)
CREATE TABLE audit_history (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(64) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    action VARCHAR(32) NOT NULL,
    actor VARCHAR(128) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    payload_before TEXT,
    payload_after TEXT,
    rationale TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_history_entity ON audit_history(entity_type, entity_id);

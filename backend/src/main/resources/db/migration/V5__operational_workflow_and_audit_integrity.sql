ALTER TABLE shipments ADD COLUMN shipment_reference VARCHAR(128);
UPDATE shipments SET shipment_reference = tracking_number WHERE shipment_reference IS NULL;
ALTER TABLE shipments ALTER COLUMN shipment_reference SET NOT NULL;
ALTER TABLE shipments ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE shipments ADD COLUMN deleted_by VARCHAR(128);
ALTER TABLE shipments ADD COLUMN delete_reason TEXT;

ALTER TABLE lots ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE lots ADD COLUMN deleted_by VARCHAR(128);
ALTER TABLE lots ADD COLUMN delete_reason TEXT;

ALTER TABLE sensor_events ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT 'SENSOR';

ALTER TABLE audit_history ADD COLUMN category VARCHAR(16) NOT NULL DEFAULT 'BUSINESS';
ALTER TABLE audit_history ADD CONSTRAINT ck_audit_history_category
    CHECK (category IN ('BUSINESS', 'TECHNICAL'));

-- Lifecycle state is represented by shipment status and incident state separately.
UPDATE shipments SET status = 'IN_TRANSIT' WHERE status = 'EXCURSION';

CREATE OR REPLACE FUNCTION prevent_audit_history_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit_history is append-only; % is not permitted', TG_OP;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_history_append_only
BEFORE UPDATE OR DELETE ON audit_history
FOR EACH ROW EXECUTE FUNCTION prevent_audit_history_mutation();

-- Hash chaining is not implemented yet. No row_hash or prev_hash is asserted by this schema.

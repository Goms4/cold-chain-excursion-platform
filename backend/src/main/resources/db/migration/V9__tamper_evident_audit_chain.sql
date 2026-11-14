CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;
ALTER TABLE audit_history ADD COLUMN seq BIGINT;
ALTER TABLE audit_history ADD COLUMN prev_hash CHAR(64);
ALTER TABLE audit_history ADD COLUMN row_hash CHAR(64);

-- One-time ordered backfill. The app role never receives UPDATE or trigger privileges.
DROP TRIGGER IF EXISTS audit_history_append_only ON audit_history;
DO $$
DECLARE r RECORD; n BIGINT := 0; prior CHAR(64) := repeat('0',64)::char(64); canonical TEXT;
BEGIN
  FOR r IN SELECT * FROM audit_history ORDER BY timestamp, id LOOP
    n := n + 1;
    canonical := jsonb_build_object(
      'seq', n,
      'timestamp', to_char(r.timestamp AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"'),
      'actor', r.actor, 'action', r.action, 'entity_type', r.entity_type,
      'entity_id', r.entity_id, 'reason', r.rationale, 'category', r.category,
      'payload_before', r.payload_before, 'payload_after', r.payload_after
    )::text;
    UPDATE audit_history SET seq=n, prev_hash=prior,
      row_hash=encode(public.digest(convert_to(prior::text || canonical, 'UTF8'), 'sha256'), 'hex')
      WHERE id=r.id;
    prior := (SELECT row_hash FROM audit_history WHERE id=r.id);
  END LOOP;
END $$;
ALTER TABLE audit_history ALTER COLUMN seq SET NOT NULL;
ALTER TABLE audit_history ALTER COLUMN prev_hash SET NOT NULL;
ALTER TABLE audit_history ALTER COLUMN row_hash SET NOT NULL;
ALTER TABLE audit_history ADD CONSTRAINT uq_audit_history_seq UNIQUE(seq);
CREATE TRIGGER audit_history_append_only
BEFORE UPDATE OR DELETE OR TRUNCATE ON audit_history
FOR EACH STATEMENT EXECUTE FUNCTION prevent_audit_history_mutation();

CREATE OR REPLACE FUNCTION audit_canonical_payload(
  p_seq BIGINT, p_timestamp TIMESTAMPTZ, p_actor TEXT, p_action TEXT,
  p_entity_type TEXT, p_entity_id TEXT, p_reason TEXT, p_category TEXT,
  p_payload_before TEXT, p_payload_after TEXT
) RETURNS TEXT LANGUAGE SQL IMMUTABLE AS $$
  SELECT jsonb_build_object(
    'seq', p_seq,
    'timestamp', to_char(p_timestamp AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"'),
    'actor', p_actor, 'action', p_action, 'entity_type', p_entity_type,
    'entity_id', p_entity_id, 'reason', p_reason, 'category', p_category,
    'payload_before', p_payload_before, 'payload_after', p_payload_after
  )::text
$$;

CREATE OR REPLACE FUNCTION set_audit_chain_hash()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE prior_row RECORD; canonical TEXT;
BEGIN
  PERFORM pg_advisory_xact_lock(731942, 22017);
  SELECT seq, row_hash INTO prior_row FROM audit_history ORDER BY seq DESC LIMIT 1;
  NEW.seq := COALESCE(prior_row.seq, 0) + 1;
  NEW.prev_hash := COALESCE(prior_row.row_hash, repeat('0',64)::char(64));
  canonical := audit_canonical_payload(NEW.seq, NEW.timestamp, NEW.actor, NEW.action,
      NEW.entity_type, NEW.entity_id, NEW.rationale, NEW.category,
      NEW.payload_before, NEW.payload_after);
  NEW.row_hash := encode(public.digest(convert_to(NEW.prev_hash::text || canonical, 'UTF8'), 'sha256'), 'hex');
  RETURN NEW;
END $$;
CREATE TRIGGER audit_hash_before_insert BEFORE INSERT ON audit_history
FOR EACH ROW EXECUTE FUNCTION set_audit_chain_hash();

CREATE OR REPLACE FUNCTION verify_audit_chain()
RETURNS TABLE(ok BOOLEAN, first_broken_seq BIGINT, rows_checked BIGINT)
LANGUAGE plpgsql STABLE AS $$
DECLARE r RECORD; expected_seq BIGINT := 1; prior CHAR(64) := repeat('0',64)::char(64);
        canonical TEXT; calculated CHAR(64);
BEGIN
  rows_checked := 0; first_broken_seq := NULL; ok := TRUE;
  FOR r IN SELECT * FROM audit_history ORDER BY seq LOOP
    rows_checked := rows_checked + 1;
    canonical := audit_canonical_payload(r.seq, r.timestamp, r.actor, r.action,
      r.entity_type, r.entity_id, r.rationale, r.category, r.payload_before, r.payload_after);
    calculated := encode(public.digest(convert_to(r.prev_hash::text || canonical, 'UTF8'), 'sha256'), 'hex');
    IF r.seq <> expected_seq OR r.prev_hash <> prior OR r.row_hash <> calculated THEN
      ok := FALSE; first_broken_seq := r.seq; RETURN NEXT; RETURN;
    END IF;
    prior := r.row_hash; expected_seq := expected_seq + 1;
  END LOOP;
  RETURN NEXT;
END $$;

COMMENT ON COLUMN audit_history.seq IS 'Strictly increasing logical order; serialized by transaction advisory lock.';
COMMENT ON COLUMN audit_history.prev_hash IS 'Previous row SHA-256; genesis is 64 zero characters.';
COMMENT ON COLUMN audit_history.row_hash IS 'SHA-256 of prev_hash UTF-8 bytes concatenated with audit_canonical_payload().';

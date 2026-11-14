DROP TRIGGER IF EXISTS audit_history_append_only ON audit_history;
CREATE TRIGGER audit_history_append_only
BEFORE UPDATE OR DELETE OR TRUNCATE ON audit_history
FOR EACH STATEMENT EXECUTE FUNCTION prevent_audit_history_mutation();

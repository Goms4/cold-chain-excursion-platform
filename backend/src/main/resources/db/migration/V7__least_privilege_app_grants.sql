REVOKE CREATE ON SCHEMA public FROM PUBLIC, coldchain_app;
GRANT USAGE ON SCHEMA public TO coldchain_app;

GRANT SELECT, INSERT, UPDATE ON shipments, lots, sensors, excursion_incidents,
    excursion_affected_lots, investigations, manual_inspections, missing_intervals
    TO coldchain_app;
GRANT SELECT, INSERT ON sensor_events, raw_sensor_events, disposition_decisions,
    audit_history TO coldchain_app;
GRANT SELECT ON storage_profiles TO coldchain_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO coldchain_app;
REVOKE UPDATE, DELETE, TRUNCATE, TRIGGER ON audit_history, disposition_decisions,
    raw_sensor_events, sensor_events, manual_inspections FROM coldchain_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE ON TABLES TO coldchain_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO coldchain_app;

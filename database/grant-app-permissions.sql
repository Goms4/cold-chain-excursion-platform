\set ON_ERROR_STOP on
REVOKE CREATE ON SCHEMA public FROM PUBLIC, coldchain_app;
GRANT USAGE ON SCHEMA public TO coldchain_app;
GRANT SELECT, INSERT, UPDATE ON shipments, lots, sensors, excursion_incidents,
    excursion_affected_lots, investigations, manual_inspections, missing_intervals
    TO coldchain_app;
GRANT SELECT, INSERT ON sensor_events, raw_sensor_events, disposition_decisions,
    audit_history TO coldchain_app;
GRANT SELECT ON storage_profiles, flyway_schema_history TO coldchain_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO coldchain_app;
REVOKE UPDATE, DELETE, TRUNCATE, TRIGGER ON audit_history, disposition_decisions,
    raw_sensor_events, sensor_events, manual_inspections FROM coldchain_app;
ALTER DEFAULT PRIVILEGES FOR ROLE coldchain_migrator IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE ON TABLES TO coldchain_app;
ALTER DEFAULT PRIVILEGES FOR ROLE coldchain_migrator IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO coldchain_app;
GRANT CONNECT ON DATABASE :"db_name" TO coldchain_migrator, coldchain_app;

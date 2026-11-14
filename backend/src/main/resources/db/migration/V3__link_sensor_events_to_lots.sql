ALTER TABLE sensor_events
    ADD COLUMN lot_id VARCHAR(64) REFERENCES lots(id);

CREATE INDEX idx_events_lot_event_time ON sensor_events(lot_id, event_time);

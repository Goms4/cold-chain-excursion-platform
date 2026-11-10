# Simulated Sensor Data Contract

No external sensor API is assumed. Simulate the source and document it.

```json
{
  "event_id": "EV-10001",
  "sensor_id": "S-001",
  "shipment_id": "SHP-001",
  "sequence_no": 101,
  "event_time": "2026-11-10T10:00:00Z",
  "ingestion_time": "2026-11-10T10:01:00Z",
  "temperature_c": 5.4
}
```

Fields:
- event_id: unique event identity
- sensor_id: source sensor
- shipment_id: shipment
- sequence_no: source sequence for diagnostics
- event_time: measurement time
- ingestion_time: platform arrival time
- temperature_c: measured temperature

Provide deterministic scenario generators for normal, excursion, late,
duplicate, out-of-order, missing and contradictory-inspection cases.

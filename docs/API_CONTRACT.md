# API Contract

Use JSON REST APIs with validation and structured errors.

## Shipments
POST /api/shipments
GET /api/shipments
GET /api/shipments/{id}

## Lots
POST /api/lots
GET /api/shipments/{id}/lots

## Sensors
POST /api/sensors
POST /api/shipments/{id}/sensors

## Sensor events
POST /api/sensor-events
POST /api/sensor-events/batch
GET /api/shipments/{id}/sensor-events

## Excursions
GET /api/excursions
GET /api/excursions/{id}
GET /api/shipments/{id}/excursions

## Investigations
GET /api/investigations/{id}
POST /api/investigations/{id}/manual-inspection
POST /api/investigations/{id}/decision

## Audit
GET /api/audit/{entityType}/{entityId}

## Health
GET /actuator/health

## Error example
{
  "timestamp": "2026-11-10T10:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "temperature_c is required",
  "path": "/api/sensor-events"
}

# Reproducible Test Scenarios

## Normal
18.0, 20.1, 21.5, 22.0, 19.8°C
Expected: NORMAL, no incident.

## Controlled-room excursion
26°C-29°C for a short period.
Expected: MINOR under prototype rules.

## Major/critical excursion
Sustained >30°C or >40°C according to configured rules.
Expected: appropriate MAJOR/CRITICAL incident.

## Duplicate
Send the same event_id twice.
Expected: first accepted, second DUPLICATE, no duplicate incident.

## Out-of-order
Send event_time 10:00, 10:10, 10:05.
Expected: event-time ordering.

## Late
event_time 10:05, ingestion_time 11:20.
Expected: LATE and evaluation using event_time.

## Missing
Expected 5-minute cadence:
10:00, 10:05, 10:10, 10:15, 10:20.
Actual:
10:00, 10:05, 10:10, 10:25.
Expected: missing interval is UNKNOWN.

## Contradictory inspection
Sensor: 11.8°C.
Manual inspection: 6.2°C.
Expected: both evidence records retained and analyst records a reasoned decision.

## Negative tests
- missing shipment_id
- unknown sensor
- malformed timestamp
- null/impossible temperature
- duplicate event_id
- unauthorized decision
- decision without reason
- nonexistent investigation

## Reconciliation
Compare raw event count, accepted/duplicate/quarantined counts,
clean event count, incident-linked events and analytical facts.
Every difference must be explainable.

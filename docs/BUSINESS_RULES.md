# Business Rules

## Storage profiles

The shipment stores its applicable storage profile.

### Controlled Room / Ambient
Target: 15°C-25°C.
Prototype routine excursion allowance: 25.1°C-30°C.

### Cool
8°C-15°C.

### Refrigerated
2°C-8°C.

### Frozen
-25°C to -10°C for the prototype.

These are configurable prototype/business rules, not universal regulatory
stability determinations. Real disposition depends on product/manufacturer
stability data.

## Missing data
Missing expected readings are UNKNOWN, never SAFE.

The simulator should use a configurable expected sampling interval,
for example 5 minutes.

## Duplicate events
`event_id` is the primary duplicate identity.
Repeated event_id occurrences are retained for traceability but subsequent
occurrences are marked DUPLICATE and must not create another incident.

## Event-time policy
`event_time` is when the sensor measured the temperature.
`ingestion_time` is when the platform received it.

Temperature evaluation uses event_time.

Default prototype allowed lateness: 120 minutes. The configuration should
support a 15-minute to 2-hour range.

Inside the window: accept and recalculate the affected time window.
Beyond the finalization window: quarantine for investigation and do not
silently mutate a finalized disposition.

## Controlled-room severity prototype
NORMAL: 15°C-25°C.
MINOR: >25°C and <=30°C for <=240 minutes.
MAJOR: >30°C to <=40°C, or >30°C for >240 minutes and <=24 hours.
CRITICAL: >40°C, or a profile-defined critical freeze/extreme condition.

Other storage profiles use profile-specific configurable thresholds.

These thresholds are prototype assumptions and must be versioned with
every incident.

## Contradictory inspection
Sensor evidence and manual inspection evidence are stored separately.
Neither overwrites the other.

## Disposition
Allowed values:
- RELEASE
- HOLD
- REJECT

Every decision requires investigator, timestamp, reason, rule version
and affected lot(s).

## Audit
Decision/audit history is append-only from the application perspective.
No update/delete API should be exposed for historical decisions.

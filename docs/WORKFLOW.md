# End-to-End Workflow

Register shipment
 -> register lots
 -> assign sensor
 -> simulated sensor event
 -> store raw event
 -> validate + normalize
 -> deduplicate
 -> event-time handling
 -> late/out-of-order/normal classification
 -> missing interval detection
 -> evaluate storage-profile rules
 -> NORMAL / UNKNOWN / EXCURSION
 -> create/update incident
 -> trace affected lots
 -> investigation
 -> review sensor + manual evidence
 -> RELEASE / HOLD / REJECT
 -> append audit record
 -> analytical aggregation
 -> Snowflake
 -> Power BI

## Event states
RECEIVED -> VALIDATED / REJECTED / DUPLICATE / QUARANTINED

## Excursion states
OPEN -> UNDER_INVESTIGATION -> DISPOSITIONED -> CLOSED

## Traceability
A demo event must be traceable:
raw payload -> normalized event -> data-quality status ->
incident -> affected lot -> investigation -> decision -> audit ->
analytical record.

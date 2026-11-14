# KPI definitions

All KPIs are calculated from the same repeatable-read view snapshot as the CSV
dimensions and facts. No value uses the current wall clock.

| KPI | Definition | Unit / scope |
|---|---|---|
| `excursion_rate` | Number of active shipments with one or more linked excursions divided by active shipments. | Ratio, 0–1 |
| `mean_time_to_detect` | Mean of incident `flagged_at` (`created_at`) minus earliest linked `start_event_time`. | Minutes |
| `mean_time_to_disposition` | Mean of decision `decided_at` minus the linked incident `created_at` (`flagged_at`). | Minutes |
| `sla_compliance_pct` | Closed investigations whose `closed_at <= sla_due_at`, divided by closed investigations, grouped by excursion severity. Open investigations are excluded because evaluating them needs the wall clock. | Percent, severity |
| `data_completeness_pct` | `100 × (1 - unknown_minutes / total shipment minutes)`, where each sensor's interval spans first CLEAN reading through latest CLEAN reading, extended to `expected_delivery_at` where later; multi-sensor spans are summed. Unknown minutes are gap length minus the expected cadence. No observations yield an empty value. | Percent |
| `lots_at_risk` | Count of distinct active lot IDs on incidents whose investigation status is OPEN. | Lots |

The investigation service creates the stored due timestamp as `opened_at + 24
hours`; that application rule is in
`backend/src/main/java/com/coldchain/platform/investigation/service/InvestigationService.java`.
The report includes `sla_target_hours=24` as the current application default,
while compliance itself compares the actual persisted `sla_due_at` timestamp.
There is no severity-specific SLA configuration in the inspected schema or
service. A result is blank when its denominator is unavailable, rather than
presenting a false zero.

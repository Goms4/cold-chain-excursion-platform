# Project Requirements

## Business problem
A pharmaceutical distributor must detect temperature excursions across
temperature-controlled shipments, determine which lots are at risk, and
preserve an auditable decision trail. Sensor data may arrive late, contain
gaps, duplicates or arrive out of order. Manual inspection records may
contradict sensor observations.

## Users
- OPERATIONS: register shipments/lots/sensors and monitor excursions.
- ANALYST: investigate incidents and record disposition.
- ADMIN: manage configuration and access.

## Core workflow
Shipment registration -> lot registration -> sensor assignment ->
sensor-event ingestion -> raw storage -> validation/deduplication ->
event-time handling -> excursion evaluation -> incident -> affected-lot
tracing -> investigation -> disposition -> append-only audit -> analytics.

## Required capabilities
- Shipment and lot registration
- Sensor registration/assignment
- Simulated sensor-event ingestion
- Raw event retention
- Validation and normalization
- Duplicate detection
- Out-of-order handling
- Late-event policy
- Missing-interval detection
- Configurable storage profiles and excursion rules
- Incident creation/update
- Affected-lot tracing
- Investigation workflow
- Manual inspection evidence
- RELEASE / HOLD / REJECT decision
- Append-only decision/audit history
- Operational dashboard
- Analytical export/model
- Raw-to-clean-to-decision reconciliation

## Required demonstration cases
1. Normal shipment
2. Temperature excursion
3. Late event
4. Duplicate event
5. Missing interval
6. Contradictory manual inspection

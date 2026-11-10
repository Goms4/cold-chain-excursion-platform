# Antigravity Master Prompt

Read every file under `docs/` before coding. Treat those files as the source
of truth. Do not invent conflicting requirements.

## Stack
Java 17, Spring Boot, Maven, PostgreSQL 17, React, Python 3,
Pandas/NumPy, Snowflake, Power BI Service, Docker, AWS EC2/S3,
GitHub Actions and REST/JSON.

## Architecture
Build a modular monolith. Use feature/domain packages for shipment, lot,
sensor, event, excursion, investigation, decision, audit and common config.

Do NOT introduce Kafka, Kubernetes, Redis, ECS/EKS, distributed
microservices, ML or blockchain.

## Implementation order

### Phase 1
Create the Spring Boot backend, PostgreSQL configuration, migrations,
entities/repositories/services/controllers, validation/error handling,
health endpoint and Dockerfile.

### Phase 2
Implement:
shipment -> lot -> sensor -> event ingestion -> validation ->
deduplication -> event-time handling -> missing-data detection ->
excursion rules -> incident -> lot tracing -> investigation ->
decision -> audit.

### Phase 3
Implement deterministic scenario replay for all required test cases.

### Phase 4
Implement React pages from UI_REQUIREMENTS.md.

### Phase 5
Implement Python extraction, profiling, reconciliation, aggregation and
Snowflake loading. Keep operational decisions in Java.

### Phase 6
Add unit, integration, API negative and scenario tests.

### Phase 7
Create Docker Compose and the simplest EC2 deployment artifacts plus
GitHub Actions build/test/deploy workflow where practical.

## Critical correctness
- event_time controls temperature evaluation
- ingestion_time is retained separately
- missing data is UNKNOWN, not SAFE
- duplicate event_id does not create duplicate incidents
- out-of-order events are evaluated in event-time order
- late events inside the window can trigger recalculation
- late events beyond finalization are quarantined
- contradictory inspection evidence is preserved
- decisions require reason, investigator, timestamp, rule version and lots
- audit history is append-only

## Security
Use ADMIN, OPERATIONS and ANALYST roles.
Never hard-code passwords, JWT secrets, AWS or Snowflake credentials.
Use environment variables/configuration.

## Coding discipline
Prefer simple readable code. Avoid premature abstraction. Validate inputs.
Keep business rules testable. Do not generate giant files unnecessarily.
Keep the repository runnable after each phase.

## Deliverables
Runnable backend, frontend, PostgreSQL setup, deterministic scenario data,
Python pipeline, migrations, tests, Docker Compose, AWS artifacts/docs,
README run commands, reconciliation evidence and no committed secrets.

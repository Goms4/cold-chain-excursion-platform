# Architecture

## Operational path

React
  -> REST/JSON
Spring Boot modular monolith
  -> PostgreSQL

Operational domains:
shipment, lot, sensor, event ingestion, validation/deduplication,
event-time processing, excursion rules, lot traceability,
investigation, decision and audit.

## Analytical path

PostgreSQL
  -> Python/Pandas profiling + reconciliation + aggregation
  -> S3 analytical export
  -> Snowflake
  -> Power BI Service

## AWS

Internet -> EC2 -> Docker Compose -> React + Spring Boot -> PostgreSQL

S3 stores analytical exports/evidence artifacts.

Keep deployment simple. Do not introduce Kafka, Kubernetes, ECS/EKS,
Redis or distributed microservices.

## Why modular monolith
The evaluation prioritizes traceability, correctness, failure handling,
testing and deployment. A modular monolith avoids unnecessary network
failure modes while preserving component boundaries.

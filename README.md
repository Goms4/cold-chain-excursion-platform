# Cold-Chain Excursion Platform

A demonstrable pharmaceutical temperature-excursion command platform built as a clean modular monolith.

## Core Technical Stack
- **Backend**: Java 17, Spring Boot 3.3.3, Maven, Spring Security (JWT)
- **Database**: PostgreSQL 17 with Flyway versioned migrations (`V1__create_schema.sql`, `V2__seed_storage_profiles.sql`)
- **Frontend**: React (Vite, Tailwind CSS, Lucide Icons) served via Nginx in production
- **Analytics & Pipeline**: Python, Pandas, Snowflake DDL & local staging
- **Deployment & CI**: Docker, Docker Compose, AWS EC2, GitHub Actions

---

## Architectural Principles & Strict Constraints
1. **Modular Monolith**: Clean domain boundaries (`shipment`, `lot`, `sensor`, `event`, `excursion`, `investigation`, `decision`, `audit`, `scenario`) without distributed microservices, Kafka, Redis, Kubernetes, or ML.
2. **Authoritative Event-Time Telemetry**: `event_time` (when measured) strictly controls temperature evaluation; `ingestion_time` (when received) is retained separately in the raw payload ledger.
3. **Missing Data is UNKNOWN**: Telemetry gaps exceeding the expected sampling cadence (default 5 min) are recorded as `UNKNOWN`, never assumed `SAFE`.
4. **Duplicate Deduplication**: Duplicate `event_id` occurrences are safely retained for audit traceability but never trigger duplicate incidents.
5. **Separation of Watermark from Business Finalization**:
   - The allowed-lateness watermark (default 120 min) determines whether an incoming event is accepted for recalculation or quarantined.
   - Business finalization is governed by explicit shipment/excursion states (`CLOSED`, `DELIVERED`, `DISPOSITIONED`). Events arriving for finalized records are quarantined to prevent silent mutations.
6. **Dual Independent Evidence Preservation**: Sensor telemetry and receiving dock manual inspections are stored separately. Neither overwrites the other. Contradictions trigger a `CONTRADICTORY_EVIDENCE` flag requiring an authorized Analyst or Admin disposition and mandatory clinical rationale.
7. **Append-Only Ledgers**: Historical decisions (`disposition_decisions`) and platform audit events (`audit_history`) are strictly immutable and append-only.

---

## Quick Start Instructions

### 1. Run with Docker Compose (Recommended)
From the repository root:
```bash
cd docker
cp .env.example .env
docker compose up --build
```
- **Frontend UI**: [http://localhost:3000](http://localhost:3000) (Production React build served by Nginx)
- **Backend REST API**: [http://localhost:8080](http://localhost:8080)
- **Health Endpoint**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- **PostgreSQL Database**: `localhost:5432` (db: `cold_chain`, user: `cold_chain`)

---

### 2. Local Developer Execution

#### Backend (Java 17):
```bash
cd backend
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home # or your Java 17 path
mvn clean test        # Runs unit and scenario integration tests
mvn spring-boot:run   # Starts backend on http://localhost:8080
```

#### Frontend (React + Vite):
```bash
cd frontend
npm install
npm run build         # Verifies production build
npm run dev           # Starts local dev server with API proxy on http://localhost:3000
```

#### Analytical Pipeline & Reconciliation:
```bash
cd data-pipeline
python3 extract_profile.py   # Extracts, profiles, and stages analytical datasets
python3 reconcile.py         # Verifies raw vs processed mathematical reconciliation
python3 snowflake_loader.py  # Stages/loads into Snowflake warehouse using environment credentials
```

---

## Reproducible Demonstration Scenarios

You can execute the 6 required demonstration cases in 1-click from the UI header bar, or via cURL:

1. **Normal Shipment**:
   ```bash
   curl -X POST http://localhost:8080/api/scenarios/run/1
   ```
   *Expected*: Compliant temperatures (18.0°C-22.0°C), 0 incidents.

2. **Controlled-Room Minor Excursion**:
   ```bash
   curl -X POST http://localhost:8080/api/scenarios/run/2
   ```
   *Expected*: 26°C-28.2°C excursion creates `MINOR` severity incident and elevates affected lot risk.

3. **Major / Critical Excursion**:
   ```bash
   curl -X POST http://localhost:8080/api/scenarios/run/3
   ```
   *Expected*: Extreme heat (>40°C) elevates incident to `CRITICAL`.

4. **Duplicate Event Handling**:
   ```bash
   curl -X POST http://localhost:8080/api/scenarios/run/4
   ```
   *Expected*: Identical `event_id` sent twice; first accepted, second marked `DUPLICATE` with zero duplicate incidents.

5. **Out-of-Order & Late Event Handling**:
   ```bash
   curl -X POST http://localhost:8080/api/scenarios/run/5
   ```
   *Expected*: Events 14:00, 14:10, and 14:05 arrive out of order; chronologically evaluated by `event_time`.

6. **Missing Telemetry & Contradictory Manual Inspection**:
   ```bash
   curl -X POST http://localhost:8080/api/scenarios/run/6
   ```
   *Expected*: 20-minute cadence gap flagged as `UNKNOWN` (never SAFE). Sensor reports 11.8°C while receiving inspection reports 6.2°C; both evidence sources preserved independently and investigation flagged `CONTRADICTORY_EVIDENCE`.

---

## Data Pipeline Reconciliation Verification
Check platform telemetry reconciliation anytime:
```bash
curl http://localhost:8080/api/reconciliation/summary
```
Verifies:
`Total Raw Events == Total Accepted Events + Duplicate Events + Quarantined Events`

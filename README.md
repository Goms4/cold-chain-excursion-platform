# Cold-Chain Excursion Platform

A demonstrable pharmaceutical temperature-excursion command platform built as a clean modular monolith.

## Core Technical Stack
- **Backend**: Java 17, Spring Boot 3.3.3, Maven, Spring Security (role-password login, server sessions)
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

### Demo login

The local/demo profile reads login credentials from
`COLDLATCH_OPERATIONS_PASSWORD` and `COLDLATCH_ANALYST_PASSWORD`. Set them in
the ignored `docker/.env` file or the runtime environment. No login passwords
are stored in repository configuration. The Company field is a display label
only; it does not create tenant isolation. Change the PostgreSQL service
passwords in `docker/.env` before using the database.

### 1. Run with Docker Compose (Recommended)
From the repository root:
```bash
cp docker/.env.example docker/.env
# Replace COLDCHAIN_ADMIN_PASSWORD, COLDCHAIN_MIGRATOR_PASSWORD, and
# COLDCHAIN_APP_PASSWORD with local-only secrets before starting.
docker compose --env-file docker/.env -f docker/docker-compose.yml up --build
```
- **Frontend UI**: [http://localhost:3000](http://localhost:3000) (Production React build served by Nginx)
- **Backend REST API**: [http://localhost:8080](http://localhost:8080)
- **Health Endpoint**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- **PostgreSQL Database**: `localhost:5432` (db: `cold_chain`, app role: `coldchain_app`)

The postgres initialization hook creates the three database roles and installs
`pgcrypto` as admin before Flyway runs. The migrator remains `NOSUPERUSER`.

For an existing PostgreSQL database whose roles exist but `pgcrypto` is
missing, run the one-time repair, then start the backend and frontend:

```bash
scripts/fix-pgcrypto.sh cold_chain
scripts/run-backend.sh
```

In a second terminal:

```bash
scripts/run-frontend.sh
```

The scripts source ignored `docker/.env`, verify PostgreSQL, required roles,
and the extension, then print the local URLs. Login credential values are not
printed; they are loaded from the configured environment.

---

### 2. Local Developer Execution

#### Backend (Java 17):
```bash
cd backend
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home # or your Java 17 path
set -a; source ../docker/.env; set +a
mvn clean test        # Runs unit and scenario integration tests
mvn spring-boot:run   # Starts backend on http://localhost:8080
```

#### Frontend (React + Vite):
```bash
cd frontend
npm install
set -a; source ../docker/.env; set +a
npm run build         # Verifies production build
npm run dev           # Starts local dev server with API proxy on http://localhost:3000
```

#### PostgreSQL to Pandas to CSV pipeline:
```bash
set -a; source docker/.env; set +a
COLDCHAIN_PROVISIONER_USER=coldchain_admin COLDCHAIN_PROVISIONER_PASSWORD="$COLDCHAIN_ADMIN_PASSWORD" scripts/provision-analytics-role.sh
scripts/run-pipeline.sh              # exports CSVs and independently verifies view row counts
```
For a fresh local install, the analytics role needs a privileged migration/grant
operator. You can substitute a privileged PostgreSQL user via
`COLDCHAIN_PROVISIONER_USER` and `COLDCHAIN_PROVISIONER_PASSWORD`. The provision
command writes a generated analytics password only to ignored
`data-pipeline/.env`; it grants `coldchain_analytics` CONNECT and SELECT on only
the analytics views. The role defaults to read-only and cannot read `public` tables. Set
`DB_NAME` to select a database (default `cold_chain`). The database should have
Flyway migration V11 applied; starting the backend normally applies it.

Each run writes six view datasets and two derived reports under
`data-pipeline/output/<UTC-run-id>/`, plus `manifest.csv` and the `LATEST`
pointer. `scripts/run-pipeline.sh` loads ignored `data-pipeline/.env`, creates
the virtual environment if missing, installs pinned requirements, runs the
snapshot export, then performs an independent base-table reconciliation as the
privileged local operator. The pipeline does not call the REST API. The legacy
`reconcile.py` now checks the latest local manifest only. Snowflake/S3 loading
remains a later, separately authorized task; see the warehouse scripts and
`sql/snowflake_schema.sql`.

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

## Application telemetry reconciliation
Check the backend's operational telemetry reconciliation endpoint:
```bash
curl http://localhost:8080/api/reconciliation/summary
```
Verifies:
`Total Raw Events == Total Accepted Events + Duplicate Events + Quarantined Events`
# Local database access

The running service must connect as the non-owner `coldchain_app` role. Flyway
connects separately as `coldchain_migrator`; neither password is stored in the
repository. A PostgreSQL administrator provisions roles and grants with
`scripts/provision-db-roles.sh`. Supply `COLDCHAIN_ADMIN_PASSWORD`,
`COLDCHAIN_MIGRATOR_PASSWORD`, and `COLDCHAIN_APP_PASSWORD` through your local
environment or ignored `docker/.env` file. The admin connection used to
provision roles must already have PostgreSQL superuser privileges.
For Docker, copy `docker/.env.example` to ignored `docker/.env` and start with
`docker compose --env-file docker/.env -f docker/docker-compose.yml up --build`;
the Postgres init hook creates the migrator/app roles before Spring starts.

For a disposable local/demo database, run
`COLDLATCH_ENV=local DB_NAME=coldchain_demo_local ./scripts/demo-reset.sh --yes`.
The reset refuses other profiles, unsuffixed database names, and remote hosts
unless explicitly overridden. It drops and recreates only the named database,
runs Flyway as the migrator, applies app grants, and seeds through the
authenticated REST API. `scripts/db-permission-check.sh` verifies the app role
cannot mutate audit history or disable its trigger. Never use the app role to
reset a database.

The demo population is created through the same shipment, lot, temperature,
investigation, evidence, and decision endpoints used by the application.
Operational SQL seeding is removed; reference storage profiles remain in
Flyway migrations.

## Login and identity

The two fixed roles are OPERATIONS and ANALYST. The local/demo profile defaults
are documented at the top of this README. In other profiles,
`COLDLATCH_OPERATIONS_PASSWORD` and `COLDLATCH_ANALYST_PASSWORD` must be supplied
as distinct runtime secrets and must not equal the demo defaults. At startup,
the service BCrypt-hashes them into the `role_credential` table. Login creates an eight-hour HttpOnly,
SameSite=Strict session; CSRF uses the `XSRF-TOKEN` cookie and `X-XSRF-TOKEN`
header. The Company value is a display label only. There are no user accounts
or tenant boundaries: shipment records are not isolated by company.

Failed role/password login attempts are audited as TECHNICAL events and are
rate limited by source IP. No password or hash is returned by any endpoint.
Audit rows are chained in PostgreSQL; see [docs/audit-hash-chain.md](docs/audit-hash-chain.md)
and use `scripts/audit-head.sh` to retain an independent latest hash externally.

## Running the browser tests

Start the backend and frontend using the Local Developer Execution commands
above, then provide the two login passwords from `docker/.env` to the test
process. Playwright uses the installed Google Chrome channel and does not
download a browser:

```bash
cd frontend
set -a; source ../docker/.env; set +a
npm run test:e2e
```

Screenshots are written to `docs/screenshots/` at 1440, 1280, 1024, and 768px.

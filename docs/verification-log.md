# Verification log

## 2026-11-14 — Ubuntu 26.04 Python setup failure

### Python version root cause and replacement

- The only project references to Python were `.github/workflows/ci.yml`'s
  `python-version: '3.14.2'` and the README local bootstrap prerequisite
  `Python 3.14.2`. No `.python-version`, `.tool-versions`, Python matrix,
  `pyproject.toml`, or `requires-python` constraint exists. The exact patch was
  carried from the developer machine's Python 3.14.2 into CI; it was not a
  project dependency requirement. Ubuntu 26.04's preinstalled runner Python
  is 3.14.4, so the runner cache has no 3.14.2 entry.
- Changed this once to workflow-level `PYTHON_VERSION: '3.14'` and made the
  single `setup-python` step consume `${{ env.PYTHON_VERSION }}`. The live
  `actions/python-versions` manifest contains CPython 3.14 Linux x64 builds
  with `platform_version: 26.04` (3.14.6 through 3.14.10 in the manifest
  inspected). `setup-python@v7` resolves a minor-only version to an available
  patch. The project pins no Python runtime; pinned application packages work
  with CPython 3.14. The README now says Python 3.14, not a patch version.
- A binary-only pip download for CPython 3.14 x86_64 succeeded for every direct
  and transitive requirement with `manylinux2014_x86_64`, `manylinux_2_24`,
  `manylinux_2_27`, and `manylinux_2_28` platform tags. Direct packages were
  `pandas 3.1.1`, `psycopg 3.3.2`, and `pytest 9.0.2`; the download also
  resolved binary wheels for `numpy 2.5.4` and `psycopg-binary 3.3.2`, plus
  pure-Python transitive wheels. Pandas' Linux wheel is tagged
  `manylinux_2_24/2_28`, so the suggested single `manylinux2014` command alone
  reports no pandas wheel; using the Ubuntu 26 compatible tags succeeded.
  No dependency pins were changed.

### Hosted-runner compatibility audit

- `ci-credentials`: `ubuntu-latest`, no Actions setup step; uses the image's
  Bash and OpenSSL to generate random one-run passwords.
- `backend-test`: `ubuntu-latest`; `actions/checkout@v7`,
  `actions/setup-java@v6` for Temurin Java 17 and Maven caching, then the
  preinstalled Maven command. The 26.04 image lists Maven 3.9.15 and JDK 17.
- `frontend-build`: `ubuntu-latest`; checkout v7 and setup-node v7 install
  Node 20, then use npm for `npm ci` and the Vite build. There is no apt step.
- `python-test`: `ubuntu-latest`; prints image/tool versions before checkout,
  then checkout v7, setup-java v6, setup-python v7 with Python 3.14, and
  setup-node v7 with Node 20 (needed by `demo-seed.sh`). It uses Maven to build
  the JAR, pip to install the requirements, and Docker for the PostgreSQL 17
  service. The only apt command is a conditional fallback for
  `postgresql-client` if `psql` or `pg_isready` is absent. Ubuntu 26.04 includes
  PostgreSQL 18.6 client tools; a Docker `ubuntu:26.04` apt proxy installed
  `postgresql-client` 18.6, `openssl` 3.5.5, `curl` 8.18.0, and `jq` 1.8.1.
  PostgreSQL 18 client tools connect to the PostgreSQL 17 service; the pipeline
  uses psql/pg_isready, not pg_dump. The service health command uses the
  PostgreSQL 17 image's own `pg_isready`, and connects to its mapped host port,
  not a host Postgres server.
- `docker-build`: `ubuntu-latest`; checkout v7 and the image-provided Docker
  client/daemon for `docker build`. Ubuntu 26.04's runner image lists Docker
  29.4.2. There is no apt step.
- The Ubuntu 26.04 runner image lists Bash 5.3.9, Python 3.14.4, Node 24.21,
  npm 11, Maven 3.9.15, Docker 29.4.2, PostgreSQL 18.6 tools, OpenSSL 3.5.5,
  and jq 1.8.1. Setup actions then select Java 17, Node 20, and Python 3.14.
  The local `ubuntu:26.04` Docker apt check ran on the Mac's ARM64 Docker
  architecture; the official runner image software list provides the x64
  versions. No GNU/BSD-incompatible shell flags were found in the CI scripts:
  their `date`, `awk`, `sed`, `grep`, `mktemp`, and Bash usage is supported on
  the Ubuntu runner. The Docker health service is independent of these host
  tools.

### Diagnostics failure root cause and fix

- `CI_BACKEND_LOG`, `CI_BACKEND_PID_FILE`, and the output path were previously
  appended to `GITHUB_ENV` by the credential step. A setup-python failure skips
  that later step; the `if: failure()` diagnostics then expanded
  `$CI_BACKEND_LOG` under nounset and masked the original failure.
- These paths, the database host/port/name, and the API URL are now defined
  once at `python-test` job scope. The bootstrap consumes the log, PID, and
  output path values rather than defining a different CI path. Diagnostics
  keep nounset enabled, use optional-variable defaults, redact password and
  session-token forms before printing a log tail, and guard missing tools and
  files. Only this diagnostic step uses a shell without `-e`; it handles each
  failure locally and exits successfully. Artifact upload ignores missing
  files.
- Tested `scripts/ci-diagnostics.sh` in isolation with all path/password
  variables unset and with explicit nonexistent log/output paths. Both runs
  exited 0, printed the expected “backend log not created” and “pipeline output
  not created” notes, and emitted no unbound-variable error.

## 2026-11-14 — Clean-checkout GitHub Actions pipeline bootstrap

### Root cause and read-only findings

- Confirmed the failure mechanism from a clean-checkout contract: CI ran
  `pytest -q` without first creating `data-pipeline/output/LATEST`; three of
  the five tests read `LATEST` and the timestamped directory. The exporter
  writes `data-pipeline/output/<UTC timestamp>/` containing six dimension/fact
  CSVs (`dim_shipment`, `dim_lot`, `fact_sensor_event`, `fact_excursion`,
  `fact_investigation`, `fact_decision`), `telemetry_quality_report.csv`,
  `kpi_summary.csv`, and `manifest.csv`, then atomically updates `LATEST`.
- Pipeline connection variables are `PGHOST`, `PGPORT`, `PGDATABASE`,
  `PGUSER`, and `PGPASSWORD`; extraction must use `coldchain_analytics`.
  `scripts/run-pipeline.sh` also runs `scripts/pipeline-verify.sh`, whose
  read-only base-table reconciliation must use `coldchain_migrator` and its
  password. `requirements.txt` pins pandas, psycopg[binary], and pytest.
  Tests require actual seeded normal/excursion telemetry, duplicate, accepted
  late, quarantined, missing-gap records, and refrigerated plus controlled /
  ambient profile ranges. They also enforce CSV field contracts and manifest
  reconciliation.
- Before parameterization, role scripts defaulted their provisioning
  administrator to `PGUSER` or the current Unix `$USER`, defaulted target DB
  to `cold_chain`, and connected to a hard-coded `postgres` database. The
  analytics provisioner generated and wrote `data-pipeline/.env` if its
  password was absent. The pipeline runner always sourced that file when it
  existed. CI avoids these machine-local paths by setting every role/database
  credential, provisioning database, host, and port; provisioning database
  is now parameterized, analytics auto-file creation is bypassed with an
  explicit password, and the runner accepts an opt-out from sourcing `.env`.
- Role names in provisioning SQL and Docker init are intentional shared
  contracts, not local-machine dependencies. `docker/init-roles.sh` runs under
  the Postgres image entrypoint with injected POSTGRES credentials; it does not
  rely on `docker exec`. Provisioning scripts use explicit TCP `-h` arguments,
  not Unix sockets. The `_demo`/`_local` destructive reset interlock belongs
  to `demo-reset.sh`; CI does not call it. It provisions a fresh uniquely named
  database and does not drop or reset any database.
- `scripts/demo-seed.sh` needs curl, jq, node, `COLDLATCH_API_URL`, and
  distinct Operations and Analyst passwords. Its normal authenticated session
  and CSRF requests are used. The backend has `local` and `demo` profiles, and
  outside those profiles it rejects blank, equal, or built-in demo credentials.
  CI supplies per-run distinct values and explicitly starts the `local`
  profile.
- `/actuator/health` is public (`SecurityConfig` permits it) and exposes DB
  health. `application.yml` uses `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`,
  `DB_PASSWORD` for the application and `FLYWAY_USER`, `FLYWAY_PASSWORD` for
  migrations. V9 creates `pgcrypto`; V11 creates the `analytics` schema and
  read views; V12 extends decision evidence context.

### Required operation order

1. Wait for PostgreSQL 17 over TCP; create the least-privilege application
   roles and a fresh target database using the existing role provisioning.
2. Install `pgcrypto` before Flyway starts.
3. Build/start the backend with the `local` profile; Flyway applies V1–V12 as
   `coldchain_migrator`, and the runtime uses `coldchain_app`.
4. Wait for unauthenticated `/actuator/health` to report healthy. Only now
   provision `coldchain_analytics` and grant SELECT on the migrated views.
5. Seed through `scripts/demo-seed.sh`; verify with `scripts/demo-verify.sh`.
6. Run the exporter as `coldchain_analytics`, reconcile the source counts as
   `coldchain_migrator`, validate `LATEST`/CSV/manifest, then run pytest.
7. Stop the backend; Docker image builds depend on all three validation jobs.

### Implementation state

- Added `scripts/ci-pipeline-env.sh` as the CI/local orchestration entry point.
  Existing provisioning keeps its previous defaults; new environment
  parameters select the bootstrap connection database and optional creation
  of a fresh target database. Analytics provisioning can be deferred, while
  its default behavior remains enabled for existing callers. The pipeline
  runner's default `.env` loading is unchanged; the bootstrap explicitly opts
  out so it cannot be redirected by a local ignored file.
- The workflow now generates throwaway credentials, masks them in the
  database job, provisions PostgreSQL 17, builds the jar, seeds through REST,
  exports from the analytics role, and rejects skipped pytest cases. It has
  least-privilege workflow permissions, ref-scoped cancellation, bounded
  runtime, failure diagnostics, and seven-day diagnostics artifacts guarded
  by a password scan. Action versions were updated for checkout/setup-java/
  setup-node/setup-python and upload-artifact where used; Java remains 17.
- `.gitignore` already excluded `.env`, `data-pipeline/.env`, pipeline output,
  and `backups/`; added key/certificate and common database dump suffixes.
  SQL source migrations remain trackable.

### Isolated clean-checkout validation

- Prepared a fresh clone under `/tmp` and confirmed it had neither
  `data-pipeline/output/` nor `data-pipeline/.env`. Used a disposable PostgreSQL
  17 container on `127.0.0.1:55432`, database `coldlatch_ci_final`, and the
  backend on port `18080`; no command-line connection targeted the user's
  `cold_chain` database. The container and temporary checkout were removed
  after verification.
- During the first failed local attempt, port 8080 was already occupied. Before
  adding the TCP-port guard, the helper issued one read-only public
  `/actuator/health` request to that existing service, then failed before app
  grants or REST seeding because the temporary database had not migrated.
  No local database writes or seed operations occurred. The final successful
  validation used port 18080; the helper now checks port availability without
  sending a request to an existing service.
- Bootstrap output confirmed 12 successful Flyway migrations (V1 through V12),
  `/actuator/health` became UP, and the existing REST seed/demo verification
  completed. Role verification showed `coldchain_admin` as superuser and
  `coldchain_migrator`, `coldchain_app`, and `coldchain_analytics` as
  `super=false`. The app grants and analytics view grants both completed only
  after migrations.
- Export produced 10 shipments, 11 lots, 27 sensor events, 5 excursions, 5
  investigations, 3 decisions, 11 quality-report rows, and 7 KPI rows. Every
  dataset, reconciliation, and raw-event identity row in `manifest.csv` was
  `PASS`; identity was `27 = 22 + 3 + 2`. Pytest reported `5 passed in 0.54s`
  with no skipped tests. A prior isolated run reported `5 passed in 0.74s`.
- A second export without changing the database produced the same row counts
  and SHA-256 for each of the nine CSVs (including manifest):

  | CSV | SHA-256 |
  | --- | --- |
  | `dim_shipment.csv` | `59d8ef65468d778e05b4350ae8aca23fdfc62a1a70e5e19ef88db16e2cb8f147` |
  | `dim_lot.csv` | `1e67b699d4ae2af0f07d53170ff57d8d655747a380b207b56e4001272b27db80` |
  | `fact_sensor_event.csv` | `040d1e1b12b6830446f433e3f1da41a293dc69c089b17ab4ffc0f43cbd859586` |
  | `fact_excursion.csv` | `7b5dd330105123c9d5a04fa041a42d8aeb930d357b23e752b115c3fd57ec40ea` |
  | `fact_investigation.csv` | `0e54d8e0f1e25d979276f6013667f194827b09f65ec081036fb9db98428123cc` |
  | `fact_decision.csv` | `144792f0be958e9d545ec2b6d780751dc8f4bcc22162145506f7332ab69d356a` |
  | `telemetry_quality_report.csv` | `7a61181f58bfa3c05c32519a6ecac9d7d6664425ca0bad64d430ac48332d5527` |
  | `kpi_summary.csv` | `e039f4c5ce2518d1b851fcdd16943c0b81be4872266b9495a29c04697013385e` |
  | `manifest.csv` | `47acd1fffe1b2e84e3d31a5337f97c2a54d67926f587bc87933e55d03f9b60b0` |

- Deleting `output/` in that clone and running pytest alone reproduced the
  initial issue exactly: `3 failed, 2 passed`; the three export-dependent
  tests reported `FileNotFoundError` for `data-pipeline/output/LATEST`.
- Final checks: Maven reported `23 tests, 0 failures, 0 errors, 0 skipped`;
  `npm run build` passed; both backend and frontend Docker image builds passed;
  Ruby's YAML parser and `bash -n` accepted the workflow and scripts; `git diff
  --check` passed. `git check-ignore` confirmed output, `.env`, backup, PEM,
  key, and dump paths are ignored. No actionlint binary was installed, so YAML
  was validated with Ruby's parser instead.
- The official action documentation/release notes were checked for checkout
  v7, setup-java v6, setup-node v7, setup-python v7, and upload-artifact v7.
  The action examples confirm the configured Java, Node, Python, caching,
  artifact path/name/retention inputs remain valid. The setup actions and
  artifact action use supported Node 24 runtimes; the application continues
  to build with Java 17 and Node 20.
- Not locally verifiable: a run on GitHub-hosted Ubuntu runners and their
  external action/image download timing. Local Docker, REST, PostgreSQL,
  migrations, export, and pytest paths all passed. No commit or push was made.

## 2026-11-14 — PostgreSQL to Pandas to CSV pipeline

### Read-only inspection before changes

- Workspace database configuration: `backend/src/main/resources/application.yml`
  defaults to host `localhost`, port `5432`, database `cold_chain`, app role
  `coldchain_app`; `application-local.yml`/`application-demo.yml` use the
  demo-only app credential defaults. The running JDBC application sessions
  were connected as `coldchain_app` to `cold_chain`.
- Databases present were `cold_chain`, `coldchain_demo_local`, and `postgres`.
  The reset script defaults to `coldchain_demo_local` and enforces a `_demo` or
  `_local` suffix. This pipeline's main target is `cold_chain`; the separate
  reset database is used only for the fresh/reset verification below.
- The local operator role `boyallagoamthichowdary` had `rolsuper=true` and
  `rolcreaterole=true`. `coldchain_migrator` and `coldchain_app` were
  non-superusers; `coldchain_analytics` did not exist before provisioning.
- Existing tables inspected with `\d+`: `shipments` (`id`, `tracking_number`,
  `origin`, `destination`, `storage_profile_id`, `status`,
  `expected_cadence_minutes`, `allowed_lateness_minutes`, `created_at`,
  `updated_at`, `archived_at`, `expected_delivery_at`, `shipment_reference`,
  `deleted_at`, `deleted_by`, `delete_reason`); `lots` (`id`, `shipment_id`,
  `product_name`, `batch_number`, `quantity`, `risk_level`,
  `disposition_status`, `created_at`, `archived_at`, `expiry_date`,
  `deleted_at`, `deleted_by`, `delete_reason`, `unloaded_at`);
  `raw_sensor_events` (`id`, `event_id`, `sensor_id`, `shipment_id`,
  `sequence_no`, `event_time`, `ingestion_time`, `raw_payload`,
  `processing_status`, `created_at`); `sensor_events` (`id`, `event_id`,
  `sensor_id`, `shipment_id`, `sequence_no`, `event_time`, `ingestion_time`,
  `temperature_c`, `status`, `quality_flag`, `is_duplicate`,
  `duplicate_of_event_id`, `excursion_incident_id`, `created_at`, `lot_id`,
  `source`); `excursion_incidents` (`id`, `shipment_id`, `rule_version`,
  `severity`, `status`, `start_event_time`, `end_event_time`,
  `peak_temperature_c`, `duration_minutes`, `created_at`, `updated_at`);
  `excursion_affected_lots` (`id`, `excursion_id`, `lot_id`,
  `initial_risk_level`, `disposition_status`, `created_at`); `investigations`
  (`id`, `excursion_id`, `status`, `assigned_investigator`, `sla_due_at`,
  `opened_at`, `closed_at`, `notes`, `created_at`, `updated_at`);
  `manual_inspections` (`id`, `investigation_id`, `shipment_id`,
  `inspector_name`, `inspection_time`, `observed_temperature_c`,
  `package_integrity_status`, `visual_condition`, `notes`,
  `contradicts_sensor`, `created_at`); `disposition_decisions` (`id`,
  `investigation_id`, `excursion_id`, `decision`, `investigator_name`,
  `reason`, `rule_version`, `affected_lot_ids`, `decided_at`, `created_at`);
  `missing_intervals` (`id`, `shipment_id`, `sensor_id`, `gap_start_time`,
  `gap_end_time`, `expected_cadence_minutes`, `status`, `detected_at`);
  `audit_history` was inspected and intentionally excluded from analytics.
- `deleted_at` is the soft-delete field for shipments and lots; archives use
  `archived_at`. Cadence and lateness are persisted per shipment. The actual
  storage profiles in the database include Refrigerated 2–8 °C and Controlled
  Room / Ambient 15–25 °C. Before migration, the successful Flyway schema
  version was 10 (the version column is text, so `max(version)` is not a valid
  way to determine the latest migration).
- Before migration, backed up `cold_chain` using `pg_dump -Fc` to
  `backups/coldlatch-pipeline/cold_chain-20261114T043559Z.dump`; `backups/` is
  git-ignored. The current legacy `extract_profile.py` used unauthenticated
  HTTP API requests and wrote root-level CSV/Parquet files; `reconcile.py`
  called the API and returned success if it could not connect, explaining its
  unhelpful “Sign in to continue” behavior. The historical CSVs were retained.
  Removed the Parquet implementation backup and its stale SQLAlchemy probe.
  `snowflake_loader.py` and Snowflake DDL were inspected and left unchanged;
  they will need their staging/source contracts aligned to the new CSV output
  before a later Snowflake task. No S3 integration exists in this pipeline.

### Migration, least privilege, pipeline

- First ordinary startup exposed missing `CREATE ON DATABASE cold_chain` for
  the existing `coldchain_migrator`; Flyway did not apply V11. Granted that
  privilege as the local administrator, restarted Spring Boot with the local
  profile on port 8081, and observed `Successfully applied 1 migration ...
  now at version v11`, followed by actuator health `UP` with database `UP`.
- V11 created seven views, all owned by `coldchain_migrator`: six requested
  export views plus `v_missing_intervals`. It has no `security_invoker` setting.
  The raw event view preserves one row per raw ledger row, including the raw
  duplicate that references the same normalized sensor event. The actual
  schema has no stored quarantine reason, so the view returns NULL; no raw
  payload is exported. The application-derived shipment condition is omitted.
- V12 adds investigation status, investigation opened time, and contradictory-
  evidence context to the decision view. It applied successfully on `cold_chain`,
  `coldchain_demo_local`, and the fresh-path database; the decision CSV includes
  those context fields.
- `scripts/provision-analytics-role.sh` created `coldchain_analytics` with
  `NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS`, connection
  limit 5, and `default_transaction_read_only=on`; it stores the generated
  password only in ignored `data-pipeline/.env`. The role has CONNECT to
  `cold_chain`, USAGE on `analytics`, and SELECT on the seven views. Direct
  `SELECT public.shipments` returned `permission denied for table shipments`;
  `CREATE TABLE analytics.probe` returned `cannot execute CREATE TABLE in a
  read-only transaction`. Further permission results are recorded after the
  complete permission harness run.
- First pipeline attempt found a query accidentally selecting derived Python
  columns from the SQL view; fixed the source/output column mapping and reran.
  Two successful runs on unchanged `cold_chain` produced identical data CSV
  hashes: shipments 15, lots 15, sensor events 42, excursions 13,
  investigations 13, decisions 8; report rows 16 (15 shipments plus TOTAL)
  and 8 KPIs. Layer 1 counts matched all six views and raw identity was
  `42 = 39 + 1 + 2`. Layer 2 direct base-table counts matched all six views.
  Identical hashes are printed by each run in the terminal session.
- The seed contains CLEAN, DUPLICATE, and QUARANTINED raw records, a persisted
  LATE quality flag, missing telemetry gaps, both storage profile ranges, and
  events linked to investigations/decisions. `is_late` is separately
  calculated from event/ingestion time and each shipment's persisted 120-minute
  threshold; a backend LATE quality marker can therefore exist when the
  computed threshold flag is false.

### Final checks

- Additional inspected tables: `sensors` (`id`, `shipment_id`, `model`,
  `battery_level`, `status`, `registered_at`); `audit_history` (`id`,
  `entity_type`, `entity_id`, `action`, `actor`, `timestamp`, `payload_before`,
  `payload_after`, `rationale`, `created_at`, `category`, `seq`, `prev_hash`,
  `row_hash`); `role_credential` (`role`, `password_hash`, `updated_at`).
  `processed_sensor_events` and `sessions` are not tables in this schema.
- The initial fresh-path test used new local database
  `coldlatch_pipeline_fresh_local`: PostgreSQL extension setup completed,
  normal Spring Boot startup applied V1–V11 from an empty schema, V12 then
  applied as an update, and actuator health reported database UP. A second
  empty database, `coldlatch_pipeline_fresh_v12_local`, was then created and
  normal Spring Boot startup applied all 12 migrations from an empty schema;
  all seven analytics views exist and health reported UP. `coldchain_demo_local`
  passed the guarded `scripts/demo-reset.sh --yes`
  flow and finished with the REST seed: 10 shipments, 11 lots, 27 events, 5
  incidents, 5 investigations, and 3 decisions. The final reset ran after the
  HTTP harness so the demo database is left at its clean seed.
- `scripts/http-checks.sh`: all 81 checks passed, including authenticated
  Operations/Analyst flows, normal/high/duplicate/late/future/invalid/cross-
  shipment readings, manual investigation registration, Analyst retrieval and
  decision workflow, soft deletes, and app-role audit immutability.
- `scripts/db-permission-check.sh`: all app-role and analytics-role checks
  passed. The analytics login read all seven views and was denied base SELECTs
  for shipments, lots, audit_history, and role_credential; INSERT, UPDATE,
  DELETE, TRUNCATE, CREATE TABLE, and CREATE VIEW were denied. It has the
  requested role flags, connection limit 5, and default read-only setting.
- Final pipeline pair on `cold_chain`: 15 shipments, 15 lots, 42 raw event
  rows, 13 excursions, 13 investigations, 8 decisions, 16 quality-report rows,
  and 8 KPI rows. Each of the six view-backed datasets had zero layer-one
  difference; raw identity was `42 = 39 + 1 + 2`. Layer two matched direct
  counts for each export view plus the missing-interval helper (2 rows). Both
  final run hashes matched byte-for-byte: dimension shipment
  `f71c9f1bdf9f451ab6a50a256295f27153b26e8eb377411e3943083b4066d7c4`, lot
  `8fcf9b00e7c9e6e0921009aa5afd69907c5766b89ddeeaacfea9afed9ec890e8`, event
  `2b78e05646598eef1ca1bf3cc9bbf10ff64bd1a96ba0e6d1f08211f3d49934c9`,
  excursion `cfac4972259be11ba0cfe6775aaf4c61251342744dd07a0c1d5963016fc94f4e`,
  investigation `79eeafb4bec366ee687038f2b61aa4c903257b1e2c68b1214c1195267d043980`,
  decision `5886847d5f55c28be9ff2f8df62f705721141f9d39f3aabaf7e7234647db19dd`,
  quality report `600e3ca5ecf811d20ee00a076a4b5e65380e8939dc070920217b4fc2fa6b3684`,
  KPI summary `d7c94377a8b97850bd676f11b601ad35f0d3a2cffac6614d3e845b7522a5597c`.
- `pytest`: 5 passed. `mvn test -q`: 23 tests passed (14 application, 7
  condition service, 2 event-time). `npm run build`: passed (1,596 modules).
  `git diff --check`: passed. Audit verification: `Chain verified: 100
  records` after final demo reset; the `cold_chain` audit chain verified 145
  records after the HTTP harness.
- The final configured-secret scan checked 415 source/output/log files
  (excluding ignored env files, build artifacts, virtualenv, and backups): 0
  exact configured secret matches. No Parquet artifacts remain under
  `data-pipeline`. Demo profile passwords come from environment/ignored
  configuration, README and Spring profile YAML contain no login-password
  literals, and `scripts/run-backend.sh` no longer prints credentials.
- Shipment condition parity remains untested and the condition is omitted.
  The schema has no quarantine reason or persisted decision actor role; the
  export intentionally returns an empty quarantine reason and infers the
  decision role as ANALYST from endpoint RBAC. Severity-specific SLA targets
  are not configured; the KPI uses the service's stored 24-hour deadline.

## 2026-11-13 — Current pass, Part 1: manual startup and pgcrypto

- Reproduced manual startup against `cold_chain`: after repairing `pgcrypto`, Flyway exposed a V9 checksum mismatch (`DB=1339528122`, edited file=`-1070388376`). Restored the original `CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;` statement; no `flyway repair` was run and no seeded database rows were changed.
- `CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public` executed as `coldchain_migrator` while the extension existed: `NOTICE: extension "pgcrypto" already exists, skipping` and `CREATE EXTENSION`.
- On a disposable clone of the existing database, removed pgcrypto, confirmed extension count `0`, then ran `scripts/fix-pgcrypto.sh coldlatch_existing_probe`. It reported `pgcrypto installed or already present ...`. Manual `mvn spring-boot:run` then reported actuator health `UP`, including database `UP`.
- On a separate empty database without pgcrypto, `mvn spring-boot:run` failed with the registered analysis: `PostgreSQL extension pgcrypto is missing from the configured database.` and `Run: scripts/fix-pgcrypto.sh coldlatch_missing_crypto_probe and then restart the backend.` The normal log had no Java stack trace after the analysis.
- Fresh PostgreSQL 17 container on isolated host port `55432` initialized from an empty volume. Init hook completed; Spring Boot health was `UP`; CSRF-protected `POST /api/auth/login` returned HTTP `200` with `Operations officer · Fresh Start`.
- `scripts/run-backend.sh` and `scripts/run-frontend.sh` both passed preflight and printed their local URLs and demo logins. README manual commands now source `docker/.env` before running either app.

## 2026-11-13 — Step 1: startup and reset

- `scripts/demo-reset.sh --yes`: passed. REST seed created 10 shipments, 11 lots, 27 events, 5 incidents, 5 investigations, and 3 decisions; `scripts/demo-verify.sh` passed.
- `GET http://localhost:8080/actuator/health`: `UP`, including database `UP`.
- `GET http://localhost:3000/login`: HTTP 200.
- PostgreSQL app connection: `current_user=coldchain_app`, `rolsuper=false`.

## 2026-11-13 — Step 2: backend verification

- `mvn test`: 22 tests, 0 failures, 0 errors, 0 skipped.
- `scripts/http-checks.sh`: all 77 checks passed; no failing or flaky checks observed. The session actor assertion confirms `Analyst · ColdLatch`.
- Concurrent chain check: 300 rows across six concurrent connections inserted; 445 total rows verified at the time; no duplicate `prev_hash`, no sequence gaps, `verify_audit_chain()` returned `ok=true`.
- Tamper update: rollback-isolated admin UPDATE was detected at sequence 2. Middle-row deletion: rollback-isolated admin DELETE was detected at sequence 223.
- Harness permission probes: app UPDATE and DELETE returned `permission denied for table audit_history`; TRUNCATE returned the same; disabling triggers returned `must be owner of table audit_history`.
- `scripts/audit-verify.sh`: `Chain verified: 143 records` before the concurrent insert probe.
- Fresh post-reset backend: health `UP`; PostgreSQL component `UP`; frontend `http://localhost:3000` returned 200; app DB role check `coldchain_app|f`.
- `GET /api/audit/verify` returned `{"ok":true,"rowsChecked":100}` for both OPERATIONS and ANALYST.

## 2026-11-13 — Steps 3–5: reset repeatability and final checks

- Both final `scripts/demo-reset.sh --yes` runs passed, each reporting the same service-seeded totals: 10 shipments, 11 lots, 27 events, 5 incidents, 5 investigations, and 3 decisions. Both runs completed `scripts/demo-verify.sh` successfully.
- After the final reset, `scripts/audit-verify.sh` reported `Chain verified: 100 records`.
- Final seeded SQL counts: shipments 10, lots 11, events 27, incidents 5, investigations 5, decisions 3.
- `psql \\du` showed `coldchain_app` and `coldchain_migrator` with no superuser attributes; the earlier connection check returned `coldchain_app|f`. `audit_history` owner is `coldchain_migrator`.
- After the final reset, `scripts/http-checks.sh` passed 77/77 checks, including session auth, 401/403 behavior, ingestion validation, duplicate/late/future handling, decisions, soft deletes, and append-only permissions.
- `npm run build` passed: 1592 modules transformed, no warnings.
- `mvn test` passed: 22 tests, 0 failures, 0 errors, 0 skipped.
- Secret scan: no credential literals or non-genesis 64-character hashes. The only credential-like match is `database/provision-roles.sql` using psql variable references (`:'admin_password'`, etc.), not stored credentials. The all-zero genesis constant is documented and intentional.
- `scripts/http-checks.sh` was rerun after the second successful reset and passed 77/77. A final third reset restored the clean seed; final Maven/build checks passed afterward.

## 2026-11-13 — Browser verification

- Built-in browser refused `http://localhost:3000` with: `Browser Use rejected this action due to browser security policy. Reason: A saved user permission setting blocks this action. Browser use could not acquire authority to trigger network egress. The origin approval gate did not grant access for http://localhost:3000. A saved permission or managed policy denies access.`
- Per that browser policy, I did not attempt Playwright, direct Chrome, CDP, or another browser route. `frontend/playwright.config.ts` and `frontend/e2e/coldlatch.spec.ts` were created for later user-run verification. Browser verification was not performed; `docs/screenshots/` has no verified screenshots.

## 2026-11-13 — Current pass, Parts 2–5: credentials, visual layer, and final checks

- Fresh empty PostgreSQL 17 container: admin init hook created the three roles and installed pgcrypto. Against its empty `cold_chain` database, backend health returned `UP` and CSRF-protected Operations login returned HTTP `200` (`Operations officer · Fresh Start`). This ran on isolated host port `55432` because local port `5432` already serves the user's database.
- On a cloned PostgreSQL database, the `prod` profile failed startup when both credentials were absent and when both were the demo defaults. It reported the configured-secret requirement. With distinct non-demo credentials, actuator health returned `UP`.
- Final clean demo reset on the isolated database passed `scripts/demo-verify.sh` with 10 shipments, 11 lots, 27 events, 5 incidents, 5 investigations, and 3 decisions. Two reset runs produced those same counts; the latest reset restored the clean seed after API tests.
- SQL check on `role_credential` showed both roles with bcrypt `$2a$` hashes of length 60; no hashes or credentials were returned by the API. Production build grep: `PASS production build contains no demo password`.
- Final `mvn test -q`: 22 tests total, 0 failures, 0 errors, 0 skipped (13 application, 7 condition-service, 2 event-time tests). `npm run build`: 1,596 modules transformed, build passed.
- Expanded `scripts/http-checks.sh`: all 81 REST/database checks passed, including normal/high/duplicate/late/future/invalid/cross-shipment readings, Operations investigation registration, Analyst retrieval, RELEASE/HOLD/REJECT, session/RBAC, audit immutability and role grants.
- `psql \\du`: `coldchain_app` and `coldchain_migrator` have no superuser attributes; `audit_history` owner is `coldchain_migrator`. Rollback-isolated tamper probe returned `tamper_detected=true`; afterward `scripts/audit-verify.sh` returned `Chain verified: 100 records`.
- CSS static check: one `:root` token block, no gradient declarations, no shadow declarations beyond the global `box-shadow:none` reset, IBM Plex Mono globally, and square 1px controls with the numbered-step marker as the only circular exception. Sampled colors and AA contrast are in `docs/design-tokens.md`.
- Built-in browser again returned: `Browser Use rejected this action due to browser security policy. Reason: A saved user permission setting blocks this action. Browser use could not acquire authority to observe page content. The origin approval gate did not grant access for http://localhost:3000. A saved permission or managed policy denies access.` No UI screenshots were captured and browser/e2e visual acceptance remains NOT PERFORMED. The policy explicitly disallowed Playwright/CDP as a workaround, so I stopped after that denial.

### Local verification of the workflow fix

- Ran the edited bootstrap from a clean temporary Git checkout with a fresh
  PostgreSQL 17 Docker service on `127.0.0.1:55432`, backend port 18080,
  per-run random credentials, no `data-pipeline/.env`, and a fresh output
  directory. The application reported healthy; all twelve Flyway migrations
  succeeded; demo data was created through the REST seed; dashboard, condition,
  issue, audit, and pipeline cross-checks passed; and the export manifest
  reported PASS for all datasets. PostgreSQL role checks showed the expected
  `coldchain_admin` superuser and non-superuser app, migrator, and analytics
  roles.
- In that checkout, `python3 -m pytest -q -rs -ra` passed all five tests with
  no skips. After removing only the temporary export directory, the same test
  command failed three output-dependent tests with explicit `FileNotFoundError`
  for `LATEST` (two independent tests passed), confirming absent output cannot
  silently pass. The temporary PostgreSQL container was stopped afterward.
- `mvn -q -f backend/pom.xml clean test` passed; `npm ci && npm run build`
  passed; both backend and frontend Docker image builds passed. `bash -n` passed
  for the two changed shell scripts, the workflow parsed as YAML, and
  `git diff --check` passed. ShellCheck was not completed because installing it
  in the Ubuntu container stalled while fetching apt metadata; no workflow or
  application code depends on ShellCheck.
- Backend Surefire reports show 23 tests total: 0 failures, 0 errors, 0 skips.
  Diagnostics isolation was rerun with unset variables, missing paths, and a
  synthetic log containing a dummy password, bearer token, and session cookie;
  all runs exited 0 and the visible log tail contained only redacted values.
  A changed-file scan found no literal credential patterns.
- The npm install reported four dependency audit advisories (three moderate,
  one high); package manifests/locks were not changed. This run did not access a
  GitHub-hosted runner or trigger an Actions run; the next hosted run remains
  the final confirmation against GitHub's live image rollout and action
  downloads.

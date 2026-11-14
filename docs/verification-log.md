# Verification log

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

# Audit hash chain

`audit_history.seq` is a unique ascending chain position. The genesis `prev_hash` is 64 ASCII zeroes. On every insert, a PostgreSQL `BEFORE INSERT` trigger serializes writers with transaction advisory lock `(731942, 22017)`, selects the current head, and overwrites `seq`, `prev_hash`, and `row_hash`. The digest is SHA-256 over UTF-8 `prev_hash || canonical_payload` using `pgcrypto`.

The canonical payload is PostgreSQL JSONB text built from fixed keys (`seq`, UTC timestamp at six fractional digits, actor, action, entity type/id, reason, category, before/after payload). JSONB gives stable key ordering; strings are escaped by PostgreSQL and nulls remain JSON null. Migration V9 backfills existing rows in timestamp/id order. An empty table starts with genesis directly.

`SELECT * FROM verify_audit_chain()` checks sequence continuity, predecessor links, and each digest. The global lock serializes audit inserts and is suitable for this single application scale, but adds write contention. The chain detects ordinary edits, deletions, and reordering. A database superuser who rewrites all rows from a point onward can create a new internally consistent suffix. `scripts/audit-head.sh` prints the head for storage outside this database; an independently retained, immutable external copy (for example, object-locked S3) closes that remaining gap.

# Database Migrations

All versioned database migrations are maintained in a single location:
`backend/src/main/resources/db/migration/`

- `V1__create_schema.sql`
- `V2__seed_storage_profiles.sql`

Flyway runs these migrations on application startup.

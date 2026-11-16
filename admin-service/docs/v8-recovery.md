# V8 startup failure: MySQL error 1215

The supplied startup log fails at the first ALTER TABLE in V8. Tomcat/API-key repository/Hikari failures are downstream of Flyway initialization failure.

Cause: V8 used a STORED generated tenant_scope based on tenant_id. MySQL prohibits cascading foreign-key actions on the base column of a stored generated column. Both field_definitions and lookup_categories now use VIRTUAL generated scope columns with the same unique indexes; existing foreign keys and data are preserved.

The subsequent log reported error 1060 for an already-existing field_definitions.tenant_scope. V8 now resumes partial or manually applied schema changes: it adds each missing column/index independently, validates an existing scope column's generated expression and BIGINT type, and establishes scoped uniqueness before removing the old global-only code index. It accepts equivalent unique indexes under other names. Incompatible columns and duplicate data still fail explicitly.

Stop the application, rebuild/refresh resources and restart with the corrected migration. The current FlywayRepairConfig already calls repair before migrate, which removes failed migration history entries. Prefer letting Flyway execute V8 rather than manually managing its history. Do not disable foreign-key checks or remove tenant foreign keys.

Operational requirement: the migration uses a temporary stored-procedure helper for IF/SIGNAL validation. The migration account needs CREATE ROUTINE, ALTER ROUTINE and EXECUTE permissions in addition to its schema-alter permissions. The helper is removed on success and recreated on retry after failure. This does not change tenant API or data-model semantics.

If V8 has already succeeded elsewhere, do not treat that database as the reported failed case. Inspect its Flyway history and SHOW CREATE TABLE output before changing a successful migration/checksum. Automatic repair on every startup also realigns checksums and merits a separate migration-governance review; it was not changed as part of this fix.

Validation: Database/tests/V8_scope_regression.ps1 ran against a disposable MySQL 8.0.46 instance. It reproduced the original STORED-column error, applied and repeated the actual corrected V8, resumed missing indexes and first-table-complete states, rejected an incompatible ordinary scope column, rejected duplicate global and same-tenant keys, allowed separate-tenant overrides, and preserved ON DELETE CASCADE. V8FlywayMigrationTest provides an opt-in real Flyway/MySQL test using V8_MYSQL_INTEGRATION=true and verifies the disposable datadir before any mutation. No application database was changed.

Reference: https://dev.mysql.com/doc/refman/8.0/en/create-table-generated-columns.html

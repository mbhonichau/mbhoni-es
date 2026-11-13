# V6 duplicate-column recovery

Error 1060 for `stripe_subscription_id` means that column already exists. It does not prove the remaining V6 columns, both unique indexes, or the receipt table exist. Hibernate `ddl-auto=update` is enabled by default in this repository and can create mapped columns before a manually executed migration.

The revised V6 checks every column separately, accepts equivalent single-column full unique indexes regardless of their names, and creates the receipt table only when absent. It does not drop columns, change stored mappings, delete receipts, or infer billing state. Existing incompatible column/table definitions still need inspection using the final SHOW CREATE TABLE results. Duplicate non-null Stripe IDs will prevent unique-index creation and require reviewed correction, not automatic deletion.

For the reported manual SQL execution:

1. Stop application writes and select the same application database.
2. Run the revised `src/main/resources/db/migration/V6__subscription_lifecycle.sql` in full, on one connection, with execution configured to stop on error.
3. Review both final table definitions. Preserve the script output and any error. Do not run the rollback script merely to remove the duplicate-column error.

If Flyway also manages this database, inspect its schema history before restarting. Manual SQL execution does not record a successful Flyway migration. A failed Flyway entry may require repair after inspecting the schema; repair itself only changes migration history, not application schema. If an older V6 is already recorded as successful, the revised file changes its checksum: review that separately instead of blindly repairing checksums or deleting history.

Validation: source review only for this change. No MySQL client/server or Docker executable was available in this session, and the script was not run against the user's database. Existing Java test results do not validate MySQL DDL.

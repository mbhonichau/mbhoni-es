-- Schema only; safe to resume after Hibernate schema updates or a partial manual run.
-- Select the application database and stop application writes before executing.
-- No existing columns, mappings, receipts or tenant data are removed or overwritten.
-- If this migration was already recorded as successful by Flyway, review its history
-- and checksum before deploying this revised file; do not blindly run repair.
SET @v6_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'tenant_subscriptions' AND column_name = 'stripe_subscription_id');
SET @v6_ddl = IF(@v6_exists = 0,
    'ALTER TABLE tenant_subscriptions ADD COLUMN stripe_subscription_id VARCHAR(255) NULL', 'SELECT 1');
PREPARE v6_statement FROM @v6_ddl;
EXECUTE v6_statement;
DEALLOCATE PREPARE v6_statement;
SET @v6_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'tenant_subscriptions' AND column_name = 'stripe_customer_id');
SET @v6_ddl = IF(@v6_exists = 0,
    'ALTER TABLE tenant_subscriptions ADD COLUMN stripe_customer_id VARCHAR(255) NULL', 'SELECT 1');
PREPARE v6_statement FROM @v6_ddl;
EXECUTE v6_statement;
DEALLOCATE PREPARE v6_statement;
SET @v6_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'tenant_subscriptions' AND column_name = 'state_changed_at');
SET @v6_ddl = IF(@v6_exists = 0,
    'ALTER TABLE tenant_subscriptions ADD COLUMN state_changed_at DATETIME(6) NULL', 'SELECT 1');
PREPARE v6_statement FROM @v6_ddl;
EXECUTE v6_statement;
DEALLOCATE PREPARE v6_statement;
SET @v6_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'tenant_subscriptions' AND column_name = 'grace_ends_at');
SET @v6_ddl = IF(@v6_exists = 0,
    'ALTER TABLE tenant_subscriptions ADD COLUMN grace_ends_at DATETIME(6) NULL', 'SELECT 1');
PREPARE v6_statement FROM @v6_ddl;
EXECUTE v6_statement;
DEALLOCATE PREPARE v6_statement;
SET @v6_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'tenant_subscriptions' AND column_name = 'retention_ends_at');
SET @v6_ddl = IF(@v6_exists = 0,
    'ALTER TABLE tenant_subscriptions ADD COLUMN retention_ends_at DATETIME(6) NULL', 'SELECT 1');
PREPARE v6_statement FROM @v6_ddl;
EXECUTE v6_statement;
DEALLOCATE PREPARE v6_statement;
-- Accept an equivalent full-column unique index even when Hibernate chose its name.
SET @v6_exists = (SELECT COUNT(*) FROM (
    SELECT index_name FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'tenant_subscriptions'
    GROUP BY index_name
    HAVING COUNT(*) = 1 AND MAX(non_unique) = 0
       AND MAX(column_name) = 'stripe_subscription_id' AND MAX(sub_part) IS NULL
) AS v6_unique_indexes);
SET @v6_ddl = IF(@v6_exists = 0,
    'ALTER TABLE tenant_subscriptions ADD CONSTRAINT uk_stripe_subscription UNIQUE (stripe_subscription_id)', 'SELECT 1');
PREPARE v6_statement FROM @v6_ddl;
EXECUTE v6_statement;
DEALLOCATE PREPARE v6_statement;
-- Accept an equivalent full-column unique index even when Hibernate chose its name.
SET @v6_exists = (SELECT COUNT(*) FROM (
    SELECT index_name FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'tenant_subscriptions'
    GROUP BY index_name
    HAVING COUNT(*) = 1 AND MAX(non_unique) = 0
       AND MAX(column_name) = 'stripe_customer_id' AND MAX(sub_part) IS NULL
) AS v6_unique_indexes);
SET @v6_ddl = IF(@v6_exists = 0,
    'ALTER TABLE tenant_subscriptions ADD CONSTRAINT uk_stripe_customer UNIQUE (stripe_customer_id)', 'SELECT 1');
PREPARE v6_statement FROM @v6_ddl;
EXECUTE v6_statement;
DEALLOCATE PREPARE v6_statement;
CREATE TABLE IF NOT EXISTS stripe_webhook_receipts (
    event_id VARCHAR(255) PRIMARY KEY,
    processed_at DATETIME(6) NOT NULL,
    outcome VARCHAR(80) NOT NULL
);

-- Inspect these definitions after execution, especially for pre-existing objects.
SHOW CREATE TABLE tenant_subscriptions;
SHOW CREATE TABLE stripe_webhook_receipts;

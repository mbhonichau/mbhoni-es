-- Schema only. Some installations previously acquired this field through Hibernate update.
-- Do not infer or backfill trial deadlines from created_at or deployment time.
SET @trial_deadline_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'tenant_subscriptions' AND column_name = 'trial_ends_at');
SET @trial_deadline_ddl = IF(@trial_deadline_exists = 0,
    'ALTER TABLE tenant_subscriptions ADD COLUMN trial_ends_at DATE NULL', 'SELECT 1');
PREPARE trial_deadline_statement FROM @trial_deadline_ddl;
EXECUTE trial_deadline_statement;
DEALLOCATE PREPARE trial_deadline_statement;

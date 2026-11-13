-- Stop the new application first; export these columns and the receipts table.
-- Do not run automatically: removing receipt history loses replay protection.
-- Resumable V6 can reuse pre-existing columns, indexes and the receipts table.
-- Only remove objects proven to have been created by this deployment; preserve
-- pre-existing Hibernate objects. The statements below are for a fresh V6 only.
DROP TABLE stripe_webhook_receipts;
ALTER TABLE tenant_subscriptions DROP INDEX uk_stripe_subscription, DROP INDEX uk_stripe_customer,
    DROP COLUMN stripe_subscription_id, DROP COLUMN stripe_customer_id,
    DROP COLUMN state_changed_at, DROP COLUMN grace_ends_at, DROP COLUMN retention_ends_at;

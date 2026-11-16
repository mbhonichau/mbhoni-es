-- Stop the application and export these columns before rollback. No legacy data is rewritten.
ALTER TABLE tenant_subscriptions
    DROP COLUMN stripe_items_json,
    DROP COLUMN cancel_at_period_end,
    DROP COLUMN current_period_end,
    DROP COLUMN provider_synced_at,
    DROP COLUMN provider_status;

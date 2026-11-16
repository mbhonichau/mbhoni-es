-- Schema only. Existing NULL snapshots are populated only by verified Stripe reads.
ALTER TABLE tenant_subscriptions
    ADD COLUMN stripe_items_json TEXT NULL,
    ADD COLUMN cancel_at_period_end BOOLEAN NULL,
    ADD COLUMN current_period_end DATETIME(6) NULL,
    ADD COLUMN provider_synced_at DATETIME(6) NULL,
    ADD COLUMN provider_status VARCHAR(255) NULL;

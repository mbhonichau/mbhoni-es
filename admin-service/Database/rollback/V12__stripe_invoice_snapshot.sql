-- Export provider-backed invoice rows first; stop the application before rollback.
-- Do not turn provider invoices into editable legacy rows: archive/delete imported rows only after a verified export.
-- This schema rollback deliberately leaves all invoice rows intact.
ALTER TABLE invoices DROP INDEX uq_invoice_stripe_id,
    DROP COLUMN stripe_invoice_id, DROP COLUMN currency, DROP COLUMN provider_amount_remaining,
    DROP COLUMN hosted_invoice_url, DROP COLUMN stripe_invoice_json;

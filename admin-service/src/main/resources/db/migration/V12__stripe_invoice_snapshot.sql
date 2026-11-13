-- Schema only; provider identity cannot safely be inferred for legacy invoices.
-- V1 already creates currency. Preserve its definition and existing invoice values.
ALTER TABLE invoices
    ADD COLUMN stripe_invoice_id VARCHAR(255) NULL,
    ADD COLUMN provider_amount_remaining DECIMAL(38,2) NULL,
    ADD COLUMN hosted_invoice_url VARCHAR(2048) NULL,
    ADD COLUMN stripe_invoice_json LONGTEXT NULL,
    ADD CONSTRAINT uq_invoice_stripe_id UNIQUE (stripe_invoice_id);

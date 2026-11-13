-- Additive only. Existing owners must be explicitly assigned, not inferred from role/order.
ALTER TABLE tenants ADD COLUMN owner_user_id BIGINT NULL,
    ADD CONSTRAINT uk_tenant_owner UNIQUE (owner_user_id),
    ADD CONSTRAINT fk_tenant_owner FOREIGN KEY (owner_user_id) REFERENCES users(id);

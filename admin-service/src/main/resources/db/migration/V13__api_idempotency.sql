-- Phase 3a prepares storage only. No endpoint writes these rows until Phase 3b.
-- Binary keys preserve exact, case-sensitive ASCII identity on MySQL and H2.
CREATE TABLE api_idempotency (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    api_key_id BIGINT NOT NULL,
    idempotency_key VARBINARY(128) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    response_status INTEGER NULL,
    response_body LONGTEXT NULL,
    response_location VARCHAR(2048) NULL,
    resource_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    CONSTRAINT uq_api_idempotency_tenant_key UNIQUE (tenant_id, idempotency_key),
    CONSTRAINT fk_api_idempotency_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);
CREATE INDEX idx_api_idempotency_expiry ON api_idempotency(expires_at);

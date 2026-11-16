CREATE TABLE api_document_sequence (
    tenant_id BIGINT NOT NULL,
    document_type VARCHAR(16) NOT NULL,
    next_value BIGINT NOT NULL,
    CONSTRAINT uq_api_document_sequence UNIQUE (tenant_id, document_type),
    CONSTRAINT ck_api_document_type CHECK (document_type IN ('INVOICE', 'QUOTATION')),
    CONSTRAINT fk_api_document_sequence_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

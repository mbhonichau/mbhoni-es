USE mbhoni_es;

-- =========================================================
-- ALTER TENANTS TABLE FOR ENTERPRISE GOVERNANCE
-- =========================================================

ALTER TABLE tenants ADD COLUMN tenant_code VARCHAR(100);
ALTER TABLE tenants ADD COLUMN isolation_strategy VARCHAR(50) DEFAULT 'SHARED_DATABASE';
ALTER TABLE tenants ADD COLUMN primary_region VARCHAR(50) DEFAULT 'us-east-1';
ALTER TABLE tenants ADD COLUMN status VARCHAR(50) DEFAULT 'ACTIVE';
ALTER TABLE tenants ADD COLUMN data_retention_days INT DEFAULT 365;

-- =========================================================
-- MASTER DATA MANAGEMENT TABLES
-- =========================================================

CREATE TABLE IF NOT EXISTS lookup_categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    is_system BIT(1) NOT NULL DEFAULT b'0',
    tenant_id BIGINT,
    created_at DATETIME(6),
    created_by VARCHAR(255),
    updated_at DATETIME(6),
    updated_by VARCHAR(255),
    PRIMARY KEY (id),
    UNIQUE KEY uk_lookup_categories_code (code),
    KEY idx_lookup_categories_tenant (tenant_id),
    CONSTRAINT fk_lookup_categories_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenants (id)
);

CREATE TABLE IF NOT EXISTS lookup_codes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    code VARCHAR(100) NOT NULL,
    value VARCHAR(255) NOT NULL,
    locale VARCHAR(20) DEFAULT 'en_US',
    display_order INT DEFAULT 0,
    active BIT(1) NOT NULL DEFAULT b'1',
    metadata TEXT,
    created_at DATETIME(6),
    created_by VARCHAR(255),
    updated_at DATETIME(6),
    updated_by VARCHAR(255),
    PRIMARY KEY (id),
    KEY idx_lookup_codes_category (category_id),
    CONSTRAINT fk_lookup_codes_category
        FOREIGN KEY (category_id)
        REFERENCES lookup_categories (id)
);

-- =========================================================
-- CONTRACT MANAGEMENT TABLES
-- =========================================================

CREATE TABLE IF NOT EXISTS contracts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT,
    contract_number VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    contract_type VARCHAR(50) NOT NULL DEFAULT 'TENANT_SLA',
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_value DECIMAL(19, 2),
    currency VARCHAR(10) DEFAULT 'USD',
    auto_renew BIT(1) NOT NULL DEFAULT b'0',
    notice_period_days INT DEFAULT 30,
    document_url VARCHAR(500),
    terms_and_conditions TEXT,
    created_at DATETIME(6),
    created_by VARCHAR(255),
    updated_at DATETIME(6),
    updated_by VARCHAR(255),
    PRIMARY KEY (id),
    UNIQUE KEY uk_contracts_number (contract_number),
    KEY idx_contracts_tenant (tenant_id),
    CONSTRAINT fk_contracts_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenants (id)
);

CREATE TABLE IF NOT EXISTS contract_milestones (
    id BIGINT NOT NULL AUTO_INCREMENT,
    contract_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    due_date DATE NOT NULL,
    amount DECIMAL(19, 2),
    status VARCHAR(50) DEFAULT 'PENDING',
    completion_date DATE,
    created_at DATETIME(6),
    created_by VARCHAR(255),
    updated_at DATETIME(6),
    updated_by VARCHAR(255),
    PRIMARY KEY (id),
    KEY idx_milestones_contract (contract_id),
    CONSTRAINT fk_milestones_contract
        FOREIGN KEY (contract_id)
        REFERENCES contracts (id)
);

-- =========================================================
-- ORGANIZATION MANAGEMENT TABLE
-- =========================================================

CREATE TABLE IF NOT EXISTS organization_units (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    unit_code VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    unit_type VARCHAR(50) NOT NULL,
    parent_unit_id BIGINT,
    cost_center VARCHAR(50),
    location VARCHAR(150),
    created_at DATETIME(6),
    created_by VARCHAR(255),
    updated_at DATETIME(6),
    updated_by VARCHAR(255),
    PRIMARY KEY (id),
    KEY idx_org_units_tenant (tenant_id),
    KEY idx_org_units_parent (parent_unit_id),
    CONSTRAINT fk_org_units_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenants (id),
    CONSTRAINT fk_org_units_parent
        FOREIGN KEY (parent_unit_id)
        REFERENCES organization_units (id)
);

-- =========================================================
-- EMPLOYEE MANAGEMENT TABLE
-- =========================================================

CREATE TABLE IF NOT EXISTS employees (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    employee_number VARCHAR(100) NOT NULL,
    user_id BIGINT,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    work_phone VARCHAR(50),
    job_title VARCHAR(150),
    organization_unit_id BIGINT,
    manager_employee_id BIGINT,
    cost_center VARCHAR(50),
    employment_type VARCHAR(50) NOT NULL DEFAULT 'FULL_TIME',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    hire_date DATE,
    termination_date DATE,
    created_at DATETIME(6),
    created_by VARCHAR(255),
    updated_at DATETIME(6),
    updated_by VARCHAR(255),
    PRIMARY KEY (id),
    UNIQUE KEY uk_employees_number (employee_number),
    KEY idx_employees_tenant (tenant_id),
    KEY idx_employees_user (user_id),
    KEY idx_employees_org_unit (organization_unit_id),
    KEY idx_employees_manager (manager_employee_id),
    CONSTRAINT fk_employees_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenants (id),
    CONSTRAINT fk_employees_user
        FOREIGN KEY (user_id)
        REFERENCES users (id),
    CONSTRAINT fk_employees_org_unit
        FOREIGN KEY (organization_unit_id)
        REFERENCES organization_units (id),
    CONSTRAINT fk_employees_manager
        FOREIGN KEY (manager_employee_id)
        REFERENCES employees (id)
);

-- =========================================================
-- SERVICE CATALOG & SLA TABLES
-- =========================================================

CREATE TABLE IF NOT EXISTS service_catalogs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    service_code VARCHAR(100) NOT NULL,
    service_name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    category VARCHAR(50),
    owner_team VARCHAR(100),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    health_endpoint VARCHAR(255),
    created_at DATETIME(6),
    created_by VARCHAR(255),
    updated_at DATETIME(6),
    updated_by VARCHAR(255),
    PRIMARY KEY (id),
    UNIQUE KEY uk_service_catalogs_code (service_code)
);

CREATE TABLE IF NOT EXISTS service_slas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    service_catalog_id BIGINT NOT NULL,
    tier VARCHAR(50) NOT NULL DEFAULT 'STANDARD',
    target_uptime_percentage DECIMAL(5, 2) DEFAULT 99.90,
    max_latency_ms INT DEFAULT 500,
    support_response_hours INT DEFAULT 24,
    created_at DATETIME(6),
    created_by VARCHAR(255),
    updated_at DATETIME(6),
    updated_by VARCHAR(255),
    PRIMARY KEY (id),
    KEY idx_slas_service_catalog (service_catalog_id),
    CONSTRAINT fk_slas_service_catalog
        FOREIGN KEY (service_catalog_id)
        REFERENCES service_catalogs (id)
);

-- =========================================================
-- TENANT QUOTA TABLE
-- =========================================================

CREATE TABLE IF NOT EXISTS tenant_quotas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    max_users INT DEFAULT 50,
    max_api_requests_per_minute INT DEFAULT 1000,
    max_storage_gb INT DEFAULT 100,
    custom_quotas_json TEXT,
    created_at DATETIME(6),
    created_by VARCHAR(255),
    updated_at DATETIME(6),
    updated_by VARCHAR(255),
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_quotas_tenant (tenant_id),
    CONSTRAINT fk_tenant_quotas_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenants (id)
);

-- =========================================================
-- SEED ENTERPRISE PERMISSIONS
-- =========================================================

INSERT INTO permissions (name) VALUES
('MASTER_DATA_VIEW'),
('MASTER_DATA_EDIT'),
('CONTRACT_VIEW'),
('CONTRACT_EDIT'),
('EMPLOYEE_VIEW'),
('EMPLOYEE_EDIT'),
('SERVICE_VIEW'),
('SERVICE_EDIT'),
('ORG_VIEW'),
('ORG_EDIT'),
('TENANT_EDIT')
ON DUPLICATE KEY UPDATE name = VALUES(name);

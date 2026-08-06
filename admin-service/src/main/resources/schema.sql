-- =============================================================================
-- MBHONI-ES ADMIN SERVICE - AUTOMATIC DATABASE SCHEMA & TABLE INITIALIZER
-- Target: MySQL 8.0+ / MariaDB / PostgreSQL compatible SQL
-- Automatically creates any missing database tables on application startup.
-- =============================================================================

-- 1. ALTER STATEMENTS FOR EXISTING TABLES (Safe execution)
ALTER TABLE `users` 
    ADD COLUMN IF NOT EXISTS `phone_number` VARCHAR(50) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS `avatar_url` VARCHAR(500) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS `auth_provider` VARCHAR(50) NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN IF NOT EXISTS `provider_id` VARCHAR(255) DEFAULT NULL;

-- 2. FULL DDL TABLE CREATION SCRIPTS (IF NOT EXISTS)

-- Tenants
CREATE TABLE IF NOT EXISTS `tenants` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(255) NOT NULL,
    `active` BOOLEAN NOT NULL DEFAULT TRUE,
    `tenant_code` VARCHAR(100) UNIQUE,
    `isolation_strategy` VARCHAR(50) DEFAULT 'SHARED_DATABASE',
    `primary_region` VARCHAR(50) DEFAULT 'us-east-1',
    `status` VARCHAR(50) DEFAULT 'ACTIVE',
    `data_retention_days` INT DEFAULT 365,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Industry Profiles
CREATE TABLE IF NOT EXISTS `industry_profiles` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(80) NOT NULL UNIQUE,
    `name` VARCHAR(120) NOT NULL,
    `description` VARCHAR(500),
    `active` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Permissions
CREATE TABLE IF NOT EXISTS `permissions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Roles
CREATE TABLE IF NOT EXISTS `roles` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `description` VARCHAR(255),
    `tenant_id` BIGINT,
    `system_role` BOOLEAN NOT NULL DEFAULT FALSE,
    `is_system_role` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Role Permissions Join Table
CREATE TABLE IF NOT EXISTS `role_permissions` (
    `role_id` BIGINT NOT NULL,
    `permission_id` BIGINT NOT NULL,
    PRIMARY KEY (`role_id`, `permission_id`),
    FOREIGN KEY (`role_id`) REFERENCES `roles`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`permission_id`) REFERENCES `permissions`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Users
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(100) NOT NULL UNIQUE,
    `email` VARCHAR(150) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `first_name` VARCHAR(100),
    `last_name` VARCHAR(100),
    `phone_number` VARCHAR(50),
    `avatar_url` VARCHAR(500),
    `department` VARCHAR(100),
    `job_title` VARCHAR(100),
    `time_zone` VARCHAR(100) DEFAULT 'Africa/Johannesburg',
    `auth_provider` VARCHAR(50) NOT NULL DEFAULT 'LOCAL',
    `provider_id` VARCHAR(255),
    `active` BOOLEAN NOT NULL DEFAULT TRUE,
    `is_global_admin` BOOLEAN NOT NULL DEFAULT FALSE,
    `password_change_required` BOOLEAN NOT NULL DEFAULT FALSE,
    `mfa_enforced` BOOLEAN NOT NULL DEFAULT FALSE,
    `sso_enforced` BOOLEAN NOT NULL DEFAULT FALSE,
    `api_access_allowed` BOOLEAN NOT NULL DEFAULT TRUE,
    `audit_extended` BOOLEAN NOT NULL DEFAULT FALSE,
    `password_reset_token` VARCHAR(100),
    `password_reset_token_expiry` DATETIME,
    `tenant_id` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- User Roles Join Table
CREATE TABLE IF NOT EXISTS `user_roles` (
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    PRIMARY KEY (`user_id`, `role_id`),
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`role_id`) REFERENCES `roles`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Platform Modules
CREATE TABLE IF NOT EXISTS `platform_modules` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(80) NOT NULL UNIQUE,
    `name` VARCHAR(120) NOT NULL,
    `category` VARCHAR(100),
    `description` VARCHAR(500),
    `active` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Subscription Plans
CREATE TABLE IF NOT EXISTS `subscription_plans` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(50) NOT NULL UNIQUE,
    `name` VARCHAR(100) NOT NULL,
    `description` VARCHAR(255),
    `monthly_price` DECIMAL(10, 2) NOT NULL,
    `annual_price` DECIMAL(10, 2) NOT NULL,
    `max_users` INT NOT NULL DEFAULT 10,
    `max_storage_mb` INT NOT NULL DEFAULT 5000,
    `api_access_enabled` BOOLEAN NOT NULL DEFAULT TRUE,
    `branding_enabled` BOOLEAN NOT NULL DEFAULT FALSE,
    `custom_domain_enabled` BOOLEAN NOT NULL DEFAULT FALSE,
    `active` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Subscription Plan Modules
CREATE TABLE IF NOT EXISTS `subscription_plan_modules` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `plan_id` BIGINT NOT NULL,
    `module_id` BIGINT NOT NULL,
    `allowed` BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (`plan_id`) REFERENCES `subscription_plans`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`module_id`) REFERENCES `platform_modules`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_plan_module` (`plan_id`, `module_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tenant Subscriptions
CREATE TABLE IF NOT EXISTS `tenant_subscriptions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL UNIQUE,
    `plan_id` BIGINT NOT NULL,
    `status` VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    `billing_cycle` VARCHAR(50) NOT NULL DEFAULT 'MONTHLY',
    `auto_renew` BOOLEAN NOT NULL DEFAULT TRUE,
    `start_date` DATETIME,
    `end_date` DATETIME,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`plan_id`) REFERENCES `subscription_plans`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tenant Modules
CREATE TABLE IF NOT EXISTS `tenant_modules` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL,
    `module_id` BIGINT NOT NULL,
    `enabled` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`module_id`) REFERENCES `platform_modules`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_tenant_module` (`tenant_id`, `module_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Organization Units
CREATE TABLE IF NOT EXISTS `organization_units` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL,
    `unit_code` VARCHAR(100) NOT NULL,
    `name` VARCHAR(150) NOT NULL,
    `unit_type` VARCHAR(50) NOT NULL,
    `parent_unit_id` BIGINT,
    `cost_center` VARCHAR(50),
    `location` VARCHAR(150),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`parent_unit_id`) REFERENCES `organization_units`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Employees (HCM)
CREATE TABLE IF NOT EXISTS `employees` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL,
    `employee_number` VARCHAR(100) NOT NULL UNIQUE,
    `user_id` BIGINT,
    `first_name` VARCHAR(100) NOT NULL,
    `last_name` VARCHAR(100) NOT NULL,
    `email` VARCHAR(150) NOT NULL,
    `work_phone` VARCHAR(50),
    `job_title` VARCHAR(150),
    `organization_unit_id` BIGINT,
    `manager_employee_id` BIGINT,
    `cost_center` VARCHAR(50),
    `employment_type` VARCHAR(50) NOT NULL DEFAULT 'FULL_TIME',
    `status` VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    `hire_date` DATE,
    `termination_date` DATE,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`organization_unit_id`) REFERENCES `organization_units`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`manager_employee_id`) REFERENCES `employees`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Contracts
CREATE TABLE IF NOT EXISTS `contracts` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `contract_number` VARCHAR(100) NOT NULL,
    `vendor_name` VARCHAR(150),
    `contract_type` VARCHAR(50) NOT NULL DEFAULT 'SERVICE',
    `status` VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    `value` DECIMAL(12, 2),
    `start_date` DATE,
    `end_date` DATE,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Contract Milestones
CREATE TABLE IF NOT EXISTS `contract_milestones` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `contract_id` BIGINT NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `due_date` DATE,
    `completed` BOOLEAN NOT NULL DEFAULT FALSE,
    `amount` DECIMAL(12, 2),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`contract_id`) REFERENCES `contracts`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Lookup Categories & Codes
CREATE TABLE IF NOT EXISTS `lookup_categories` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tenant_id` BIGINT,
    `code` VARCHAR(100) NOT NULL,
    `name` VARCHAR(150) NOT NULL,
    `description` VARCHAR(500),
    `system_defined` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `lookup_codes` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `category_id` BIGINT NOT NULL,
    `code` VARCHAR(100) NOT NULL,
    `display_label` VARCHAR(150) NOT NULL,
    `sort_order` INT DEFAULT 0,
    `active` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`category_id`) REFERENCES `lookup_categories`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Service Catalog & SLAs
CREATE TABLE IF NOT EXISTS `service_catalog` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `service_code` VARCHAR(100) NOT NULL UNIQUE,
    `service_name` VARCHAR(150) NOT NULL,
    `description` VARCHAR(500),
    `category` VARCHAR(50),
    `owner_team` VARCHAR(100),
    `status` VARCHAR(50) DEFAULT 'ACTIVE',
    `health_endpoint` VARCHAR(255),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `service_slas` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `service_catalog_id` BIGINT NOT NULL,
    `sla_name` VARCHAR(150) NOT NULL,
    `target_availability_percentage` DOUBLE,
    `max_response_time_ms` INT,
    `support_hours` VARCHAR(50),
    `penalty_clause` VARCHAR(500),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`service_catalog_id`) REFERENCES `service_catalog`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Billing Accounts, Invoices & Payments
CREATE TABLE IF NOT EXISTS `billing_accounts` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL UNIQUE,
    `account_number` VARCHAR(100) UNIQUE,
    `billing_email` VARCHAR(150) NOT NULL,
    `company_name` VARCHAR(200),
    `tax_number` VARCHAR(100),
    `currency` VARCHAR(10) NOT NULL DEFAULT 'USD',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `invoices` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL,
    `subscription_id` BIGINT,
    `invoice_number` VARCHAR(100) NOT NULL UNIQUE,
    `amount` DECIMAL(10, 2) NOT NULL,
    `tax_amount` DECIMAL(10, 2) DEFAULT 0.00,
    `total_amount` DECIMAL(10, 2) NOT NULL,
    `currency` VARCHAR(10) DEFAULT 'ZAR',
    `status` VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    `issue_date` DATE NOT NULL,
    `due_date` DATE NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `payments` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `invoice_id` BIGINT NOT NULL,
    `payment_method` VARCHAR(40) NOT NULL,
    `status` VARCHAR(40) NOT NULL DEFAULT 'SUCCESSFUL',
    `amount` DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    `paid_at` DATETIME,
    `reference` VARCHAR(255),
    `notes` VARCHAR(500),
    `payment_provider` VARCHAR(60) DEFAULT 'MANUAL',
    `provider_payment_id` VARCHAR(255),
    `provider_status` VARCHAR(255),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`invoice_id`) REFERENCES `invoices`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tenant Customizations (Branding)
CREATE TABLE IF NOT EXISTS `tenant_customizations` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL UNIQUE,
    `logo_url` VARCHAR(500),
    `primary_color` VARCHAR(50) DEFAULT '#0d6efd',
    `secondary_color` VARCHAR(50) DEFAULT '#6c757d',
    `theme_mode` VARCHAR(20) DEFAULT 'LIGHT',
    `custom_domain` VARCHAR(255),
    `payslip_template_html` LONGTEXT,
    `payslip_excel_template` LONGBLOB,
    `payslip_excel_file_name` VARCHAR(255),
    `payslip_engine_type` VARCHAR(20) DEFAULT 'EXCEL',
    `payslip_word_template` LONGBLOB,
    `payslip_word_file_name` VARCHAR(255),
    `invoice_engine_type` VARCHAR(20) DEFAULT 'EXCEL',
    `invoice_excel_template` LONGBLOB,
    `invoice_excel_file_name` VARCHAR(255),
    `invoice_word_template` LONGBLOB,
    `invoice_word_file_name` VARCHAR(255),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tenant Quotas
CREATE TABLE IF NOT EXISTS `tenant_quotas` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL UNIQUE,
    `max_users` INT DEFAULT 50,
    `max_api_requests_per_minute` INT DEFAULT 1000,
    `max_storage_gb` INT DEFAULT 100,
    `custom_quotas_json` TEXT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tenant API Keys & Permissions
CREATE TABLE IF NOT EXISTS `tenant_api_keys` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL,
    `name` VARCHAR(120) NOT NULL,
    `key_prefix` VARCHAR(24) NOT NULL,
    `key_hash` VARCHAR(64) NOT NULL UNIQUE,
    `active` BOOLEAN NOT NULL DEFAULT TRUE,
    `expires_at` DATETIME,
    `last_used_at` DATETIME,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE,
    INDEX `idx_tenant_api_keys_key_prefix` (`key_prefix`),
    INDEX `idx_tenant_api_keys_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `tenant_api_key_permissions` (
    `api_key_id` BIGINT NOT NULL,
    `permission_id` BIGINT NOT NULL,
    PRIMARY KEY (`api_key_id`, `permission_id`),
    FOREIGN KEY (`api_key_id`) REFERENCES `tenant_api_keys`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`permission_id`) REFERENCES `permissions`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tenant Content & Metrics
CREATE TABLE IF NOT EXISTS `tenant_content` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL,
    `content_name` VARCHAR(255),
    `content_type` VARCHAR(100),
    `ip_address` VARCHAR(100),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tenant Invoices & Items
CREATE TABLE IF NOT EXISTS `tenant_invoices` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL,
    `invoice_number` VARCHAR(80) NOT NULL,
    `customer_name` VARCHAR(255),
    `customer_email` VARCHAR(255),
    `customer_phone` VARCHAR(50),
    `customer_address` VARCHAR(500),
    `customer_tax_number` VARCHAR(80),
    `status` VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
    `issue_date` DATE,
    `due_date` DATE,
    `subtotal` DECIMAL(12,2) DEFAULT 0.00,
    `tax_amount` DECIMAL(12,2) DEFAULT 0.00,
    `total_amount` DECIMAL(12,2) DEFAULT 0.00,
    `amount_paid` DECIMAL(12,2) DEFAULT 0.00,
    `notes` TEXT,
    `payment_terms` VARCHAR(500),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `tenant_invoice_items` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `invoice_id` BIGINT NOT NULL,
    `description` VARCHAR(255) NOT NULL,
    `quantity` DECIMAL(12,2) NOT NULL DEFAULT 1.00,
    `unit_price` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `tax_rate` DECIMAL(5,2) NOT NULL DEFAULT 15.00,
    `total_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`invoice_id`) REFERENCES `tenant_invoices`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tenant Quotations & Items
CREATE TABLE IF NOT EXISTS `tenant_quotations` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL,
    `quotation_number` VARCHAR(80) NOT NULL,
    `customer_name` VARCHAR(255),
    `customer_email` VARCHAR(255),
    `customer_phone` VARCHAR(50),
    `customer_address` VARCHAR(500),
    `status` VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
    `issue_date` DATE,
    `valid_until_date` DATE,
    `subtotal` DECIMAL(12,2) DEFAULT 0.00,
    `tax_amount` DECIMAL(12,2) DEFAULT 0.00,
    `total_amount` DECIMAL(12,2) DEFAULT 0.00,
    `notes` TEXT,
    `converted_invoice_id` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `tenant_quotation_items` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `quotation_id` BIGINT NOT NULL,
    `description` VARCHAR(255) NOT NULL,
    `quantity` DECIMAL(12,2) NOT NULL DEFAULT 1.00,
    `unit_price` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `tax_rate` DECIMAL(5,2) NOT NULL DEFAULT 15.00,
    `total_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`quotation_id`) REFERENCES `tenant_quotations`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `tenant_metrics` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL,
    `metric_type` VARCHAR(100),
    `metric_value` DOUBLE,
    `recorded_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Audit Logs
CREATE TABLE IF NOT EXISTS `audit_logs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tenant_id` BIGINT,
    `username` VARCHAR(150),
    `actor_type` VARCHAR(50) NOT NULL DEFAULT 'USER',
    `action` VARCHAR(100) NOT NULL,
    `resource_type` VARCHAR(100) NOT NULL,
    `resource_id` VARCHAR(100),
    `description` VARCHAR(1000),
    `ip_address` VARCHAR(100),
    `user_agent` VARCHAR(500),
    `success` BOOLEAN NOT NULL DEFAULT TRUE,
    `occurred_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

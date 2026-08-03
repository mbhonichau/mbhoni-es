-- =============================================================================
-- MBHONI-ES ADMIN SERVICE - DATABASE SCHEMA & MIGRATION SCRIPT
-- Database Target: MySQL 8.0+ / MariaDB / PostgreSQL compatible SQL
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. ALTER STATEMENTS FOR EXISTING TABLES (New Fields Added to 'users')
-- -----------------------------------------------------------------------------

ALTER TABLE `users` 
    ADD COLUMN IF NOT EXISTS `phone_number` VARCHAR(50) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS `avatar_url` VARCHAR(500) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS `auth_provider` VARCHAR(50) NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN IF NOT EXISTS `provider_id` VARCHAR(255) DEFAULT NULL;

-- -----------------------------------------------------------------------------
-- 2. FULL DDL TABLE CREATION SCRIPTS (IF NOT EXISTS)
-- -----------------------------------------------------------------------------

-- Tenants
CREATE TABLE IF NOT EXISTS `tenants` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(255) NOT NULL,
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
    `is_system_role` BOOLEAN NOT NULL DEFAULT FALSE,
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
    `auth_provider` VARCHAR(50) NOT NULL DEFAULT 'LOCAL',
    `provider_id` VARCHAR(255),
    `active` BOOLEAN NOT NULL DEFAULT TRUE,
    `is_global_admin` BOOLEAN NOT NULL DEFAULT FALSE,
    `password_change_required` BOOLEAN NOT NULL DEFAULT FALSE,
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
    `code` VARCHAR(50) NOT NULL UNIQUE,
    `name` VARCHAR(100) NOT NULL,
    `category` VARCHAR(50) NOT NULL,
    `description` VARCHAR(255),
    `active` BOOLEAN NOT NULL DEFAULT TRUE
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
    `active` BOOLEAN NOT NULL DEFAULT TRUE
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
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`plan_id`) REFERENCES `subscription_plans`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tenant Modules
CREATE TABLE IF NOT EXISTS `tenant_modules` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL,
    `module_id` BIGINT NOT NULL,
    `enabled` BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`module_id`) REFERENCES `platform_modules`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_tenant_module` (`tenant_id`, `module_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Organization Units
CREATE TABLE IF NOT EXISTS `organization_units` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL,
    `name` VARCHAR(150) NOT NULL,
    `code` VARCHAR(50),
    `parent_unit_id` BIGINT,
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`parent_unit_id`) REFERENCES `organization_units`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Employees (HCM)
CREATE TABLE IF NOT EXISTS `employees` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL,
    `employee_number` VARCHAR(50) NOT NULL,
    `first_name` VARCHAR(100) NOT NULL,
    `last_name` VARCHAR(100) NOT NULL,
    `email` VARCHAR(150) NOT NULL,
    `job_title` VARCHAR(100),
    `org_unit_id` BIGINT,
    `manager_id` BIGINT,
    `employment_type` VARCHAR(50) NOT NULL DEFAULT 'FULL_TIME',
    `status` VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    `hire_date` DATE,
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`org_unit_id`) REFERENCES `organization_units`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`manager_id`) REFERENCES `employees`(`id`) ON DELETE SET NULL
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
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Master Data Categories & Codes
CREATE TABLE IF NOT EXISTS `master_data_categories` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL,
    `code` VARCHAR(50) NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `description` VARCHAR(255),
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `master_data_codes` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `category_id` BIGINT NOT NULL,
    `code` VARCHAR(50) NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `active` BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (`category_id`) REFERENCES `master_data_categories`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Service Catalog & SLAs
CREATE TABLE IF NOT EXISTS `service_catalog` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL,
    `name` VARCHAR(150) NOT NULL,
    `description` TEXT,
    `category` VARCHAR(100),
    `active` BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `service_slas` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `service_id` BIGINT NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `target_hours` INT NOT NULL,
    `priority` VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    FOREIGN KEY (`service_id`) REFERENCES `service_catalog`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Billing Accounts & Invoices
CREATE TABLE IF NOT EXISTS `billing_accounts` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL UNIQUE,
    `billing_email` VARCHAR(150) NOT NULL,
    `company_name` VARCHAR(200),
    `tax_number` VARCHAR(100),
    `currency` VARCHAR(10) NOT NULL DEFAULT 'USD',
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `invoices` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL,
    `invoice_number` VARCHAR(100) NOT NULL UNIQUE,
    `amount` DECIMAL(10, 2) NOT NULL,
    `status` VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    `due_date` DATE NOT NULL,
    `issued_date` DATE NOT NULL,
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE
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
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tenant API Keys
CREATE TABLE IF NOT EXISTS `tenant_api_keys` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `key_prefix` VARCHAR(20) NOT NULL,
    `hashed_key` VARCHAR(255) NOT NULL,
    `active` BOOLEAN NOT NULL DEFAULT TRUE,
    `expires_at` DATETIME,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Audit Logs
CREATE TABLE IF NOT EXISTS `audit_logs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tenant_id` BIGINT,
    `actor_username` VARCHAR(100),
    `action` VARCHAR(100) NOT NULL,
    `entity_type` VARCHAR(100),
    `entity_id` VARCHAR(100),
    `details` TEXT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

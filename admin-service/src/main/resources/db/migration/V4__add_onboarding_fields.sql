-- =============================================================================
-- MBHONI-ES ADMIN SERVICE - V4 MIGRATION
-- Industry-Tailored Dynamic Onboarding & Registration Field Schema Engine
-- MySQL 8.0 / MariaDB / PostgreSQL compatible SQL
-- =============================================================================

-- 1. Create Tenant Onboarding Fields Table
CREATE TABLE IF NOT EXISTS `tenant_onboarding_fields` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL,
    `target_entity` VARCHAR(30) NOT NULL DEFAULT 'EMPLOYEE',
    `field_key` VARCHAR(80) NOT NULL,
    `field_label` VARCHAR(120) NOT NULL,
    `field_category` VARCHAR(50) NOT NULL DEFAULT 'IDENTITY',
    `requirement_state` VARCHAR(30) NOT NULL DEFAULT 'OPTIONAL',
    `display_order` INT NOT NULL DEFAULT 0,
    `help_text` VARCHAR(255) DEFAULT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_tenant_target_field` (`tenant_id`, `target_entity`, `field_key`),
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Create Industry Profile Fields Table
CREATE TABLE IF NOT EXISTS `industry_profile_fields` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `industry_profile_id` BIGINT NOT NULL,
    `target_entity` VARCHAR(30) NOT NULL DEFAULT 'EMPLOYEE',
    `field_key` VARCHAR(80) NOT NULL,
    `field_label` VARCHAR(120) NOT NULL,
    `field_category` VARCHAR(50) NOT NULL DEFAULT 'IDENTITY',
    `requirement_state` VARCHAR(30) NOT NULL DEFAULT 'OPTIONAL',
    `display_order` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_ind_target_field` (`industry_profile_id`, `target_entity`, `field_key`),
    FOREIGN KEY (`industry_profile_id`) REFERENCES `industry_profiles`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Dynamic Column Additions for MySQL 8.0 Compatibility (Employees)
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'employees' AND column_name = 'national_id');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE `employees` ADD COLUMN `national_id` VARCHAR(100) DEFAULT NULL', 'SELECT 1');
PREPARE migration_stmt FROM @stmt; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'employees' AND column_name = 'tax_number');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE `employees` ADD COLUMN `tax_number` VARCHAR(100) DEFAULT NULL', 'SELECT 1');
PREPARE migration_stmt FROM @stmt; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'employees' AND column_name = 'license_number');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE `employees` ADD COLUMN `license_number` VARCHAR(100) DEFAULT NULL', 'SELECT 1');
PREPARE migration_stmt FROM @stmt; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'employees' AND column_name = 'license_category');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE `employees` ADD COLUMN `license_category` VARCHAR(50) DEFAULT NULL', 'SELECT 1');
PREPARE migration_stmt FROM @stmt; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'employees' AND column_name = 'license_expiry_date');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE `employees` ADD COLUMN `license_expiry_date` DATE DEFAULT NULL', 'SELECT 1');
PREPARE migration_stmt FROM @stmt; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'employees' AND column_name = 'emergency_contact_name');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE `employees` ADD COLUMN `emergency_contact_name` VARCHAR(150) DEFAULT NULL', 'SELECT 1');
PREPARE migration_stmt FROM @stmt; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'employees' AND column_name = 'emergency_contact_phone');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE `employees` ADD COLUMN `emergency_contact_phone` VARCHAR(50) DEFAULT NULL', 'SELECT 1');
PREPARE migration_stmt FROM @stmt; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'employees' AND column_name = 'emergency_contact_relation');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE `employees` ADD COLUMN `emergency_contact_relation` VARCHAR(50) DEFAULT NULL', 'SELECT 1');
PREPARE migration_stmt FROM @stmt; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'employees' AND column_name = 'residential_address');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE `employees` ADD COLUMN `residential_address` VARCHAR(500) DEFAULT NULL', 'SELECT 1');
PREPARE migration_stmt FROM @stmt; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'employees' AND column_name = 'extended_attributes_json');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE `employees` ADD COLUMN `extended_attributes_json` TEXT DEFAULT NULL', 'SELECT 1');
PREPARE migration_stmt FROM @stmt; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;

-- 4. Dynamic Column Additions for MySQL 8.0 Compatibility (Users)
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'national_id');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE `users` ADD COLUMN `national_id` VARCHAR(100) DEFAULT NULL', 'SELECT 1');
PREPARE migration_stmt FROM @stmt; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'tax_number');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE `users` ADD COLUMN `tax_number` VARCHAR(100) DEFAULT NULL', 'SELECT 1');
PREPARE migration_stmt FROM @stmt; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'extended_attributes_json');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE `users` ADD COLUMN `extended_attributes_json` TEXT DEFAULT NULL', 'SELECT 1');
PREPARE migration_stmt FROM @stmt; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;

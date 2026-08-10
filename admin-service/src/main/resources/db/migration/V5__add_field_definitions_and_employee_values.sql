-- =============================================================================
-- MBHONI-ES ADMIN SERVICE - V5 MIGRATION
-- Field Definitions, Employee Field Values & Hardcoded Compliance Columns
-- Compatible with MySQL 8.0, MariaDB & PostgreSQL SQL dialects
-- =============================================================================

-- 1. Create Field Definitions Table (Tenant-scoped per section configurable fields)
CREATE TABLE IF NOT EXISTS `field_definitions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tenant_id` BIGINT DEFAULT NULL,                     -- NULL = global default, applies to all tenants
    `section` VARCHAR(50) NOT NULL,                     -- QUALIFICATION, BACKGROUND_CHECK, EMPLOYMENT_STATUS, FIELD_ASSIGNMENT
    `field_key` VARCHAR(100) NOT NULL,
    `label` VARCHAR(150) NOT NULL,
    `data_type` VARCHAR(20) NOT NULL,                   -- TEXT, NUMBER, DATE, BOOLEAN, SELECT, FILE, GPS_COORD
    `select_options` TEXT DEFAULT NULL,                 -- JSON array, only when data_type = SELECT
    `is_required` BOOLEAN NOT NULL DEFAULT FALSE,
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    `display_order` INT NOT NULL DEFAULT 0,
    `validation_rule` VARCHAR(255) DEFAULT NULL,       -- optional regex / min / max / date-range expression
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_tenant_section_field_key` (`tenant_id`, `section`, `field_key`),
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Create Employee Field Values Table (Actual data captured against a field definition)
CREATE TABLE IF NOT EXISTS `employee_field_values` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `employee_id` BIGINT NOT NULL,
    `field_definition_id` BIGINT NOT NULL,
    `value` TEXT DEFAULT NULL,                         -- cast by data_type on read
    `recorded_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_employee_field_def` (`employee_id`, `field_definition_id`),
    FOREIGN KEY (`employee_id`) REFERENCES `employees`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`field_definition_id`) REFERENCES `field_definitions`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Create Performance Indexes
CREATE INDEX `idx_field_definitions_tenant_section` ON `field_definitions` (`tenant_id`, `section`);
CREATE INDEX `idx_employee_field_values_employee` ON `employee_field_values` (`employee_id`);

-- 4. Add Fixed Hardcoded Compliance & Provider Columns to Employees Table
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'employees' AND column_name = 'employment_status');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE `employees` ADD COLUMN `employment_status` VARCHAR(50) NOT NULL DEFAULT \'ACTIVE\'', 'SELECT 1');
PREPARE migration_stmt FROM @stmt; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'employees' AND column_name = 'background_check_status');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE `employees` ADD COLUMN `background_check_status` VARCHAR(50) NOT NULL DEFAULT \'NOT_STARTED\'', 'SELECT 1');
PREPARE migration_stmt FROM @stmt; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'employees' AND column_name = 'service_provider_id');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE `employees` ADD COLUMN `service_provider_id` BIGINT DEFAULT NULL', 'SELECT 1');
PREPARE migration_stmt FROM @stmt; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;

-- Add Foreign Key constraint for service_provider_id if not present
SET @fk_exists = (SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'employees' AND constraint_name = 'fk_employee_service_provider');
SET @stmt = IF(@fk_exists = 0, 'ALTER TABLE `employees` ADD CONSTRAINT `fk_employee_service_provider` FOREIGN KEY (`service_provider_id`) REFERENCES `tenants`(`id`) ON DELETE SET NULL', 'SELECT 1');
PREPARE migration_stmt FROM @stmt; EXECUTE migration_stmt; DEALLOCATE PREPARE migration_stmt;

-- 5. Seed Baseline Global Default Field Definitions (tenant_id = NULL)
INSERT IGNORE INTO `field_definitions` 
(`tenant_id`, `section`, `field_key`, `label`, `data_type`, `select_options`, `is_required`, `is_active`, `display_order`, `validation_rule`) 
VALUES
(NULL, 'QUALIFICATION', 'qualification_type', 'Qualification / Degree Type', 'SELECT', '["BACHELORS","MASTERS","DIPLOMA","CERTIFICATE","TRADE_LICENSE"]', false, true, 10, NULL),
(NULL, 'QUALIFICATION', 'license_number', 'Professional / Trade License #', 'TEXT', NULL, false, true, 20, NULL),
(NULL, 'QUALIFICATION', 'issuing_body', 'Issuing Authority / Licensing Body', 'TEXT', NULL, false, true, 30, NULL),
(NULL, 'QUALIFICATION', 'license_expiry_date', 'License Expiration Date', 'DATE', NULL, false, true, 40, NULL),

(NULL, 'BACKGROUND_CHECK', 'clearance_reference', 'Police / Clearance Reference #', 'TEXT', NULL, false, true, 10, NULL),
(NULL, 'BACKGROUND_CHECK', 'vetting_provider', 'Vetting / Verification Agency', 'TEXT', NULL, false, true, 20, NULL),
(NULL, 'BACKGROUND_CHECK', 'clearance_expiry_date', 'Clearance Expiration Date', 'DATE', NULL, false, true, 30, NULL),

(NULL, 'EMPLOYMENT_STATUS', 'probation_end_date', 'Probation Period End Date', 'DATE', NULL, false, true, 10, NULL),
(NULL, 'EMPLOYMENT_STATUS', 'notice_period_days', 'Notice Period (Days)', 'NUMBER', NULL, false, true, 20, NULL),
(NULL, 'EMPLOYMENT_STATUS', 'work_permit_number', 'Work Permit / Visa #', 'TEXT', NULL, false, true, 30, NULL),

(NULL, 'FIELD_ASSIGNMENT', 'site_location_code', 'Primary Site / Facility Code', 'TEXT', NULL, false, true, 10, NULL),
(NULL, 'FIELD_ASSIGNMENT', 'gps_checkin_radius', 'GPS Check-in Radius (Meters)', 'NUMBER', NULL, false, true, 20, '^[0-9]+$'),
(NULL, 'FIELD_ASSIGNMENT', 'assigned_equipment_id', 'Assigned Equipment / Serial ID', 'TEXT', NULL, false, true, 30, NULL);

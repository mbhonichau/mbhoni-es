-- =============================================================================
-- MBHONI-ES ADMIN SERVICE - V3 MIGRATION
-- Tenant Administration Settings & Feature Governance Table
-- =============================================================================

CREATE TABLE IF NOT EXISTS `tenant_settings` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tenant_id` BIGINT NOT NULL UNIQUE,
    
    -- Security & Authentication Governance
    `mfa_enforced` BOOLEAN NOT NULL DEFAULT FALSE,
    `sso_enforced` BOOLEAN NOT NULL DEFAULT FALSE,
    `password_rotation_days` INT NOT NULL DEFAULT 90,
    `max_failed_login_attempts` INT NOT NULL DEFAULT 5,
    `session_timeout_minutes` INT NOT NULL DEFAULT 30,
    `ip_whitelist_enabled` BOOLEAN NOT NULL DEFAULT FALSE,
    `allowed_ip_ranges` VARCHAR(1000) DEFAULT NULL,
    
    -- Audit, Logging & Compliance Governance
    `audit_extended_logging` BOOLEAN NOT NULL DEFAULT FALSE,
    `data_retention_days` INT NOT NULL DEFAULT 365,
    `gdpr_compliance_mode` BOOLEAN NOT NULL DEFAULT FALSE,
    `compliance_export_enabled` BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- API & Integration Administration
    `api_access_enabled` BOOLEAN NOT NULL DEFAULT TRUE,
    `webhook_events_enabled` BOOLEAN NOT NULL DEFAULT FALSE,
    `rate_limit_per_minute` INT NOT NULL DEFAULT 1000,
    `custom_domain_enabled` BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Workspace Operations & Self-Service
    `maintenance_mode` BOOLEAN NOT NULL DEFAULT FALSE,
    `self_registration_allowed` BOOLEAN NOT NULL DEFAULT FALSE,
    `allow_custom_roles` BOOLEAN NOT NULL DEFAULT TRUE,
    `multi_currency_enabled` BOOLEAN NOT NULL DEFAULT FALSE,
    `max_file_upload_mb` INT NOT NULL DEFAULT 50,
    
    -- Subscription & Billing Entitlement
    `overage_allowed` BOOLEAN NOT NULL DEFAULT FALSE,
    `auto_renewal_enabled` BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Timestamps
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (`tenant_id`) REFERENCES `tenants`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

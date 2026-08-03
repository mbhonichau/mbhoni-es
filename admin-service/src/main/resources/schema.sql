-- =============================================================================
-- MBHONI-ES ADMIN SERVICE - DATABASE SCHEMA SCRIPT
-- Database Target: MySQL 8.0+ / MariaDB / PostgreSQL compatible SQL
-- =============================================================================

ALTER TABLE `users` 
    ADD COLUMN IF NOT EXISTS `phone_number` VARCHAR(50) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS `avatar_url` VARCHAR(500) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS `auth_provider` VARCHAR(50) NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN IF NOT EXISTS `provider_id` VARCHAR(255) DEFAULT NULL;

-- Applies the legacy user-profile additions to installations created before V1.
-- Dynamic SQL is used because older MySQL installations do not support
-- ALTER TABLE ... ADD COLUMN IF NOT EXISTS.

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'phone_number'
);
SET @statement = IF(@column_exists = 0,
    'ALTER TABLE `users` ADD COLUMN `phone_number` VARCHAR(50) DEFAULT NULL',
    'SELECT 1');
PREPARE migration_statement FROM @statement;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'avatar_url'
);
SET @statement = IF(@column_exists = 0,
    'ALTER TABLE `users` ADD COLUMN `avatar_url` VARCHAR(500) DEFAULT NULL',
    'SELECT 1');
PREPARE migration_statement FROM @statement;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'auth_provider'
);
SET @statement = IF(@column_exists = 0,
    'ALTER TABLE `users` ADD COLUMN `auth_provider` VARCHAR(50) NOT NULL DEFAULT ''LOCAL''',
    'SELECT 1');
PREPARE migration_statement FROM @statement;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'provider_id'
);
SET @statement = IF(@column_exists = 0,
    'ALTER TABLE `users` ADD COLUMN `provider_id` VARCHAR(255) DEFAULT NULL',
    'SELECT 1');
PREPARE migration_statement FROM @statement;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

-- Schema-only, resumable migration. Stop application writes during migration.
-- Existing compatible columns/indexes are preserved; duplicate data fails explicitly.
-- VIRTUAL avoids MySQL's restriction on cascading FKs on STORED column bases.
-- The helper is migration-owned and recreated to recover an interrupted CALL.
DROP PROCEDURE IF EXISTS v8_migrate_shared_default_scope;
DELIMITER $$
CREATE PROCEDURE v8_migrate_shared_default_scope()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'field_definitions' AND column_name = 'tenant_scope') THEN
        ALTER TABLE field_definitions ADD COLUMN tenant_scope BIGINT
            GENERATED ALWAYS AS (COALESCE(tenant_id, 0)) VIRTUAL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'field_definitions' AND column_name = 'tenant_scope' AND data_type = 'bigint'
        AND extra LIKE '%GENERATED%'
        AND LOWER(REPLACE(REPLACE(generation_expression, '`', ''), ' ', '')) = 'coalesce(tenant_id,0)') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V8: incompatible field_definitions.tenant_scope; inspect definition before repair';
    END IF;
    IF NOT EXISTS (SELECT index_name FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'field_definitions'
        GROUP BY index_name HAVING MAX(non_unique) = 0 AND COUNT(*) = 3
        AND COUNT(sub_part) = 0
        AND GROUP_CONCAT(column_name ORDER BY seq_in_index) = 'tenant_scope,section,field_key') THEN
        ALTER TABLE field_definitions ADD CONSTRAINT uk_field_scope_key UNIQUE (tenant_scope, section, field_key);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'lookup_categories' AND column_name = 'tenant_scope') THEN
        ALTER TABLE lookup_categories ADD COLUMN tenant_scope BIGINT
            GENERATED ALWAYS AS (COALESCE(tenant_id, 0)) VIRTUAL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'lookup_categories' AND column_name = 'tenant_scope' AND data_type = 'bigint'
        AND extra LIKE '%GENERATED%'
        AND LOWER(REPLACE(REPLACE(generation_expression, '`', ''), ' ', '')) = 'coalesce(tenant_id,0)') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V8: incompatible lookup_categories.tenant_scope; inspect definition before repair';
    END IF;
    IF NOT EXISTS (SELECT index_name FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'lookup_categories'
        GROUP BY index_name HAVING MAX(non_unique) = 0 AND COUNT(*) = 2
        AND COUNT(sub_part) = 0
        AND GROUP_CONCAT(column_name ORDER BY seq_in_index) = 'tenant_scope,code') THEN
        ALTER TABLE lookup_categories ADD CONSTRAINT uk_lookup_scope_code UNIQUE (tenant_scope, code);
    END IF;

    -- Establish scoped uniqueness BEFORE removing obsolete global-only uniqueness.
    SET @v8_old_indexes = (SELECT GROUP_CONCAT(CONCAT('DROP INDEX `', REPLACE(index_name, '`', '``'), '`') SEPARATOR ', ')
        FROM (SELECT index_name FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = 'lookup_categories'
                AND non_unique = 0 AND index_name <> 'PRIMARY'
            GROUP BY index_name HAVING COUNT(*) = 1 AND MAX(column_name) = 'code') AS old_code_indexes);
    IF @v8_old_indexes IS NOT NULL THEN
        SET @v8_ddl = CONCAT('ALTER TABLE lookup_categories ', @v8_old_indexes);
        PREPARE v8_statement FROM @v8_ddl;
        EXECUTE v8_statement;
        DEALLOCATE PREPARE v8_statement;
    END IF;
END$$
DELIMITER ;
CALL v8_migrate_shared_default_scope();
DROP PROCEDURE v8_migrate_shared_default_scope;

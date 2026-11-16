-- Stop writes and back up lookup categories/codes and field definitions before rollback.
-- This first statement safely refuses rollback if tenant overrides now share a code.
-- Preserve those records in a separate reversible export/repair before retrying; do not delete them here.
ALTER TABLE lookup_categories ADD CONSTRAINT uk_lookup_global_code UNIQUE (code),
    DROP INDEX uk_lookup_scope_code, DROP COLUMN tenant_scope;
ALTER TABLE field_definitions DROP INDEX uk_field_scope_key, DROP COLUMN tenant_scope;

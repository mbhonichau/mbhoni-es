-- Stop application writes; export tenants.id/owner_user_id and the audit history before rollback.
-- Restore the export if reapplying the schema. This removes ownership enforcement, so do not
-- reopen tenant traffic with the older application until authorization is reviewed.
ALTER TABLE tenants DROP FOREIGN KEY fk_tenant_owner, DROP INDEX uk_tenant_owner, DROP COLUMN owner_user_id;

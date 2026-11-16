-- Stop tenant traffic first. Export organization_memberships and organization isolation flags.
-- Removing these controls reopens isolated data to tenant-wide access. Restore the export
-- when reapplying this migration, before reopening traffic.
DROP TABLE organization_memberships;
ALTER TABLE organization_units DROP COLUMN restrict_parent_visibility, DROP COLUMN isolated;

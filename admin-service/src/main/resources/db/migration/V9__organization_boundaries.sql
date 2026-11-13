-- Existing organizations remain descriptive. No memberships or inherited privileges are guessed.
ALTER TABLE organization_units ADD COLUMN isolated BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN restrict_parent_visibility BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE organization_memberships (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    unit_id BIGINT NOT NULL,
    administrator BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE KEY uk_org_user_unit (user_id, unit_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (unit_id) REFERENCES organization_units(id) ON DELETE CASCADE
);

-- V14: Add applicable_org_types and explicit_denials to rbac_roles table
-- These fields allow roles to declare their target org-type(s) and
-- list permissions that are explicitly denied even if inherited.

ALTER TABLE rbac_roles
    ADD COLUMN IF NOT EXISTS applicable_org_types VARCHAR(500),
    ADD COLUMN IF NOT EXISTS explicit_denials TEXT;

COMMENT ON COLUMN rbac_roles.applicable_org_types IS
    'CSV of org-type names this role is designed for (advisory, e.g. INDIVIDUAL_PROPERTY_OWNER)';

COMMENT ON COLUMN rbac_roles.explicit_denials IS
    'CSV of permissions explicitly denied even if granted via inheritance (enforced by RBAC engine)';

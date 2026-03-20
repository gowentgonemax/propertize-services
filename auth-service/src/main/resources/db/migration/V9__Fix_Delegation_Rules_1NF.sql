-- =============================================================================
-- V9: Fix delegation_rules - 1NF Violation
-- =============================================================================
-- Issue C-05: delegation_rules stores comma-separated values in VARCHAR columns
--   - delegatable_permissions VARCHAR(1000) — e.g. "payment:approve,lease:view"
--   - allowed_delegate_roles  VARCHAR(500)  — e.g. "PROPERTY_MANAGER,LEASING_AGENT"
--
-- Fix: Extract to proper junction tables.
-- =============================================================================

-- -----------------------------------------------------------------------
-- 1. Create junction tables
-- -----------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS delegation_rule_permissions (
    delegation_rule_id BIGINT      NOT NULL REFERENCES delegation_rules(id) ON DELETE CASCADE,
    permission         VARCHAR(150) NOT NULL,
    PRIMARY KEY (delegation_rule_id, permission)
);

CREATE INDEX IF NOT EXISTS idx_drp_rule_id ON delegation_rule_permissions(delegation_rule_id);
CREATE INDEX IF NOT EXISTS idx_drp_permission ON delegation_rule_permissions(permission);

CREATE TABLE IF NOT EXISTS delegation_rule_allowed_roles (
    delegation_rule_id BIGINT      NOT NULL REFERENCES delegation_rules(id) ON DELETE CASCADE,
    role               VARCHAR(100) NOT NULL,
    PRIMARY KEY (delegation_rule_id, role)
);

CREATE INDEX IF NOT EXISTS idx_drar_rule_id ON delegation_rule_allowed_roles(delegation_rule_id);
CREATE INDEX IF NOT EXISTS idx_drar_role  ON delegation_rule_allowed_roles(role);


-- -----------------------------------------------------------------------
-- 2. Migrate existing data (parse comma-separated values)
-- -----------------------------------------------------------------------

INSERT INTO delegation_rule_permissions (delegation_rule_id, permission)
SELECT dr.id, trim(p.permission)
FROM delegation_rules dr
CROSS JOIN unnest(string_to_array(dr.delegatable_permissions, ',')) AS p(permission)
ON CONFLICT DO NOTHING;

INSERT INTO delegation_rule_allowed_roles (delegation_rule_id, role)
SELECT dr.id, trim(r.role)
FROM delegation_rules dr
CROSS JOIN unnest(string_to_array(dr.allowed_delegate_roles, ',')) AS r(role)
ON CONFLICT DO NOTHING;


-- -----------------------------------------------------------------------
-- 3. (Optional) Drop old VARCHAR columns after verifying migrated data
--    Run manually after validating migration output:
--    ALTER TABLE delegation_rules DROP COLUMN IF EXISTS delegatable_permissions;
--    ALTER TABLE delegation_rules DROP COLUMN IF EXISTS allowed_delegate_roles;
-- -----------------------------------------------------------------------

-- Add soft deprecation comment
COMMENT ON COLUMN delegation_rules.delegatable_permissions IS
    'DEPRECATED: Migrated to delegation_rule_permissions table. Will be dropped in V10.';
COMMENT ON COLUMN delegation_rules.allowed_delegate_roles IS
    'DEPRECATED: Migrated to delegation_rule_allowed_roles table. Will be dropped in V10.';

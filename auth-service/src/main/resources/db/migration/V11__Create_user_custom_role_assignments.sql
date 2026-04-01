-- ============================================================================
-- V11: Create user_custom_role_assignments table
--
-- Maps users to custom (org-scoped) roles created at runtime.
-- System roles stay in the existing user_roles table (enum-backed).
-- This table handles only custom roles created via CustomRoleController.
-- ============================================================================

CREATE TABLE IF NOT EXISTS user_custom_role_assignments (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    rbac_role_id    BIGINT          NOT NULL REFERENCES rbac_roles(id),
    organization_id BIGINT          NOT NULL,
    assigned_by     BIGINT,
    assigned_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMP,                  -- optional TTL for temporary grants
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE
);

-- A user can only have each custom role once (active)
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_custom_role_active
    ON user_custom_role_assignments (user_id, rbac_role_id)
    WHERE is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_ucra_user_id   ON user_custom_role_assignments (user_id);
CREATE INDEX IF NOT EXISTS idx_ucra_role_id   ON user_custom_role_assignments (rbac_role_id);
CREATE INDEX IF NOT EXISTS idx_ucra_org_id    ON user_custom_role_assignments (organization_id);
CREATE INDEX IF NOT EXISTS idx_ucra_active    ON user_custom_role_assignments (is_active);

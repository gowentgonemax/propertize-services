-- ============================================================================
-- V10: Create rbac_roles table  — DB-first RBAC system role catalog
--
-- This table is the single runtime source of truth for role definitions.
--   is_system = true  → seeded from rbac.yml on startup (RbacSeederService)
--   is_system = false → created by org admins at runtime via /api/v1/rbac/custom-roles
--
-- System roles have organization_id = NULL (platform-wide).
-- Custom org-scoped roles have organization_id = <org numeric id>.
-- ============================================================================

CREATE TABLE IF NOT EXISTS rbac_roles (
    id              BIGSERIAL       PRIMARY KEY,
    role_name       VARCHAR(150)    NOT NULL,
    display_name    VARCHAR(255)    NOT NULL,
    description     VARCHAR(500),
    scope           VARCHAR(50)     NOT NULL DEFAULT 'self',
    level           INTEGER         NOT NULL DEFAULT 0,
    category        VARCHAR(50)     NOT NULL DEFAULT '',
    permissions     TEXT,                          -- comma-separated permission strings
    inherits_from   VARCHAR(500),                  -- comma-separated base role names
    is_system       BOOLEAN         NOT NULL DEFAULT FALSE,
    organization_id BIGINT,                        -- NULL = platform-wide system role
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_by      BIGINT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

-- System roles: role_name globally unique
CREATE UNIQUE INDEX IF NOT EXISTS uq_rbac_role_name_system
    ON rbac_roles (role_name)
    WHERE is_system = TRUE;

-- Custom roles: role_name unique per org (active only)
CREATE UNIQUE INDEX IF NOT EXISTS uq_rbac_role_name_org_active
    ON rbac_roles (role_name, organization_id)
    WHERE is_system = FALSE AND is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_rbac_role_is_system   ON rbac_roles (is_system);
CREATE INDEX IF NOT EXISTS idx_rbac_role_org_id      ON rbac_roles (organization_id);
CREATE INDEX IF NOT EXISTS idx_rbac_role_is_active   ON rbac_roles (is_active);
CREATE INDEX IF NOT EXISTS idx_rbac_role_name        ON rbac_roles (role_name);

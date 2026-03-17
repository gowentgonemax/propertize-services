-- ============================================================================
-- V6: Create custom_roles table
-- Phase 4a: Custom Role Builder
--
-- Custom roles allow organizations to create tailored permission profiles
-- scoped to their organization. Permissions must be a subset of the creator's
-- effective permissions.
-- ============================================================================

CREATE TABLE IF NOT EXISTS custom_roles (
    id                BIGSERIAL       PRIMARY KEY,
    role_name         VARCHAR(150)    NOT NULL,
    display_name      VARCHAR(255)    NOT NULL,
    description       VARCHAR(500),
    organization_id   BIGINT          NOT NULL,
    permissions       VARCHAR(4000)   NOT NULL,
    inherits_from     VARCHAR(150),
    max_level         INTEGER         NOT NULL DEFAULT 0,
    created_by        BIGINT          NOT NULL,
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    is_active         BOOLEAN         NOT NULL DEFAULT TRUE,
    is_system         BOOLEAN         NOT NULL DEFAULT FALSE
);

-- Unique constraint: role_name must be unique within an organization (for active roles)
CREATE UNIQUE INDEX IF NOT EXISTS uq_custom_role_name_org_active
    ON custom_roles (role_name, organization_id) WHERE is_active = TRUE;

-- Index on organization_id for org-scoped queries
CREATE INDEX IF NOT EXISTS idx_custom_role_org_id
    ON custom_roles (organization_id);

-- Index on role_name for lookups
CREATE INDEX IF NOT EXISTS idx_custom_role_name
    ON custom_roles (role_name);

-- Index on inherits_from for inheritance chain queries
CREATE INDEX IF NOT EXISTS idx_custom_role_inherits
    ON custom_roles (inherits_from);

-- Index on is_active for filtering
CREATE INDEX IF NOT EXISTS idx_custom_role_active
    ON custom_roles (is_active);

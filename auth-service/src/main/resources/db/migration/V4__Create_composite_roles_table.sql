-- ============================================================================
-- V4: Create composite_roles table
-- Phase 3: Dynamic Role Composition
--
-- Composite roles combine multiple base roles into a single assignable unit.
-- The effective permission set is the union of all component role permissions.
-- ============================================================================

CREATE TABLE IF NOT EXISTS composite_roles (
    id                BIGSERIAL       PRIMARY KEY,
    name              VARCHAR(150)    NOT NULL UNIQUE,
    description       VARCHAR(500),
    component_roles   VARCHAR(2000)   NOT NULL,
    created_by        BIGINT,
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active         BOOLEAN         NOT NULL DEFAULT TRUE,
    organization_id   BIGINT
);

-- Index on name for fast lookup
CREATE INDEX IF NOT EXISTS idx_composite_role_name
    ON composite_roles (name);

-- Index on organization_id for org-scoped queries
CREATE INDEX IF NOT EXISTS idx_composite_role_org_id
    ON composite_roles (organization_id);

-- Index on is_active for filtering active roles
CREATE INDEX IF NOT EXISTS idx_composite_role_active
    ON composite_roles (is_active);

-- ============================================================================
-- V3: Create temporal_permissions table
-- Phase 1: Time-Based Access Control
--
-- Stores time-bound permission grants that automatically expire after a
-- configurable duration. Supports manual revocation with full audit trail.
-- ============================================================================

CREATE TABLE IF NOT EXISTS temporal_permissions (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    permission      VARCHAR(255)    NOT NULL,
    role            VARCHAR(100),
    resource_type   VARCHAR(100),
    resource_id     BIGINT,
    granted_by      BIGINT          NOT NULL,
    granted_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP       NOT NULL,
    reason          VARCHAR(500),
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    revoked_at      TIMESTAMP,
    revoked_by      BIGINT
);

-- Index for looking up active permissions for a user (primary query path)
CREATE INDEX idx_temporal_user_id ON temporal_permissions (user_id);

-- Index for the scheduled expiration cleanup job
CREATE INDEX idx_temporal_expires_at ON temporal_permissions (expires_at);

-- Index for filtering by active status
CREATE INDEX idx_temporal_is_active ON temporal_permissions (is_active);

-- Composite index for the most common query: active, non-expired permissions for a user
CREATE INDEX idx_temporal_user_active_expires ON temporal_permissions (user_id, is_active, expires_at);

-- Index for finding permissions granted by a specific user (audit queries)
CREATE INDEX idx_temporal_granted_by ON temporal_permissions (granted_by);

-- Add foreign key constraints referencing the users table
ALTER TABLE temporal_permissions
    ADD CONSTRAINT fk_temporal_user_id
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE temporal_permissions
    ADD CONSTRAINT fk_temporal_granted_by
    FOREIGN KEY (granted_by) REFERENCES users (id) ON DELETE RESTRICT;

-- Comment on table and key columns for documentation
COMMENT ON TABLE temporal_permissions IS 'Time-bound permission grants that auto-expire after a configurable duration';
COMMENT ON COLUMN temporal_permissions.user_id IS 'The user who receives the temporal permission';
COMMENT ON COLUMN temporal_permissions.permission IS 'The permission string being granted (e.g., PROPERTY_WRITE)';
COMMENT ON COLUMN temporal_permissions.granted_by IS 'The user who granted this temporal permission';
COMMENT ON COLUMN temporal_permissions.expires_at IS 'When this permission automatically expires';
COMMENT ON COLUMN temporal_permissions.is_active IS 'Whether the permission is currently active (false when expired or revoked)';
COMMENT ON COLUMN temporal_permissions.revoked_at IS 'Timestamp of manual revocation (NULL if not manually revoked)';
COMMENT ON COLUMN temporal_permissions.revoked_by IS 'User who manually revoked the permission (NULL if not manually revoked)';

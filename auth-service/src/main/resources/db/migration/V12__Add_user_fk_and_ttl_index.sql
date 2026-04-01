-- ============================================================================
-- V12: Referential integrity + TTL index for user_custom_role_assignments
--
-- 1. Add FK from user_id → users(id) ON DELETE CASCADE so that removing a
--    user automatically removes all their custom role assignments.
-- 2. Add a partial index on expires_at for fast TTL sweep queries.
-- ============================================================================

ALTER TABLE user_custom_role_assignments
    ADD CONSTRAINT fk_ucra_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Partial index: only rows that are active AND have an expiry set.
-- The nightly TTL sweep queries exactly this shape.
CREATE INDEX IF NOT EXISTS idx_ucra_expires_active
    ON user_custom_role_assignments (expires_at)
    WHERE is_active = TRUE AND expires_at IS NOT NULL;

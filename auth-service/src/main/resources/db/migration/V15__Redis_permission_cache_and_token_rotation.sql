-- V15: JWT Size Reduction — Redis-based Permission Cache
--
-- This migration documents the architectural change introduced in v8.0:
-- Permissions are NO LONGER embedded in the JWT payload.
--
-- Instead:
--  1. Auth-service stores permissions in Redis at login/refresh/org-switch time.
--     Key format : perms:jti:{jti}
--     Value      : comma-separated permission list
--     TTL        : 900 seconds (= access token lifetime)
--  2. API Gateway fetches permissions from Redis using the token's jti claim.
--  3. Gateway injects X-Permissions header as before — downstream services unchanged.
--  4. On logout, the Redis entry is proactively evicted.
--  5. On refresh, old entry expires naturally; new entry is written with new jti.
--
-- Impact:
--  * JWT size reduced by ~80-90% (removes 50+ permission strings × ~20 chars each)
--  * Fixes HTTP 431 "Request Header Fields Too Large" errors
--  * Refresh token rotation is now enforced (replay detection via Redis)
--  * HOA_DIRECTOR level changed: 920 → 890
--  * CFO level changed:          940 → 890
--
-- No database schema changes are required for this migration.
-- The changes are purely in application code and Redis.

-- Record this migration event in a platform_changes log (if it exists).
-- Safely ignored if the table is absent.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_name = 'platform_changes'
    ) THEN
        INSERT INTO platform_changes (version, description, applied_at)
        VALUES ('v8.0', 'JWT size reduction via Redis permission cache; refresh token rotation', NOW())
        ON CONFLICT DO NOTHING;
    END IF;
END $$;


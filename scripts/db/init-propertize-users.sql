-- ============================================================
-- Seed Users into propertize_db (propertize-core read mirror)
-- The propertize User entity is @Immutable - managed by auth-service.
-- These users must match users in the auth-service propertize_auth DB.
-- Run: psql -h localhost -U ravishah -d propertize_db -f init-propertize-users.sql
-- ============================================================

-- Admin user (matches auth-service admin@propertize.com)
INSERT INTO users (
    username, email, password, first_name, last_name, phone_number,
    organization_id, enabled, account_non_expired, account_non_locked,
    credentials_non_expired, created_at, updated_at
)
SELECT 'admin', 'admin@propertize.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5lDfyOlkPe3Gy',
    'Admin', 'User', NULL, NULL,
    true, true, true, true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

-- ravishah user
INSERT INTO users (
    username, email, password, first_name, last_name, phone_number,
    organization_id, enabled, account_non_expired, account_non_locked,
    credentials_non_expired, created_at, updated_at
)
SELECT 'ravishah', 'ravi@test.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5lDfyOlkPe3Gy',
    'Ravi', 'Shah', NULL, NULL,
    true, true, true, true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'ravishah');

-- Roles for admin (PLATFORM_OVERSIGHT + PLATFORM_OPERATIONS)
INSERT INTO user_roles (user_id, role)
SELECT u.id, 'PLATFORM_OVERSIGHT'
FROM users u WHERE u.username = 'admin'
AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role = 'PLATFORM_OVERSIGHT');

INSERT INTO user_roles (user_id, role)
SELECT u.id, 'PLATFORM_OPERATIONS'
FROM users u WHERE u.username = 'admin'
AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role = 'PLATFORM_OPERATIONS');

-- Roles for ravishah
INSERT INTO user_roles (user_id, role)
SELECT u.id, 'ORGANIZATION_OWNER'
FROM users u WHERE u.username = 'ravishah'
AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role = 'ORGANIZATION_OWNER');

-- Verify
SELECT u.id, u.username, u.email, u.enabled, STRING_AGG(ur.role, ', ') AS roles
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
GROUP BY u.id, u.username, u.email, u.enabled;


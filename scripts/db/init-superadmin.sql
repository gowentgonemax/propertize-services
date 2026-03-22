-- ============================================
-- Create Superadmin User
-- Username: superadmin
-- Password: password
-- Role: PLATFORM_OVERSIGHT (highest platform role)
-- ============================================

-- First, check if user already exists and delete if found
DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE username = 'superadmin');
DELETE FROM users WHERE username = 'superadmin';

-- Insert the superadmin user
-- Password: password (BCrypt hash with strength 12)
INSERT INTO users (
    id,
    username,
    email,
    password,
    first_name,
    last_name,
    phone_number,
    organization_id,
    account_non_expired,
    account_non_locked,
    credentials_non_expired,
    enabled,
    created_at,
    updated_at,
    last_login
) VALUES (
    1,
    'superadmin',
    'superadmin@propertize.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5lDfyOlkPe3Gy', -- BCrypt hash of 'password'
    'Super',
    'Admin',
    '+1234567890',
    NULL, -- Platform level users don't need organization_id
    true,
    true,
    true,
    true,
    NOW(),
    NOW(),
    NULL
);

-- Insert the PLATFORM_OVERSIGHT role for superadmin
INSERT INTO user_roles (user_id, role) VALUES (1, 'PLATFORM_OVERSIGHT');

-- Reset sequence to start from 2 for next users
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users) + 1);

-- Verify the creation
SELECT 
    u.id,
    u.username,
    u.email,
    u.first_name,
    u.last_name,
    u.organization_id,
    u.enabled,
    array_agg(ur.role) as roles
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
WHERE u.username = 'superadmin'
GROUP BY u.id, u.username, u.email, u.first_name, u.last_name, u.organization_id, u.enabled;

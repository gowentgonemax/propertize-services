-- Insert test users into the database
-- Password for all users: Password123!
-- BCrypt hashed with strength 10

USE wagecraft;

-- Insert admin user
INSERT INTO `users` (`id`, `email`, `password`, `first_name`, `last_name`, `role`, `enabled`, `account_non_expired`, `account_non_locked`, `credentials_non_expired`, `created_at`, `updated_at`)
VALUES
(UNHEX(REPLACE(UUID(), '-', '')), 'admin@wagecraft.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzJ6PjZN7wnCVITZvCmzHv6A5W7yS', 'Admin', 'User', 'ROLE_ADMIN', 1, 1, 1, 1, NOW(), NOW()),
(UNHEX(REPLACE(UUID(), '-', '')), 'manager@wagecraft.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzJ6PjZN7wnCVITZvCmzHv6A5W7yS', 'Manager', 'User', 'ROLE_MANAGER', 1, 1, 1, 1, NOW(), NOW()),
(UNHEX(REPLACE(UUID(), '-', '')), 'payroll@wagecraft.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzJ6PjZN7wnCVITZvCmzHv6A5W7yS', 'Payroll', 'Admin', 'ROLE_PAYROLL_ADMIN', 1, 1, 1, 1, NOW(), NOW()),
(UNHEX(REPLACE(UUID(), '-', '')), 'user@wagecraft.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzJ6PjZN7wnCVITZvCmzHv6A5W7yS', 'Regular', 'User', 'ROLE_USER', 1, 1, 1, 1, NOW(), NOW()),
(UNHEX(REPLACE(UUID(), '-', '')), 'john.doe@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzJ6PjZN7wnCVITZvCmzHv6A5W7yS', 'John', 'Doe', 'ROLE_USER', 1, 1, 1, 1, NOW(), NOW()),
(UNHEX(REPLACE(UUID(), '-', '')), 'jane.smith@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzJ6PjZN7wnCVITZvCmzHv6A5W7yS', 'Jane', 'Smith', 'ROLE_MANAGER', 1, 1, 1, 1, NOW(), NOW());

SELECT 'Test users created successfully!' as message;
SELECT email, first_name, last_name, role, enabled FROM users;


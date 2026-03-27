-- Insert user: edsu@email.com
-- Password: Kathmandu@977
-- BCrypt hash for "Kathmandu@977"

USE wagecraft;

INSERT INTO `users` (`id`, `email`, `password`, `first_name`, `last_name`, `role`, `enabled`, `account_non_expired`, `account_non_locked`, `credentials_non_expired`, `created_at`, `updated_at`)
VALUES
(UNHEX(REPLACE(UUID(), '-', '')), 'edsu@email.com', '$2a$10$8fqQVZ3kZYJYWf5qGX.nF.xJYqxE5VLZ4jQM5RJ9hKqN5VRqGqYJm', 'Edsu', 'User', 'ROLE_USER', 1, 1, 1, 1, NOW(), NOW());

SELECT 'User edsu@email.com created successfully!' as message;
SELECT email, first_name, last_name, role, enabled FROM users WHERE email = 'edsu@email.com';


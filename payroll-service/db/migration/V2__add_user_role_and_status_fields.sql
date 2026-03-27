-- Add role and account status fields to users table
-- Using individual ALTER statements with error handling

ALTER TABLE `users` ADD COLUMN `role` VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER' AFTER `last_name`;
ALTER TABLE `users` ADD COLUMN `account_non_expired` TINYINT(1) NOT NULL DEFAULT 1 AFTER `enabled`;
ALTER TABLE `users` ADD COLUMN `account_non_locked` TINYINT(1) NOT NULL DEFAULT 1 AFTER `account_non_expired`;
ALTER TABLE `users` ADD COLUMN `credentials_non_expired` TINYINT(1) NOT NULL DEFAULT 1 AFTER `account_non_locked`;

-- Update existing users to have the default role if null
UPDATE `users` SET `role` = 'ROLE_USER' WHERE `role` IS NULL OR `role` = '';

-- Create index on role for performance
CREATE INDEX `idx_users_role` ON `users` (`role`);


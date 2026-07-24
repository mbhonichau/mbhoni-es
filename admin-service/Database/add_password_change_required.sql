-- Add password_change_required column to users table
-- This column tracks whether a user is required to change their password on first login

ALTER TABLE users 
ADD COLUMN password_change_required BOOLEAN DEFAULT FALSE 
AFTER is_global_admin;

-- Add password reset token columns
-- These columns are used for password reset functionality via email links

ALTER TABLE users 
ADD COLUMN password_reset_token VARCHAR(255) NULL 
AFTER password_change_required;

ALTER TABLE users 
ADD COLUMN password_reset_token_expiry DATETIME NULL 
AFTER password_reset_token;

-- Update existing users to not require password change (optional, depending on your requirements)
-- Uncomment the following line if you want existing users to not be forced to change password
-- UPDATE users SET password_change_required = FALSE WHERE password_change_required IS NULL;


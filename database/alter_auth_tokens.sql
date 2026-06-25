-- Adds email verification + password reset token tracking to users.
-- Run this yourself against your existing database.

USE climbingwiki;

ALTER TABLE users
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE AFTER role,
    ADD COLUMN verification_token VARCHAR(100) NULL AFTER email_verified,
    ADD COLUMN verification_token_expires_at DATETIME NULL AFTER verification_token,
    ADD COLUMN reset_token VARCHAR(100) NULL AFTER verification_token_expires_at,
    ADD COLUMN reset_token_expires_at DATETIME NULL AFTER reset_token;

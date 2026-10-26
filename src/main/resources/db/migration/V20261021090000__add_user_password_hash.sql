-- Preserve legacy accounts and all course ownership references.
-- NULL hashes cannot authenticate; new signups always store a BCrypt hash.
ALTER TABLE app_user ADD COLUMN password_hash VARCHAR(255);

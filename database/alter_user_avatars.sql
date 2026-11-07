-- Brings an already-created climbingwiki DB in line with the updated database/schema.sql
-- (uploaded profile pictures). Run this yourself against your existing database.

USE climbingwiki;

-- Uploaded profile pictures, kept apart so user queries never load image bytes.
CREATE TABLE IF NOT EXISTS user_avatars (
    user_id      BIGINT UNSIGNED PRIMARY KEY,
    content_type VARCHAR(30) NOT NULL,
    data         MEDIUMBLOB NOT NULL,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_avatars_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

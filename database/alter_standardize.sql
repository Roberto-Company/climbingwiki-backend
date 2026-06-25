-- Brings an already-created climbingwiki DB in line with the updated database/schema.sql
-- Run this yourself against your existing database.

USE climbingwiki;

ALTER TABLE users
    MODIFY COLUMN role ENUM('USER', 'MODERATOR', 'ADMIN') NOT NULL DEFAULT 'USER';

ALTER TABLE routes
    MODIFY COLUMN route_type ENUM('SPORT', 'BOULDER', 'TRAD') NOT NULL;

ALTER TABLE logbook_entries
    MODIFY COLUMN result ENUM('ATTEMPT', 'TOP_ROPE', 'REDPOINT', 'FLASH', 'ONSIGHT') NOT NULL;

-- grading_systems.category has no safe default across both categories,
-- so add it nullable first, backfill per row, then enforce NOT NULL.
ALTER TABLE grading_systems
    ADD COLUMN category ENUM('ROPED', 'BOULDER') NULL AFTER name;

-- Example backfill — adjust names to match your actual rows:
-- UPDATE grading_systems SET category = 'ROPED'   WHERE name IN ('YDS', 'FRENCH', 'EGRADE');
-- UPDATE grading_systems SET category = 'BOULDER' WHERE name IN ('VSCALE', 'FONTSCALE');

ALTER TABLE grading_systems
    MODIFY COLUMN category ENUM('ROPED', 'BOULDER') NOT NULL;

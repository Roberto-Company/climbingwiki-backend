-- Adds optional per-route coordinates so boulders/routes spread across a
-- sector can be pinned individually instead of inheriting the sector's point.
-- Run this yourself against your existing database.

USE climbingwiki;

ALTER TABLE routes
    ADD COLUMN latitude  DECIMAL(10, 7) NULL AFTER first_ascent_date,
    ADD COLUMN longitude DECIMAL(10, 7) NULL AFTER latitude;

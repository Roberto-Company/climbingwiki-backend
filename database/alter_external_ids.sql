-- Adds provenance tracking (external_source/external_id) to the tables an
-- automated importer (e.g. OpenBeta) can populate, so re-running an import
-- upserts instead of duplicating rows. NULL values don't collide under a
-- UNIQUE KEY in MySQL, so manually-created rows (no external_id) are unaffected.
-- Run this yourself against your existing database.

USE climbingwiki;

ALTER TABLE zones
    ADD COLUMN external_source VARCHAR(50) NULL,
    ADD COLUMN external_id VARCHAR(100) NULL,
    ADD UNIQUE KEY uq_zones_external (external_source, external_id);

ALTER TABLE crags
    ADD COLUMN external_source VARCHAR(50) NULL AFTER approach_description,
    ADD COLUMN external_id VARCHAR(100) NULL AFTER external_source,
    ADD UNIQUE KEY uq_crags_external (external_source, external_id);

ALTER TABLE sectors
    ADD COLUMN external_source VARCHAR(50) NULL,
    ADD COLUMN external_id VARCHAR(100) NULL,
    ADD UNIQUE KEY uq_sectors_external (external_source, external_id);

ALTER TABLE routes
    ADD COLUMN external_source VARCHAR(50) NULL AFTER longitude,
    ADD COLUMN external_id VARCHAR(100) NULL AFTER external_source,
    ADD UNIQUE KEY uq_routes_external (external_source, external_id);

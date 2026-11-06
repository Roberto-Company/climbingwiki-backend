-- Verified / unverified climbing data.
-- Every zone, crag, sector, route and pitch records who added it and whether a moderator has verified it.
-- Data that already exists (OpenStreetMap / theCrag / OpenBeta imports) is marked verified.
-- Run with: mysql --default-character-set=utf8mb4 ... < database/alter_catalog_verification.sql

USE climbingwiki;

ALTER TABLE zones
    ADD COLUMN created_by BIGINT UNSIGNED NULL AFTER parent_zone_id,
    ADD COLUMN verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT fk_zones_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE crags
    ADD COLUMN verified BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE sectors
    ADD COLUMN created_by BIGINT UNSIGNED NULL AFTER crag_id,
    ADD COLUMN verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT fk_sectors_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE routes
    ADD COLUMN verified BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE pitches
    ADD COLUMN created_by BIGINT UNSIGNED NULL AFTER grade_id,
    ADD COLUMN verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT fk_pitches_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL;

START TRANSACTION;
UPDATE zones SET verified = TRUE;
UPDATE crags SET verified = TRUE;
UPDATE sectors SET verified = TRUE;
UPDATE routes SET verified = TRUE, updated_at = updated_at; -- keep updated_at: this is not an edit
UPDATE pitches SET verified = TRUE;
COMMIT;

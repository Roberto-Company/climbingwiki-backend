-- Brings ratings.score from whole-star 1-5 to half-star 0-5 (0, 0.5, 1, ..., 5).
-- Run this yourself against your existing database.

USE climbingwiki;

ALTER TABLE ratings
    DROP CHECK chk_ratings_score;

ALTER TABLE ratings
    MODIFY COLUMN score DECIMAL(2, 1) UNSIGNED NOT NULL;

ALTER TABLE ratings
    ADD CONSTRAINT chk_ratings_score CHECK (score BETWEEN 0 AND 5 AND MOD(score * 2, 1) = 0);

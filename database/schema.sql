-- ClimbingWiki schema
-- Engine: MySQL 8.4+ (InnoDB, utf8mb4)

CREATE DATABASE IF NOT EXISTS climbingwiki
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE climbingwiki;

-- ===================== USERS =====================

CREATE TABLE users (
    id                          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username                    VARCHAR(50)  NOT NULL UNIQUE,
    email                       VARCHAR(255) NOT NULL UNIQUE,
    password_hash               VARCHAR(255) NOT NULL,
    full_name                   VARCHAR(150),
    bio                         TEXT,
    avatar_url                  VARCHAR(500),
    role                        ENUM('USER', 'MODERATOR', 'ADMIN') NOT NULL DEFAULT 'USER',
    email_verified              BOOLEAN NOT NULL DEFAULT FALSE,
    verification_token          VARCHAR(100) NULL,
    verification_token_expires_at DATETIME NULL,
    reset_token                 VARCHAR(100) NULL,
    reset_token_expires_at      DATETIME NULL,
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ===================== GEOGRAPHY =====================

CREATE TABLE zones (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    parent_zone_id  BIGINT UNSIGNED NULL,
    name            VARCHAR(150) NOT NULL,
    description     TEXT,
    external_source VARCHAR(50) NULL,
    external_id     VARCHAR(100) NULL,
    CONSTRAINT fk_zones_parent FOREIGN KEY (parent_zone_id) REFERENCES zones (id) ON DELETE SET NULL,
    UNIQUE KEY uq_zones_external (external_source, external_id)
) ENGINE=InnoDB;

CREATE TABLE crags (
    id                    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    zone_id               BIGINT UNSIGNED NULL,
    created_by            BIGINT UNSIGNED NULL,
    name                  VARCHAR(150) NOT NULL,
    description           TEXT,
    latitude              DECIMAL(10, 7),
    longitude             DECIMAL(10, 7),
    altitude_m            SMALLINT,
    orientation           VARCHAR(20),
    rock_type             VARCHAR(100),
    approach_description  TEXT,
    external_source       VARCHAR(50) NULL,
    external_id           VARCHAR(100) NULL,
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_crags_zone FOREIGN KEY (zone_id) REFERENCES zones (id) ON DELETE SET NULL,
    CONSTRAINT fk_crags_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
    UNIQUE KEY uq_crags_external (external_source, external_id)
) ENGINE=InnoDB;

CREATE TABLE sectors (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    crag_id         BIGINT UNSIGNED NOT NULL,
    name            VARCHAR(150) NOT NULL,
    description     TEXT,
    latitude        DECIMAL(10, 7),
    longitude       DECIMAL(10, 7),
    external_source VARCHAR(50) NULL,
    external_id     VARCHAR(100) NULL,
    CONSTRAINT fk_sectors_crag FOREIGN KEY (crag_id) REFERENCES crags (id) ON DELETE CASCADE,
    UNIQUE KEY uq_sectors_external (external_source, external_id)
) ENGINE=InnoDB;

-- ===================== GRADES =====================

CREATE TABLE grading_systems (
    id       BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(50) NOT NULL UNIQUE,
    category ENUM('ROPED', 'BOULDER') NOT NULL
) ENGINE=InnoDB;

CREATE TABLE grades (
    id                 BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    grading_system_id  BIGINT UNSIGNED NOT NULL,
    label              VARCHAR(20) NOT NULL,
    difficulty_score   SMALLINT NOT NULL,
    CONSTRAINT fk_grades_system FOREIGN KEY (grading_system_id) REFERENCES grading_systems (id) ON DELETE CASCADE,
    UNIQUE (grading_system_id, label)
) ENGINE=InnoDB;

-- ===================== ROUTES =====================

CREATE TABLE routes (
    id                 BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    sector_id          BIGINT UNSIGNED NOT NULL,
    created_by         BIGINT UNSIGNED NULL,
    name               VARCHAR(150) NOT NULL,
    route_type         ENUM('SPORT', 'BOULDER', 'TRAD') NOT NULL,
    description        TEXT,
    equipment_notes    TEXT,
    first_ascent_by    VARCHAR(150),
    first_ascent_date  DATE,
    latitude           DECIMAL(10, 7),
    longitude          DECIMAL(10, 7),
    external_source    VARCHAR(50) NULL,
    external_id        VARCHAR(100) NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_routes_sector FOREIGN KEY (sector_id) REFERENCES sectors (id) ON DELETE CASCADE,
    CONSTRAINT fk_routes_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
    UNIQUE KEY uq_routes_external (external_source, external_id)
) ENGINE=InnoDB;

CREATE TABLE pitches (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    route_id     BIGINT UNSIGNED NOT NULL,
    grade_id     BIGINT UNSIGNED NULL,
    order_index  SMALLINT NOT NULL,
    length_m     SMALLINT,
    bolts_count  SMALLINT,
    description  TEXT,
    CONSTRAINT fk_pitches_route FOREIGN KEY (route_id) REFERENCES routes (id) ON DELETE CASCADE,
    CONSTRAINT fk_pitches_grade FOREIGN KEY (grade_id) REFERENCES grades (id) ON DELETE SET NULL,
    UNIQUE (route_id, order_index)
) ENGINE=InnoDB;

-- ===================== TAGS =====================

CREATE TABLE tags (
    id   BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE route_tags (
    route_id BIGINT UNSIGNED NOT NULL,
    tag_id   BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (route_id, tag_id),
    CONSTRAINT fk_route_tags_route FOREIGN KEY (route_id) REFERENCES routes (id) ON DELETE CASCADE,
    CONSTRAINT fk_route_tags_tag FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ===================== CONTENT =====================

CREATE TABLE photos (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT UNSIGNED NOT NULL,
    route_id   BIGINT UNSIGNED NULL,
    crag_id    BIGINT UNSIGNED NULL,
    url        VARCHAR(500) NOT NULL,
    caption    VARCHAR(255),
    is_topo    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_photos_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_photos_route FOREIGN KEY (route_id) REFERENCES routes (id) ON DELETE CASCADE,
    CONSTRAINT fk_photos_crag FOREIGN KEY (crag_id) REFERENCES crags (id) ON DELETE CASCADE,
    CONSTRAINT chk_photos_target CHECK (route_id IS NOT NULL OR crag_id IS NOT NULL)
) ENGINE=InnoDB;

CREATE TABLE comments (
    id                 BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id            BIGINT UNSIGNED NOT NULL,
    route_id           BIGINT UNSIGNED NULL,
    crag_id            BIGINT UNSIGNED NULL,
    parent_comment_id  BIGINT UNSIGNED NULL,
    body               TEXT NOT NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_route FOREIGN KEY (route_id) REFERENCES routes (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_crag FOREIGN KEY (crag_id) REFERENCES crags (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_comment_id) REFERENCES comments (id) ON DELETE CASCADE,
    CONSTRAINT chk_comments_target CHECK (route_id IS NOT NULL OR crag_id IS NOT NULL)
) ENGINE=InnoDB;

-- ===================== LOGBOOK =====================

CREATE TABLE logbook_entries (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT UNSIGNED NOT NULL,
    route_id      BIGINT UNSIGNED NOT NULL,
    attempt_date  DATE NOT NULL,
    result        ENUM('ATTEMPT', 'TOP_ROPE', 'REDPOINT', 'FLASH', 'ONSIGHT') NOT NULL,
    notes         TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_logbook_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_logbook_route FOREIGN KEY (route_id) REFERENCES routes (id) ON DELETE CASCADE,
    INDEX idx_logbook_route_result (route_id, result)
) ENGINE=InnoDB;

CREATE TABLE ratings (
    user_id    BIGINT UNSIGNED NOT NULL,
    route_id   BIGINT UNSIGNED NOT NULL,
    score      DECIMAL(2, 1) UNSIGNED NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, route_id),
    CONSTRAINT fk_ratings_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_ratings_route FOREIGN KEY (route_id) REFERENCES routes (id) ON DELETE CASCADE,
    CONSTRAINT chk_ratings_score CHECK (score BETWEEN 0 AND 5 AND MOD(score * 2, 1) = 0)
) ENGINE=InnoDB;

-- ===================== SOCIAL =====================

CREATE TABLE favorites (
    user_id    BIGINT UNSIGNED NOT NULL,
    route_id   BIGINT UNSIGNED NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, route_id),
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_favorites_route FOREIGN KEY (route_id) REFERENCES routes (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE lists (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT UNSIGNED NOT NULL,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    is_public   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lists_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE list_routes (
    list_id  BIGINT UNSIGNED NOT NULL,
    route_id BIGINT UNSIGNED NOT NULL,
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (list_id, route_id),
    CONSTRAINT fk_list_routes_list FOREIGN KEY (list_id) REFERENCES lists (id) ON DELETE CASCADE,
    CONSTRAINT fk_list_routes_route FOREIGN KEY (route_id) REFERENCES routes (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE follows (
    follower_id BIGINT UNSIGNED NOT NULL,
    followed_id BIGINT UNSIGNED NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (follower_id, followed_id),
    CONSTRAINT fk_follows_follower FOREIGN KEY (follower_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_follows_followed FOREIGN KEY (followed_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_follows_not_self CHECK (follower_id <> followed_id)
) ENGINE=InnoDB;

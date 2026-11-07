#!/usr/bin/env bash
# Regenerates database/docker/02-catalog-data.sql from a running MySQL container.
# Usage: scripts/dump-catalog-data.sh [container] [root-password]
set -euo pipefail

CONTAINER="${1:-climbingwiki-db-1}"
PASSWORD="${2:-${MYSQL_ROOT_PASSWORD:-climbingwiki}}"
OUT="$(dirname "$0")/../database/docker/02-catalog-data.sql"

dump() {
  docker exec "$CONTAINER" mysqldump -uroot -p"$PASSWORD" \
    --no-create-info --skip-extended-insert --complete-insert --skip-triggers \
    --skip-add-locks --skip-comments --skip-dump-date --set-gtid-purged=OFF \
    --default-character-set=utf8mb4 climbingwiki "$@" 2>/dev/null
}

{
  cat <<'HEADER'
-- ClimbingWiki catalog data for the Docker demo, loaded once into an empty database
-- (docker-compose.yml mounts this folder into /docker-entrypoint-initdb.d).
--
-- Contents: grading systems and grades, zones, crags, sectors, routes and pitches,
-- plus the two importer accounts that own the imported rows. Their password hashes
-- are replaced with "!" (matches no password): they cannot log in.
--
-- Licences: crag locations (c) OpenStreetMap contributors, ODbL.
-- Mula routes: theCrag contributors, CC BY-NC-SA. This file is shared under the same terms.
--
-- Regenerate from a local database with scripts/dump-catalog-data.sh.

USE climbingwiki;
HEADER
  # Only the importer accounts: real or demo users are never exported.
  # Their password hashes are replaced with "!", which matches no password, so the
  # importers' real credentials never leave the local database.
  dump users --where="username IN ('osm-importer','data-importer')" \
    | sed -E 's/'"'"'[$]2[aby][$][0-9]{2}[$][./A-Za-z0-9]{53}'"'"'/'"'"'!'"'"'/g'
  dump grading_systems grades zones crags sectors routes pitches
} > "$OUT"

echo "Wrote $OUT ($(grep -c '^INSERT' "$OUT") rows)"

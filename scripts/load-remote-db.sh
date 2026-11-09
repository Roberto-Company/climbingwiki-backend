#!/usr/bin/env bash
# Loads the schema and the catalog data into an empty hosted MySQL-compatible database (TiDB Cloud).
# Runs the mysql client from the mysql:8.4 image, so no local install is needed (Docker only).
#
# Usage: scripts/load-remote-db.sh [env-file]
# The env file (default: .env.remote-db, git-ignored) must define:
#   REMOTE_DB_HOST, REMOTE_DB_PORT, REMOTE_DB_USER, REMOTE_DB_PASSWORD
#
# The demo accounts (database/docker/03-demo-users.sql) are NOT loaded: their password is public.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${1:-$ROOT/.env.remote-db}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing $ENV_FILE (see the header of this script)." >&2
  exit 1
fi
# shellcheck disable=SC1090
source "$ENV_FILE"
: "${REMOTE_DB_HOST:?not set}" "${REMOTE_DB_PORT:?not set}" "${REMOTE_DB_USER:?not set}" "${REMOTE_DB_PASSWORD:?not set}"

run_sql() {
  MSYS_NO_PATHCONV=1 docker run --rm -i -e MYSQL_PWD="$REMOTE_DB_PASSWORD" mysql:8.4 \
    mysql -h "$REMOTE_DB_HOST" -P "$REMOTE_DB_PORT" -u "$REMOTE_DB_USER" \
    --ssl-mode=VERIFY_IDENTITY --ssl-ca=/etc/pki/tls/certs/ca-bundle.crt \
    --default-character-set=utf8mb4 "$@"
}

existing="$(run_sql -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'climbingwiki'")"
if [[ "$existing" != "0" ]]; then
  echo "Database climbingwiki already has $existing tables; refusing to load over it." >&2
  exit 1
fi

echo "Loading schema..."
run_sql < "$ROOT/database/schema.sql"
echo "Loading catalog data (this can take a few minutes)..."
run_sql < "$ROOT/database/docker/02-catalog-data.sql"

echo "Row counts:"
run_sql -t climbingwiki -e "
  SELECT 'zones' AS t, COUNT(*) AS n FROM zones UNION ALL
  SELECT 'crags', COUNT(*) FROM crags UNION ALL
  SELECT 'sectors', COUNT(*) FROM sectors UNION ALL
  SELECT 'routes', COUNT(*) FROM routes UNION ALL
  SELECT 'pitches', COUNT(*) FROM pitches UNION ALL
  SELECT 'users', COUNT(*) FROM users"

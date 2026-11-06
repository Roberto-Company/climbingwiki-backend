# Known Issues & Deferred Decisions

Bugs found and fixed, and gaps left open on purpose. Entries move from Open to Resolved when fixed.

## Open

- **No pitch renumbering** (2026-06-23)
  `Pitch.orderIndex` is supplied by the client. Inserting a pitch in the middle of a route means updating every later pitch with `PUT`; there is no endpoint that shifts the rest automatically.

- **No order for routes in a sector** (2026-11-04)
  Routes have no position field, so the left-to-right order from the topo is lost.

- **Source fields not exposed** (2026-11-04)
  `external_source` / `external_id` exist in the database but not in the request/response records, so import scripts match records by name and attribution is stored inside `description`. The frontend splits it out for display.

- **Schema changes are manual** (2026-06-23)
  `database/schema.sql` creates a fresh database; `alter_*.sql` files upgrade older ones by hand. Flyway is planned.

- **`forgot-password` reveals whether an email exists** (2026-06-25)
  404 vs 200. Acceptable for a portfolio project; listed under "If this went to production" in the roadmap.

- **Import accounts are regular users** (2026-11-06)
  `osm-importer` and `data-importer` have the `USER` role, so anything the import scripts add from now on starts unverified. Give them `MODERATOR` before re-running an import.

- **Owner deletes of routes don't check personal data** (2026-11-06)
  An owner can delete their own unverified route when it has no pitches, even if someone logged it or commented on it; those entries go with it (cascade). Rare, since unverified routes are new.

## Future ideas

- **Roles table instead of a single enum column** (2026-06-24)
  `User.role` is one enum (`USER`, `MODERATOR`, `ADMIN`). Simple and enough for now.

- **Grade equivalence across systems** (2026-06-23)
  Map equivalents (French 6a ≈ YDS 5.10a ≈ V3) so a route's grade can be shown in the system the viewer prefers.

## Resolved

- **Any logged-in user could edit or delete any climbing data** (found and fixed 2026-11-06)
  Catalog writes only required a login, and `createdById` was taken from the request body, so anyone could claim to be someone else. Now the creator comes from the token and `CatalogAccess` enforces owner/moderator rules. Grade and grading-system writes are admin-only and tag writes moderator-only.

- **Easy French grades didn't follow Spanish topos** (found 2026-11-04, fixed 2026-11-06)
  The table had 5a/5b/5c (theCrag style) but no 5+. It now uses 3, 4, 4+, 5, 5+, then 6a… Existing pitches moved with `database/alter_french_easy_grades.sql` (5a/5b → 5, 5c → 5+).

- **Catalog filters didn't combine** (found 2026-11-04, fixed 2026-11-05)
  `/routes?sectorId=&name=` used only the first filter. Lists now AND every filter through JPA Specifications.

- **Route lists needed one extra request per route for the grade** (fixed 2026-11-06)
  `RouteResponse` now includes `hardestGradeId` and `totalLengthM`, loaded for a whole page in one query.

- **Anyone could read every user's email** (fixed 2026-11-05)
  `GET /api/users` and `/api/users/{id}` are public; `email` is now returned only to the user themselves or an admin.

- **Login limit shared by all users** (fixed 2026-11-05)
  Logins arrive from the frontend server, so limiting by `getRemoteAddr()` meant 5 attempts per minute for everyone. The limit now uses the visitor's IP from `X-Forwarded-For`, trusted only when the caller is a configured proxy.

- **Rate-limit map grew forever** (fixed 2026-11-05)
  Buckets idle for 10 minutes (fully refilled by then) are evicted.

- **Package and artifact named `project1`** (fixed 2026-11-06)
  Renamed to `com.roberto.climbingwiki` / `climbingwiki`.

- **Security was wide open** (resolved 2026-06-24)
  `permitAll()` everywhere and passwords never verified. Replaced by the JWT auth pass.

- **Duplicate `orderIndex` per route allowed silently** (fixed 2026-06-23)
  `PitchService` now rejects it with 409.

- **Catalog lists had no parent filters** (fixed 2026-06-25)
  Added `?parentZoneId=`, `?rootOnly=`, `?zoneId=`, `?cragId=`, `?sectorId=`.

- **Pending migrations for auth tokens and half-star ratings** (2026-06)
  Both are now part of `database/schema.sql`. Databases created before that need `alter_auth_tokens.sql` and `alter_ratings_half_star.sql`.

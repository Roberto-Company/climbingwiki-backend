# Known Issues & Deferred Decisions

Tracks bugs found, fixed, and design gaps left open on purpose. Updated continuously as work happens — entries move from Open to Resolved when fixed, new ones get added when found.

## Open

- **No pitch reordering/renumbering** (added 2026-06-23)
  `Pitch.orderIndex` is client-supplied (see `PitchRequest`). Inserting a pitch in the middle of an existing sequence requires manually `PUT`-ing every subsequent pitch with a new `orderIndex` — there's no endpoint that shifts the rest of the sequence automatically. Deferred by design: single-user/hand-curated data, YAGNI for now. Revisit if manual renumbering becomes a real pain point.

- **Ratings DB migration pending** (added 2026-06-23)
  `Rating.score` changed from whole-star `1-5` to half-star `0-5` (entity now `BigDecimal`, app validates the 0.5 step). The matching schema change (`database/alter_ratings_half_star.sql`) has **not been run yet** — run it yourself against your existing DB (same as `alter_standardize.sql` before it). Remove this entry once applied. `database/schema.sql` is already updated for fresh installs.

## Future Ideas

- **Migrate `User.role` from a single enum column to a roles table** (added 2026-06-24)
  Auth pass uses a single `role` enum on `User` (`USER`, `MODERATOR`, `ADMIN`) — simplest option, matches existing enum usage elsewhere in the schema (e.g. `LogbookEntry.Result`). Deliberate choice to keep it simple for now. Ownership checks (`AuthenticatedUser.isSelfOrModeratorOrAdmin`) treat `MODERATOR` and `ADMIN` both as override-capable; only role-assignment itself (`AuthenticatedUser.isAdmin`) is admin-strict. If more roles or multiple roles per user are ever needed, migrate to a separate roles/permissions table (many-to-many with `User`) instead of growing the enum.

- **Master grade-equivalence table** (added 2026-06-23)
  Idea from conversation: a master table of climbing grades that maps equivalents across systems (e.g. French 6a ≈ YDS 5.10a ≈ V3), so a route's difficulty can be displayed in whichever grading system the viewing user/caller prefers, instead of being locked to the system it was originally graded in. Not built — current `GradingSystem`/`Grade` model is per-system only, no cross-system mapping. Revisit if multi-system display becomes a real requirement.

## Resolved

- **Security was wide open** (flagged in handoff, predates this doc; resolved 2026-06-24)
  `SecurityConfig` previously had `permitAll()` on every request with CSRF disabled, and `User.passwordHash` was write-only (hashed on save, never verified). Resolved by a full JWT auth pass: `POST /api/auth/register` + `POST /api/auth/login` (`AuthController`/`AuthService`) issue HS256-signed tokens (`jjwt`, `JwtService`) carrying user id/username/role; `JwtAuthenticationFilter` validates the `Authorization: Bearer <token>` header on every request (stateless, no refresh token, no server-side revocation — client just discards the token to "log out"). `SecurityConfig` now requires authentication for all non-GET `/api/**` requests (GETs and `/api/auth/**` stay public; `POST /api/users` is `ADMIN`-only). Wiki-style write policy: any authenticated user can create/edit climbing data (Route, Crag, Sector, Zone, Tag, Pitch, GradingSystem, Grade) — enforced at the URL level, no extra checks. Personal resources (User, Comment, Photo, LogbookEntry, Rating, Favorite, Follow, RouteList, ListRoute) additionally enforce ownership at the service layer via `AuthenticatedUser.isSelfOrModeratorOrAdmin(ownerId)`, throwing `ForbiddenException` (403) otherwise.

- **Duplicate `orderIndex` per route allowed silently** (found 2026-06-23, fixed 2026-06-23)
  `POST`/`PUT /api/pitches` could create two pitches with the same `routeId` + `orderIndex` with no error — silently inconsistent data, no warning. Fixed in `PitchService.applyRequest` via `PitchRepository.findByRouteIdAndOrderIndex`, throwing `PitchOrderConflictException` → `409 Conflict` (`GlobalExceptionHandler`). Updating a pitch while keeping its own `orderIndex` is correctly excluded from the conflict check.

- **Auth tokens DB migration pending** (added 2026-06-25, resolved 2026-06-25)
  `User` gained `email_verified`/`verification_token`/`verification_token_expires_at`/`reset_token`/`reset_token_expires_at` for the email-verification and password-reset flows. The matching schema change (`database/alter_auth_tokens.sql`) had not been run against the dev DB. Resolved by the FrontendApp session, which needed it to test Phase 1 auth end-to-end: asked the user first, got approval, ran it against the local `climbingwiki-db` container.

- **Catalog list endpoints had no parent-filter query params** (found 2026-06-25 by the FrontendApp session, fixed 2026-06-25)
  `ZoneController`/`CragController`/`SectorController`/`RouteController` only ever supported full unfiltered pagination — only `PitchController` had a parent filter (`?routeId=`). This made hierarchical catalog browsing (e.g. "this zone's child crags") impossible without fetching every page and filtering client-side. Added `?parentZoneId=`/`?rootOnly=true` to `/api/zones`, `?zoneId=` to `/api/crags`, `?cragId=` to `/api/sectors`, `?sectorId=` to `/api/routes` — same pattern as the existing Pitch filter. Default (no param) behavior is unchanged. 12 new tests added (service + `@WebMvcTest` per new method); full suite 484/484 green.

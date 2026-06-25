# Roadmap

A running picture of where ClimbingWiki stands and what's left. Updated as milestones land — see `KNOWN_ISSUES.md` for granular bugs/deferred decisions, this file is the bird's-eye view.

## Done

**Domain model & CRUD (12 resources, full REST surface each)**
Zone (recursive) → Crag → Sector → Route → Pitch, plus Tag, GradingSystem, Grade, and the personal-data layer: Comment, Photo, LogbookEntry, Rating, Favorite, Follow, RouteList, ListRoute, User. Every resource has entity, repository, DTO request/response, service, controller, and unit tests (service + controller, Mockito/`@WebMvcTest`).

**Data integrity rules**
- Pitch `orderIndex` conflict detection (409 on duplicate `routeId`+`orderIndex`)
- Rating half-star (`0–5`, `BigDecimal`) step validation
- Route-level optional `latitude`/`longitude` for per-route pinning independent of sector location

**Authentication & authorization (full pass, 2026-06-24)**
- `POST /api/auth/register` / `POST /api/auth/login` issuing HS256 JWTs (`jjwt`), 24h expiry, no refresh token
- `JwtAuthenticationFilter` validating `Authorization: Bearer` on every request, stateless (no server-side revocation)
- 3-tier role model (`USER`/`MODERATOR`/`ADMIN`) on `User`
- URL-level policy: wiki data (Route/Crag/Sector/Zone/Tag/Pitch/GradingSystem/Grade) writable by any authenticated user; GETs public; `POST /api/users` admin-only
- Service-level ownership checks (`AuthenticatedUser.isSelfOrModeratorOrAdmin`) on all 8 personal resources + User, throwing `ForbiddenException` → 403

**Tooling/infra**
- Swagger/OpenAPI UI (`springdoc-openapi`)
- Docker Compose MySQL 8.4 dev DB
- Global exception handler (31 domain exceptions mapped to proper HTTP statuses)
- 439 unit tests passing, 0 known failures

**Pagination & sorting (2026-06-25)**
- `PagedResponse<T>` envelope (`content`/`page`/`size`/`totalElements`/`totalPages`) wrapping Spring Data's `Page<T>`
- Applied to the 9 list endpoints most likely to grow with usage: Route, Crag, Sector, Zone, Tag, Pitch, Comment, Photo, LogbookEntry — each accepts `?page=&size=&sort=`, default size 20, hard cap 100 (`spring.data.web.pageable.max-page-size`)
- Deliberately *not* applied to: GradingSystem/Grade (small fixed lookup tables), Favorite/Follow/RouteList/ListRoute/User and Rating (composite-key/personal resources with small per-user cardinality) — easy to extend later with the same pattern if usage proves otherwise

**OpenBeta bulk import (2026-06-25)**
- `external_source`/`external_id` columns (nullable, composite-unique) added to zones/crags/sectors/routes, enabling re-import without duplication
- `openbeta` package: `OpenBetaClient` (GraphQL via `RestClient` against `bulkAreas`, fetches a full area tree in one call), DTOs mirroring OpenBeta's schema, `OpenBetaImportService` (idempotent upsert mapping: non-leaf Area → Zone recursively, leaf Area → Crag + one default Sector, each climb → Route + Pitch)
- Route type resolution priority SPORT > TRAD > BOULDER; grade resolution tries French > YDS > V-Scale > Font > UIAA > Ewbank against existing `GradingSystem`/`Grade` rows, leaving unmatched grades null (reported via `gradesUnmatched` counter) rather than inventing data
- `OpenBetaImportRunner` (`CommandLineRunner`) only active under the `openbeta-import` Spring profile — never runs on a normal boot
- Requires the relevant `GradingSystem`/`Grade` rows to be seeded first (exact names: French, YDS, V-Scale, Font, UIAA, Ewbank) or grades stay unmatched
- Verified: compiles clean, full test suite passes (`OpenBetaImportServiceTest` included), no HTTP-facing attack surface since the import path is only reachable via profile-gated CLI

**Login rate limiting (2026-06-25)**
- `LoginRateLimiterService` — in-memory token bucket per client IP (`bucket4j_jdk17-core`), 5 attempts/minute, greedy refill
- Wired into `AuthController.login` before delegating to `AuthService`; rejection throws `TooManyLoginAttemptsException` → 429 via `GlobalExceptionHandler`
- Single-instance/in-memory by design, consistent with current deployment (no Redis/shared store needed); buckets are never evicted, which is fine at personal scale but would need a TTL/cleanup pass before any high-traffic multi-instance deployment
- 450/450 tests passing after the change (`LoginRateLimiterServiceTest` + new `AuthControllerTest` 429 case)

**Email verification & password reset (2026-06-25)**
- `User` gains `emailVerified`, `verificationToken`/`verificationTokenExpiresAt`, `resetToken`/`resetTokenExpiresAt` (migration: `database/alter_auth_tokens.sql`, not yet run against the live DB — see Open gaps)
- `POST /api/auth/register` now also generates a verification token (24h TTL) and logs it server-side; `POST /api/auth/verify-email` consumes it
- `POST /api/auth/forgot-password` generates a reset token (1h TTL), logs it, and returns it directly in the response; `POST /api/auth/reset-password` consumes it to set a new password hash
- Deliberately stubbed, no real email delivery (no SMTP dependency, no external provider account needed) — tokens are returned/logged instead of emailed. Swap in real email later without changing the token logic
- Reused/added: `InvalidOrExpiredTokenException` (400) for bad/stale tokens, `UserNotFoundException(String email)` overload for the forgot-password lookup
- 470/470 tests passing

## Open gaps (known, deferred on purpose — see KNOWN_ISSUES.md)

- Ratings DB migration (`alter_ratings_half_star.sql`) not yet run against your live DB
- Auth tokens DB migration (`alter_auth_tokens.sql`) not yet run against your live DB
- No pitch auto-renumbering (manual `PUT` cascade required)
- `User.role` is a single enum column, not a roles table (fine at current scale)
- No cross-grading-system equivalence table (a route's grade is locked to the system it was graded in)
- OpenBeta import is one-shot per `ancestors` path with no scheduling/automation — re-running is a manual CLI invocation
- Login rate-limit buckets are per-IP and never evicted (unbounded map growth under high distinct-IP traffic) — non-issue at personal scale
- Email verification/password reset are stubs: tokens are logged/returned directly instead of emailed — fine for personal/dev use, must be replaced with real email delivery before any real multi-user exposure
- `forgot-password` reveals whether an email exists (404 vs 200), a minor enumeration leak — consistent with how the rest of the API already does explicit 404s, not hardened further

## Not started — candidate next steps

Roughly in order of "blocks other things" → "polish," not a commitment:

1. **Search/filtering beyond exact-match params** — current `@RequestParam` filters are simple equality (e.g. `?listId=1`). No text search on route/crag names, no combined filters (grade range + region + tag). Blocked for now: no real data loaded yet to search against.
2. **Integration tests** — current suite is unit tests only (Mockito-mocked repositories). No Testcontainers/`@SpringBootTest` coverage exercising real JPA queries against MySQL, no end-to-end auth-flow test hitting actual endpoints. Blocked for now: no real data loaded yet to validate against.
3. **Deployment** — only local Docker Compose for the DB exists. No production build profile, no CI pipeline, no deployment target decided.

Auth hardening (rate limiting + email verification + password reset, all above) is now done at the "no real email provider" stub level. Frontend is intentionally being built as a separate repo, not tracked here.

## Not a gap, just noting

Photo storage is URL-only (no file upload/hosting) — consistent with the wiki's "you bring your own image host" design, not a missing feature unless that assumption changes.

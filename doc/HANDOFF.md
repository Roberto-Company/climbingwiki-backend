# Handoff — ClimbingWiki (2026-06-25)

Written because the Claude Code session tied to the old path (`C:\Users\robec\Desktop\DAM-2\Acceso-a-datos\project1`) won't carry over once this folder moves. Read this first in the new session.

## Relocation — DONE (via copy, not move) — one cleanup item left

- **Old path (still exists, do not assume it's gone):** `C:\Users\robec\Desktop\DAM-2\Acceso-a-datos\project1`
- **New backend path (this is it):** `C:\Users\robec\Desktop\spring-boot-projects\ClimbingWikiProject\BackendApi`
- **New frontend path (to be created next):** `C:\Users\robec\Desktop\spring-boot-projects\ClimbingWikiProject\FrontendApp`
- `Move-Item`/`mv` kept failing with "file/resource in use" even with IntelliJ fully closed and no `java.exe` processes running. Root cause: the project1 directory **itself** (not any file inside it) was held by a directory-level lock that didn't go away — most likely a long-lived shell process tied to this Claude Code session's "primary working directory," which the harness keeps pinned to that path between tool calls (confirmed via a plain in-place `Rename-Item` on `project1` itself also failing the same way, while renaming every item *inside* it succeeded fine).
- **Resolved by `robocopy /E /COPY:DAT` instead of a true move**: copied 568 files / 80 directories, 0 errors, to `BackendApi`. Verified file count matches at destination.
- **Cleanup still needed:** the old `C:\Users\robec\Desktop\DAM-2\Acceso-a-datos\project1` folder still exists and is still locked — don't try to delete it from inside a Claude Code session whose primary working directory is still pointed at it. Either: (a) the user deletes it manually once they've closed/restarted whatever session has it pinned, or (b) a *future* Claude Code session whose primary working directory is `BackendApi` (not the old path) tries `Remove-Item -Recurse -Force` on the old path — ask the user first, it's a delete.
- **`.idea/` was copied along with everything else** — it has stale absolute paths pointing at the old `project1` location. Don't expect IntelliJ to just reopen cleanly; the user will likely need to re-open `BackendApi` as a project (or delete `.idea/` and let IntelliJ regenerate it).
- **`spring-boot-projects`** is itself an existing git repo (remote: `https://github.com/robertocompanyzestudios/spring-boot-projects.git`) full of unrelated coursework subprojects (Springboot-di, springboot-aop, springboot-crud, etc.).
- **Important — do not confuse:** `spring-boot-projects/climbingWikiLoc` already exists there but is a **completely unrelated** practice project (package `com.example.climbingwikiloc`, hexagonal architecture, Vite+React+Tailwind frontend, from the repo's initial commit). It is *not* related to this ClimbingWiki API and should not be touched, merged, or referenced.
- Next session: scaffold `FrontendApp` with **Next.js + TypeScript** (user's explicit choice) as a sibling folder; revisit the old-path cleanup above when convenient.

## What this project is

Spring Boot REST API for a climbing wiki (zones/crags/sectors/routes/pitches + personal layer: comments, photos, logbook, ratings, favorites, follows, route lists, users). Java, Maven, MySQL via Docker Compose, JWT auth, Swagger/OpenAPI.

## Critical context: another session is seeding the DB

**Do not touch DB seeding/import work** — per explicit, repeated user instruction, a *different* Claude Code session owns this. This session only verified the existing seed/import code compiles and has no HTTP attack surface; it did not run it and should not.

The seeding machinery already exists in this repo, ready for that other session to use:
- `seed/GradeSeedRunner` — `@Profile("seed-grades")`, reads `src/main/resources/seed/grades.csv` (165 rows: French/YDS/V-Scale/Font/UIAA/Ewbank), populates `GradingSystem`/`Grade`. Run: `./mvnw spring-boot:run -Dspring-boot.run.profiles=seed-grades`
- `openbeta/OpenBetaImportRunner` — `@Profile("openbeta-import")`, pulls a real area tree from OpenBeta's GraphQL API (`bulkAreas`) and upserts Zone→Crag→Sector→Route→Pitch idempotently. Run: `./mvnw spring-boot:run -Dspring-boot.run.profiles=openbeta-import -Dspring-boot.run.arguments="--openbeta.import.ancestors=<uuid>,...,<uuid>"`. Needs grades seeded first or grades stay unmatched.

**For the other session's own up-to-date status (already run grade seeding live: `grading_systems=6`/`grades=165`; an `openbeta-preview` dry-run pending a retry after an OpenBeta API 504), see `doc/HANDOFF-openbeta-import.md` — that one is theirs, this paragraph is just a pointer, don't merge the two files.**

## Database — pending manual migrations (not yet run against the live DB)

Already reflected in `database/schema.sql` for fresh installs; this is a delta for an existing DB. **Don't run it yourself without the user's explicit go-ahead** — same boundary as always.

- `database/alter_ratings_half_star.sql` — `Rating.score` whole-star → half-star `BigDecimal`. **Still pending.**
- `database/alter_auth_tokens.sql` — adds `email_verified`/`verification_token(_expires_at)`/`reset_token(_expires_at)` to `users`. **Applied 2026-06-25** by the FrontendApp session (asked the user first, approved, then ran it against the local `climbingwiki-db` container) — Phase 1 auth needed it to test register/login end-to-end. Remove this line once you've confirmed; kept for now as a trail.

## Done (full detail in ROADMAP.md)

- Full CRUD domain model (12 resources), unit-tested (service + controller, Mockito/`@WebMvcTest`)
- JWT auth (register/login, `JwtService`, `JwtAuthenticationFilter`, stateless, no refresh/revocation), 3-tier roles (USER/MODERATOR/ADMIN), ownership checks via `AuthenticatedUser.isSelfOrModeratorOrAdmin`
- Pagination/sorting on the 9 highest-growth list endpoints
- OpenBeta bulk import (profile-gated, see above)
- Login rate limiting (`LoginRateLimiterService`, Bucket4j, 5/min/IP → 429)
- Email verification + password reset, deliberately stubbed (tokens logged/returned directly, no real SMTP) — `POST /api/auth/verify-email`, `/forgot-password`, `/reset-password`
- **Just fixed and verified this session:** Swagger UI 401 — `/swagger-ui.html` (bare path) wasn't covered by the `/swagger-ui/**` matcher in `SecurityConfig.java`; added it explicitly to `permitAll()`. Verified live: 470/470 tests pass, and `/swagger-ui.html`→302, `/swagger-ui/index.html`→200, `/v3/api-docs`→200, `/actuator/health`→200.
- **Added 2026-06-25 by the FrontendApp session (Phase 2 catalog browsing):** optional parent-filter query params on the catalog list endpoints, mirroring the pattern `PitchController` already had (`?routeId=`). None of `ZoneController`/`CragController`/`SectorController`/`RouteController` had any filter param before this — only full unfiltered pagination — which made "show me this zone's children" impossible without fetching everything client-side and filtering in the browser. Now:
  - `GET /api/zones?parentZoneId=<id>` — children of a zone
  - `GET /api/zones?rootOnly=true` — top-level zones only (`parentZone IS NULL`)
  - `GET /api/crags?zoneId=<id>`, `GET /api/sectors?cragId=<id>`, `GET /api/routes?sectorId=<id>`
  - Default behavior (no param) unchanged — still returns everything, paginated, for any future flat/admin view.
  - 484/484 tests pass (12 new ones added, one per new `Service`/`Controller` method, following existing Mockito/`@WebMvcTest` conventions).

## Open gaps (see KNOWN_ISSUES.md for full detail)

- The two pending SQL migrations above
- No pitch auto-renumbering (manual `PUT` cascade)
- `User.role` is a single enum, not a roles table
- No cross-grading-system equivalence table
- Login rate-limit buckets never evicted (non-issue at personal scale)
- Email verification/password reset are stubs — must add real email delivery before any real multi-user exposure
- `forgot-password` reveals whether an email exists (404 vs 200) — minor, consistent with rest of API

## Not started — candidate next steps

1. **Search/filtering** beyond exact-match — blocked until real data is loaded (the other session's job)
2. **Integration tests** (Testcontainers/`@SpringBootTest`) — same blocker
3. **Deployment prep** — fully unblocked, doesn't depend on data. Nothing exists yet beyond the dev `docker-compose.yml` for MySQL: no Dockerfile for the app itself, no CI, no prod profile hardening, no deployment target chosen. This was the next thing under discussion when the relocation came up.

## Frontend

User explicitly decided: **separate repo**, not inside this backend project. Stack: Next.js + TypeScript. Status as of 2026-06-25: Phase 0 (scaffold) and Phase 1 (auth) done and pushed to a private GitHub repo (`climbingwiki-frontend`); Phase 2 (catalog browsing) in progress, which is what prompted the filter-param additions above. See `FrontendApp/doc/HANDOFF.md` and `FrontendApp/doc/DECISIONS.md` for full detail — don't duplicate it here.

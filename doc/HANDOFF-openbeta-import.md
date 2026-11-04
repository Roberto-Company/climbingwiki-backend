# Handoff — OpenBeta import pipeline (this session)

This is a **second, separate handoff** scoped to the OpenBeta data-import work specifically
(grading-system seeding + crag/route import from OpenBeta's API). For the project-relocation
context (the project is mid-move to `spring-boot-projects/ClimbingWikiProject/BackendApi`,
blocked by IntelliJ file locks) and the other session's auth/password-reset work, see
`HANDOFF.md` in this same `doc/` folder — that one is owned by the other concurrent session,
don't merge into it.

All paths below are relative to the project root, wherever it ends up after the move.

## Critical constraints (read first)

- **Two Claude Code sessions work on this repo concurrently.** Re-read files before editing —
  don't trust file state matches what's described here. The other session owns the
  auth/password-reset feature (`config/SecurityConfig.java`, `controllers/AuthController.java`,
  `services/AuthService.java`, `dtos/{ForgotPasswordRequest,ForgotPasswordResponse,
  ResetPasswordRequest,VerifyEmailRequest}.java`, `exceptions/UserNotFoundException.java`, plus
  tests) and the project relocation. **Don't touch those.**
- **The project may be mid-relocation.** Per `../HANDOFF.md`, the move to
  `C:\Users\robec\Desktop\spring-boot-projects\ClimbingWikiProject\BackendApi` was blocked by
  IntelliJ holding file locks (compile server + a running `Project1Application` instance on port
  8080). Confirm the current project path before doing anything — it may no longer be
  `C:\Users\robec\Desktop\DAM-2\Acceso-a-datos\project1`.
- **Shared dev DB caution.** `ddl-auto=none`; schema changes only via manual SQL in `database/`.
  The permission classifier already blocked one autonomous direct-SQL write to the shared DB
  earlier in this work — flag any live-DB write to the user first, even additive/non-destructive
  ones.
- **Docker DB access pattern** (password env var must be passed *into* `docker exec`):
  ```
  docker exec -e MYSQL_PWD=climbingwiki climbingwiki-db mysql -u root -N -e "<query>" climbingwiki
  ```
  Container: `climbingwiki-db`, host port 3307→3306.
- **Don't touch port 8080 / IntelliJ's processes** — that's the other session's app and/or the
  relocation blocker, not something to kill casually.
- One-shot `CommandLineRunner` profiles (`seed-grades`, `openbeta-preview`, `openbeta-import`)
  boot a full web server and **don't exit on their own** — after a run finishes/fails, find and
  stop the specific JVM you started (match by start time / port), e.g.:
  ```
  powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Select-Object ProcessId, CreationDate, CommandLine"
  ```

## What's done

1. **Grade seeding, CSV-based (replaced an earlier raw-SQL approach per user's choice).**
   - `src/main/resources/seed/grades.csv` — 6 grading systems (French, YDS, V-Scale, Font, UIAA,
     Ewbank), header `grading_system,category,label,difficulty_score`.
   - `src/main/java/.../seed/{GradeSeedResult,GradeSeedService,GradeSeedRunner}.java` —
     idempotent (matches existing rows by name/label, never overwrites admin edits).
   - Profile `seed-grades`. **Already run successfully against the live DB**: confirmed
     `grading_systems`=6, `grades`=165 rows.
   - Command: `./mvnw spring-boot:run -Dspring-boot.run.profiles=seed-grades -Dspring-boot.run.arguments="--server.port=<free port>"`

2. **OpenBeta import bug fixes** (found via live API verification, before any real import ran):
   - `bulkAreas` takes `ancestors: [String!]!` (a **list**), not a single string. Fixed in
     `OpenBetaClient.fetchBulkAreas(List<String>)`, `OpenBetaImportService.importFromAncestorsPath(List<String>)`,
     `OpenBetaImportRunner` (still takes a comma-separated CLI string, splits it into the list).
   - OpenBeta uses `-1` as a "length unknown" sentinel — normalized to `null` via
     `OpenBetaImportService.normalizeLength(Integer)`.

3. **Dry-run / CSV preview mode** — the data-sanitization step the user asked for, so nothing
   untrusted hits the shared DB sight-unseen:
   - `OpenBetaPreviewRow.java` — flat record (level, external id, parent id, name, lat/lng,
     route_type, grade_system, grade_label, grade_matched, length_m, first_ascent_by/date, issues).
   - `OpenBetaPreviewCsvWriter.java` — writes that to CSV (handles comma/quote escaping).
   - `OpenBetaImportService.preview(List<OpenBetaAreaDto>)` — walks the same area/climb tree the
     real import would, **never calls a repository save**, flags:
     `MISSING_NAME`, `MISSING_COORDINATES`, `NO_GRADE_DATA`, `UNMATCHED_GRADE` (label didn't match
     any seeded `Grade` — how Spain's non-standard "4C"-style grades would show up),
     `LENGTH_UNKNOWN` (the -1 sentinel), `DUPLICATE_COORDS_WITH_PARENT` (route coords identical to
     the crag centroid — confirmed present in the real Lizard Boulder data).
   - `OpenBetaPreviewRunner.java` — profile `openbeta-preview`, writes to
     `target/openbeta-preview.csv` by default (override with `--openbeta.preview.output=<path>`).
   - Extracted `normalizeLength`/`parseFirstAscent` as shared pure helpers so the preview and the
     real import can't drift apart on business rules.
   - Tests added in `OpenBetaImportServiceTest` (preview flags) and new
     `OpenBetaPreviewCsvWriterTest` (CSV escaping). **Full suite: 474 tests, 0 failures, 0 errors**
     as of the last run before this handoff was written.

## Verified test target (chosen over Spain)

**Lizard Boulder**, Smith Rock Bouldering, Smith Rock, Central Oregon, USA. 6 real climbs, real
names, real Font/V-scale grades (V0–V3, Font 4–6A), confirmed leaf area.

Full ancestors path (root-to-target, inclusive — pass exactly this as `openbeta.import.ancestors`,
comma-separated):
```
1db1e8ba-a40e-587c-88a4-64f5ea814b8e,5f05ed84-1a18-52d9-b8cf-77f8fd01e061,e7aeefae-15e1-5de7-a395-a48727784e98,f3c655b6-9c95-597f-aaa6-fd7d8a4865ec,e09fd62a-61aa-5668-aa0d-35813f7c9dcc,ef8e5f0a-5180-5ed0-93db-073c64c6fae6
```

**Spain was explicitly deferred** (user decision via AskUserQuestion): OpenBeta's Spain coverage
is too sparse (61 climbs country-wide; Siurana/Margalef/El Chorro/Montserrat/Mallos de
Riglos/Chulilla are all empty placeholders). Only El Helechal (25), Valencia (19), Jérica (8),
Catalunya (7), Alcañiz (2) have real data, and El Helechal's quality is poor (mostly unnamed
climbs, no FA, length always -1, non-standard grade notation like "4C"/"5B"). Revisit later, not
abandoned.

## Status: done

Lizard Boulder import completed successfully against the live shared DB this session. No more
next steps for this target — Spain remains explicitly deferred (see above) for whenever that's
picked up.

Timeline: two `openbeta-preview` attempts hit Cloudflare 504s from `api.openbeta.io`
("retryable": true, "retry_after": 120 — OpenBeta being flaky, not our bug). A third attempt
succeeded cleanly (~8s, no 504): `target/openbeta-preview.csv` had 7 rows (1 crag + 6 routes), 6
flagged, all benign (`DUPLICATE_COORDS_WITH_PARENT` + `LENGTH_UNKNOWN` on every route — expected).
The first real `openbeta-import` attempt then hit a **fresh** 504 of its own, because at the time
preview and import each did their own independent live `fetchBulkAreas` call — a clean preview
gave no guarantee about import's separate fetch. Confirmed via direct log/DB inspection (not the
background-task notification, which falsely reported exit 0) that nothing was written.

**Fix #1 (decoupling):** `openbeta-preview` now also caches its raw fetch to
`target/openbeta-raw-cache.json`. `openbeta-import` accepts `--openbeta.import.cache=<path>` to
import that exact cached payload with **zero live API calls** — falls back to a live fetch only
if the flag is omitted. Removed the now-dead `OpenBetaImportService.importFromAncestorsPath` and
its direct `OpenBetaClient` dependency; updated `OpenBetaImportServiceTest` accordingly.

**Fix #2 (`DUPLICATE_COORDS_WITH_PARENT`)**: `upsertRoute` now nulls out a route's lat/lng when
they exactly match the parent crag's, via a new `sameCoordinates` helper (`BigDecimal.compareTo`,
scale-safe).

**Fix #3 (cache deserialization bug, found when actually running the cached import):**
`OpenBetaAreaDto.isLeaf()` is a record method following the JavaBean `isXxx()` getter convention,
so Jackson's *serializer* synthesized an extra `"leaf"` JSON property when the preview wrote the
cache — but the canonical record constructor (used for *deserialization*) has no `leaf` parameter,
so reading the cache back threw `UnrecognizedPropertyException: Unrecognized field "leaf"`. Fixed
by configuring `OpenBetaImportRunner`'s `ObjectMapper` with
`.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)` rather than touching the
DTO's public API. (Separately, both runners construct their own `ObjectMapper` directly rather
than relying on Spring DI — `spring-boot-starter-webmvc` in Spring Boot 4 doesn't transitively
pull in `JacksonAutoConfiguration`, so no `ObjectMapper` bean exists in this app's context.)

Full suite green (474 tests) after all three fixes. The cached import then ran clean:
`OpenBeta import finished: zones[+0 ~0] crags[+1 ~0] sectors[+1 ~0] routes[+6 ~0] grades
unmatched=0`, verified against direct DB row counts (see below) — not just the runner's own log
line.

## Current live DB state (last checked this session)

```
grading_systems = 6, grades = 165
zones = 2, crags = 2, routes = 9, pitches = 6
```

Pre-import baseline was `zones=2, crags=1, routes=3, pitches=0`; the Lizard Boulder import added
exactly `+1 crag, +1 sector, +6 routes, +6 pitches` (sectors aren't in the row-count query above —
confirmed via the runner's own counters instead).

## Useful commands

- Compile only: `./mvnw -q -o compile`
- Full test suite: `./mvnw -q -o test`, then check `target/surefire-reports/*.txt` for
  `Tests run:` lines if console output gets truncated.
- DB row counts:
  ```
  docker exec -e MYSQL_PWD=climbingwiki climbingwiki-db mysql -u root -N -e "SELECT (SELECT COUNT(*) FROM grading_systems), (SELECT COUNT(*) FROM grades), (SELECT COUNT(*) FROM zones), (SELECT COUNT(*) FROM crags), (SELECT COUNT(*) FROM routes), (SELECT COUNT(*) FROM pitches);" climbingwiki
  ```

## Resolved decisions (this session)

- **`DUPLICATE_COORDS_WITH_PARENT` handling**: `upsertRoute` now nulls out a route's lat/lng when
  they exactly match the parent crag's (instead of storing the meaningless duplicate). Implemented
  via a new `sameCoordinates(BigDecimal, BigDecimal, BigDecimal, BigDecimal)` helper using
  `compareTo` (scale-safe) in `OpenBetaImportService`; `upsertRoute`/`importClimbs` now take the
  `Crag` as a parameter to compare against.
- **Preview/import API-call coupling**: previously each profile made its own independent live
  fetch, so a clean preview gave no guarantee the import's separate fetch would also succeed (and
  in fact it didn't — see timeline above). Fixed by caching the preview's raw fetch to
  `target/openbeta-raw-cache.json` and adding `--openbeta.import.cache=<path>` to the import
  runner so it can import that exact payload with zero additional API calls.

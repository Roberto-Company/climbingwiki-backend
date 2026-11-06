# Roadmap — climbingwiki-backend

**Direction:** ClimbingWiki is a portfolio project, not a production service. The goal is a finished, well-tested core that is easy to run and review. Production-only concerns are documented as decisions instead of built (see "If this went to production").

Granular bugs and deliberate trade-offs: [`KNOWN_ISSUES.md`](./KNOWN_ISSUES.md).

## Done

- **Domain model and CRUD:** Zone (recursive) → Crag → Sector → Route → Pitch, Tag, GradingSystem, Grade, plus the personal layer (Comment, Photo, LogbookEntry, Rating, Favorite, Follow, RouteList, ListRoute, User). Entities, repositories, request/response records, services, controllers.
- **Data rules:** unique pitch order per route (409 on conflict), half-star ratings (0–5 in 0.5 steps), optional per-route coordinates.
- **Auth:** register/login with JWT (24 h, no refresh), three roles, public reads, authenticated writes, ownership checks on personal data. Email verification and password reset in demo mode (tokens returned/logged, no email sent).
- **Privacy:** user emails are returned only to the user themselves and admins.
- **Login rate limit:** 5 attempts per minute per real client IP. `X-Forwarded-For` is trusted only from `TRUSTED_PROXIES`; idle buckets are evicted after 10 minutes.
- **Lists:** pagination with a `PagedResponse` envelope (max size 100); catalog filters combine (`/routes?sectorId=&name=`, `/crags?zoneId=&name=`, `/zones?parentZoneId=|rootOnly=&name=`) via JPA Specifications.
- **Route summaries:** `RouteResponse` includes `hardestGradeId` and `totalLengthM`, loaded for a whole page in one query.
- **Data:** grade tables seeded from CSV (`seed-grades` profile); OpenBeta import (`openbeta-import` profile); crags from OpenStreetMap and Mula routes from theCrag, loaded with import scripts.
- **Tooling:** Swagger UI, Docker Compose MySQL, global exception handler, ~520 tests (unit + H2 repository tests).

## Next

1. **Flyway** so `schema.sql` and its upgrades apply automatically.
2. **One-command run:** Dockerfile and a full-stack `docker compose up` with seeded data and demo accounts.
3. **Integration tests** with Testcontainers (MySQL), including the full register → login → write flow.
4. **CI:** GitHub Actions running the test suite.
5. **Light production settings:** no default `JWT_SECRET` outside a dev profile, `show-sql` off, CORS configuration.
6. **README polish:** architecture diagram and screenshots.

## If this went to production

Deliberately not built, because nobody runs this in production:

- Real email delivery (SMTP or a provider) for verification and password reset, and no tokens in responses.
- Refresh tokens and token revocation.
- Shared rate-limit store (e.g. Redis) for more than one instance.
- Image upload and storage instead of photo URLs.
- Neutral `forgot-password` response, so it doesn't reveal whether an email is registered.
- Hosting, backups and monitoring.

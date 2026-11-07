# ClimbingWiki — backend

[![CI](https://github.com/Roberto-Company/climbingwiki-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/Roberto-Company/climbingwiki-backend/actions/workflows/ci.yml)

REST API for **ClimbingWiki**, a community wiki and logbook for rock climbing. It models the guidebook hierarchy (zones → crags → sectors → routes → pitches, with grading systems and grades) plus a personal layer for climbers (logbook, ratings, comments, photos, favorites, route lists, follows).

> Portfolio project. Not a production service. The web client lives in [climbingwiki-frontend](https://github.com/Roberto-Company/climbingwiki-frontend).

## Stack

- **Java 21**, **Spring Boot 4** (Web, Data JPA, Security, Validation)
- **MySQL 8.4** (Docker Compose for local development)
- **JWT** auth (jjwt, HS256) with `USER` / `MODERATOR` / `ADMIN` roles
- **Bucket4j** login rate limiting, **springdoc-openapi** (Swagger UI)
- **JUnit 5, Mockito, AssertJ**, H2 for repository query tests

## Highlights

- **18 resources** under `/api/*`, paginated (`?page=&size=&sort=`, max 100) with a consistent `PagedResponse` envelope.
- **Combinable filters** on catalog lists (e.g. `/api/routes?sectorId=6&name=marta`), built with JPA Specifications; LIKE wildcards in user input are escaped.
- **Route summaries:** every route carries its hardest grade and total length, computed for a whole page in one query (no N+1).
- **Verified and unverified climbing data:** anyone logged in can add zones, crags, sectors, routes and pitches; their additions start **unverified**. Owners can change their own unverified items; moderators and admins can change anything and verify it (`PUT /api/{type}/{id}/verification`). The creator always comes from the login token, never from the request body.
- **Profiles:** users edit their name and bio (`PUT /api/users/{id}/profile`) and upload a profile picture (`PUT /api/users/{id}/avatar`, multipart). The picture is stored in the database, its type is checked from the file's bytes (JPEG, PNG or WebP only), and it is served with a versioned URL so caches never show an old one. `GET /api/users/{id}/stats` returns ascents, sends, hardest send and follower counts.
- **Moderation queue:** `GET /api/moderation/unverified` (moderators and admins) lists the newest unverified items of every kind with their parent and creator, loaded with entity graphs in one query per kind.
- **Security:** public reads, authenticated writes, ownership checks on personal data (owner, moderator or admin). Grade scales are admin-only, tags moderator-only. Emails are visible only to their owner and admins.
- **Login rate limit per real client IP:** `X-Forwarded-For` is trusted only from configured proxies; idle buckets are evicted.
- **Data import:** grade tables seeded from CSV; crags from OpenStreetMap and routes from theCrag/OpenBeta, upserted by external id so imports can be re-run.

## Run it

### Whole stack with Docker (one command)

Requires Docker only. Clone this repo and [climbingwiki-frontend](https://github.com/Roberto-Company/climbingwiki-frontend) into the same folder, then:

```bash
docker compose up --build
```

| What | URL |
|---|---|
| Website | http://localhost:3000 |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| Health check | http://localhost:8080/actuator/health |

On the first start MySQL creates the schema and loads the catalog (19 zones, 24 crags, 59 sectors, 129 routes) and three demo accounts, all with the password `climbing-demo`:

| User | Role | Can |
|---|---|---|
| `demo` | USER | logbook, ratings, comments, lists; add data (starts unverified) |
| `moderator` | MODERATOR | everything above, plus verify/edit any data and use the review page |
| `admin` | ADMIN | everything, including grade scales |

- Start over with fresh data: `docker compose down -v && docker compose up`
- Ports clash? Set `FRONTEND_PORT`, `BACKEND_PORT` or `DB_PORT` (defaults 3000, 8080, 3308).
- Frontend repo elsewhere? Set `FRONTEND_DIR`.
- The seed files live in `database/docker/`. `scripts/dump-catalog-data.sh` regenerates the catalog dump from a running database.

### Backend development

Requires Java 21 and Docker.

```bash
docker compose up -d db      # MySQL 8.4 on localhost:3308, schema and data loaded
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # API on http://localhost:8080
```

`database/schema.sql` creates a fresh database. The `database/alter_*.sql` files only upgrade databases created with older versions of the schema. The `dev` profile supplies a local JWT secret, logs SQL and writes reset/verification links to the log; without it (or `JWT_SECRET`) the API refuses to start. The `seed-grades` profile (`./mvnw spring-boot:run -Dspring-boot.run.profiles=seed-grades`) reloads the grade tables from CSV.

### Configuration

| Variable | Default | Purpose |
|---|---|---|
| `DB_PASSWORD` | `climbingwiki` | MySQL root password |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3308/climbingwiki…` | Override the database location |
| `SPRING_JPA_SHOW_SQL` | `false` (`true` in `dev`) | Log every SQL statement |
| `JWT_SECRET` | **required** | Token signing key, at least 32 bytes (`openssl rand -base64 48`). No fallback outside the `dev` profile |
| `JWT_EXPIRATION_MS` | `86400000` (24 h) | Token lifetime |
| `TRUSTED_PROXIES` | loopback | Proxies whose `X-Forwarded-For` is trusted (the frontend server) |
| `CORS_ALLOWED_ORIGINS` | none | Browser origins allowed to call the API directly, comma-separated. The site calls it server-side, so leave empty. Behind an HTTPS proxy, add the API's own public origin so Swagger UI's "Try it out" works |
| `FRONTEND_URL` | `http://localhost:3000` | Site address used in password-reset and verification links |
| `SPRING_MAIL_HOST` | unset | SMTP server for account emails. Unset: no email is sent |
| `SPRING_MAIL_PORT` / `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD` | unset | SMTP port and credentials (STARTTLS) |
| `MAIL_FROM` | `ClimbingWiki <no-reply@localhost>` | Sender of account emails |
| `MAIL_SMTP_AUTH` / `MAIL_SMTP_STARTTLS` | `true` / `true` | Turn off for an SMTP relay without login or TLS |
| `MAIL_LOG_LINKS` | `false` (`true` in `dev` and Docker) | Write reset/verification links to the log when there is no SMTP. Never on a public server |

## Tests

```bash
./mvnw test
```

About 520 tests: service and controller unit tests (Mockito, MockMvc) and repository query tests on H2 in MySQL mode. `ClimbingWikiApplicationTests` starts the full context and needs the MySQL container running.

## Project layout

```
src/main/java/com/roberto/climbingwiki/
  controllers/   REST endpoints        services/      business rules, ownership checks
  repositories/  Spring Data + specs   dtos/          request/response records
  model/         JPA entities          security/      JWT filter, client IP, rate limiter
  config/        security, OpenAPI     exceptions/    domain errors -> HTTP status
  seed/          grade CSV seeding     openbeta/      OpenBeta GraphQL import
database/        schema.sql and upgrade scripts
doc/             roadmap and known issues
```

## Docs

- [`doc/ROADMAP.md`](doc/ROADMAP.md): what's done and what's next
- [`doc/KNOWN_ISSUES.md`](doc/KNOWN_ISSUES.md): open issues and deliberate trade-offs

## Data and licences

Crag locations © OpenStreetMap contributors (ODbL). Route data from theCrag contributors (CC BY-NC-SA) and OpenBeta. Because of CC BY-NC-SA, ClimbingWiki must stay non-commercial.

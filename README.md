# ClimbingWiki — backend

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
- **Security:** public reads, authenticated writes, ownership checks on personal data (owner, moderator or admin). Grade scales are admin-only, tags moderator-only. Emails are visible only to their owner and admins.
- **Login rate limit per real client IP:** `X-Forwarded-For` is trusted only from configured proxies; idle buckets are evicted.
- **Data import:** grade tables seeded from CSV; crags from OpenStreetMap and routes from theCrag/OpenBeta, upserted by external id so imports can be re-run.

## Run locally

Requires Java 21 and Docker.

```bash
# 1. Database (MySQL 8.4 on localhost:3307)
echo MYSQL_ROOT_PASSWORD=climbingwiki > .env
docker compose up -d
docker exec -i climbingwiki-db mysql -uroot -pclimbingwiki < database/schema.sql

# 2. API on http://localhost:8080
DB_PASSWORD=climbingwiki ./mvnw spring-boot:run

# 3. Optional: load the grading systems and grades
DB_PASSWORD=climbingwiki ./mvnw spring-boot:run -Dspring-boot.run.profiles=seed-grades
```

`database/schema.sql` creates a fresh database. The `database/alter_*.sql` files only upgrade databases created with older versions of the schema.

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Health check: http://localhost:8080/actuator/health

### Configuration

| Variable | Default | Purpose |
|---|---|---|
| `DB_PASSWORD` | `climbingwiki` | MySQL root password |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3307/climbingwiki…` | Override the database location |
| `JWT_SECRET` | dev-only value | Token signing key; **set it** anywhere but your machine |
| `JWT_EXPIRATION_MS` | `86400000` (24 h) | Token lifetime |
| `TRUSTED_PROXIES` | loopback | Proxies whose `X-Forwarded-For` is trusted (the frontend server) |

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

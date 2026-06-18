# LinkSnip — URL Shortener SaaS (Spring Boot)

A production-grade URL shortener built as a portfolio project demonstrating
full-stack engineering, system design, Redis caching, and a fully containerized
local stack.

**Stack:** React + Vite · Spring Boot 3 (Java 21) · Spring Data JPA / Hibernate ·
Spring Security + JWT · PostgreSQL · Redis · Flyway · Docker

> This is a from-scratch rebuild of the same product in the Java/Spring
> ecosystem — same API contract, same system-design decisions, expressed
> idiomatically in Spring rather than translated line-for-line.

---

## Quick start (Docker)

```bash
git clone <repo>
cd url-shortener-java
cp .env.example .env          # set JWT_SECRET at minimum
docker compose up --build
```

- Frontend: http://localhost:5173
- API base: http://localhost:8080/api/v1
- Health: http://localhost:8080/api/v1/health

---

## Local development (without Docker)

**Backend** (requires JDK 21 + Maven 3.9+, plus local Postgres + Redis — or `docker compose up db redis -d`)

```bash
cd backend
mvn spring-boot:run
```

Defaults point at `localhost:5432` (db `linksnip`/`linksnip`) and
`localhost:6379`. Override via the env vars in `.env.example`.

**Frontend**

```bash
cd frontend
npm install
npm run dev
```

---

## Running tests

```bash
cd backend
mvn test                      # unit tests + JPA slice test (H2)
```

---

## Project structure

```
url-shortener-java/
├── backend/
│   └── src/main/java/com/linksnip/
│       ├── auth/            # register, login, JWT refresh, profile
│       ├── user/            # User entity + repository
│       ├── security/        # JwtService, JWT filter, UserDetails, principal
│       ├── url/             # URL CRUD, redirect, short-code generation, service layer
│       ├── analytics/       # click recording (async), aggregations
│       ├── ratelimit/       # Redis fixed-window rate limiter
│       ├── config/          # security, redis/cache, async pool
│       └── common/          # exceptions, global handler, pagination, helpers
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/    # Flyway schema
├── frontend/
│   └── src/
│       ├── api/             # axios client + JWT refresh interceptor
│       ├── context/         # AuthContext
│       ├── hooks/           # useUrls, useAnalytics
│       └── pages/           # Login, Register, Dashboard
├── docker-compose.yml
└── .env.example
```

---

## API reference

### Authentication

| Method    | Endpoint               | Auth   |
| --------- | ---------------------- | ------ |
| POST      | /api/v1/auth/register  | Public |
| POST      | /api/v1/auth/login     | Public |
| POST      | /api/v1/auth/refresh   | Public |
| GET/PATCH | /api/v1/auth/profile   | JWT    |

### URLs

| Method | Endpoint           | Auth |
| ------ | ------------------ | ---- |
| GET    | /api/v1/urls       | JWT  |
| POST   | /api/v1/urls       | JWT  |
| GET    | /api/v1/urls/{id}  | JWT  |
| PUT    | /api/v1/urls/{id}  | JWT  |
| DELETE | /api/v1/urls/{id}  | JWT  |

### Analytics

| Method | Endpoint                    | Auth |
| ------ | --------------------------- | ---- |
| GET    | /api/v1/analytics/{urlId}   | JWT  |
| GET    | /api/v1/analytics/top-urls  | JWT  |

### Public

| Method | Endpoint                 |
| ------ | ------------------------ |
| GET    | /{shortCode} — redirect  |
| GET    | /api/v1/health           |

---

## Configuration

All runtime config is supplied via environment variables (see `.env.example`),
so the same build runs unchanged across environments:

```
JWT_SECRET            # base64-encoded 32-byte secret — openssl rand -base64 32
DATABASE_URL          # jdbc:postgresql://host:5432/db
DATABASE_USER
DATABASE_PASSWORD
REDIS_URL             # redis://host:6379  (rediss:// for TLS)
BASE_URL              # public base the short links are built from
CORS_ALLOWED_ORIGINS  # comma-separated frontend origin(s)
```

Schema is owned by Flyway and applied automatically on startup; nothing has to
be run by hand.

---

## Architecture decisions (interview talking points)

### Layered architecture & a thin controller boundary

Controllers handle HTTP only — validation, auth principal, status codes. All
business logic lives in `@Service` classes (`create`, `resolveForRedirect`,
etc.), which keeps it unit-testable without standing up the web layer and makes
it impossible for two controllers to drift in behaviour.

### Redis caching strategy

Implemented with Spring Cache (`@Cacheable`) over a `RedisCacheManager` with
per-cache TTLs:

- **Redirect (`url::{shortCode}`):** 24h. The hottest path — every redirect
  hits it. A cache miss falls back to Postgres and re-warms the entry.
- **Analytics (`analytics::{userId}:{urlId}`):** 5min. Eventually consistent;
  a short stale window is an acceptable trade for skipping repeated `GROUP BY`
  aggregations.
- **Top URLs (`topUrls::{userId}`):** 10min, scoped per user.
- **Invalidation:** on every URL update/delete, done in the service layer next
  to the write so it can't be forgotten.

One Spring-specific subtlety worth calling out: eviction is done through
`CacheManager` directly rather than a second `@CacheEvict` method, because
calling that method from within the same bean (`update()` → `evict()`) is a
self-invocation that bypasses the caching proxy and silently does nothing.

### Why async click recording?

Redirects are latency-sensitive; writing a `ClickEvent` (plus the counter bump)
would add tens of milliseconds to every redirect. Recording runs on a dedicated
`@Async` executor with a bounded queue and a `CallerRunsPolicy` — under extreme
load it degrades to synchronous recording rather than dropping clicks or
exhausting memory. At higher scale this becomes a durable queue (Kafka/SQS).
The counter itself is bumped with an atomic `UPDATE … SET click_count = click_count + 1`
so concurrent redirects don't lose counts to a read-modify-write race.

### Rate limiting

A Redis fixed-window limiter (`INCR` + `EXPIRE` on first hit):

- URL create: 20/hour per user
- Redirect: 200/min per IP (anti-scraping)
- Login: 10/min per IP (brute-force protection)

It **fails open** — if Redis is unreachable, requests are allowed rather than
taking the whole API down.

### Short-code generation

A `SecureRandom` over a 62-char alphabet (a–z, A–Z, 0–9). At length 7 that's
62⁷ ≈ 3.5 trillion combinations, so collisions are effectively nonexistent; a
bounded retry loop (5 attempts) handles the theoretical case, with the unique
DB index as the final guarantee.

### JWT access / refresh split

Access token: 15-minute lifetime, sent as `Authorization: Bearer`. Refresh
token: 7-day lifetime, accepted only at `/auth/refresh`. A `typ` claim
distinguishes the two so a refresh token can't be replayed as an access token.
The axios interceptor refreshes transparently on a 401, so users never see
token expiry. Auth is fully stateless (`SessionCreationPolicy.STATELESS`).

### Schema ownership: Flyway, not Hibernate

The schema is owned by Flyway migrations; Hibernate runs in `validate` mode and
will refuse to start if the entities and schema drift apart. This keeps schema
changes reviewable and reproducible instead of being implicit in entity edits.

### Database indexes

- `short_code` — unique B-tree, used on every redirect (and the collision guard)
- `(user_id, created_at DESC)` — covers the per-user list query
- `(url_id, clicked_at DESC)` — covers analytics time-series queries
- `(is_active, expires_at)` — covers active/expired filtering

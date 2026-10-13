# Shortlink Analytics

A URL shortener with click analytics, built with Spring Boot.

[![CI](https://github.com/anslemerumeh/shortlink-analytics/actions/workflows/ci.yml/badge.svg)](https://github.com/anslemerumeh/shortlink-analytics/actions/workflows/ci.yml)

## Features

- **Create short links**, with an optional custom code (`myproject` instead of a random one).
- **Redirect + track** — visiting a short link 302-redirects to the original URL and records a click event (timestamp, referrer, user agent).
- **Click analytics per link** — total clicks, clicks in the last 24 hours, and clicks in the last 7 days.
- **In-memory caching on the redirect hot path** — short-code lookups are cached with Caffeine, so the busiest endpoint in a URL shortener (the redirect itself) doesn't hit the database on every click.
- **Runs with zero external setup** — uses an in-memory H2 database, so `./gradlew bootRun` is all it takes; no Postgres/Redis container required.

## API

| Method | Path                     | Description                                   |
|--------|--------------------------|------------------------------------------------|
| POST   | `/api/links`             | Create a short link (`{"url": "...", "customCode": "optional"}`) |
| GET    | `/api/links`              | List all short links (paginated: `?page=0&size=20`) |
| GET    | `/api/links/{code}`       | Get details for one short link                |
| GET    | `/api/links/{code}/stats` | Get click analytics for one short link         |
| GET    | `/{code}`                 | Redirect to the original URL (records a click) |

### Example

```bash
curl -X POST http://localhost:8080/api/links \
  -H "Content-Type: application/json" \
  -d '{"url": "https://github.com/anslemerumeh"}'
```

```json
{
  "code": "aZ3kQ1x",
  "shortUrl": "http://localhost:8080/aZ3kQ1x",
  "originalUrl": "https://github.com/anslemerumeh",
  "createdAt": "2026-10-13T09:00:00Z",
  "clickCount": 0
}
```

```bash
curl -i http://localhost:8080/aZ3kQ1x
# HTTP/1.1 302 Found
# Location: https://github.com/anslemerumeh

curl http://localhost:8080/api/links/aZ3kQ1x/stats
```

```json
{
  "code": "aZ3kQ1x",
  "originalUrl": "https://github.com/anslemerumeh",
  "createdAt": "2026-10-13T09:00:00Z",
  "totalClicks": 1,
  "clicksLast24Hours": 1,
  "clicksLast7Days": 1
}
```

## Tech stack

Java 21, Spring Boot 3.4, Spring Data JPA, H2, Caffeine, Gradle, JUnit 5, Mockito, JaCoCo, GitHub Actions, Docker.

## Running it

```bash
git clone https://github.com/anslemerumeh/shortlink-analytics.git
cd shortlink-analytics
./gradlew bootRun
```

The API is live at `http://localhost:8080`. The H2 console (handy for poking at the data while developing) is at `http://localhost:8080/h2-console` — JDBC URL `jdbc:h2:mem:shortlinkdb`, user `sa`, no password.

## Running tests

```bash
./gradlew test
```

Coverage report: `build/reports/jacoco/test/html/index.html`.

## Design decisions

- **H2 in-memory instead of Postgres.** This project is meant to be cloned and run in one command — no `docker run` for a database first. Swapping in Postgres for a real deployment is a one-line change to `application.yaml` plus a Liquibase/Flyway migration set.
- **Caffeine instead of Redis for caching.** The redirect endpoint is the busiest path in a URL shortener, so it's cached — but running Redis is extra infrastructure a demo project shouldn't need. Caffeine gives the same "don't hit the DB on every read" benefit as a single-process, in-memory cache. A multi-instance production deployment would move this to Redis so all instances share one cache.
- **Cache is bypassed on writes.** `recordClick` always reads the entity fresh from the repository (not through the cache) before mutating and saving it, and evicts the cache entry afterward — so the write path never operates on a possibly-stale cached object.
- **Denormalized `clickCount` on `ShortLink`** alongside the full `ClickEvent` audit log — cheap reads for "how many clicks has this link had" without a `COUNT(*)` query, while still keeping per-click detail (timestamp, referrer, user agent) for the `/stats` endpoint.

## License

MIT — see [LICENSE](LICENSE).

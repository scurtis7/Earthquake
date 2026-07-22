# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A Spring Boot (WebFlux) service that fetches earthquake data from the USGS GeoJSON API
(`https://earthquake.usgs.gov/fdsnws/event/1/query`), aggregates it into daily counts, and
persists those counts to Postgres via R2DBC so historical analytics can be run without
re-hitting USGS.

## Stack

- Java 21, Maven, Spring Boot 3.4.3 (WebFlux, reactive)
- R2DBC + PostgreSQL for persistence (`postgres` schema `earthquake`, table `DayCount`)
- Lombok, Apache Commons Lang3

## Commands

- Build without tests: `mvn clean compile`
- Build and run tests: `mvn clean test`
- Full build with coverage + checkstyle: `mvn clean verify`
- Run a single test class: `mvn test -Dtest=ClassName`
- Run a single test method: `mvn test -Dtest=ClassName#methodName`
- Checkstyle only: `mvn checkstyle:check` (config at `config/checkstyle/checkstyle.xml`, Google style based, 200-char
  line limit; bound to the `verify` phase, so `mvn verify` always runs it)
- Run locally: `mvn spring-boot:run -Dspring-boot.run.profiles=local` (see Local setup below)

Coverage is enforced by JaCoCo: `mvn verify` fails if any class drops below 80% line coverage
(`Application.class` is excluded).

## Local setup

The `local` profile (`src/main/resources/application-local.yml`) points at
`r2dbc:postgresql://localhost:5432/earthquakeDB` with user `postgres` / password `scurtis`,
and runs the app on port 9777. Postgres runs as a native local install (not containerized) in
this dev environment. If you don't have a local Postgres install, start one with:

```
docker run --name postgres -e POSTGRES_PASSWORD=scurtis -d -p 5432:5432 postgres
```

`PostgresR2dbc` (`config/PostgresR2dbc.java`) binds `spring.r2dbc.{url,username,password}` and
fails startup via `@PostConstruct` if any are blank — set these (e.g. via `SPRING_R2DBC_URL`,
etc., or the `local` profile) before running.

Elasticsearch and Kibana run via Docker Compose: `docker-compose.yml` at the repo root defines
both, pinned to `9.2.8`. Start them with `docker compose up -d` — no prerequisite setup needed,
Compose creates and owns its own network plus the `es-data` volume (pinned to that literal name
rather than the default project-prefixed one, so data survives `docker compose down`/`up` cycles).
Elasticsearch will be at `http://localhost:9200` (matching `spring.elasticsearch.uris` in
`application-local.yml`) and Kibana at `http://localhost:5601`.

**Version pinning matters here**: Spring Boot 4.0.6 bundles `elasticsearch-java` 9.2.8 (via
Spring Data Elasticsearch 6.0.5), which only speaks to Elasticsearch 9.x servers — pointing it at
an 8.x server fails with a confusing `TransportException` on basic calls like `indices.exists`.
Kibana's version must also match its Elasticsearch server's major version or it reports itself as
`critical`. If you bump the Spring Boot parent version, check whether the bundled
`elasticsearch-java` version moved to a new Elasticsearch major version and update the image tags
in `docker-compose.yml` (both services) to match.

Note: the `local` profile sets `spring.flyway.enabled: true`, but there are no Flyway migration
scripts checked into the repo (nothing under `src/main/resources/db/migration`). The
`earthquake.DayCount` schema currently has to exist already in the target database; `sql/earthquakes.sql`
has ad hoc reference queries (not migrations) showing the expected shape:
`earthquake.DayCount(id, day, count)`.

Tests run against an in-memory H2 database (`application-test.yml`, `r2dbc:h2:mem`) with Flyway
disabled, so `mvn test` doesn't need a real Postgres instance.

## Architecture

Request flow: `EarthquakeController` → `EarthquakeService` → (`EarthquakeConverter` +
`DayCountRepository`).

- **`model/`** — DTOs that mirror the USGS GeoJSON response shape (`FeatureCollection` →
  `List<Feature>`, `Geometry`, `Properties`, `Metadata`). These are only used for
  deserializing USGS responses, never persisted directly.
- **`entity/DayCount`** — the persisted row (`id`, `day`, `count`), mapped via Spring Data
  R2DBC to `earthquake.DayCount`. Implements `Persistable<Integer>` with `isNew()` based on
  `id == null || id == 0`, because R2DBC can't tell insert from update from a manually
  constructed entity otherwise.
- **`converter/EarthquakeConverter`** — turns a `FeatureCollection` into a `DayCount`. Notably,
  it derives the `day` string by parsing it back out of the `metadata.url` (the USGS query URL
  echoes the `starttime` parameter), not from any field on the response body directly — see
  `getDay()`.
- **`service/EarthquakeService`** — has two distinct call patterns:
    - `getEarthquakesByYearAndMonth` — a single reactive call through the injected `WebClient`,
      returning `Mono<FeatureCollection>` straight from USGS (no persistence).
    - `getEarthquakesByYear` / `getDailyCounts` — a **blocking** day-by-day backfill: for each day
      in the target year it makes a synchronous `RestTemplate` call to USGS, converts the result,
      looks up any existing row for that day via `repository.findByDay(...).blockFirst()` (to
      decide insert vs. update), and saves it with `repository.save(...).subscribe()`. This method
      mixes blocking I/O into an otherwise reactive codebase deliberately — if you touch it, keep
      in mind it runs on the request thread and does ~365 sequential HTTP calls.
- **`repository/DayCountRepository`** — `ReactiveCrudRepository`, plus a hand-written `findAll()`
  (`@Query`) that orders by `day`, overriding the default unordered one.
- **`config/AppConfig`** — defines the `WebClient` (reactive, base URL = USGS query endpoint,
  5MB in-memory buffer) and the `RestTemplate` bean used by the blocking backfill path.
- **`config/PostgresR2dbc`** — `@ConfigurationProperties` for `spring.r2dbc.*`, validated at
  startup.

There's commented-out scaffolding across `controller`, `service`, `converter`, `repository`, and
`model/counts/EarthquakeCount` for a not-yet-implemented "counts by year" aggregate endpoint —
leave it as-is unless you're specifically finishing that feature.

## Endpoints

| Method | Url                   | Parameters      | Description                                                                                   |
|:-------|:----------------------|:----------------|:----------------------------------------------------------------------------------------------|
| GET    | `/earthquakes`        | `year`, `month` | Calls USGS directly for that year/month and returns the raw `FeatureCollection` (no DB write) |
| GET    | `/earthquakes/range`  | `year`          | Backfills/reads daily counts for that year, one USGS call per day, upserting into Postgres    |
| GET    | `/earthquakes/counts` | —               | Returns every `DayCount` row in the database, ordered by day                                  |
| GET    | `/actuator/health`    | —               | Health check                                                                                  |

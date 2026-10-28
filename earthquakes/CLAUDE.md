# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A Spring Boot (WebFlux) service that fetches earthquake data from the USGS FDSN event API
(`https://earthquake.usgs.gov/fdsnws/event/1/`), persists the raw per-event data to Postgres via
R2DBC, aggregates it into year/month/day counts so historical analytics can be run without
re-hitting USGS, and can copy the raw events into Elasticsearch for searching in Kibana.

## Stack

- Java 21, Maven, Spring Boot 4.0.6 (WebFlux, reactive)
- R2DBC + PostgreSQL for persistence (schema `earthquake`, tables `year_count`, `month_count`,
  `day_count`, `earthquake`)
- Spring Data Elasticsearch (Elasticsearch/Kibana 9.2.8 via Docker Compose)
- Spring Boot Actuator, Lombok, Apache Commons Lang3

## Commands

- Build without tests: `mvn clean compile`
- Build and run tests: `mvn clean test`
- Full build with coverage + checkstyle: `mvn clean verify`
- Run a single test class: `mvn test -Dtest=ClassName`
- Run a single test method: `mvn test -Dtest=ClassName#methodName`
- Checkstyle only: `mvn checkstyle:check` (config at `config/checkstyle/checkstyle.xml`, Google style based, 200-char
  line limit; bound to the `verify` phase, so `mvn verify` always runs it)
- Run locally: `mvn spring-boot:run -Dspring-boot.run.profiles=local` (see Local setup below)

Coverage is enforced by JaCoCo: `mvn verify` fails if any class drops below 80% line coverage.
`Application.class` and the `config`, `entity`, `model`, and `repository` packages are excluded.

## Local setup

The `local` profile (`src/main/resources/application-local.yml`) points at
`r2dbc:postgresql://localhost:5432/earthquake` with user `postgres` / password `scurtis`,
Elasticsearch at `http://localhost:9200`, and runs the app on port 9777. Postgres runs as a native
local install (not containerized) in this dev environment. If you don't have a local Postgres
install, start one with:

```
docker run --name postgres -e POSTGRES_PASSWORD=scurtis -d -p 5432:5432 postgres
```

`PostgresR2dbc` (`config/PostgresR2dbc.java`) binds `spring.r2dbc.{url,username,password}` and
fails startup via `@PostConstruct` if any are blank — set these (e.g. via `SPRING_R2DBC_URL`,
etc., or the `local` profile) before running.

There are no schema migrations (no Flyway/Liquibase). The schema must be created manually with
`sql/init.sql` (`psql -h localhost -U postgres -d earthquake -f sql/init.sql`). `sql/README.md`
documents init, plus backup/restore via `sql/backup.sh` / `sql/restore.sh`; `sql/earthquakes.sql`
holds ad hoc reference queries.

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

Tests run against an in-memory H2 database (`src/test/resources/application-test.yml`,
`r2dbc:h2:mem`), so `mvn test` doesn't need a real Postgres instance.

## Architecture

Request flow: `EarthquakeController` → `ValidationService` (input checks) → `EarthquakeService` /
`ElasticsearchIndexService` → (`EarthquakeConverter` + repositories).

- **`model/`** — DTOs that mirror the USGS GeoJSON response shape (`FeatureCollection` →
  `List<Feature>`, `Geometry`, `Properties`, `Metadata`). Only used for deserializing USGS
  responses; `EarthquakeConverter.toEntity` maps a `Feature` to the persisted `Earthquake`.
- **`entity/`** — persisted rows, mapped via Spring Data R2DBC to the `earthquake` schema:
  `YearCount`, `MonthCount`, `DayCount` (`id`, period key string, `count`) and `Earthquake` (one
  row per USGS event, unique on `featureId`). All implement `Persistable<Integer>` with `isNew()`
  based on `id == null || id == 0`, because R2DBC can't tell insert from update on a manually
  constructed entity otherwise. `PeriodCount` (`period`, `count`) is not a table; it's the row shape
  returned by the `EarthquakeRepository.getEarthquakeCountsByPeriod` aggregate query.
- **`dto/EarthquakeCountDto`** — `(period, date, count)` response shape for the counts endpoint.
- **`converter/EarthquakeConverter`** — count entities → `EarthquakeCountDto`, and `Feature` →
  `Earthquake` (epoch millis become UTC `DATE_TIME_FORMATTER` strings, `uuuu-MM-dd HH:mm:ss`;
  coordinates are joined into a comma-separated string). Because `date_time` is stored as a
  sortable string, period counts and range queries work with plain string comparison and `LEFT()`.
- **`service/ValidationService`** — validates `period` (`year`/`month`/`day`) and `uuuu-MM-dd`
  dates (`toDate` must be after `fromDate`), throwing `ValidationException`.
- **`service/EarthquakeService`** — uses two USGS clients from `AppConfig`, both with base URL
  `https://earthquake.usgs.gov`:
    - `getUSGSRawData` — a single reactive `WebClient` call to `/query`, returned straight through
      (no persistence).
    - `saveUSGSRawData` — a **blocking** `RestClient` call to `/query` for the range; each feature
      is then upserted into `earthquake` by `featureId` with fire-and-forget `.subscribe()`. An
      update rebuilds the whole row from USGS and keeps only the existing `id`, so re-saving a range
      refreshes every column. Once every row in the batch is saved, the saved rows are upserted into
      Elasticsearch in one bulk write (`ElasticsearchIndexService.indexEarthquakes`). A failed row
      save is logged and skipped; an Elasticsearch failure is only logged (Postgres is the source of
      truth, and `PUT /earthquake/es-index` rebuilds the index). `saveUSGSRawDataFromStartDate`
      loops this month by month from `startDate` to today.
    - `saveEarthquakeCounts` — does **not** call USGS. It aggregates the `earthquake` table with
      `getEarthquakeCountsByPeriod` (`LEFT(date_time, 4|7|10)`, `type = 'earthquake'` only, so
      quarry blasts etc. are excluded), then upserts each period into the matching count table,
      only saving when the count changed. The input dates become period keys with an exclusive
      upper bound one period past `toDate` (e.g. year: `2024-01-15`..`2024-02-15` → `"2024"`..
      `"2025"`). Run the raw save first, or the counts will be stale. `saveAllEarthquakeCounts`
      does year, month, and day in one call.
    - `getEarthquakeCounts` — reads the matching count table between the two period keys, ordered.
    - `getEarthquakeLastDate` — `LEFT(MAX(date_time), 10)` from `earthquake`, used by the UI to know
      where to resume loading.

  The save paths deliberately mix blocking I/O and unawaited subscriptions into an otherwise
  reactive codebase: they run on the request thread, the raw save can make many sequential USGS
  calls, and the endpoint can return before every DB write has finished.
- **`service/ElasticsearchIndexService`** — deletes and recreates the `earthquake_index` index from
  `src/main/resources/elasticsearch/earthquake-mapping.json`, then bulk-loads every `Earthquake`
  row from Postgres in batches of 1000. Spring Data writes documents using the entity's **camelCase
  Java field names** (`dateTime`, `featureId`, …), so the mapping's property names must match them
  exactly. A snake_case name silently creates an empty field, and the real field gets dynamically
  mapped as `text` (breaking date range queries). The mapping is only applied when the index is
  created, so a mapping change needs `PUT /earthquake/es-index`. `indexEarthquakes` is the
  incremental path used by the raw save: it creates the index (settings + mapping in one call) if
  it's missing, then bulk-upserts by Postgres `id`. A raw save running during a rebuild can lose
  its writes or hit the index between delete and create, so don't run both at once.
- **`repository/`** — `ReactiveCrudRepository`s with derived queries (`findByX`,
  `findByXBetweenOrderByX`, `findByFeatureId`), plus native `@Query` aggregates on
  `EarthquakeRepository` (`getEarthquakeCountsByPeriod`, `getEarthquakeLastDate`).
- **`exception/GlobalExceptionHandler`** — maps `ValidationException` → 400 and
  `ElasticsearchIndexException` → 500, both as an `ErrorResponse` JSON body.
- **`config/AppConfig`** — `WebClient` (10MB in-memory buffer) and `RestClient` beans, plus logs
  `scurtis.app-version` at startup. `config/PostgresR2dbc` validates `spring.r2dbc.*`;
  `config/CorsGlobalConfig` allows all origins/methods (for the sibling `earthquakes-ui` app).

Shared constants (USGS URL, formatters, error messages, `MAX_DAYS`, Elasticsearch settings) live in
`common/AppConstants`.

## Endpoints

All dates are `uuuu-MM-dd`; `period` is `year`, `month`, or `day`.

| Method | Url                         | Parameters                     | Description                                                                                    |
|:-------|:----------------------------|:-------------------------------|:-----------------------------------------------------------------------------------------------|
| GET    | `/earthquake/raw`           | `fromDate`, `toDate`           | Calls USGS `/query` and returns the raw `FeatureCollection` (no DB write)                      |
| GET    | `/earthquake/raw/save`      | `fromDate`, `toDate`           | Calls USGS `/query` for the range and upserts each event into `earthquake`                     |
| GET    | `/earthquake/raw/save/from` | `startDate`                    | Same as above, month by month from `startDate` through today                                   |
| GET    | `/earthquake/counts/save`   | `period`, `fromDate`, `toDate` | Aggregates `earthquake` rows per year/month/day in the range and upserts the count table       |
| GET    | `/earthquake/counts/save/all` | `fromDate`, `toDate`         | Same as above for year, month, and day in one call                                             |
| GET    | `/earthquake/counts`        | `period`, `fromDate`, `toDate` | Returns saved counts for the range as `EarthquakeCountDto`s, ordered by period                 |
| GET    | `/earthquake/last/date`     | —                              | Returns the latest `uuuu-MM-dd` date in the `earthquake` table                                 |
| PUT    | `/earthquake/es-index`      | —                              | Recreates `earthquake_index` in Elasticsearch and reloads every `earthquake` row (~30 minutes) |
| GET    | `/actuator/health`          | —                              | Health check                                                                                   |

# Database

This app uses a local (non-containerized) PostgreSQL install. Everything below assumes the
`local` profile defaults from `application-local.yml`:

* Host: `localhost`
* Port: `5432`
* Database: `earthquakeDB`
* User: `postgres`
* Password: `scurtis`

The desktop PostgreSQL 16 install's client tools (`pg_dump`, `pg_restore`, `psql`, etc.) live at
`/Library/PostgreSQL/16/bin` and aren't on `PATH` by default. `backup.sh`/`restore.sh` account for
this via a `PG_BIN` env var (defaulting to that path) — for the commands below, either add that
directory to your `PATH` or prefix them the same way.

The app persists to four tables in the `earthquake` schema, mapped by the classes in
`src/main/java/com/scurtis/earthquakes/entity`:

* `day_count(id, day, count)` — `DayCount.java`
* `month_count(id, month, count)` — `MonthCount.java`
* `year_count(id, year, count)` — `YearCount.java`
* `earthquake(id, feature_id, ...)` — `Earthquake.java`, the raw per-event data

## 1. Fresh init

If you're setting up Postgres from scratch (e.g. a new machine, or the container from the root
`README.md`), create the database and then run [`init.sql`](./init.sql) to create the schema and
table:

```
/Library/PostgreSQL/16/bin/createdb -h localhost -U postgres earthquakeDB
/Library/PostgreSQL/16/bin/psql -h localhost -U postgres -d earthquakeDB -f sql/init.sql
```

`init.sql` is idempotent (`CREATE SCHEMA/TABLE IF NOT EXISTS`), so it's safe to re-run.

## 2. Backup

[`backup.sh`](./backup.sh) dumps `earthquakeDB` to a timestamped custom-format file using
`pg_dump`, defaulting to `./backups/`:

```
./sql/backup.sh
```

Or specify a different output directory:

```
./sql/backup.sh ~/earthquake-backups
```

Override connection details via env vars (`PGHOST`, `PGPORT`, `PGUSER`, `PGDATABASE`,
`PGPASSWORD`) if you're not using the local defaults.

## 3. Restore

[`restore.sh`](./restore.sh) restores `earthquakeDB` from a dump file produced by `backup.sh`.
**This drops and recreates the target database first**, so anything currently in it is lost —
the script asks for confirmation before doing so.

```
./sql/restore.sh ./backups/earthquakeDB_20260729_120000.dump
```

Same env var overrides as `backup.sh` apply.

## Reference queries

[`earthquakes.sql`](./earthquakes.sql) has ad hoc `SELECT` queries useful for poking at the data
(counts, ordering, filtering by date range, etc.) — not migrations, just scratch queries.

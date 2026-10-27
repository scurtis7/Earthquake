# TODO

Tasks still to do after the 2026-10-27 fixes: the 24-hour clock fix (`hh` → `HH`), the
Elasticsearch mapping fix, and the test fixes. Check items off as they're done.

## 1. Ship the code fixes

- [ ] Commit the working-tree changes (`AppConstants` `HH` fix, `earthquake-mapping.json` camelCase
      fields, `EarthquakeServiceTest` / `EarthquakeControllerTest` / `EarthquakeConverterTest`,
      `CLAUDE.md`). `mvn clean verify` passes as of 2026-10-27.
- [ ] Restart the app (`mvn spring-boot:run -Dspring-boot.run.profiles=local`) so it runs the new
      formatter and mapping.

## 2. Repair the stored times in Postgres

Every row saved before the fix stored its time on a 12-hour clock with no AM/PM, so a 15:30 UTC
event reads `03:30:00`. Dates are correct; only the hours of PM events are wrong. The AM/PM
information is lost, so the only fix is to re-fetch from USGS. Re-saving overwrites the whole row
(matched by `featureId`), so there's no need to delete anything first.

- [ ] Re-save raw data from the earliest stored event (`1900-01-05`, ~4.88M rows):
      `GET /earthquake/raw/save/from?startDate=1900-01-01`
      - This makes one USGS call per month (~1,520 calls) on the request thread, so expect it to
        take a long time. It can be run in smaller pieces with
        `/earthquake/raw/save?fromDate=…&toDate=…` instead.
      - Spot-check afterwards: `SELECT count(*) FROM earthquake.earthquake WHERE date_time >= '2026-10-26' AND date_time < '2026-10-27' AND substring(date_time, 12, 2)::int >= 13;`
        should be non-zero.
- [ ] Optional: re-run `GET /earthquake/counts/save/all?fromDate=1900-01-01&toDate=<today>`. The
      counts only use the date part, so the time bug didn't affect them. This is only needed if
      raw data was added since the counts were last saved.

## 3. Rebuild Elasticsearch / Kibana

Do this **after** step 2, or the index will carry the old wrong times.

- [ ] Rebuild the index (~30 minutes): `curl -X PUT localhost:9777/earthquake/es-index`
- [ ] Confirm the mapping took: `curl localhost:9200/earthquake_index/_mapping` should show
      `dateTime` as `date` and no snake_case fields such as `date_time`.
- [ ] In Kibana, delete and recreate the `earthquake_index` data view with `dateTime` as the time
      field.
- [ ] Verify the 2026-10-26 query in Dev Tools returns ~210 hits:

      GET earthquake_index/_search
      {
        "size": 1000,
        "track_total_hits": true,
        "query": { "range": { "dateTime": { "gte": "2026-10-26", "lt": "2026-10-27", "format": "yyyy-MM-dd" } } },
        "sort": [{ "dateTime": "asc" }]
      }

## 4. Keep the Elasticsearch index in sync incrementally

Right now the only way to update Elasticsearch is `PUT /earthquake/es-index`, which deletes the
index and reloads all ~4.88M rows (~30 minutes). That's why the index was stuck at 2026-09-30
while Postgres already had October.

- [ ] Update the index whenever `earthquake` rows are inserted or updated, instead of rebuilding it.
      - Natural hook: `EarthquakeService.saveRawData`. After `earthquakeRepository.save(...)`,
        save the returned entity to Elasticsearch too. Documents already use the Postgres `id` as
        `_id`, so saving by id is an upsert: new events get added and changed ones overwritten.
      - Prefer bulk writes per USGS batch (one month is up to thousands of events) over one
        request per event.
      - Make sure the index exists with `earthquake-mapping.json` before writing, e.g. create it
        at startup if missing. Otherwise the first write auto-creates it with dynamic mapping, and
        `dateTime` becomes `text` again.
      - Decide what an Elasticsearch failure should do: fail the save, or log it and let a
        catch-up job fix it later. Postgres stays the source of truth either way.
      - Keep `PUT /earthquake/es-index` for full rebuilds, e.g. after a mapping change.
- [ ] Optional: add a catch-up endpoint that indexes only rows changed since the newest document
      in the index, to backfill after outages or Elasticsearch failures. This needs a reliable
      "changed since" column. `updated_date` is USGS's update time, not ours, so it would need a
      new column such as a `last_modified` timestamp set on save.
- [ ] Add tests for the new indexing path (the JaCoCo 80% per-class line coverage still applies).

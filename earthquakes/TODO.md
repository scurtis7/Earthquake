# TODO

Tasks still to do after the 2026-10-27 fixes: the 24-hour clock fix (`hh` → `HH`), the
Elasticsearch mapping fix, and the test fixes. Check items off as they're done.

## 1. Ship the code fixes

- [x] Commit the working-tree changes (`AppConstants` `HH` fix, `earthquake-mapping.json` camelCase
      fields, `EarthquakeServiceTest` / `EarthquakeControllerTest` / `EarthquakeConverterTest`,
      `CLAUDE.md`). `mvn clean verify` passes as of 2026-10-27.
- [x] Restart the app (`mvn spring-boot:run -Dspring-boot.run.profiles=local`) so it runs the new
      formatter and mapping.

## 2. Repair the stored times in Postgres

Every row saved before the fix stored its time on a 12-hour clock with no AM/PM, so a 15:30 UTC
event reads `03:30:00`. Dates are correct; only the hours of PM events are wrong. The AM/PM
information is lost, so the only fix is to re-fetch from USGS. Re-saving overwrites the whole row
(matched by `featureId`), so there's no need to delete anything first.

- [x] Re-save raw data from the earliest stored event (`1900-01-05`, ~4.88M rows):
      `GET /earthquake/raw/save/from?startDate=1900-01-01`
      - This makes one USGS call per month (~1,520 calls) on the request thread, so expect it to
        take a long time. It can be run in smaller pieces with
        `/earthquake/raw/save?fromDate=…&toDate=…` instead.
      - Spot-check afterwards: `SELECT count(*) FROM earthquake.earthquake WHERE date_time >= '2026-10-26' AND date_time < '2026-10-27' AND substring(date_time, 12, 2)::int >= 13;`
        should be non-zero.
- [x] Optional: re-run `GET /earthquake/counts/save/all?fromDate=1900-01-01&toDate=<today>`. The
      counts only use the date part, so the time bug didn't affect them. This is only needed if
      raw data was added since the counts were last saved.

## 3. Rebuild Elasticsearch / Kibana

Do this **after** step 2, or the index will carry the old wrong times.

- [x] Rebuild the index (~30 minutes): `curl -X PUT localhost:9777/earthquake/es-index`
- [x] Confirm the mapping took: `curl localhost:9200/earthquake_index/_mapping` should show
      `dateTime` as `date` and no snake_case fields such as `date_time`.
- [x] In Kibana, delete and recreate the `earthquake_index` data view with `dateTime` as the time
      field.
- [x] Verify the 2026-10-26 query in Dev Tools returns ~210 hits:

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

- [x] Update the index whenever `earthquake` rows are inserted or updated, instead of rebuilding it.
      Done 2026-10-28: `saveRawData` bulk-indexes each USGS batch via
      `ElasticsearchIndexService.indexEarthquakes`, which creates the index with the mapping if
      missing. Elasticsearch failures are logged, not fatal.
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
- [x] Add tests for the new indexing path (the JaCoCo 80% per-class line coverage still applies).
- [ ] `PUT /earthquake/es-index` is commented out in `EarthquakeController` (and its tests) as of
      2026-10-28. While it's off, batches that fail to index aren't backfilled and mapping changes
      can't be applied. Later, either restore it (optionally behind a flag or a running-rebuild
      guard) or delete it along with the unused controller dependency and the docs that mention it
      (`CLAUDE.md`, `README.md`, the `saveRawData` javadoc).

## 5. Let Claude check the UI in Firefox

Claude can't look at the running UI yet: the Claude in Chrome extension only works in Chrome, and
development here happens in Firefox. Set up one of these so UI changes (e.g. the sticky footer and
dark-mode message colors from 2026-10-28) can be checked visually:

- [ ] Preferred: add the Playwright MCP server with Firefox to the UI project, e.g.
      `claude mcp add playwright -- npx @playwright/mcp@latest --browser firefox`. Gives Claude
      clicks, typing and screenshots, close to what the Chrome extension does.
- [ ] Quick alternative: headless Firefox screenshots, e.g.
      `firefox --headless --screenshot out.png http://localhost:4200/admin`. Layout checks only,
      no clicking or dark-mode toggle.
- [ ] Once set up, verify the sticky footer spacing on Home and Admin, and the dark-mode message
      colors.

## 6. Search the Elasticsearch index

Add the ability to search `earthquake_index` from the service, instead of only through Kibana.
Details (query parameters, filters, paging, response shape) are still to be worked out.

- [ ] Add a service method that runs a search against `earthquake_index`.
- [ ] Add a REST endpoint on `EarthquakeController` that calls it, with input checks in
      `ValidationService`.
- [ ] Add tests for both (the JaCoCo 80% per-class line coverage still applies) and document the
      endpoint in `CLAUDE.md`.

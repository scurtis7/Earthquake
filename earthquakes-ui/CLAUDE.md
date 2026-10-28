# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

- `npm start` / `ng serve` — dev server at `http://localhost:4200/`, auto-reloads on change.
- `ng build` — production build, output to `dist/earthquakes-ui`. Use `npm run watch` for a dev-mode build that rebuilds on change.
- `ng test` — run unit tests via Karma/Jasmine (Chrome launcher). There are currently no `.spec.ts` files in `src/`, so this will not execute any specs until tests are added.
- `ng generate component path/to/name` (or `directive|pipe|service|class|guard|interface|enum|module`) — scaffold new pieces; components default to SCSS styles per `angular.json` schematics config.

There is no e2e test setup and no lint script configured in `package.json`.

## Architecture

Angular 18 standalone-component app (no `NgModule`s) using PrimeNG 18 (with `@primeng/themes`) for UI components.

- **Bootstrapping**: `src/main.ts` bootstraps `AppComponent` with `appConfig` from `src/app/app.config.ts`, which wires up the router, async animations, and PrimeNG theming via `providePrimeNG`.
- **Theming**: `src/app/color-presets.ts` defines `ColorPresets`, a PrimeNG preset built with `definePreset(Aura, ...)` that customizes the `sky`/`slate` semantic color scales for light and dark color schemes. Dark-mode surfaces map to `sky` (blue), so Aura's faint tinted defaults wash out on them; the preset's `components.message` block overrides the dark `info`/`warn`/`success`/`error` message colors (`*.950` background at 80%, `*.200` text, `*.600` border) for readable contrast. Fix other washed-out components the same way, in the preset rather than per component. Dark mode is toggled by adding/removing the `.my-app-dark` class on the `<html>` element (see `toggleDarkMode()` in `HeaderComponent`), matching the `darkModeSelector: '.my-app-dark'` configured in `app.config.ts`.
- **Routing**: `src/app/app.routes.ts` defines `/home` (`HomeComponent`) and `/admin` (`AdminComponent`); any other path redirects to `/home`. Add new routes here as new top-level views are introduced.
- **Shell layout**: `AppComponent` composes `HeaderComponent`, `<router-outlet>`, and `FooterComponent` around routed page content. `app.component.scss` makes `.main-layout` a full-height flex column (with `body { margin: 0 }` in `src/styles.scss`) so the footer sticks to the bottom of the window on short pages and follows the content on long ones. `.main-content` is `display: flow-root` so each page's top-level card margin (20px, e.g. `.home-layout`, `.admin-layout`) sets both the gap below the header and the gap above the footer — keep new pages on that same margin. `HeaderComponent` builds its `Menubar` items (via PrimeNG `MenuItem[]`) in `ngOnInit` and also owns the dark-mode toggle.
- **Backend calls**: `RestService` (`src/app/service/rest.service.ts`) wraps the Spring Boot backend at `http://localhost:9777/earthquake` (the sibling `earthquakes` project). The admin "save" endpoints are synchronous `GET`s that return plain text, so they're requested with `responseType: 'text'` and can run for a long time. Response/request shapes live in `src/app/model/` (`Count`, `ErrorResponse`).
- **Error handling**: the backend's `GlobalExceptionHandler` returns an `ErrorResponse` JSON (`code`, `status`, `exception`, `message`, `date`). With `responseType: 'text'` that body arrives as a string, so `AdminComponent.getErrorMessage()` parses it, falls back to the HTTP status, and treats status `0` as "server unreachable". Errors and results are shown with PrimeNG `p-message` (`severity="error"` / `"success"`), not alerts or console logs.
- **Admin page**: `AdminComponent` has two cards — "Load Raw Earthquake Data by Period" (`/raw/save`, `/counts/save/all`) and "Load Raw Earthquake Data From Date" (`/raw/save/from`). All long-running calls go through `runUpdate(section, request, …)`, which sets `loading` (disables and spins every button), shows an indeterminate `p-progressbar` with an elapsed-time counter, and records `activeSection` so the shared `#requestStatus` template (progress + messages) renders only in the card that started the request. Successful raw loads re-fetch the "Last Date". Add new admin actions through `runUpdate` to keep this behavior.
- **Components**: live under `src/app/components/<name>/`, each with `.ts`/`.html`/`.scss`, all standalone with explicit PrimeNG module imports (e.g. `Button`, `Menubar`, `ToggleButton`) rather than a shared UI module.

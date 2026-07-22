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
- **Theming**: `src/app/color-presets.ts` defines `ColorPresets`, a PrimeNG preset built with `definePreset(Aura, ...)` that customizes the `sky`/`slate` semantic color scales for light and dark color schemes. Dark mode is toggled by adding/removing the `.my-app-dark` class on the `<html>` element (see `toggleDarkMode()` in `HeaderComponent`), matching the `darkModeSelector: '.my-app-dark'` configured in `app.config.ts`.
- **Routing**: `src/app/app.routes.ts` is minimal — everything redirects to `/home`, which renders `HomeComponent`. Add new routes here as new top-level views are introduced.
- **Shell layout**: `AppComponent` composes `HeaderComponent`, `<router-outlet>`, and `FooterComponent` around routed page content. `HeaderComponent` builds its `Menubar` items (via PrimeNG `MenuItem[]`) in `ngOnInit` and also owns the dark-mode toggle.
- **Components**: live under `src/app/components/<name>/`, each with `.ts`/`.html`/`.scss`, all standalone with explicit PrimeNG module imports (e.g. `Button`, `Menubar`, `ToggleButton`) rather than a shared UI module.

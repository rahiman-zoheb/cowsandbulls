# Cows and Bulls: frontend

The browser UI for Cows and Bulls, built with React and [Vite](https://vite.dev/). It lives in the
[cowsandbulls](https://github.com/rahiman-zoheb/cowsandbulls) repository next to the backend; see
the [root README](../README.md) for the API and for running both together.

Requires Node.js 22.12 or later.

## Scripts

| Command | What it does |
|---|---|
| `npm start` | Dev server on http://localhost:3000, proxying `/api` to the backend on port 8080 |
| `npm test` | [Vitest](https://vitest.dev/) in watch mode; `npm test -- --run --coverage` runs once, the way CI does |
| `npm run lint` | ESLint |
| `npm run build` | Production build into `build/` |
| `npm run preview` | Serves the production build locally |

Coverage thresholds (95% statements, functions and lines, 90% branches) are set in
`vite.config.js`, and a run that drops below them fails.

## Configuration

All backend calls go through `src/api.js`, which prefixes them with `/api`. To point a build at a
different origin, set `VITE_API_BASE` when building, e.g.
`VITE_API_BASE=https://example.com/api npm run build`. The dev-server proxy only exists in
development; in production the backend serves the app itself, so `/api` is already same-origin.

Everything in the build, including every `VITE_*` variable, is public. Never put a secret here.

## Production

There is no separate frontend image. The root `Dockerfile` builds this app into the backend jar,
which serves it from the same origin as the API; see the [root README](../README.md#docker).

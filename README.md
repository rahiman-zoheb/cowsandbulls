# Cows and Bulls

A word-guessing game. Guess the secret four-letter word; after each guess you're told how many
letters are **bulls** (right letter, right place) and how many are **cows** (right letter, wrong
place). [Rules on Wikipedia](https://en.wikipedia.org/wiki/Bulls_and_Cows).

This repository holds both halves of the game:

| Directory | What it is | Stack |
|---|---|---|
| [`backend/`](backend/) | REST API that picks the secret and scores guesses | Spring Boot 4.1, Java 17+, H2 |
| [`frontend/`](frontend/) | Browser UI | React 19, React Router 7, Vite, Bootstrap 5 |

The frontend used to live in
[cowsandbulls_reactjs](https://github.com/rahiman-zoheb/cowsandbulls_reactjs); its full
history was imported here under `frontend/`.

## Running locally

Start the backend on port 8080 (`mvnw.cmd` on Windows):

```
cd backend
./mvnw spring-boot:run
```

In a second terminal, start the frontend on port 3000. The dev server proxies `/api` to port 8080:

```
cd frontend
npm install
npm start
```

## API

Every game is independent and addressed by a random UUID. The server keeps the secret; responses
never include it until the game is over.

[`openapi.json`](openapi.json) is the full contract (OpenAPI 3.1), generated from the backend
code. Both sides are tested against it: the backend fails if the file is out of date, and the
frontend fails if a request it sends, or a mock response its tests render, doesn't match it. After
changing the API, regenerate it with
`cd backend && ./mvnw test -Dtest=OpenApiSpecTest -Dopenapi.update=true`. The running app doesn't
serve the spec; springdoc is only a test dependency.

| Request | Response |
|---|---|
| `POST /api/games` | `201`, a new game |
| `GET /api/games/{id}` | `200` with the game, or `404` for an unknown id |
| `POST /api/games/{id}/guesses` with `{ "name": "word" }` | `201` with the updated game |

A game is `{ id, status, guesses: [{ attempt, name, cows, bulls }], attemptsLeft, answer }`.
`status` is `IN_PROGRESS`, `WON` (four bulls) or `LOST` (20 guesses without finding the word), and
`answer` holds the word once the game is won or lost.

Guesses are case-insensitive and may repeat letters (only the secret can't). A guess is rejected
with an [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) problem object whose `detail` says why:

| Status | When |
|---|---|
| `400` | not exactly four letters, missing, or not JSON |
| `422` | four letters, but not in the word list |
| `409` | already guessed in this game, or the game is over |

Limits, all configurable in `backend/src/main/resources/application.properties`:

| Limit | Default | Response |
|---|---|---|
| Requests per client per minute | 120 | `429` with `Retry-After` |
| New games per client per minute | 10 | `429` with `Retry-After` |
| Request body size | 1 KB | `413` (`411` for chunked bodies) |
| Live games | 50,000 | `503` with `Retry-After` |
| Idle game lifetime | 24 hours | deleted, then `404` |

Behind a reverse proxy, the client is taken from `X-Forwarded-For`, but only when the proxy is on a
private network.

The frontend mirrors this: `/games/new` starts a game and replaces its own URL with
`/games/{id}`, so refreshing the page resumes the same game.

## Tests

```
cd backend && ./mvnw verify
cd frontend && npm run lint && npm test -- --run --coverage
```

Browser tests in [`e2e/`](e2e) use Playwright to play whole games against the packaged app:
winning (by actually solving the puzzle, since the API never gives the answer away), losing,
refreshing, bad input, two tabs, a backend outage, and dead links. They also fail on any
Content-Security-Policy violation, and run axe with every rule, colour contrast included.
Start the app with the rate limits raised, then:

```
docker run -p 8080:8080 -e COWSANDBULLS_API_GAMESPERMINUTE=1000 -e COWSANDBULLS_API_REQUESTSPERMINUTE=10000 cowsandbulls
cd e2e && npm ci && npx playwright install chromium && npx playwright test
```

Set `E2E_BASE_URL` to test another address, and `E2E_CHANNEL=msedge` (or `chrome`) to use an
installed browser instead of downloading one.

CI ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)) runs only the side a change touches,
then reports a single `ci-success` check. That is the only check branch protection requires.

## Docker

`docker compose up --build`, then open http://localhost:8080.

The root `Dockerfile` builds **one image** for the whole game. Node builds the frontend, Maven
builds the backend jar with that build inside it, and the jar serves both the app and `/api` from
the same origin, so the browser needs no CORS and no proxy. Client-side routes such as
`/games/{id}` get the app; unknown `/api` paths and missing assets stay 404s. Hashed assets are
cached for a year; the app shell is revalidated on every load.

Every response carries a strict Content-Security-Policy (same-origin scripts, styles and
connections only; no framing), plus `X-Content-Type-Options`, `X-Frame-Options` and
`Referrer-Policy`. HSTS is left to whatever terminates TLS in front of the app.

The image runs as a non-root user with the `prod` profile, so games are stored in an H2 database
file on the `game-data` volume and survive restarts and rebuilds. CI builds the image whenever
either app changes, smoke-tests the running container, and restarts it to check a game outlives
it.

## Configuration and secrets

Settings live in `backend/src/main/resources/application.properties`, and the `prod` profile in
`application-prod.properties`. Anything secret comes from the environment and is never committed:

| Variable | Used for |
|---|---|
| `COWSANDBULLS_DB_PASSWORD` | the H2 database password (`prod`). H2 fixes it when the file is first created. |
| `COWSANDBULLS_DB_PATH` | where the database file lives (`prod`, default `/data/cowsandbulls`) |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | to use another database instead; add its JDBC driver to the pom |

For Docker Compose, copy `.env.example` to `.env` (gitignored) and fill it in. In CI, use GitHub
Actions secrets. Push protection blocks commits that contain known secret formats.

The frontend has no secrets and must never be given any: everything in its build, including
`VITE_*` variables, ships to every visitor's browser.

## Known issues

Everything found in the October 2026 audit is tracked in
[#2](https://github.com/rahiman-zoheb/cowsandbulls/issues/2).

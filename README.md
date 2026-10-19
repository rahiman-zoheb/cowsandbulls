# Cows and Bulls

A word-guessing game. Guess the secret four-letter word; after each guess you're told how many
letters are **bulls** (right letter, right place) and how many are **cows** (right letter, wrong
place). [Rules on Wikipedia](https://en.wikipedia.org/wiki/Bulls_and_Cows).

This repository holds both halves of the game:

| Directory | What it is | Stack |
|---|---|---|
| [`backend/`](backend/) | REST API that picks the secret and scores guesses | Spring Boot 4.1, Java 17+, H2 |
| [`frontend/`](frontend/) | Browser UI | React 16, Create React App, reactstrap |

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
never include it until the game is won.

| Request | Response |
|---|---|
| `POST /api/games` | `201`, a new game: `{ id, status: "IN_PROGRESS", guesses: [], answer: null }` |
| `GET /api/games/{id}` | `200` with the game, or `404` for an unknown id |
| `POST /api/games/{id}/guesses` with `{ "name": "word" }` | `201` with the updated game; `409` once the game is won |

Each guess comes back as `{ attempt, name, cows, bulls }`. When a guess scores four bulls, `status`
becomes `WON` and `answer` holds the word. Errors are
[RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) problem objects.

The frontend mirrors this: `/games/new` starts a game and replaces its own URL with
`/games/{id}`, so refreshing the page resumes the same game.

## Tests

```
cd backend && ./mvnw verify
cd frontend && npm test -- --watchAll=false --coverage
```

CI ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)) runs only the side a change touches,
then reports a single `ci-success` check. That is the only check branch protection requires.

## Docker

`docker-compose up --build` builds and runs the backend; the image compiles the jar itself, so
nothing needs building first. CI builds the same image on every backend change and
smoke-tests the running container. The frontend image is described in
[`frontend/README.md`](frontend/README.md).

## Known issues

Everything found in the October 2026 audit is tracked in
[#2](https://github.com/rahiman-zoheb/cowsandbulls/issues/2).

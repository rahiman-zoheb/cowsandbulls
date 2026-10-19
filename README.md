# Cows and Bulls

A word-guessing game. Guess the secret four-letter word; after each guess you're told how many
letters are **bulls** (right letter, right place) and how many are **cows** (right letter, wrong
place). [Rules on Wikipedia](https://en.wikipedia.org/wiki/Bulls_and_Cows).

This repository holds both halves of the game:

| Directory | What it is | Stack |
|---|---|---|
| [`backend/`](backend/) | REST API that picks the secret and scores guesses | Spring Boot 2.3, Java 8+, H2 |
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

## Tests

```
cd backend && ./mvnw verify
cd frontend && npm test -- --watchAll=false --coverage
```

CI ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)) runs only the side a change touches,
then reports a single `ci-success` check. That is the only check branch protection requires.

## Docker

`docker-compose up --build` runs the backend. It currently needs the jar built first
(`cd backend && ./mvnw package`), which is
[#24](https://github.com/rahiman-zoheb/cowsandbulls/issues/24). The frontend image is
described in [`frontend/README.md`](frontend/README.md).

## Known issues

Everything found in the October 2026 audit is tracked in
[#2](https://github.com/rahiman-zoheb/cowsandbulls/issues/2).

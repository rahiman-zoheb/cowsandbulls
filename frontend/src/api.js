// Central place for backend calls so every call site agrees on the base path
// and on how failures are surfaced.
//
// The backend (https://github.com/rahiman-zoheb/cowsandbulls) has no 4xx
// validation path: a wrong-length guess throws in Calculator.calculate, and a
// guess containing anything outside [a-z] overflows its 26-slot tally array.
// Both reach the client as a generic Spring 500 with no usable message, so the
// guess must be normalized and validated here before it is ever sent.

export const API_BASE = process.env.REACT_APP_API_BASE || '/api';

// Secrets are lowercase four-letter words drawn from the backend's word list.
export const GUESS_PATTERN = /^[a-z]{4}$/;

export function normalizeGuess(value) {
  return String(value == null ? '' : value).trim().toLowerCase();
}

export function isValidGuess(value) {
  return GUESS_PATTERN.test(normalizeGuess(value));
}

const JSON_HEADERS = {
  Accept: 'application/json',
  'Content-Type': 'application/json',
};

function failed(response) {
  return new Error(`Request failed with status ${response.status}`);
}

// Spring's default error handler replies with a JSON *object*, so a failed
// request still parses cleanly. Without the ok check that object would be
// stored as the guess list and blow up the next render on .map.
async function readJson(response) {
  if (!response.ok) {
    throw failed(response);
  }
  return response.json();
}

// POST /api/game clears the guess list server-side. The controller takes no
// @RequestBody, so no payload is sent.
export function createGame() {
  return fetch(`${API_BASE}/game`, {
    method: 'POST',
    headers: { Accept: 'application/json' },
  }).then(response => {
    if (!response.ok) {
      throw failed(response);
    }
    return response;
  });
}

export async function fetchGuesses() {
  const data = await readJson(await fetch(`${API_BASE}/guesses`, {
    headers: { Accept: 'application/json' },
  }));
  // GET /api/guesses returns a bare array; anything else means the backend is
  // reporting an error in a shape we cannot render.
  return Array.isArray(data) ? data : [];
}

export async function submitGuess(name) {
  const guess = normalizeGuess(name);
  if (!GUESS_PATTERN.test(guess)) {
    throw new Error('Please provide a valid 4 letter word');
  }
  // Always POST: the backend exposes no PUT for guesses.
  return readJson(await fetch(`${API_BASE}/guess`, {
    method: 'POST',
    headers: JSON_HEADERS,
    body: JSON.stringify({ name: guess }),
  }));
}

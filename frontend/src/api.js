// Central place for backend calls so every call site agrees on the base path
// and on how failures are surfaced.
//
// The server is authoritative: it keeps the secret and only ever returns a
// game as { id, status, guesses, answer }, where answer stays null until the
// game is won. Nothing here can learn the word early.

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
  const error = new Error(`Request failed with status ${response.status}`);
  // Lets callers tell "this game doesn't exist" (404) from "the server broke".
  error.status = response.status;
  return error;
}

async function readGame(response) {
  if (!response.ok) {
    throw failed(response);
  }
  const game = await response.json();
  // Anything that isn't shaped like a game (an error envelope, say) is
  // rejected here rather than being rendered.
  if (!game || typeof game.id !== 'string' || !Array.isArray(game.guesses)) {
    throw new Error('Unexpected response from the server');
  }
  return game;
}

const gameUrl = id => `${API_BASE}/games/${encodeURIComponent(id)}`;

export async function createGame() {
  return readGame(await fetch(`${API_BASE}/games`, {
    method: 'POST',
    headers: { Accept: 'application/json' },
  }));
}

export async function fetchGame(id) {
  return readGame(await fetch(gameUrl(id), {
    headers: { Accept: 'application/json' },
  }));
}

export async function submitGuess(id, name) {
  const guess = normalizeGuess(name);
  if (!GUESS_PATTERN.test(guess)) {
    throw new Error('Please provide a valid 4 letter word');
  }
  // Returns the whole updated game, so the caller learns the score and whether
  // it won from this one response.
  return readGame(await fetch(`${gameUrl(id)}/guesses`, {
    method: 'POST',
    headers: JSON_HEADERS,
    body: JSON.stringify({ name: guess }),
  }));
}

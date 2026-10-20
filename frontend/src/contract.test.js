import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import process from 'node:process';
import Ajv2020 from 'ajv/dist/2020.js';
import { createGame, fetchGame, submitGuess } from './api';
import { errorResponse, gameResponse, GAME_ID, jsonResponse } from './test-utils';

// #48: openapi.json is generated from the backend, and the backend's
// OpenApiSpecTest fails if it drifts from the code. These tests hold the
// client to it from the other side: the requests api.js sends, and the mock
// responses the other suites render, must both match the spec. Change the API
// without updating this side, and one of them fails.

const spec = JSON.parse(readFileSync(resolve(process.cwd(), '..', 'openapi.json'), 'utf8'));

// Ajv speaks JSON Schema, which OpenAPI 3.1 schemas are, but resolves refs
// within a schema document; so the components move under $defs.
const defs = JSON.parse(JSON.stringify(spec.components.schemas).replaceAll('#/components/schemas/', 'openapi#/$defs/'));
const ajv = new Ajv2020({ allErrors: true, strict: true });
ajv.addKeyword('example');
// Strict mode rejects formats it doesn't know, so every one in the spec has
// to be checked here, not silently ignored.
ajv.addFormat('uuid', /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i);
ajv.addFormat('int32', { type: 'number', validate: n => Number.isInteger(n) && Math.abs(n) <= 2 ** 31 });
ajv.addFormat('uri-reference', { type: 'string', validate: s => !/\s/.test(s) });
ajv.addSchema({ $id: 'openapi', $defs: defs });

const validator = name => ajv.compile({ $ref: `openapi#/$defs/${name}` });

function expectValid(name, value) {
  const validate = validator(name);
  validate(value);
  expect(validate.errors ?? []).toEqual([]);
}

function expectInvalid(name, value) {
  expect(validator(name)(value)).toBe(false);
}

/** Finds the spec operation for a request the client made, or fails. */
function operationFor(method, url) {
  const path = new URL(url, 'http://localhost').pathname;
  const match = Object.entries(spec.paths).find(([template]) =>
    new RegExp(`^${template.replace(/\{[^}]+\}/g, '([^/]+)')}$`).test(path));
  expect(match, `${method} ${path} is in the spec`).toBeDefined();
  const [template, operations] = match;
  const operation = operations[method.toLowerCase()];
  expect(operation, `${method} ${template} is in the spec`).toBeDefined();

  const values = path.match(new RegExp(`^${template.replace(/\{[^}]+\}/g, '([^/]+)')}$`)).slice(1);
  const names = [...template.matchAll(/\{([^}]+)\}/g)].map(m => m[1]);
  return { operation, params: Object.fromEntries(names.map((n, i) => [n, decodeURIComponent(values[i])])) };
}

function expectRequestMatchesSpec([url, init = {}]) {
  const method = init.method ?? 'GET';
  const { operation, params } = operationFor(method, url);

  for (const parameter of operation.parameters ?? []) {
    const validate = ajv.compile(parameter.schema);
    expect(validate(params[parameter.name]), `path parameter ${parameter.name}`).toBe(true);
  }

  const success = Object.entries(operation.responses).find(([code]) => code.startsWith('2'))[1];
  expect(Object.keys(success.content)).toContain(init.headers.Accept);

  if (operation.requestBody) {
    expect(Object.keys(operation.requestBody.content)).toContain(init.headers['Content-Type']);
    const { schema } = operation.requestBody.content[init.headers['Content-Type']];
    expectValid(schema.$ref.split('/').pop(), JSON.parse(init.body));
  } else {
    expect(init.body).toBeUndefined();
  }
  return operation;
}

describe('requests the client sends', () => {
  beforeEach(() => {
    globalThis.fetch = vi.fn(() => jsonResponse(gameResponse(), { status: 201 }));
  });

  it('creates a game the way the spec describes', async () => {
    await createGame();
    expect(expectRequestMatchesSpec(fetch.mock.calls[0]).operationId).toBe('create');
  });

  it('fetches a game the way the spec describes', async () => {
    await fetchGame(GAME_ID);
    expect(expectRequestMatchesSpec(fetch.mock.calls[0]).operationId).toBe('get');
  });

  it('submits a guess the way the spec describes, normalised to match its pattern', async () => {
    await submitGuess(GAME_ID, '  ForK ');
    expect(expectRequestMatchesSpec(fetch.mock.calls[0]).operationId).toBe('guess');
  });

  it('the checker rejects a request the spec does not allow', () => {
    expect(() => expectRequestMatchesSpec(['/api/games/not-a-uuid', { headers: { Accept: 'application/json' } }])).toThrow();
    expect(() => expectRequestMatchesSpec(['/api/games', { method: 'DELETE', headers: {} }])).toThrow();
    expect(() => expectValid('GuessRequest', { name: 'forks' })).toThrow();
  });
});

describe('mock responses the other suites render', () => {
  it.each([
    ['a game in progress', gameResponse()],
    ['a new game', gameResponse({ guesses: [], attemptsLeft: 20 })],
    ['a won game', gameResponse({ status: 'WON', answer: 'quiz', attemptsLeft: 0 })],
    ['a lost game', gameResponse({ status: 'LOST', answer: 'gold', attemptsLeft: 0 })],
  ])('%s is a valid GameResponse', (_, game) => {
    expectValid('GameResponse', game);
  });

  it('the error fixture is a valid Problem', async () => {
    const response = await errorResponse(500);
    expectValid('Problem', await response.json());
  });

  it.each([
    ['without an answer field', Object.fromEntries(Object.entries(gameResponse()).filter(([key]) => key !== 'answer'))],
    ['with an unknown status', gameResponse({ status: 'PAUSED' })],
    ['with a non-UUID id', gameResponse({ id: '42' })],
    ['with an uppercase answer', gameResponse({ status: 'WON', answer: 'QUIZ' })],
    ['with five bulls', gameResponse({ guesses: [{ attempt: 1, name: 'quiz', cows: 0, bulls: 5 }] })],
  ])('the checker rejects a game %s', (_, game) => {
    expectInvalid('GameResponse', game);
  });
});

describe('errors the client relies on', () => {
  it('declares 404 wherever the client shows "game not found"', () => {
    expect(spec.paths['/api/games/{id}'].get.responses).toHaveProperty('404');
    expect(spec.paths['/api/games/{id}/guesses'].post.responses).toHaveProperty('404');
  });

  it('always includes the detail the client shows for a rejected guess', () => {
    expect(spec.paths['/api/games/{id}/guesses'].post.responses).toHaveProperty('422');
    expect(spec.components.schemas.Problem.required).toContain('detail');
  });
});

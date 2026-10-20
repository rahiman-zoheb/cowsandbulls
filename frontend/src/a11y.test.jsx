import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import process from 'node:process';
import { fireEvent, screen, waitForElementToBeRemoved } from '@testing-library/react';
import axe from 'axe-core';
import { errorResponse, GAME_ID, gameResponse, jsonResponse, renderApp, silenceConsoleError } from './test-utils';

// Automated accessibility checks (#42) for every screen and state, using
// axe-core's default WCAG 2.x A/AA and best-practice rules.
//
// One rule is off: colour contrast needs real layout and computed styles,
// which jsdom doesn't do. Bootstrap's default palette is designed to pass it.
async function expectNoViolations() {
  const results = await axe.run(document, { rules: { 'color-contrast': { enabled: false } } });
  const summary = results.violations.map(v => `${v.id} (${v.impact}): ${v.nodes.map(n => n.target.join(' ')).join(', ')}`);
  expect(summary).toEqual([]);
}

// index.html provides these for the real page (checked below); jsdom starts
// with an empty document.
beforeAll(() => {
  document.documentElement.lang = 'en';
  document.title = 'Cows And Bulls';
});

beforeEach(() => {
  globalThis.fetch = vi.fn(() => jsonResponse(gameResponse()));
});

async function renderGame(game) {
  globalThis.fetch = vi.fn(() => jsonResponse(game));
  renderApp(`/games/${GAME_ID}`);
  await waitForElementToBeRemoved(() => screen.queryByText('Loading...'));
}

it('index.html declares the page language and a title', () => {
  const html = readFileSync(resolve(process.cwd(), 'index.html'), 'utf8');
  expect(html).toMatch(/<html lang="en">/);
  expect(html).toMatch(/<title>[^<]+<\/title>/);
});

it('home page', async () => {
  renderApp('/');
  await expectNoViolations();
});

it('page not found', async () => {
  renderApp('/nowhere');
  await expectNoViolations();
});

it('starting a new game', async () => {
  globalThis.fetch = vi.fn(() => new Promise(() => {}));
  renderApp('/games/new');
  await expectNoViolations();
});

it('a game in progress, with guesses', async () => {
  await renderGame(gameResponse());
  await expectNoViolations();
});

it('a game with an invalid guess typed', async () => {
  await renderGame(gameResponse());
  fireEvent.change(screen.getByLabelText(/your guess/i), { target: { name: 'name', value: 'ab1' } });
  await expectNoViolations();
});

it('a won game', async () => {
  await renderGame(gameResponse({ status: 'WON', answer: 'quiz', attemptsLeft: 0 }));
  await expectNoViolations();
});

it('a lost game', async () => {
  await renderGame(gameResponse({ status: 'LOST', answer: 'quiz', attemptsLeft: 0 }));
  await expectNoViolations();
});

it('a game that does not exist', async () => {
  globalThis.fetch = vi.fn(() => errorResponse(404));
  renderApp(`/games/${GAME_ID}`);
  await screen.findByText(/doesn't exist/i);
  await expectNoViolations();
});

it('the expanded navigation menu', async () => {
  renderApp('/');
  fireEvent.click(screen.getByRole('button', { name: /toggle navigation/i }));
  await expectNoViolations();
});

describe('error states', () => {
  silenceConsoleError();

  it('a game that failed to load', async () => {
    globalThis.fetch = vi.fn(() => Promise.reject(new TypeError('Failed to fetch')));
    renderApp(`/games/${GAME_ID}`);
    await screen.findByRole('alert');
    await expectNoViolations();
  });

  it('a new game that failed to start', async () => {
    globalThis.fetch = vi.fn(() => Promise.reject(new TypeError('Failed to fetch')));
    renderApp('/games/new');
    await screen.findByRole('button', { name: 'Try again' });
    await expectNoViolations();
  });
});

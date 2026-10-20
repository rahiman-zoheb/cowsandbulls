import { act, fireEvent, screen, waitFor } from '@testing-library/react';
import { currentPath, GAME_ID, gameResponse, jsonResponse, renderApp, silenceConsoleError } from './test-utils';

const freshGame = gameResponse({ guesses: [] });

beforeEach(() => {
  globalThis.fetch = vi.fn((url, options) => jsonResponse(freshGame, { status: options && options.method === 'POST' ? 201 : 200 }));
});

it('creates a game and replaces its own URL with the game\'s', async () => {
  const { history } = renderApp('/games/new');

  expect(screen.getByText('Starting a new game...')).toBeInTheDocument();
  await waitFor(() => expect(currentPath()).toBe(`/games/${GAME_ID}`));

  expect(globalThis.fetch.mock.calls[0][0]).toBe('/api/games');
  expect(globalThis.fetch.mock.calls[0][1].method).toBe('POST');
  // Replaced, not pushed: Back must not return to /games/new, which would
  // start yet another game.
  expect(history.action).toBe('REPLACE');
  act(() => history.back());
  expect(currentPath()).toBe(`/games/${GAME_ID}`);
  expect(globalThis.fetch.mock.calls.filter(([, options]) => options && options.method === 'POST')).toHaveLength(1);
});

it("creates exactly one game under StrictMode's double mount", async () => {
  // In development React 19 mounts, unmounts and remounts NewGame. Without the
  // deferred, cancellable start, both mounts posted, so every page load created
  // two games and spent two of the per-minute creation allowance.
  renderApp('/games/new', { strict: true });

  await waitFor(() => expect(currentPath()).toBe(`/games/${GAME_ID}`));
  const posts = globalThis.fetch.mock.calls.filter(([, options]) => options && options.method === 'POST');
  expect(posts).toHaveLength(1);
});

it('hands over to the game page, which loads the game it just created', async () => {
  renderApp('/games/new');

  expect(await screen.findByText('No guesses yet.')).toBeInTheDocument();
  expect(globalThis.fetch.mock.calls.map(([url]) => url)).toEqual(['/api/games', `/api/games/${GAME_ID}`]);
});

describe('when the backend is unavailable', () => {
  silenceConsoleError();

  it.each([
    [429, 'Too many new games; try again in a minute'],
    [503, 'Too many games are in progress right now; try again in a few minutes'],
  ])('passes on the server’s reason for a %i instead of blaming the backend', async (status, detail) => {
    globalThis.fetch = vi.fn(() => jsonResponse({ status, detail }, { ok: false, status }));
    renderApp('/games/new');

    expect(await screen.findByRole('alert')).toHaveTextContent(detail);
    expect(screen.queryByText(/backend running/i)).not.toBeInTheDocument();
  });

  it('shows the failure and lets the player try again', async () => {
    globalThis.fetch = vi.fn(() => Promise.reject(new TypeError('Failed to fetch')));
    renderApp('/games/new');

    expect(await screen.findByRole('alert')).toHaveTextContent(/backend running/i);
    expect(currentPath()).toBe('/games/new');

    globalThis.fetch = vi.fn(() => jsonResponse(freshGame, { status: 201 }));
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }));

    await waitFor(() => expect(currentPath()).toBe(`/games/${GAME_ID}`));
  });

});

it('does not navigate or update state if the player leaves before the game is created', async () => {
  const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});
  let resolveCreate;
  globalThis.fetch = vi.fn(() => new Promise(resolve => { resolveCreate = resolve; }));
  const { history } = renderApp('/games/new');
  await waitFor(() => expect(globalThis.fetch).toHaveBeenCalledTimes(1));

  act(() => history.push('/'));
  await act(async () => {
    resolveCreate({ ok: true, status: 201, json: () => Promise.resolve(freshGame) });
  });

  expect(currentPath()).toBe('/');
  const unmountWarnings = consoleError.mock.calls.map(args => String(args[0])).filter(m => m.includes('unmounted component'));
  expect(unmountWarnings).toEqual([]);
  consoleError.mockRestore();
});

it('stays quiet if creation fails after the player has already left', async () => {
  const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});
  let rejectCreate;
  globalThis.fetch = vi.fn(() => new Promise((_resolve, reject) => { rejectCreate = reject; }));
  const { history } = renderApp('/games/new');
  await waitFor(() => expect(globalThis.fetch).toHaveBeenCalledTimes(1));

  act(() => history.push('/'));
  await act(async () => {
    rejectCreate(new TypeError('Failed to fetch'));
  });

  expect(currentPath()).toBe('/');
  expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  const unmountWarnings = consoleError.mock.calls.map(args => String(args[0])).filter(m => m.includes('unmounted component'));
  expect(unmountWarnings).toEqual([]);
  consoleError.mockRestore();
});

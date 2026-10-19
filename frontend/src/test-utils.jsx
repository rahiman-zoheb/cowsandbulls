import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route } from 'react-router-dom';
import { AppRoutes } from './App';

// Not named *.test.js on purpose: CRA's testMatch would run it as a suite.

// The client only ever reads response.ok, status and json(), so a plain stub
// is lighter than constructing a real Response.
export function jsonResponse(data, { ok = true, status = 200 } = {}) {
  return Promise.resolve({
    ok,
    status,
    json: () => Promise.resolve(data),
  });
}

export function errorResponse(status = 500) {
  // What Spring returns: an RFC 9457 problem object. It parses as JSON fine,
  // so only the ok check and the game-shape check catch it.
  return jsonResponse(
    { type: 'about:blank', title: 'Internal Server Error', status, detail: 'boom', instance: '/api/games' },
    { ok: false, status }
  );
}

export const GAME_ID = '3f1c2a6e-8a4b-4d2c-9e7f-1a2b3c4d5e6f';
export const OTHER_GAME_ID = '9d8e7f6a-5b4c-4a3b-8c2d-1e0f9a8b7c6d';

export function gameResponse(overrides = {}) {
  return {
    id: GAME_ID,
    status: 'IN_PROGRESS',
    attemptsLeft: 18,
    answer: null,
    guesses: [
      { attempt: 1, name: 'abcd', cows: 1, bulls: 0 },
      { attempt: 2, name: 'quiz', cows: 0, bulls: 2 },
    ],
    ...overrides,
  };
}

// AppNavbar renders react-router's <Link>, which throws outside a Router.
export function renderWithRouter(ui, { route = '/' } = {}) {
  return render(<MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>);
}

// Mounts the real route table at `path`. The returned `history` is the live
// router history, for asserting on redirects and driving navigation.
export function renderApp(path = '/') {
  const router = {};
  const result = render(
    <MemoryRouter initialEntries={[path]}>
      <AppRoutes/>
      <Route render={({ history, location }) => {
        router.history = history;
        return <span data-testid="location" hidden>{location.pathname}</span>;
      }}/>
    </MemoryRouter>
  );
  return { ...result, history: router.history };
}

export function currentPath() {
  return screen.getByTestId('location').textContent;
}

// Several components log the underlying cause when they surface an error. That
// is wanted behaviour, but in tests that trigger failures on purpose it buries
// the reporter output.
export function silenceConsoleError() {
  let spy;
  beforeEach(() => {
    spy = vi.spyOn(console, 'error').mockImplementation(() => {});
  });
  afterEach(() => {
    spy.mockRestore();
  });
}

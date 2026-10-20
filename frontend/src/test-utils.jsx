import { StrictMode, useEffect } from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, useLocation, useNavigate, useNavigationType } from 'react-router';
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

// Mounts the real route table at `path`. The returned `history` drives
// navigation (push, back) and reports how the current location was reached
// (action: 'PUSH', 'REPLACE' or 'POP'). React Router 7 exposes these only
// through hooks, so a probe component inside the router collects them.
export function renderApp(path = '/', { strict = false } = {}) {
  const router = {};
  function RouterProbe() {
    const navigate = useNavigate();
    const action = useNavigationType();
    const location = useLocation();
    useEffect(() => {
      router.navigate = navigate;
      router.action = action;
    });
    return <span data-testid="location" hidden>{location.pathname}</span>;
  }
  const app = (
    <MemoryRouter initialEntries={[path]}>
      <AppRoutes/>
      <RouterProbe/>
    </MemoryRouter>
  );
  // StrictMode is what index.jsx uses; in development it mounts, unmounts and
  // remounts every component, which tests otherwise never exercise.
  const result = render(strict ? <StrictMode>{app}</StrictMode> : app);
  const history = {
    push: to => router.navigate(to),
    back: () => router.navigate(-1),
    get action() {
      return router.action;
    },
  };
  return { ...result, history };
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

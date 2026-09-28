import React from 'react';
import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

// Not named *.test.js on purpose: CRA's testMatch would run it as a suite.

// GuessList only ever reads response.ok and response.json(), so a plain stub is
// lighter than constructing a real Response.
export function jsonResponse(data, { ok = true, status = 200 } = {}) {
  return Promise.resolve({
    ok,
    status,
    json: () => Promise.resolve(data),
  });
}

export function errorResponse(status = 500) {
  // What Spring Boot actually returns: a JSON *object*, so .json() resolves fine
  // and only the ok check catches it.
  return jsonResponse(
    { timestamp: '2026-01-01T00:00:00.000+00:00', status, error: 'Internal Server Error', path: '/api/guess' },
    { ok: false, status }
  );
}

// AppNavbar renders react-router's <Link>, which throws outside a Router.
export function renderWithRouter(ui, { route = '/' } = {}) {
  return render(<MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>);
}

export const SAMPLE_GUESSES = [
  { id: 1, name: 'abcd', cows: 1, bulls: 0 },
  { id: 2, name: 'quiz', cows: 0, bulls: 2 },
];

// Several components log the underlying cause when they surface an error. That
// is wanted behaviour, but in tests that trigger failures on purpose it buries
// the reporter output.
export function silenceConsoleError() {
  let spy;
  beforeEach(() => {
    spy = jest.spyOn(console, 'error').mockImplementation(() => {});
  });
  afterEach(() => {
    spy.mockRestore();
  });
}

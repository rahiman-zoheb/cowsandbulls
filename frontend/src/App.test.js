import React from 'react';
import { render, screen, waitForElementToBeRemoved } from '@testing-library/react';
import App from './App';
import { jsonResponse, SAMPLE_GUESSES } from './test-utils';

// App mounts a BrowserRouter, so the route comes from jsdom's URL rather than
// from initialEntries. jsdom shares one window per file, so it has to be reset.
function visit(path) {
  window.history.pushState({}, '', path);
  return render(<App />);
}

beforeEach(() => {
  global.fetch = jest.fn(() => jsonResponse(SAMPLE_GUESSES));
});

afterEach(() => {
  window.history.pushState({}, '', '/');
});

it('renders the home route by default', () => {
  visit('/');

  expect(screen.getByRole('link', { name: 'Play A Game' })).toHaveAttribute('href', '/guesses');
  expect(global.fetch).not.toHaveBeenCalled();
});

it('renders the guess list at /guesses', async () => {
  visit('/guesses');

  await waitForElementToBeRemoved(() => screen.queryByText('Loading...'));
  expect(screen.getByRole('heading', { name: 'My guesses' })).toBeInTheDocument();
});

it('renders a not-found page for an unknown route', () => {
  // Previously <Switch> matched nothing and rendered null, leaving a blank page
  // with no way to navigate back.
  visit('/does-not-exist');

  expect(screen.getByRole('heading', { name: 'Page not found' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Go back home' })).toHaveAttribute('href', '/');
});

it('renders a not-found page for /guess, a plausible typo for /guesses', () => {
  visit('/guess');

  expect(screen.getByRole('heading', { name: 'Page not found' })).toBeInTheDocument();
  expect(global.fetch).not.toHaveBeenCalled();
});

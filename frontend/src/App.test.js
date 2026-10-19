import React from 'react';
import { render, screen, waitFor, waitForElementToBeRemoved } from '@testing-library/react';
import App from './App';
import { GAME_ID, gameResponse, jsonResponse } from './test-utils';

// App mounts a BrowserRouter, so the route comes from jsdom's URL rather than
// from initialEntries. jsdom shares one window per file, so it has to be reset.
function visit(path) {
  window.history.pushState({}, '', path);
  return render(<App />);
}

beforeEach(() => {
  global.fetch = jest.fn(() => jsonResponse(gameResponse()));
});

afterEach(() => {
  window.history.pushState({}, '', '/');
});

it('renders the home route by default without touching the API', () => {
  visit('/');

  expect(screen.getByRole('link', { name: 'Play A Game' })).toHaveAttribute('href', '/games/new');
  expect(global.fetch).not.toHaveBeenCalled();
});

it('renders a game at /games/:gameId', async () => {
  visit(`/games/${GAME_ID}`);

  await waitForElementToBeRemoved(() => screen.queryByText('Loading...'));
  expect(screen.getByRole('heading', { name: 'My guesses' })).toBeInTheDocument();
  expect(global.fetch.mock.calls[0][0]).toBe(`/api/games/${GAME_ID}`);
});

it('starts a game at /games/new and lands on its URL', async () => {
  visit('/games/new');

  await waitFor(() => expect(window.location.pathname).toBe(`/games/${GAME_ID}`));
  expect(global.fetch.mock.calls[0][1].method).toBe('POST');
});

it('sends the old /guesses link to a new game', async () => {
  // Bookmarks and shared links from before games had their own URLs.
  visit('/guesses');

  await waitFor(() => expect(window.location.pathname).toBe(`/games/${GAME_ID}`));
});

it('renders a not-found page for an unknown route', () => {
  // Previously <Switch> matched nothing and rendered null, leaving a blank page
  // with no way to navigate back.
  visit('/does-not-exist');

  expect(screen.getByRole('heading', { name: 'Page not found' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Go back home' })).toHaveAttribute('href', '/');
  expect(global.fetch).not.toHaveBeenCalled();
});

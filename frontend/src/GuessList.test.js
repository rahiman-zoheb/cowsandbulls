import React from 'react';
import { screen, waitFor, waitForElementToBeRemoved, fireEvent } from '@testing-library/react';
import GuessList from './GuessList';
import { errorResponse, jsonResponse, renderWithRouter, SAMPLE_GUESSES, silenceConsoleError } from './test-utils';

// CRA sets resetMocks:true, which wipes any implementation assigned in
// beforeAll or at module scope. It has to be set per test.
beforeEach(() => {
  global.fetch = jest.fn(() => jsonResponse(SAMPLE_GUESSES));
});

function guessInput() {
  return screen.getByLabelText(/your guess/i);
}

function typeGuess(value) {
  // user-event 7 has no clear(), and fireEvent.change drives the controlled
  // input identically.
  fireEvent.change(guessInput(), { target: { name: 'name', value } });
}

function submitButton() {
  return screen.getByRole('button', { name: /submit/i });
}

async function renderLoaded() {
  renderWithRouter(<GuessList />);
  await waitForElementToBeRemoved(() => screen.queryByText('Loading...'));
}

describe('mount', () => {
  it('shows a loading placeholder before the guesses arrive', () => {
    renderWithRouter(<GuessList />);
    expect(screen.getByText('Loading...')).toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  it('keeps the navbar visible while loading so there is always a way out', () => {
    renderWithRouter(<GuessList />);
    expect(screen.getByRole('link', { name: 'Home' })).toBeInTheDocument();
  });

  it('creates the game before fetching guesses', async () => {
    await renderLoaded();

    expect(global.fetch).toHaveBeenCalledTimes(2);
    // Ordering matters: POST /api/game clears the list server-side, so a
    // concurrent GET could observe either side of the reset.
    expect(global.fetch.mock.calls[0][0]).toBe('/api/game');
    expect(global.fetch.mock.calls[0][1].method).toBe('POST');
    expect(global.fetch.mock.calls[1][0]).toBe('/api/guesses');
  });

  it('renders one row per guess with name, cows and bulls', async () => {
    await renderLoaded();

    const rows = screen.getAllByRole('row');
    expect(rows).toHaveLength(SAMPLE_GUESSES.length + 1); // + header row
    expect(Array.from(rows[1].cells).map(cell => cell.textContent)).toEqual(['abcd', '1', '0']);
    expect(Array.from(rows[2].cells).map(cell => cell.textContent)).toEqual(['quiz', '0', '2']);
  });

  it('still renders rows when the backend omits the id', async () => {
    // The real backend always sends an id, so this pins the key fallback rather
    // than letting duplicate undefined keys silently drop rows.
    global.fetch = jest.fn(() => jsonResponse([
      { name: 'abcd', cows: 1, bulls: 0 },
      { name: 'quiz', cows: 0, bulls: 2 },
    ]));
    await renderLoaded();

    expect(screen.getAllByRole('row')).toHaveLength(3);
    expect(screen.getByText('abcd')).toBeInTheDocument();
    expect(screen.getByText('quiz')).toBeInTheDocument();
  });

  it('tells the user when there are no guesses yet', async () => {
    global.fetch = jest.fn(() => jsonResponse([]));
    await renderLoaded();

    expect(screen.getByText('No guesses yet.')).toBeInTheDocument();
  });
});

describe('failure on mount', () => {
  silenceConsoleError();

  it('surfaces an error instead of hanging on the loading placeholder', async () => {
    global.fetch = jest.fn(() => Promise.reject(new TypeError('Failed to fetch')));
    renderWithRouter(<GuessList />);

    expect(await screen.findByRole('alert')).toHaveTextContent(/backend running/i);
    // The old code only cleared isLoading inside the success path, so a dead
    // backend left "Loading..." on screen forever.
    expect(screen.queryByText('Loading...')).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Home' })).toBeInTheDocument();
  });

  it('surfaces an error when the backend returns a 500', async () => {
    global.fetch = jest.fn(() => errorResponse(500));
    renderWithRouter(<GuessList />);

    expect(await screen.findByRole('alert')).toBeInTheDocument();
    expect(screen.queryByText('Loading...')).not.toBeInTheDocument();
  });

  it('does not crash when the payload is an error object rather than an array', async () => {
    // Spring's error handler replies with JSON, so .json() resolved and the
    // object used to be stored as the guess list and then .map()ed.
    global.fetch = jest.fn(() => jsonResponse({ status: 500, error: 'Internal Server Error' }));
    await renderLoaded();

    expect(screen.getByText('No guesses yet.')).toBeInTheDocument();
  });
});

describe('validation', () => {
  it('marks a four-letter guess valid and enables submit', async () => {
    await renderLoaded();
    typeGuess('gold');

    expect(guessInput()).toHaveClass('is-valid');
    expect(submitButton()).toBeEnabled();
  });

  it.each([
    ['too short', 'abc'],
    ['a digit', '12ab'],
    ['an inner space', 'ab d'],
  ])('marks a guess with %s invalid and blocks submit', async (_label, value) => {
    await renderLoaded();
    typeGuess(value);

    expect(guessInput()).toHaveClass('is-invalid');
    expect(screen.getByText('Please provide a valid 4 letter word')).toBeInTheDocument();
    expect(submitButton()).toBeDisabled();
  });

  it('shows neither state before the field is touched', async () => {
    await renderLoaded();

    expect(guessInput()).not.toHaveClass('is-valid');
    expect(guessInput()).not.toHaveClass('is-invalid');
    expect(submitButton()).toBeDisabled();
  });

  it('accepts uppercase input, which the backend cannot handle itself', async () => {
    await renderLoaded();
    typeGuess('GOLD');

    expect(guessInput()).toHaveClass('is-valid');
    expect(submitButton()).toBeEnabled();
  });
});

describe('submit', () => {
  silenceConsoleError();

  it('posts the guess, refetches, and clears the field', async () => {
    await renderLoaded();
    typeGuess('gold');
    global.fetch.mockClear();

    fireEvent.click(submitButton());

    await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(2));
    expect(global.fetch.mock.calls[0][0]).toBe('/api/guess');
    expect(global.fetch.mock.calls[0][1].method).toBe('POST');
    expect(global.fetch.mock.calls[0][1].body).toBe(JSON.stringify({ name: 'gold' }));
    expect(global.fetch.mock.calls[1][0]).toBe('/api/guesses');
    await waitFor(() => expect(guessInput()).toHaveValue(''));
  });

  it('lowercases the guess before sending it', async () => {
    await renderLoaded();
    typeGuess('GOLD');
    global.fetch.mockClear();

    fireEvent.click(submitButton());

    // Uppercase reaches an unguarded charAt(i) - 'a' on the backend and 500s.
    await waitFor(() => {
      expect(global.fetch.mock.calls[0][1].body).toBe(JSON.stringify({ name: 'gold' }));
    });
  });

  it('never posts an invalid guess, even if the form is submitted directly', async () => {
    await renderLoaded();
    typeGuess('abc');
    global.fetch.mockClear();

    // Bypasses the disabled button the way the Enter key would.
    fireEvent.submit(guessInput().closest('form'));

    await screen.findByRole('alert');
    expect(global.fetch).not.toHaveBeenCalled();
  });

  it('posts only once when submit is clicked twice in a row', async () => {
    await renderLoaded();
    typeGuess('gold');
    global.fetch.mockClear();

    const button = submitButton();
    fireEvent.click(button);
    fireEvent.click(button);

    await waitFor(() => expect(guessInput()).toHaveValue(''));
    const guessPosts = global.fetch.mock.calls.filter(call => call[0] === '/api/guess');
    expect(guessPosts).toHaveLength(1);
  });

  it('ignores a second submit while the first is still in flight', async () => {
    await renderLoaded();
    typeGuess('gold');
    global.fetch.mockClear();

    // The disabled button already blocks a second click, so this goes straight
    // at the form the way the Enter key does, to exercise the in-flight guard.
    const form = guessInput().closest('form');
    fireEvent.submit(form);
    fireEvent.submit(form);

    await waitFor(() => expect(guessInput()).toHaveValue(''));
    const guessPosts = global.fetch.mock.calls.filter(call => call[0] === '/api/guess');
    expect(guessPosts).toHaveLength(1);
  });

  it('keeps the typed guess and reports the failure when the post fails', async () => {
    await renderLoaded();
    typeGuess('gold');
    global.fetch.mockReturnValue(errorResponse(500));

    fireEvent.click(submitButton());

    expect(await screen.findByRole('alert')).toHaveTextContent(/could not submit/i);
    // Clearing the field on a failed submit made the guess look accepted.
    expect(guessInput()).toHaveValue('gold');
  });
});

describe('concurrent loads', () => {
  it('ignores a stale guess list that resolves after a newer one', async () => {
    // The in-flight submit guard serializes the UI paths, so this drives the
    // component directly to pin the latest-wins rule against future callers
    // (a refresh button, polling) that could overlap two loads.
    const listRef = React.createRef();
    global.fetch = jest.fn(() => jsonResponse([]));
    renderWithRouter(<GuessList ref={listRef} />);
    await waitForElementToBeRemoved(() => screen.queryByText('Loading...'));

    let resolveStale;
    global.fetch = jest
      .fn()
      .mockReturnValueOnce(new Promise(resolve => { resolveStale = resolve; }))
      .mockReturnValueOnce(jsonResponse(SAMPLE_GUESSES));

    const stale = listRef.current.loadGuesses();
    const fresh = listRef.current.loadGuesses();

    await fresh;
    resolveStale({ ok: true, status: 200, json: () => Promise.resolve([]) });
    await stale;

    // The older, now-empty response must not overwrite the newer list.
    await waitFor(() => expect(screen.getByText('abcd')).toBeInTheDocument());
    expect(screen.queryByText('No guesses yet.')).not.toBeInTheDocument();
  });
});

describe('unmounting', () => {
  it('does not warn about setting state after unmount', async () => {
    const consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});
    let resolveGuesses;
    global.fetch = jest
      .fn()
      .mockReturnValueOnce(jsonResponse({ id: 1 }, { status: 201 }))
      .mockReturnValueOnce(new Promise(resolve => { resolveGuesses = resolve; }));

    const { unmount } = renderWithRouter(<GuessList />);
    await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(2));
    unmount();
    resolveGuesses({ ok: true, status: 200, json: () => Promise.resolve(SAMPLE_GUESSES) });
    await waitFor(() => {});

    const unmountWarnings = consoleError.mock.calls
      .map(args => String(args[0]))
      .filter(message => message.includes('unmounted component'));
    expect(unmountWarnings).toEqual([]);
    consoleError.mockRestore();
  });
});

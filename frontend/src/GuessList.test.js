import { act, screen, waitFor, waitForElementToBeRemoved, fireEvent } from '@testing-library/react';
import { describeGuess } from './GuessList';
import {
  currentPath, errorResponse, GAME_ID, gameResponse, jsonResponse, OTHER_GAME_ID, renderApp, silenceConsoleError,
} from './test-utils';

const GAME_PATH = `/games/${GAME_ID}`;

// CRA sets resetMocks:true, which wipes any implementation assigned in
// beforeAll or at module scope. It has to be set per test.
beforeEach(() => {
  globalThis.fetch = vi.fn(() => jsonResponse(gameResponse()));
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

const posts = () => globalThis.fetch.mock.calls.filter(([, options]) => options && options.method === 'POST');

async function renderLoaded(path = GAME_PATH) {
  const rendered = renderApp(path);
  await waitForElementToBeRemoved(() => screen.queryByText('Loading...'));
  return rendered;
}

describe('loading a game', () => {
  it('shows a loading placeholder and the navbar before the game arrives', () => {
    renderApp(GAME_PATH);
    expect(screen.getByText('Loading...')).toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Home' })).toBeInTheDocument();
  });

  it('reads the game named in the URL', async () => {
    await renderLoaded();

    expect(globalThis.fetch).toHaveBeenCalledTimes(1);
    expect(globalThis.fetch.mock.calls[0][0]).toBe(`/api/games/${GAME_ID}`);
  });

  it('never creates a game, so refreshing resumes the same one', async () => {
    // The old page called POST /api/game on every mount, and that endpoint
    // deleted every guess, so a refresh silently threw your game away.
    const { unmount } = await renderLoaded();
    unmount();
    await renderLoaded();

    expect(posts()).toEqual([]);
    expect(screen.getAllByRole('row')).toHaveLength(3);
  });

  it('renders one row per guess with attempt, word, cows and bulls', async () => {
    await renderLoaded();

    const rows = screen.getAllByRole('row');
    expect(rows).toHaveLength(3); // + header row
    expect(Array.from(rows[1].cells).map(cell => cell.textContent)).toEqual(['1', 'abcd', '1', '0']);
    expect(Array.from(rows[2].cells).map(cell => cell.textContent)).toEqual(['2', 'quiz', '0', '2']);
  });

  it('tells the player when there are no guesses yet', async () => {
    globalThis.fetch = vi.fn(() => jsonResponse(gameResponse({ guesses: [] })));
    await renderLoaded();

    expect(screen.getByText('No guesses yet.')).toBeInTheDocument();
  });

  it('shows how many guesses are left', async () => {
    await renderLoaded();

    expect(screen.getByText('18 guesses left')).toBeInTheDocument();
  });

  it('says "guess" when one is left', async () => {
    globalThis.fetch = vi.fn(() => jsonResponse(gameResponse({ attemptsLeft: 1 })));
    await renderLoaded();

    expect(screen.getByText('1 guess left')).toBeInTheDocument();
  });

  it('offers a new game while playing', async () => {
    await renderLoaded();

    expect(screen.getByRole('link', { name: 'New game' })).toHaveAttribute('href', '/games/new');
  });
});

describe('a game that cannot be loaded', () => {
  silenceConsoleError();

  it('explains that an unknown or expired game is gone and offers a new one', async () => {
    globalThis.fetch = vi.fn(() => errorResponse(404));
    await renderLoaded();

    expect(screen.getByText(/doesn't exist, or it has expired/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'New game' })).toHaveAttribute('href', '/games/new');
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  it('surfaces an error instead of hanging when the backend is unreachable', async () => {
    globalThis.fetch = vi.fn(() => Promise.reject(new TypeError('Failed to fetch')));
    renderApp(GAME_PATH);

    expect(await screen.findByRole('alert')).toHaveTextContent(/backend running/i);
    expect(screen.queryByText('Loading...')).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Home' })).toBeInTheDocument();
  });

  it('surfaces an error when the backend returns a 500', async () => {
    globalThis.fetch = vi.fn(() => errorResponse(500));
    renderApp(GAME_PATH);

    expect(await screen.findByRole('alert')).toHaveTextContent(/could not load/i);
  });

  it('refuses to render a payload that is not a game', async () => {
    globalThis.fetch = vi.fn(() => jsonResponse({ status: 500, error: 'Internal Server Error' }));
    renderApp(GAME_PATH);

    expect(await screen.findByRole('alert')).toHaveTextContent(/could not load/i);
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
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

  it('accepts uppercase input', async () => {
    await renderLoaded();
    typeGuess('GOLD');

    expect(guessInput()).toHaveClass('is-valid');
  });
});

describe('submitting a guess', () => {
  silenceConsoleError();

  const afterGuess = gameResponse({
    guesses: [...gameResponse().guesses, { attempt: 3, name: 'gold', cows: 0, bulls: 1 }],
  });

  it('posts to this game and renders the updated game it returns, without refetching', async () => {
    await renderLoaded();
    typeGuess('gold');
    globalThis.fetch = vi.fn(() => jsonResponse(afterGuess, { status: 201 }));

    fireEvent.click(submitButton());

    await waitFor(() => expect(screen.getAllByRole('row')).toHaveLength(4));
    expect(globalThis.fetch).toHaveBeenCalledTimes(1);
    const [url, options] = globalThis.fetch.mock.calls[0];
    expect(url).toBe(`/api/games/${GAME_ID}/guesses`);
    expect(options.method).toBe('POST');
    expect(options.body).toBe(JSON.stringify({ name: 'gold' }));
    expect(guessInput()).toHaveValue('');
  });

  it('lowercases the guess before sending it', async () => {
    await renderLoaded();
    typeGuess('GOLD');
    globalThis.fetch = vi.fn(() => jsonResponse(afterGuess, { status: 201 }));

    fireEvent.click(submitButton());

    await waitFor(() => expect(globalThis.fetch.mock.calls[0][1].body).toBe(JSON.stringify({ name: 'gold' })));
  });

  it('never posts an invalid guess, even if the form is submitted directly', async () => {
    await renderLoaded();
    typeGuess('abc');
    globalThis.fetch.mockClear();

    // Bypasses the disabled button the way the Enter key would.
    fireEvent.submit(guessInput().closest('form'));

    await screen.findByRole('alert');
    expect(globalThis.fetch).not.toHaveBeenCalled();
  });

  it('posts only once when submit is clicked twice in a row', async () => {
    await renderLoaded();
    typeGuess('gold');
    globalThis.fetch = vi.fn(() => jsonResponse(afterGuess, { status: 201 }));

    const button = submitButton();
    fireEvent.click(button);
    fireEvent.click(button);

    await waitFor(() => expect(guessInput()).toHaveValue(''));
    expect(posts()).toHaveLength(1);
  });

  it('ignores a second submit while the first is still in flight', async () => {
    await renderLoaded();
    typeGuess('gold');
    globalThis.fetch = vi.fn(() => jsonResponse(afterGuess, { status: 201 }));

    // The disabled button already blocks a second click, so this goes straight
    // at the form the way the Enter key does, to exercise the in-flight guard.
    const form = guessInput().closest('form');
    fireEvent.submit(form);
    fireEvent.submit(form);

    await waitFor(() => expect(guessInput()).toHaveValue(''));
    expect(posts()).toHaveLength(1);
  });

  it.each([
    [422, "'gold' is not in the word list"],
    [409, "You already guessed 'gold'"],
    [429, 'Too many requests; try again in a minute'],
  ])("shows the server's reason for a %i and keeps the guess to correct", async (status, detail) => {
    await renderLoaded();
    typeGuess('gold');
    globalThis.fetch = vi.fn(() => jsonResponse({ status, detail }, { ok: false, status }));

    fireEvent.click(submitButton());

    expect(await screen.findByRole('alert')).toHaveTextContent(detail);
    expect(guessInput()).toHaveValue('gold');
    expect(submitButton()).toBeEnabled();
  });

  it('falls back to a generic message when a rejection has no reason', async () => {
    await renderLoaded();
    typeGuess('gold');
    globalThis.fetch = vi.fn(() => Promise.resolve({ ok: false, status: 422, json: () => Promise.resolve({}) }));

    fireEvent.click(submitButton());

    expect(await screen.findByRole('alert')).toHaveTextContent(/could not submit/i);
  });

  it('keeps the typed guess and reports the failure when the post fails', async () => {
    await renderLoaded();
    typeGuess('gold');
    globalThis.fetch = vi.fn(() => errorResponse(500));

    fireEvent.click(submitButton());

    expect(await screen.findByRole('alert')).toHaveTextContent(/could not submit/i);
    // Clearing the field on a failed submit made the guess look accepted.
    expect(guessInput()).toHaveValue('gold');
  });
});

describe('winning', () => {
  const won = gameResponse({
    status: 'WON',
    answer: 'gold',
    guesses: [...gameResponse().guesses, { attempt: 3, name: 'gold', cows: 0, bulls: 4 }],
  });

  it('shows the answer and attempt count, and replaces the form with a new-game button', async () => {
    await renderLoaded();
    typeGuess('gold');
    globalThis.fetch = vi.fn(() => jsonResponse(won, { status: 201 }));

    fireEvent.click(submitButton());

    expect(await screen.findByText(/you got it in 3 guesses/i)).toHaveTextContent('The word was gold.');
    expect(screen.queryByLabelText(/your guess/i)).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'New game' })).toHaveAttribute('href', '/games/new');
    expect(screen.getAllByRole('row')).toHaveLength(4);
  });

  it('shows the win when an already-won game is reopened', async () => {
    globalThis.fetch = vi.fn(() => jsonResponse(won));
    await renderLoaded();

    expect(screen.getByText(/you got it in 3 guesses/i)).toBeInTheDocument();
  });

  it('says "guess" for a first-try win', async () => {
    globalThis.fetch = vi.fn(() => jsonResponse(gameResponse({
      status: 'WON', answer: 'gold', guesses: [{ attempt: 1, name: 'gold', cows: 0, bulls: 4 }],
    })));
    await renderLoaded();

    expect(screen.getByText(/you got it in 1 guess!/i)).toBeInTheDocument();
  });
});

describe('running out of guesses', () => {
  const lost = gameResponse({ status: 'LOST', answer: 'gold', attemptsLeft: 0 });

  it('reveals the answer and offers a new game instead of the form', async () => {
    globalThis.fetch = vi.fn(() => jsonResponse(lost));
    await renderLoaded();

    expect(screen.getByText(/out of guesses/i)).toHaveTextContent('The word was gold.');
    expect(screen.queryByLabelText(/your guess/i)).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'New game' })).toHaveAttribute('href', '/games/new');
  });

  it('switches to the loss screen when the last guess misses', async () => {
    globalThis.fetch = vi.fn(() => jsonResponse(gameResponse({ attemptsLeft: 1 })));
    await renderLoaded();
    typeGuess('gold');
    globalThis.fetch = vi.fn(() => jsonResponse(lost, { status: 201 }));

    fireEvent.click(submitButton());

    expect(await screen.findByText(/out of guesses/i)).toBeInTheDocument();
  });
});

describe('accessibility', () => {
  const afterGuess = gameResponse({
    attemptsLeft: 17,
    guesses: [...gameResponse().guesses, { attempt: 3, name: 'fork', cows: 0, bulls: 1 }],
  });
  const liveRegion = () => document.querySelector('[aria-live="polite"]');

  it('announces the score of each guess to screen readers', async () => {
    await renderLoaded();
    expect(liveRegion()).toHaveTextContent('');
    typeGuess('fork');
    globalThis.fetch = vi.fn(() => jsonResponse(afterGuess, { status: 201 }));

    fireEvent.click(submitButton());

    await waitFor(() => expect(liveRegion()).toHaveTextContent('fork: 0 cows, 1 bull. 17 guesses left.'));
  });

  it('puts focus back in the input after a guess, so the next one can be typed', async () => {
    await renderLoaded();
    typeGuess('fork');
    globalThis.fetch = vi.fn(() => jsonResponse(afterGuess, { status: 201 }));

    fireEvent.click(submitButton());

    // The input is disabled while submitting, which drops focus.
    await waitFor(() => expect(guessInput()).toHaveFocus());
  });

  it('puts focus back in the input after a rejected guess, so it can be corrected', async () => {
    await renderLoaded();
    typeGuess('qqqq');
    globalThis.fetch = vi.fn(() => jsonResponse({ status: 422, detail: "'qqqq' is not in the word list" }, { ok: false, status: 422 }));

    fireEvent.click(submitButton());

    await screen.findByRole('alert');
    await waitFor(() => expect(guessInput()).toHaveFocus());
  });

  it('marks the input invalid and describes it with the error and guesses left', async () => {
    await renderLoaded();
    expect(guessInput()).toHaveAttribute('aria-invalid', 'false');

    typeGuess('ab1');

    expect(guessInput()).toHaveAttribute('aria-invalid', 'true');
    const describedBy = guessInput().getAttribute('aria-describedby').split(' ').map(id => document.getElementById(id));
    expect(describedBy.map(el => el.textContent)).toEqual(['Please provide a valid 4 letter word', '18 guesses left']);
  });

  it('does not announce on a win; the win message itself is a status', async () => {
    await renderLoaded();
    typeGuess('fork');
    globalThis.fetch = vi.fn(() => jsonResponse(
      gameResponse({ status: 'WON', answer: 'fork', attemptsLeft: 0 }), { status: 201 }));

    fireEvent.click(submitButton());

    expect(await screen.findByRole('status')).toHaveTextContent(/you got it/i);
    expect(liveRegion()).toHaveTextContent('');
  });
});

describe('describeGuess', () => {
  it.each([
    [{ name: 'fork', cows: 0, bulls: 1 }, 17, 'fork: 0 cows, 1 bull. 17 guesses left.'],
    [{ name: 'bros', cows: 1, bulls: 2 }, 1, 'bros: 1 cow, 2 bulls. 1 guess left.'],
    [{ name: 'quiz', cows: 4, bulls: 0 }, 0, 'quiz: 4 cows, 0 bulls. 0 guesses left.'],
  ])('describes %o with %i left', (latest, attemptsLeft, expected) => {
    expect(describeGuess({ attemptsLeft, guesses: [{ name: 'xxxx', cows: 0, bulls: 0 }, latest] })).toBe(expected);
  });
});

describe('navigation', () => {
  it('loads the new game when the URL changes to another game', async () => {
    const { history } = await renderLoaded();
    globalThis.fetch = vi.fn(() => jsonResponse(gameResponse({ id: OTHER_GAME_ID, guesses: [] })));

    act(() => history.push(`/games/${OTHER_GAME_ID}`));

    expect(await screen.findByText('No guesses yet.')).toBeInTheDocument();
    expect(globalThis.fetch.mock.calls[0][0]).toBe(`/api/games/${OTHER_GAME_ID}`);
    expect(currentPath()).toBe(`/games/${OTHER_GAME_ID}`);
  });

  it('ignores a slow response for a game the player has already left', async () => {
    let resolveFirst;
    globalThis.fetch = vi.fn()
      .mockReturnValueOnce(new Promise(resolve => { resolveFirst = resolve; }))
      .mockReturnValueOnce(jsonResponse(gameResponse({ id: OTHER_GAME_ID, guesses: [] })));
    const { history } = renderApp(GAME_PATH);

    act(() => history.push(`/games/${OTHER_GAME_ID}`));
    await screen.findByText('No guesses yet.');
    await act(async () => {
      resolveFirst({ ok: true, status: 200, json: () => Promise.resolve(gameResponse()) });
    });

    // The first game's two guesses must not replace the second game's empty list.
    expect(screen.getByText('No guesses yet.')).toBeInTheDocument();
    expect(screen.queryByText('abcd')).not.toBeInTheDocument();
  });
});

describe('unmounting', () => {
  it('does not warn about setting state after unmount', async () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});
    let resolveGame;
    globalThis.fetch = vi.fn(() => new Promise(resolve => { resolveGame = resolve; }));

    const { unmount } = renderApp(GAME_PATH);
    await waitFor(() => expect(globalThis.fetch).toHaveBeenCalledTimes(1));
    unmount();
    await act(async () => {
      resolveGame({ ok: true, status: 200, json: () => Promise.resolve(gameResponse()) });
    });

    const unmountWarnings = consoleError.mock.calls
      .map(args => String(args[0]))
      .filter(message => message.includes('unmounted component'));
    expect(unmountWarnings).toEqual([]);
    consoleError.mockRestore();
  });
});

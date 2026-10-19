import { API_BASE, createGame, fetchGame, isValidGuess, normalizeGuess, submitGuess } from './api';
import { errorResponse, GAME_ID, gameResponse, jsonResponse } from './test-utils';

// CRA sets resetMocks:true, so the implementation must be (re)assigned per test.
beforeEach(() => {
  global.fetch = jest.fn();
});

describe('normalizeGuess', () => {
  it('trims surrounding whitespace and lowercases', () => {
    expect(normalizeGuess('  GOLD  ')).toBe('gold');
  });

  it('coerces null and undefined to an empty string rather than throwing', () => {
    expect(normalizeGuess(null)).toBe('');
    expect(normalizeGuess(undefined)).toBe('');
  });
});

describe('isValidGuess', () => {
  it.each(['gold', 'GOLD', '  quiz  ', 'AbCd'])('accepts %p', value => {
    expect(isValidGuess(value)).toBe(true);
  });

  it.each(['', 'abc', 'abcde', '12ab', 'ab d', 'ab-d', 'über'])('rejects %p', value => {
    expect(isValidGuess(value)).toBe(false);
  });
});

describe('createGame', () => {
  it('posts to the games collection without a body', async () => {
    global.fetch.mockReturnValue(jsonResponse(gameResponse({ guesses: [] }), { status: 201 }));

    await expect(createGame()).resolves.toMatchObject({ id: GAME_ID, guesses: [] });

    const [url, options] = global.fetch.mock.calls[0];
    expect(url).toBe(`${API_BASE}/games`);
    expect(options.method).toBe('POST');
    expect(options.body).toBeUndefined();
  });

  it('rejects when the backend fails, carrying the status', async () => {
    global.fetch.mockReturnValue(errorResponse(500));
    await expect(createGame()).rejects.toMatchObject({ status: 500, message: expect.stringContaining('status 500') });
  });
});

describe('fetchGame', () => {
  it('reads the game by id', async () => {
    global.fetch.mockReturnValue(jsonResponse(gameResponse()));

    await expect(fetchGame(GAME_ID)).resolves.toEqual(gameResponse());
    expect(global.fetch.mock.calls[0][0]).toBe(`/api/games/${GAME_ID}`);
  });

  it('escapes the id, so it cannot redirect the request to another path', async () => {
    global.fetch.mockReturnValue(jsonResponse(gameResponse()));

    await fetchGame('../secrets');

    expect(global.fetch.mock.calls[0][0]).toBe('/api/games/..%2Fsecrets');
  });

  it('reports a missing game as status 404', async () => {
    global.fetch.mockReturnValue(errorResponse(404));
    await expect(fetchGame(GAME_ID)).rejects.toMatchObject({ status: 404 });
  });

  it.each([
    ['an error envelope', { status: 500, error: 'Internal Server Error' }],
    ['null', null],
    ['a game with no guesses list', { id: GAME_ID }],
  ])('rejects %s instead of returning something that cannot be rendered', async (_label, payload) => {
    global.fetch.mockReturnValue(jsonResponse(payload));
    await expect(fetchGame(GAME_ID)).rejects.toThrow('Unexpected response');
  });
});

describe('submitGuess', () => {
  it('posts the normalized lowercase guess to the game and returns the updated game', async () => {
    const updated = gameResponse({ guesses: [{ attempt: 1, name: 'gold', cows: 0, bulls: 1 }] });
    global.fetch.mockReturnValue(jsonResponse(updated, { status: 201 }));

    await expect(submitGuess(GAME_ID, '  GOLD  ')).resolves.toEqual(updated);

    const [url, options] = global.fetch.mock.calls[0];
    expect(url).toBe(`/api/games/${GAME_ID}/guesses`);
    expect(options.method).toBe('POST');
    expect(options.body).toBe(JSON.stringify({ name: 'gold' }));
  });

  it('refuses an invalid guess without touching the network', async () => {
    await expect(submitGuess(GAME_ID, 'abc')).rejects.toThrow('4 letter word');
    expect(global.fetch).not.toHaveBeenCalled();
  });

  it('rejects when the game is already finished', async () => {
    global.fetch.mockReturnValue(errorResponse(409));
    await expect(submitGuess(GAME_ID, 'gold')).rejects.toMatchObject({ status: 409 });
  });

  it("keeps the server's explanation of a rejected guess", async () => {
    global.fetch.mockReturnValue(jsonResponse(
      { title: 'Unprocessable Content', status: 422, detail: "'gold' is not in the word list" },
      { ok: false, status: 422 }
    ));
    await expect(submitGuess(GAME_ID, 'gold'))
      .rejects.toMatchObject({ status: 422, detail: "'gold' is not in the word list" });
  });

  it('still rejects cleanly when the error body is not JSON', async () => {
    global.fetch.mockReturnValue(Promise.resolve({
      ok: false, status: 502, json: () => Promise.reject(new SyntaxError('Unexpected token <')),
    }));
    const error = await submitGuess(GAME_ID, 'gold').catch(e => e);

    expect(error.status).toBe(502);
    expect(error.detail).toBeUndefined();
  });
});

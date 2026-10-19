import { API_BASE, createGame, fetchGuesses, isValidGuess, normalizeGuess, submitGuess } from './api';
import { errorResponse, jsonResponse, SAMPLE_GUESSES } from './test-utils';

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

  // The backend throws on a length mismatch and overflows a 26-slot array on
  // anything outside a-z, so each of these would come back as an opaque 500.
  it.each(['', 'abc', 'abcde', '12ab', 'ab d', 'ab-d', 'über'])('rejects %p', value => {
    expect(isValidGuess(value)).toBe(false);
  });
});

describe('createGame', () => {
  it('posts to the game endpoint without a body', async () => {
    global.fetch.mockReturnValue(jsonResponse({ id: 1 }, { status: 201 }));

    await createGame();

    expect(global.fetch).toHaveBeenCalledTimes(1);
    const [url, options] = global.fetch.mock.calls[0];
    expect(url).toBe(`${API_BASE}/game`);
    expect(options.method).toBe('POST');
    // The controller declares no @RequestBody; an empty-string body with a JSON
    // content type was meaningless.
    expect(options.body).toBeUndefined();
    expect(options.headers['Content-Type']).toBeUndefined();
  });

  it('rejects when the backend fails', async () => {
    global.fetch.mockReturnValue(errorResponse(500));
    await expect(createGame()).rejects.toThrow('status 500');
  });
});

describe('fetchGuesses', () => {
  it('requests the guesses with a root-relative path', async () => {
    global.fetch.mockReturnValue(jsonResponse(SAMPLE_GUESSES));

    await expect(fetchGuesses()).resolves.toEqual(SAMPLE_GUESSES);
    // A bare 'api/guesses' resolved correctly only while the route was exactly
    // /guesses; a trailing slash broke it.
    expect(global.fetch.mock.calls[0][0]).toBe('/api/guesses');
  });

  it('rejects on a non-ok response instead of returning the error envelope', async () => {
    global.fetch.mockReturnValue(errorResponse(500));
    await expect(fetchGuesses()).rejects.toThrow('status 500');
  });

  it('returns an empty list when the payload is not an array', async () => {
    // Guards the crash where an error object was stored and then .map()ed.
    global.fetch.mockReturnValue(jsonResponse({ status: 500 }));
    await expect(fetchGuesses()).resolves.toEqual([]);
  });

  it('returns an empty list when the payload is null', async () => {
    global.fetch.mockReturnValue(jsonResponse(null));
    await expect(fetchGuesses()).resolves.toEqual([]);
  });
});

describe('submitGuess', () => {
  it('posts the normalized lowercase guess', async () => {
    global.fetch.mockReturnValue(jsonResponse({ id: 3, name: 'gold', cows: 0, bulls: 1 }, { status: 201 }));

    await submitGuess('  GOLD  ');

    const [url, options] = global.fetch.mock.calls[0];
    expect(url).toBe(`${API_BASE}/guess`);
    // Always POST: the backend exposes no PUT for guesses.
    expect(options.method).toBe('POST');
    // Uppercase would have thrown ArrayIndexOutOfBounds server-side.
    expect(options.body).toBe(JSON.stringify({ name: 'gold' }));
  });

  it('refuses an invalid guess without touching the network', async () => {
    await expect(submitGuess('abc')).rejects.toThrow('4 letter word');
    expect(global.fetch).not.toHaveBeenCalled();
  });

  it('rejects when the backend returns an error', async () => {
    global.fetch.mockReturnValue(errorResponse(500));
    await expect(submitGuess('gold')).rejects.toThrow('status 500');
  });
});

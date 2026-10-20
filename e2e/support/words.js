import { readFileSync } from 'node:fs';

// The backend's word list, read from the repository so the tests and the
// server can never disagree about which words exist.
const words = readFileSync(new URL('../../backend/src/main/resources/four-letters.txt', import.meta.url), 'utf8')
  .split(/\r?\n/)
  .map(word => word.trim())
  .filter(word => /^[a-z]{4}$/.test(word));

export const DICTIONARY = words;

// Only words with four different letters can be a secret.
export const SECRETS = words.filter(word => new Set(word).size === 4);

// Guaranteed misses: a word with a repeated letter is a legal guess but can
// never be the secret.
export const NEVER_SECRETS = words.filter(word => new Set(word).size < 4);

/**
 * The backend's scoring rule (Calculator.java): a bull is the right letter in
 * the right place; a cow is a right letter elsewhere, each secret letter
 * counted at most once.
 */
export function score(secret, guess) {
  let bulls = 0;
  const unmatched = {};
  for (let i = 0; i < 4; i++) {
    if (guess[i] === secret[i]) {
      bulls++;
    } else {
      unmatched[secret[i]] = (unmatched[secret[i]] || 0) + 1;
    }
  }
  let cows = 0;
  for (let i = 0; i < 4; i++) {
    if (guess[i] !== secret[i] && unmatched[guess[i]] > 0) {
      cows++;
      unmatched[guess[i]]--;
    }
  }
  return { cows, bulls };
}

/** Secrets still consistent with every guess and score seen so far. */
export function remainingSecrets(history, candidates = SECRETS) {
  return candidates.filter(secret => history.every(({ guess, cows, bulls }) => {
    const expected = score(secret, guess);
    return expected.cows === cows && expected.bulls === bulls;
  }));
}

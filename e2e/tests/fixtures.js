import { test as base, expect } from '@playwright/test';

// Every test runs against the real security headers, so any script, style or
// request the Content-Security-Policy blocks fails the test that triggered it.
export const test = base.extend({
  page: async ({ page }, use) => {
    await page.addInitScript(() => {
      window.__cspViolations = [];
      document.addEventListener('securitypolicyviolation', event => {
        window.__cspViolations.push(`${event.violatedDirective} blocked ${event.blockedURI || 'inline'}`);
      });
    });
    await use(page);
    const violations = await page.evaluate(() => window.__cspViolations || []).catch(() => []);
    expect(violations, 'Content-Security-Policy violations').toEqual([]);
  },
});

export { expect };

export const guessInput = page => page.getByLabel(/your guess/i);

/** Starts a game the way a player does, and returns its id. */
export async function startGame(page) {
  await page.goto('/games/new');
  await page.waitForURL(/\/games\/[0-9a-f-]{36}$/);
  await expect(guessInput(page)).toBeVisible();
  return page.url().split('/').pop();
}

/**
 * Types and submits a guess through the UI, and returns the score from the
 * server's response to the request the UI made.
 */
export async function guess(page, word) {
  await guessInput(page).fill(word);
  const [response] = await Promise.all([
    page.waitForResponse(r => r.url().endsWith('/guesses') && r.request().method() === 'POST'),
    page.getByRole('button', { name: 'Submit' }).click(),
  ]);
  const game = await response.json();
  return { status: response.status(), game, latest: game.guesses ? game.guesses[game.guesses.length - 1] : undefined };
}

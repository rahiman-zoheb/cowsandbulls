import AxeBuilder from '@axe-core/playwright';
import { expect, guess, startGame, test } from './fixtures.js';
import { NEVER_SECRETS } from '../support/words.js';

// The unit tests run axe in jsdom, which can't lay pages out, so they have to
// switch colour contrast off. A real browser can, so this checks every rule,
// contrast included.
async function expectAccessible(page) {
  const results = await new AxeBuilder({ page }).analyze();
  expect(results.violations.map(v => `${v.id} (${v.impact}): ${v.nodes.map(n => n.target.join(' ')).join(', ')}`))
    .toEqual([]);
}

test('home page', async ({ page }) => {
  await page.goto('/');
  await expectAccessible(page);
});

test('a game in progress, with a scored guess and an error shown', async ({ page }) => {
  await startGame(page);
  await guess(page, 'fork');
  await guess(page, 'qqqq');
  await expectAccessible(page);
});

test('a finished game', async ({ page }) => {
  await startGame(page);
  for (const word of NEVER_SECRETS.slice(0, 20)) {
    await guess(page, word);
  }
  await expectAccessible(page);
});

test('the expanded menu on a narrow screen', async ({ page }) => {
  await page.setViewportSize({ width: 400, height: 800 });
  await page.goto('/');
  await page.getByRole('button', { name: /toggle navigation/i }).click();
  await expect(page.getByRole('link', { name: 'Cows And Bulls' })).toBeVisible();
  await expectAccessible(page);
});

test('page not found', async ({ page }) => {
  await page.goto('/nowhere');
  await expectAccessible(page);
});

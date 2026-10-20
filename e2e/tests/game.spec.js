import { expect, guess, guessInput, startGame, test } from './fixtures.js';
import { NEVER_SECRETS, remainingSecrets } from '../support/words.js';

test('a player starts from the home page and sees a guess scored', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('link', { name: 'Play A Game' }).click();
  await page.waitForURL(/\/games\/[0-9a-f-]{36}$/);

  const { status, latest } = await guess(page, 'fork');

  expect(status).toBe(201);
  const row = page.getByRole('row').filter({ hasText: 'fork' });
  await expect(row.getByRole('cell')).toHaveText(['1', 'fork', `${latest.cows}`, `${latest.bulls}`]);
  await expect(page.getByText('19 guesses left', { exact: true })).toBeVisible();
  // Keyboard players can type the next guess straight away.
  await expect(guessInput(page)).toBeFocused();
});

test('solving the puzzle wins and reveals the word', async ({ page }) => {
  // No backdoor: the API never gives out the answer, so this plays the game,
  // keeping only the secrets consistent with every score so far. Offline,
  // this strategy solves all 3,120 secrets within 14 guesses.
  await startGame(page);
  const history = [];
  let outcome;
  do {
    const [candidate] = remainingSecrets(history);
    outcome = await guess(page, candidate);
    history.push({ guess: candidate, cows: outcome.latest.cows, bulls: outcome.latest.bulls });
  } while (outcome.game.status === 'IN_PROGRESS');

  expect(outcome.game.status).toBe('WON');
  const answer = history[history.length - 1].guess;
  await expect(page.getByRole('status').filter({ hasText: 'You got it' })).toContainText(`The word was ${answer}.`);
  await expect(guessInput(page)).toHaveCount(0);
  await expect(page.getByRole('link', { name: 'New game' })).toBeVisible();
});

test('running out of guesses loses and reveals the word', async ({ page }) => {
  // Words with a repeated letter are legal guesses but never the secret, so
  // twenty of them always lose.
  await startGame(page);
  let outcome;
  for (const word of NEVER_SECRETS.slice(0, 20)) {
    outcome = await guess(page, word);
  }

  expect(outcome.game.status).toBe('LOST');
  await expect(page.getByRole('status').filter({ hasText: 'Out of guesses' }))
    .toContainText(`The word was ${outcome.game.answer}.`);
});

test('refreshing mid-game keeps the guesses and creates no new game', async ({ page }) => {
  const id = await startGame(page);
  await guess(page, 'fork');

  const creations = [];
  page.on('request', request => {
    if (request.method() === 'POST' && request.url().endsWith('/api/games')) {
      creations.push(request.url());
    }
  });
  await page.reload();

  await expect(page.getByRole('row').filter({ hasText: 'fork' })).toBeVisible();
  expect(page.url()).toContain(id);
  expect(creations).toEqual([]);
});

test('invalid guesses are explained, and nothing malformed is sent', async ({ page }) => {
  await startGame(page);

  await guessInput(page).fill('ab1');
  await expect(guessInput(page)).toHaveAttribute('aria-invalid', 'true');
  await expect(page.getByText('Please provide a valid 4 letter word')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Submit' })).toBeDisabled();

  const { status } = await guess(page, 'qqqq');
  expect(status).toBe(422);
  await expect(page.getByRole('alert')).toHaveText("'qqqq' is not in the word list");
  await expect(guessInput(page)).toHaveValue('qqqq');
});

test('two tabs play independent games', async ({ context }) => {
  const first = await context.newPage();
  const second = await context.newPage();
  await startGame(first);
  await startGame(second);

  await guess(first, 'fork');

  await expect(first.getByRole('row').filter({ hasText: 'fork' })).toBeVisible();
  await second.reload();
  await expect(second.getByText('No guesses yet.')).toBeVisible();
});

test('a backend outage mid-game is reported, and the game carries on afterwards', async ({ page }) => {
  await startGame(page);
  await page.route('**/api/**', route => route.abort('connectionrefused'));

  await guessInput(page).fill('fork');
  await page.getByRole('button', { name: 'Submit' }).click();
  await expect(page.getByRole('alert')).toContainText('Could not submit your guess');
  await expect(guessInput(page)).toHaveValue('fork');

  await page.unroute('**/api/**');
  const { status } = await guess(page, 'fork');
  expect(status).toBe(201);
  await expect(page.getByRole('row').filter({ hasText: 'fork' })).toBeVisible();
});

test('a link to an unknown game offers a new one', async ({ page }) => {
  await page.goto('/games/00000000-0000-4000-8000-000000000000');

  await expect(page.getByText("This game doesn't exist, or it has expired.")).toBeVisible();
  await page.getByRole('link', { name: 'New game' }).click();
  await page.waitForURL(/\/games\/[0-9a-f-]{36}$/);
});

test('an old /guesses link lands on a new game', async ({ page }) => {
  await page.goto('/guesses');
  await page.waitForURL(/\/games\/[0-9a-f-]{36}$/);
  await expect(guessInput(page)).toBeVisible();
});

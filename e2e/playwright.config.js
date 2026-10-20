import { defineConfig } from '@playwright/test';
import process from 'node:process';

// Runs against a live, single-origin app: the image built from the root
// Dockerfile in CI, or any server given by E2E_BASE_URL.
export default defineConfig({
  testDir: './tests',
  // One server, shared state: keep runs serial and deterministic.
  workers: 1,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://localhost:8080',
    // E2E_CHANNEL=msedge (or chrome) uses an installed browser; unset, the
    // bundled Chromium from `npx playwright install chromium`.
    channel: process.env.E2E_CHANNEL || undefined,
    trace: 'retain-on-failure',
  },
});

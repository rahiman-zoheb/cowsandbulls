import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    // Same contract CRA's "proxy" field had: the backend serves /api on 8080.
    proxy: { '/api': 'http://127.0.0.1:8080' },
  },
  build: {
    // Kept from CRA, so .gitignore and anything packaging the build still apply.
    outDir: 'build',
  },
  test: {
    environment: 'jsdom',
    // React Testing Library 12 registers its automatic cleanup through a global
    // afterEach, and jest-dom 5 extends a global expect.
    globals: true,
    setupFiles: ['./src/setupTests.js'],
    coverage: {
      provider: 'v8',
      include: ['src/**/*.{js,jsx}'],
      exclude: ['src/index.jsx', 'src/test-utils.jsx', 'src/setupTests.js', 'src/**/*.test.{js,jsx}'],
      thresholds: { statements: 95, branches: 90, functions: 95, lines: 95 },
      reporter: ['text', 'lcov'],
    },
  },
});

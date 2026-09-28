import React from 'react';
import { render, screen } from '@testing-library/react';
import ErrorBoundary from './ErrorBoundary';

function Boom() {
  throw new Error('render exploded');
}

let consoleError;

beforeEach(() => {
  // React logs the caught error itself; the boundary adds its own line. Neither
  // is a failure, but both would drown the reporter.
  consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});
});

afterEach(() => {
  consoleError.mockRestore();
});

it('renders its children when nothing throws', () => {
  render(
    <ErrorBoundary>
      <p>all good</p>
    </ErrorBoundary>
  );

  expect(screen.getByText('all good')).toBeInTheDocument();
});

it('shows a recovery message instead of unmounting the tree on a render error', () => {
  render(
    <ErrorBoundary>
      <Boom />
    </ErrorBoundary>
  );

  // Without the boundary React 16 unmounts everything, leaving a blank page.
  expect(screen.getByRole('heading', { name: 'Something went wrong' })).toBeInTheDocument();
  expect(screen.getByText(/reload the page/i)).toBeInTheDocument();
});

it('logs the underlying error so the cause is not lost', () => {
  render(
    <ErrorBoundary>
      <Boom />
    </ErrorBoundary>
  );

  const logged = consoleError.mock.calls.map(args => String(args[0]));
  expect(logged.some(message => message.includes('Unhandled error in component tree'))).toBe(true);
});

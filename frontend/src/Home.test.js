import React from 'react';
import { screen } from '@testing-library/react';
import Home from './Home';
import NotFound from './NotFound';
import { renderWithRouter } from './test-utils';

describe('Home', () => {
  it('offers a link into the game', () => {
    renderWithRouter(<Home />);

    expect(screen.getByRole('link', { name: 'Play A Game' })).toHaveAttribute('href', '/guesses');
  });

  it('renders the navbar', () => {
    renderWithRouter(<Home />);

    expect(screen.getByRole('link', { name: 'Home' })).toBeInTheDocument();
  });
});

describe('NotFound', () => {
  it('explains the page is missing and links home', () => {
    renderWithRouter(<NotFound />);

    expect(screen.getByRole('heading', { name: 'Page not found' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Go back home' })).toHaveAttribute('href', '/');
  });
});

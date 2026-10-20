import { screen } from '@testing-library/react';
import Home from './Home';
import NotFound from './NotFound';
import { renderWithRouter } from './test-utils';

describe('Home', () => {
  it('offers a link that starts a new game', () => {
    renderWithRouter(<Home />);

    expect(screen.getByRole('link', { name: 'Play A Game' })).toHaveAttribute('href', '/games/new');
  });

  it('renders that link as a single control, not a link nested in a button', () => {
    // <a> inside <button> is invalid HTML and puts two stops in the tab order.
    renderWithRouter(<Home />);

    const link = screen.getByRole('link', { name: 'Play A Game' });
    expect(link.closest('button')).toBeNull();
    expect(screen.queryByRole('button', { name: 'Play A Game' })).not.toBeInTheDocument();
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

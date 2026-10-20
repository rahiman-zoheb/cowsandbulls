import { screen, waitFor, fireEvent } from '@testing-library/react';
import AppNavbar from './AppNavbar';
import { renderWithRouter } from './test-utils';

it('links back to the home route', () => {
  renderWithRouter(<AppNavbar />);

  expect(screen.getByRole('link', { name: 'Home' })).toHaveAttribute('href', '/');
});

it('renders the external reference links', () => {
  renderWithRouter(<AppNavbar />);

  expect(screen.getByRole('link', { name: 'Zoheb Rahiman' }))
    .toHaveAttribute('href', 'https://zohebrahiman.com');
  expect(screen.getByRole('link', { name: 'Cows And Bulls' }))
    .toHaveAttribute('href', 'https://en.wikipedia.org/wiki/Bulls_and_Cows');
});

it('expands the collapsed menu when the toggler is clicked', async () => {
  const { container } = renderWithRouter(<AppNavbar />);
  const collapse = () => container.querySelector('.navbar-collapse');

  expect(collapse()).not.toHaveClass('show');

  fireEvent.click(screen.getByRole('button', { name: /toggle navigation/i }));

  await waitFor(() => expect(collapse()).toHaveClass('show'));
});

it('tells assistive technology whether the menu is open, and which element it controls', () => {
  renderWithRouter(<AppNavbar />);
  const toggler = screen.getByRole('button', { name: /toggle navigation/i });

  expect(toggler).toHaveAttribute('aria-expanded', 'false');
  expect(document.getElementById(toggler.getAttribute('aria-controls'))).toHaveClass('navbar-collapse');

  fireEvent.click(toggler);
  expect(toggler).toHaveAttribute('aria-expanded', 'true');
});

it('collapses again on a second click', async () => {
  const { container } = renderWithRouter(<AppNavbar />);
  const collapse = () => container.querySelector('.navbar-collapse');
  const toggler = screen.getByRole('button', { name: /toggle navigation/i });

  fireEvent.click(toggler);
  await waitFor(() => expect(collapse()).toHaveClass('show'));

  fireEvent.click(toggler);
  await waitFor(() => expect(collapse()).not.toHaveClass('show'));
});

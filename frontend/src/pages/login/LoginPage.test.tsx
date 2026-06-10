import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeEach } from 'vitest';
import { LoginPage } from './LoginPage';
import { renderWithProviders } from '@/test/helpers';

describe('LoginPage', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it('renders sign in form', () => {
    renderWithProviders(<LoginPage />, { route: '/login' });
    expect(screen.getByRole('heading', { name: 'Sign In' })).toBeInTheDocument();
    expect(screen.getByLabelText('User ID')).toBeInTheDocument();
    expect(screen.getByLabelText('Password')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Sign In' })).toBeInTheDocument();
  });

  it('renders branding text', () => {
    renderWithProviders(<LoginPage />, { route: '/login' });
    expect(screen.getByText('Investment Portfolio Manager')).toBeInTheDocument();
    expect(screen.getByText('Enterprise Financial Management System')).toBeInTheDocument();
  });

  it('shows error when fields are empty', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoginPage />, { route: '/login' });
    await user.click(screen.getByRole('button', { name: 'Sign In' }));
    expect(screen.getByText('Please enter both User ID and Password.')).toBeInTheDocument();
  });

  it('shows error when only userId is provided', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoginPage />, { route: '/login' });
    await user.type(screen.getByLabelText('User ID'), 'admin');
    await user.click(screen.getByRole('button', { name: 'Sign In' }));
    expect(screen.getByText('Please enter both User ID and Password.')).toBeInTheDocument();
  });

  it('logs in successfully with credentials', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoginPage />, { route: '/login' });
    await user.type(screen.getByLabelText('User ID'), 'admin');
    await user.type(screen.getByLabelText('Password'), 'password');
    await user.click(screen.getByRole('button', { name: 'Sign In' }));
    // On success, user is redirected and the form is no longer visible
    expect(screen.queryByRole('heading', { name: 'Sign In' })).toBeNull();
  });

  it('renders legacy system note', () => {
    renderWithProviders(<LoginPage />, { route: '/login' });
    expect(screen.getByText('Modernized from COBOL Legacy System v5.0')).toBeInTheDocument();
  });
});

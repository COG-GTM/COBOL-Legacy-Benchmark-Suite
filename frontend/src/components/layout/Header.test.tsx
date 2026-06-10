import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeEach } from 'vitest';
import { Header } from './Header';
import { AuthProvider } from '@/context/AuthContext';
import { MemoryRouter } from 'react-router-dom';

function renderHeader() {
  return render(
    <AuthProvider>
      <MemoryRouter>
        <Header />
      </MemoryRouter>
    </AuthProvider>,
  );
}

describe('Header', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it('renders the app title', () => {
    renderHeader();
    expect(screen.getByText('Investment Portfolio Manager')).toBeInTheDocument();
  });

  it('renders the IP logo', () => {
    renderHeader();
    expect(screen.getByText('IP')).toBeInTheDocument();
  });

  it('renders user ID (default ADMIN01)', () => {
    renderHeader();
    expect(screen.getByText('ADMIN01')).toBeInTheDocument();
  });

  it('renders user ID from session', () => {
    sessionStorage.setItem('auth', JSON.stringify({ isAuthenticated: true, userId: 'TESTUSER' }));
    renderHeader();
    expect(screen.getByText('TESTUSER')).toBeInTheDocument();
  });

  it('renders logout button', () => {
    renderHeader();
    expect(screen.getByTitle('Logout')).toBeInTheDocument();
    expect(screen.getByText('Logout')).toBeInTheDocument();
  });

  it('clicking logout calls logout', async () => {
    sessionStorage.setItem('auth', JSON.stringify({ isAuthenticated: true, userId: 'BOB' }));
    const user = userEvent.setup();
    renderHeader();
    expect(screen.getByText('BOB')).toBeInTheDocument();
    await user.click(screen.getByTitle('Logout'));
    expect(screen.getByText('ADMIN01')).toBeInTheDocument();
  });
});

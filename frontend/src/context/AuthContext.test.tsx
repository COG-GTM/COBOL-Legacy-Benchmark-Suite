import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AuthProvider, useAuth } from './AuthContext';

function TestConsumer() {
  const { isAuthenticated, userId, login, logout } = useAuth();
  return (
    <div>
      <span data-testid="auth">{String(isAuthenticated)}</span>
      <span data-testid="user">{userId ?? 'none'}</span>
      <button onClick={() => login('admin', 'pass')}>login</button>
      <button onClick={() => login('', '')}>login-empty</button>
      <button onClick={logout}>logout</button>
    </div>
  );
}

describe('AuthContext', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it('throws when useAuth is used outside provider', () => {
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
    expect(() => render(<TestConsumer />)).toThrow('useAuth must be used within an AuthProvider');
    spy.mockRestore();
  });

  it('starts unauthenticated', () => {
    render(<AuthProvider><TestConsumer /></AuthProvider>);
    expect(screen.getByTestId('auth')).toHaveTextContent('false');
    expect(screen.getByTestId('user')).toHaveTextContent('none');
  });

  it('login with valid credentials sets authenticated', async () => {
    const user = userEvent.setup();
    render(<AuthProvider><TestConsumer /></AuthProvider>);
    await user.click(screen.getByText('login'));
    expect(screen.getByTestId('auth')).toHaveTextContent('true');
    expect(screen.getByTestId('user')).toHaveTextContent('ADMIN');
  });

  it('login with empty credentials returns false', async () => {
    const user = userEvent.setup();
    render(<AuthProvider><TestConsumer /></AuthProvider>);
    await user.click(screen.getByText('login-empty'));
    expect(screen.getByTestId('auth')).toHaveTextContent('false');
  });

  it('logout clears auth state', async () => {
    const user = userEvent.setup();
    render(<AuthProvider><TestConsumer /></AuthProvider>);
    await user.click(screen.getByText('login'));
    expect(screen.getByTestId('auth')).toHaveTextContent('true');
    await user.click(screen.getByText('logout'));
    expect(screen.getByTestId('auth')).toHaveTextContent('false');
    expect(screen.getByTestId('user')).toHaveTextContent('none');
  });

  it('restores auth from sessionStorage', () => {
    sessionStorage.setItem('auth', JSON.stringify({ isAuthenticated: true, userId: 'BOB' }));
    render(<AuthProvider><TestConsumer /></AuthProvider>);
    expect(screen.getByTestId('auth')).toHaveTextContent('true');
    expect(screen.getByTestId('user')).toHaveTextContent('BOB');
  });

  it('handles invalid sessionStorage data gracefully', () => {
    sessionStorage.setItem('auth', 'not json');
    render(<AuthProvider><TestConsumer /></AuthProvider>);
    expect(screen.getByTestId('auth')).toHaveTextContent('false');
  });
});

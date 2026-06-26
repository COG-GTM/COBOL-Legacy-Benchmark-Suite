import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { App } from './App';
import { AuthProvider } from './context/AuthProvider';
import {
  __resetAuditLog,
  getAuditRecords,
} from './services/auditService';

function renderApp(initialPath = '/dashboard') {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <AuthProvider>
        <App />
      </AuthProvider>
    </MemoryRouter>,
  );
}

async function signIn(userId: string, password: string) {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText('User ID'), userId);
  await user.type(screen.getByLabelText('Password'), password);
  await user.click(screen.getByRole('button', { name: /sign in/i }));
}

beforeEach(() => {
  sessionStorage.clear();
  __resetAuditLog();
});

afterEach(() => {
  cleanup();
  sessionStorage.clear();
  __resetAuditLog();
});

describe('authentication & session management', () => {
  it('redirects unauthenticated users to the login screen', () => {
    renderApp('/dashboard');
    expect(screen.getByText('Sign in to continue')).toBeInTheDocument();
  });

  it('signs in a read-only user and gates admin navigation', async () => {
    renderApp('/dashboard');
    await signIn('READ0001', 'readonly123');

    expect(
      await screen.findByRole('heading', { name: 'Dashboard' }),
    ).toBeInTheDocument();
    expect(screen.getAllByText('Ravi Reader').length).toBeGreaterThan(0);
    expect(screen.getByText('Read-only')).toBeInTheDocument();
    expect(
      screen.queryByRole('link', { name: /administration/i }),
    ).not.toBeInTheDocument();
  });

  it('blocks read-only users from the admin route', async () => {
    renderApp('/admin');
    await signIn('READ0001', 'readonly123');
    expect(await screen.findByText('Access denied')).toBeInTheDocument();
  });

  it('lets an admin reach the admin audit view', async () => {
    renderApp('/admin');
    await signIn('ADMIN001', 'admin123');

    expect(
      await screen.findByRole('heading', { name: 'Administration' }),
    ).toBeInTheDocument();
    // The successful login itself produced an audit record.
    expect(
      screen.getByText('User authenticated'),
    ).toBeInTheDocument();
  });

  it('records a FAIL audit entry on an invalid login', async () => {
    renderApp('/dashboard');
    await signIn('ADMIN001', 'wrongpass');

    expect(
      await screen.findByText('Invalid user ID or password.'),
    ).toBeInTheDocument();

    const failures = getAuditRecords().filter((r) => r.status === 'FAIL');
    expect(failures).toHaveLength(1);
    expect(failures[0].action).toBe('LOGIN');
    expect(failures[0].userId).toBe('ADMIN001');
  });

  it('clears session state on logout', async () => {
    renderApp('/dashboard');
    await signIn('ADMIN001', 'admin123');
    expect(
      await screen.findByRole('heading', { name: 'Dashboard' }),
    ).toBeInTheDocument();

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /log out/i }));

    await waitFor(() =>
      expect(screen.getByText('Sign in to continue')).toBeInTheDocument(),
    );
    expect(sessionStorage.getItem('clbs.auth.user')).toBeNull();

    const logouts = getAuditRecords().filter((r) => r.action === 'LOGOUT');
    expect(logouts).toHaveLength(1);
    expect(logouts[0].status).toBe('SUCC');
  });
});

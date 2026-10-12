import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { setMockUser } from '../../mocks/handlers';
import { server } from '../../mocks/server';
import { currentPath, renderApp } from '../../test/renderApp';

describe('FR-001 authenticated session', () => {
  it('shows the signed-in user ID in the top bar', async () => {
    renderApp('/');
    expect(await screen.findByText('PINQUSR1')).toBeInTheDocument();
  });

  it('routes a 401 session to sign-in with the legacy message and makes no data calls', async () => {
    setMockUser('anon');
    let dataCalls = 0;
    server.events.on('request:start', ({ request }) => {
      if (request.url.includes('/accounts/')) dataCalls += 1;
    });
    renderApp('/positions/1000000001');
    expect(await screen.findByRole('heading', { name: 'Sign in required' })).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent('Unable to obtain user credentials');
    expect(currentPath()).toBe('/signin');
    expect(dataCalls).toBe(0);
    server.events.removeAllListeners();
  });
});

describe('FR-002 INQONLN:READ authorization', () => {
  it('shows Access denied when the session lacks the permission, before any data call', async () => {
    setMockUser('denied');
    renderApp('/history/1000000001');
    expect(await screen.findByRole('heading', { name: 'Access denied' })).toBeInTheDocument();
    expect(currentPath()).toBe('/access-denied');
  });

  it('routes a 403 from a data call to Access denied', async () => {
    renderApp('/positions/9999999995');
    expect(await screen.findByRole('heading', { name: 'Access denied' })).toBeInTheDocument();
  });

  it('FR-029: returns to the menu with "Authorization check failed" when the check itself fails (RETURN)', async () => {
    renderApp('/positions/9999999996');
    await waitFor(() => expect(currentPath()).toBe('/'));
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Authorization check failed'));
  });

  it('shows the System Error screen when the session endpoint fails', async () => {
    server.use(http.get('*/api/v1/session', () => HttpResponse.json({ code: 'SYSTEM_ERROR' }, { status: 500 })));
    renderApp('/');
    expect(await screen.findByRole('heading', { name: /System Error/ })).toBeInTheDocument();
  });
});

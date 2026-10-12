import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, useLocation } from 'react-router-dom';
import { AppRoutes } from '../App';
import { createHttpClient } from '../api/client';
import { SessionProvider } from '../state/SessionContext';

let currentLocation = '';
function LocationProbe() {
  const location = useLocation();
  currentLocation = `${location.pathname}${location.search}`;
  return null;
}

export function currentPath() {
  return currentLocation;
}

export function testApi() {
  return createHttpClient({ baseUrl: `${window.location.origin}/api/v1` });
}

export function renderApp(initialPath = '/') {
  const user = userEvent.setup();
  const utils = render(
    <MemoryRouter initialEntries={[initialPath]}>
      <SessionProvider api={testApi()}>
        <LocationProbe />
        <AppRoutes />
      </SessionProvider>
    </MemoryRouter>,
  );
  return { user, ...utils };
}

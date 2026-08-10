import { render, type RenderOptions } from '@testing-library/react';
import { type ReactElement } from 'react';
import { MemoryRouter } from 'react-router-dom';
import { ServicesProvider } from '../services/serviceContext';
import type { HistoryService } from '../services/historyService';

interface Options extends Omit<RenderOptions, 'wrapper'> {
  route?: string;
  historyService?: HistoryService;
}

/** Renders a component wrapped in the router + services providers. */
export function renderWithProviders(
  ui: ReactElement,
  { route = '/', historyService, ...options }: Options = {},
) {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <ServicesProvider
        services={historyService ? { history: historyService } : undefined}
      >
        {ui}
      </ServicesProvider>
    </MemoryRouter>,
    options,
  );
}

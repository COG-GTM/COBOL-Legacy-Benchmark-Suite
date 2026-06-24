import { render, type RenderOptions } from '@testing-library/react';
import { type ReactElement } from 'react';
import { MemoryRouter } from 'react-router-dom';
import { ServicesProvider } from '../services/serviceContext';
import type { PortfolioService } from '../services/portfolioService';

interface Options extends Omit<RenderOptions, 'wrapper'> {
  route?: string;
  portfolioService?: PortfolioService;
}

/** Renders a component wrapped in the router + services providers. */
export function renderWithProviders(
  ui: ReactElement,
  { route = '/', portfolioService, ...options }: Options = {},
) {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <ServicesProvider
        services={portfolioService ? { portfolios: portfolioService } : undefined}
      >
        {ui}
      </ServicesProvider>
    </MemoryRouter>,
    options,
  );
}

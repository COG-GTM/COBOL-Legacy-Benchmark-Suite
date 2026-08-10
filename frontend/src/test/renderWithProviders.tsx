import { render, type RenderOptions } from '@testing-library/react';
import { type ReactElement } from 'react';
import { MemoryRouter } from 'react-router-dom';
import { ServicesProvider } from '../services/serviceContext';
import type { PortfolioService } from '../services/portfolioService';
import type { PositionService } from '../services/positionService';
import type { TransactionService } from '../services/transactionService';
import type { Services } from '../services/servicesContext';

interface Options extends Omit<RenderOptions, 'wrapper'> {
  route?: string;
  portfolioService?: PortfolioService;
  positionService?: PositionService;
  transactionService?: TransactionService;
}

/** Renders a component wrapped in the router + services providers. */
export function renderWithProviders(
  ui: ReactElement,
  {
    route = '/',
    portfolioService,
    positionService,
    transactionService,
    ...options
  }: Options = {},
) {
  const services: Partial<Services> = {};
  if (portfolioService) services.portfolios = portfolioService;
  if (positionService) services.positions = positionService;
  if (transactionService) services.transactions = transactionService;

  return render(
    <MemoryRouter initialEntries={[route]}>
      <ServicesProvider
        services={Object.keys(services).length ? services : undefined}
      >
        {ui}
      </ServicesProvider>
    </MemoryRouter>,
    options,
  );
}

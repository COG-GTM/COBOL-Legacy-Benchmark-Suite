import { createContext, useContext } from 'react';
import type { PortfolioService } from './portfolioService';

export interface Services {
  portfolios: PortfolioService;
}

export const ServicesContext = createContext<Services | null>(null);

export function usePortfolioService(): PortfolioService {
  const ctx = useContext(ServicesContext);
  if (!ctx) {
    throw new Error(
      'usePortfolioService must be used within a ServicesProvider',
    );
  }
  return ctx.portfolios;
}

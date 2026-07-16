import { createContext, useContext } from 'react';
import type { PortfolioService } from './portfolioService';
import type { PositionService } from './positionService';

export interface Services {
  portfolios: PortfolioService;
  positions: PositionService;
}

export const ServicesContext = createContext<Services | null>(null);

function useServices(): Services {
  const ctx = useContext(ServicesContext);
  if (!ctx) {
    throw new Error('Service hooks must be used within a ServicesProvider');
  }
  return ctx;
}

export function usePortfolioService(): PortfolioService {
  return useServices().portfolios;
}

export function usePositionService(): PositionService {
  return useServices().positions;
}

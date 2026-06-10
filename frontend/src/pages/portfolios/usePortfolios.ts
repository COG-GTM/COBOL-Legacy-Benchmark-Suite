import { createContext, useContext } from 'react';
import type { PortfolioMaster } from './portfolioData';

export interface PortfolioContextValue {
  portfolios: PortfolioMaster[];
  loading: boolean;
  getPortfolio: (id: string) => PortfolioMaster | undefined;
  addPortfolio: (portfolio: PortfolioMaster) => void;
  updatePortfolio: (id: string, updates: Partial<PortfolioMaster>) => void;
  deletePortfolio: (id: string) => void;
}

export const PortfolioContext = createContext<PortfolioContextValue | undefined>(undefined);

export function usePortfolios(): PortfolioContextValue {
  const context = useContext(PortfolioContext);
  if (!context) {
    throw new Error('usePortfolios must be used within a PortfolioProvider');
  }
  return context;
}

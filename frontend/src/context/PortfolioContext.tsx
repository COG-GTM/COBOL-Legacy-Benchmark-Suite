import { createContext, useContext, useState, useCallback } from 'react';
import type { ReactNode } from 'react';
import type { Portfolio } from '@/data/types';
import { portfolios as initialPortfolios } from '@/data/mockData';

interface PortfolioContextType {
  portfolios: Portfolio[];
  addPortfolio: (portfolio: Portfolio) => void;
  updatePortfolio: (id: string, updates: Partial<Portfolio>) => void;
  deletePortfolio: (id: string) => void;
  getPortfolio: (id: string) => Portfolio | undefined;
}

const PortfolioContext = createContext<PortfolioContextType | null>(null);

export function PortfolioProvider({ children }: { children: ReactNode }) {
  const [portfolios, setPortfolios] = useState<Portfolio[]>(initialPortfolios);

  const addPortfolio = useCallback((portfolio: Portfolio) => {
    setPortfolios((prev) => [...prev, portfolio]);
  }, []);

  const updatePortfolio = useCallback((id: string, updates: Partial<Portfolio>) => {
    setPortfolios((prev) =>
      prev.map((p) => (p.id === id ? { ...p, ...updates } : p))
    );
  }, []);

  const deletePortfolio = useCallback((id: string) => {
    setPortfolios((prev) => prev.filter((p) => p.id !== id));
  }, []);

  const getPortfolio = useCallback(
    (id: string) => portfolios.find((p) => p.id === id),
    [portfolios]
  );

  return (
    <PortfolioContext.Provider
      value={{ portfolios, addPortfolio, updatePortfolio, deletePortfolio, getPortfolio }}
    >
      {children}
    </PortfolioContext.Provider>
  );
}

export function usePortfolios(): PortfolioContextType {
  const context = useContext(PortfolioContext);
  if (!context) {
    throw new Error('usePortfolios must be used within a PortfolioProvider');
  }
  return context;
}

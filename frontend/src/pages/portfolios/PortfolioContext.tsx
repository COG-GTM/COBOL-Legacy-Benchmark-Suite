import { useCallback, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { Outlet } from 'react-router-dom';
import { seedPortfolios } from './portfolioData';
import type { PortfolioMaster } from './portfolioData';
import { PortfolioContext } from './usePortfolios';

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

export function PortfolioProvider({ children }: { children: ReactNode }) {
  const [portfolios, setPortfolios] = useState<PortfolioMaster[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => {
      setPortfolios(seedPortfolios());
      setLoading(false);
    }, 400);
    return () => clearTimeout(timer);
  }, []);

  const getPortfolio = useCallback(
    (id: string) => portfolios.find((p) => p.id === id),
    [portfolios]
  );

  const addPortfolio = useCallback((portfolio: PortfolioMaster) => {
    setPortfolios((prev) => [...prev, { ...portfolio, lastMaint: today() }]);
  }, []);

  const updatePortfolio = useCallback((id: string, updates: Partial<PortfolioMaster>) => {
    setPortfolios((prev) =>
      prev.map((p) => (p.id === id ? { ...p, ...updates, lastMaint: today() } : p))
    );
  }, []);

  const deletePortfolio = useCallback((id: string) => {
    setPortfolios((prev) => prev.filter((p) => p.id !== id));
  }, []);

  return (
    <PortfolioContext.Provider
      value={{ portfolios, loading, getPortfolio, addPortfolio, updatePortfolio, deletePortfolio }}
    >
      {children}
    </PortfolioContext.Provider>
  );
}

export function PortfolioLayout() {
  return (
    <PortfolioProvider>
      <Outlet />
    </PortfolioProvider>
  );
}

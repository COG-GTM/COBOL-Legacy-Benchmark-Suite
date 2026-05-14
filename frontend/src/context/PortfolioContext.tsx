import { createContext, useContext, useState, useCallback, type ReactNode } from 'react';
import type { Portfolio, Position, Transaction } from '../types';
import portfoliosData from '../mocks/portfolios.json';
import positionsData from '../mocks/positions.json';
import transactionsData from '../mocks/transactions.json';

interface PortfolioContextType {
  portfolios: Portfolio[];
  positions: Position[];
  transactions: Transaction[];
  addPortfolio: (p: Portfolio) => void;
  updatePortfolio: (id: string, p: Partial<Portfolio>) => void;
  deletePortfolio: (id: string) => void;
  addTransaction: (t: Transaction) => void;
}

const PortfolioContext = createContext<PortfolioContextType | null>(null);

export function PortfolioProvider({ children }: { children: ReactNode }) {
  const [portfolios, setPortfolios] = useState<Portfolio[]>(portfoliosData as Portfolio[]);
  const [positions] = useState<Position[]>(positionsData as Position[]);
  const [transactions, setTransactions] = useState<Transaction[]>(transactionsData as Transaction[]);

  const addPortfolio = useCallback((p: Portfolio) => {
    setPortfolios((prev) => [...prev, p]);
  }, []);

  const updatePortfolio = useCallback((id: string, updates: Partial<Portfolio>) => {
    setPortfolios((prev) =>
      prev.map((p) => (p.id === id ? { ...p, ...updates } : p)),
    );
  }, []);

  const deletePortfolio = useCallback((id: string) => {
    setPortfolios((prev) => prev.filter((p) => p.id !== id));
  }, []);

  const addTransaction = useCallback((t: Transaction) => {
    setTransactions((prev) => [t, ...prev]);
  }, []);

  return (
    <PortfolioContext.Provider
      value={{ portfolios, positions, transactions, addPortfolio, updatePortfolio, deletePortfolio, addTransaction }}
    >
      {children}
    </PortfolioContext.Provider>
  );
}

export function usePortfolio(): PortfolioContextType {
  const ctx = useContext(PortfolioContext);
  if (!ctx) throw new Error('usePortfolio must be used within PortfolioProvider');
  return ctx;
}

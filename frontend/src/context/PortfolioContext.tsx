import { createContext, useContext, useState, useCallback } from 'react';
import type { ReactNode } from 'react';
import type { Portfolio, Position, Transaction } from '@/data/types';
import {
  portfolios as initialPortfolios,
  positions as initialPositions,
  transactions as initialTransactions,
} from '@/data/mockData';

interface PortfolioContextValue {
  portfolios: Portfolio[];
  positions: Position[];
  transactions: Transaction[];
  getPortfolio: (id: string) => Portfolio | undefined;
  addPortfolio: (portfolio: Portfolio) => void;
  updatePortfolio: (id: string, updates: Partial<Omit<Portfolio, 'id'>>) => void;
  deletePortfolio: (id: string) => void;
  getPositionsForAccount: (accountNo: string) => Position[];
  getTransactionsForAccount: (accountNo: string) => Transaction[];
  notification: Notification | null;
  showNotification: (message: string, type: 'success' | 'error') => void;
  clearNotification: () => void;
}

interface Notification {
  message: string;
  type: 'success' | 'error';
}

const PortfolioContext = createContext<PortfolioContextValue | null>(null);

/**
 * Maps portfolio IDs to account numbers for linking portfolios to positions/transactions.
 * In a real system this would come from a database join.
 */
const portfolioAccountMap: Record<string, string> = {
  PORT0001: '100000001',
  PORT0002: '100000002',
  PORT0003: '100000003',
  PORT0004: '100000004',
  PORT0005: '100000005',
  PORT0006: '100000006',
  PORT0007: '100000007',
  PORT0008: '100000008',
  PORT0009: '100000009',
  PORT0010: '100000010',
  PORT0011: '100000011',
  PORT0012: '100000012',
};

export function getAccountForPortfolio(portfolioId: string): string | undefined {
  return portfolioAccountMap[portfolioId];
}

export function PortfolioProvider({ children }: { children: ReactNode }) {
  const [portfolios, setPortfolios] = useState<Portfolio[]>([...initialPortfolios]);
  const [positions] = useState<Position[]>([...initialPositions]);
  const [transactions] = useState<Transaction[]>([...initialTransactions]);
  const [notification, setNotification] = useState<Notification | null>(null);

  const getPortfolio = useCallback(
    (id: string) => portfolios.find((p) => p.id === id),
    [portfolios],
  );

  const addPortfolio = useCallback((portfolio: Portfolio) => {
    setPortfolios((prev) => [...prev, portfolio]);
  }, []);

  const updatePortfolio = useCallback(
    (id: string, updates: Partial<Omit<Portfolio, 'id'>>) => {
      setPortfolios((prev) =>
        prev.map((p) => (p.id === id ? { ...p, ...updates } : p)),
      );
    },
    [],
  );

  const deletePortfolio = useCallback((id: string) => {
    setPortfolios((prev) => prev.filter((p) => p.id !== id));
  }, []);

  const getPositionsForAccount = useCallback(
    (accountNo: string) => positions.filter((p) => p.accountNo === accountNo),
    [positions],
  );

  const getTransactionsForAccount = useCallback(
    (accountNo: string) => transactions.filter((t) => t.accountNo === accountNo),
    [transactions],
  );

  const showNotification = useCallback((message: string, type: 'success' | 'error') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 4000);
  }, []);

  const clearNotification = useCallback(() => {
    setNotification(null);
  }, []);

  return (
    <PortfolioContext.Provider
      value={{
        portfolios,
        positions,
        transactions,
        getPortfolio,
        addPortfolio,
        updatePortfolio,
        deletePortfolio,
        getPositionsForAccount,
        getTransactionsForAccount,
        notification,
        showNotification,
        clearNotification,
      }}
    >
      {children}
    </PortfolioContext.Provider>
  );
}

export function usePortfolioContext(): PortfolioContextValue {
  const context = useContext(PortfolioContext);
  if (!context) {
    throw new Error('usePortfolioContext must be used within a PortfolioProvider');
  }
  return context;
}

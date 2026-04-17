import { createContext, useContext, useState, useCallback } from 'react';
import type { ReactNode } from 'react';
import type { Transaction, Position, AuditEntry } from '@/data/types';
import {
  portfolios as initialPortfolios,
  positions as initialPositions,
  transactions as initialTransactions,
  auditEntries as initialAuditEntries,
} from '@/data/mockData';

interface TransactionState {
  transactions: Transaction[];
  positions: Position[];
  auditEntries: AuditEntry[];
}

interface SubmitTransactionParams {
  accountNo: string;
  fundId: string;
  transType: 'BY' | 'SL' | 'FE';
  transDate: string;
  shareQty: number;
  price: number;
  amount: number;
}

interface SubmitTransactionResult {
  transId: string;
  beforeBalance: number;
  afterBalance: number;
}

interface TransactionContextType extends TransactionState {
  submitTransaction: (params: SubmitTransactionParams) => SubmitTransactionResult;
  getPositionBalance: (accountNo: string, fundId: string) => number;
  getPortfolioName: (portfolioId: string) => string;
  getAccountNoForPortfolio: (portfolioId: string) => string | null;
}

const TransactionContext = createContext<TransactionContextType | null>(null);

function generateTransactionId(): string {
  const now = new Date();
  const dateStr =
    now.getFullYear().toString() +
    (now.getMonth() + 1).toString().padStart(2, '0') +
    now.getDate().toString().padStart(2, '0');
  const random = Math.floor(1000 + Math.random() * 9000).toString();
  return dateStr + random;
}

function buildAccountMap(): Map<string, string> {
  const map = new Map<string, string>();
  const portfolioIds = initialPortfolios.map((p) => p.id);
  const accountNos = [...new Set(initialPositions.map((p) => p.accountNo))].sort();

  for (let i = 0; i < portfolioIds.length && i < accountNos.length; i++) {
    map.set(portfolioIds[i], accountNos[i]);
  }
  return map;
}

const portfolioAccountMap = buildAccountMap();

export function TransactionProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<TransactionState>({
    transactions: [...initialTransactions],
    positions: initialPositions.map((p) => ({ ...p })),
    auditEntries: [...initialAuditEntries],
  });

  const getPositionBalance = useCallback(
    (accountNo: string, fundId: string): number => {
      const position = state.positions.find(
        (p) => p.accountNo === accountNo && p.fundId === fundId && p.status === 'A'
      );
      return position ? position.shareBalance : 0;
    },
    [state.positions]
  );

  const getPortfolioName = useCallback((portfolioId: string): string => {
    const portfolio = initialPortfolios.find((p) => p.id === portfolioId);
    return portfolio ? portfolio.name : portfolioId;
  }, []);

  const getAccountNoForPortfolio = useCallback((portfolioId: string): string | null => {
    return portfolioAccountMap.get(portfolioId) ?? null;
  }, []);

  const submitTransaction = useCallback(
    (params: SubmitTransactionParams): SubmitTransactionResult => {
      const transId = generateTransactionId();
      const beforeBalance = state.positions.find(
        (p) => p.accountNo === params.accountNo && p.fundId === params.fundId && p.status === 'A'
      )?.shareBalance ?? 0;

      let afterBalance = beforeBalance;

      if (params.transType === 'BY') {
        afterBalance = beforeBalance + params.shareQty;
      } else if (params.transType === 'SL') {
        afterBalance = beforeBalance - params.shareQty;
      }
      // Fee: no share balance change

      const newTransaction: Transaction = {
        transId,
        accountNo: params.accountNo,
        fundId: params.fundId,
        transType: params.transType,
        transDate: params.transDate,
        shareQty: params.shareQty,
        price: params.price,
        amount: params.amount,
        status: 'P',
        beforeBalance,
        afterBalance,
      };

      const auditAction = params.transType === 'BY' ? 'CREATE' : params.transType === 'SL' ? 'DELETE' : 'UPDATE';
      const newAudit: AuditEntry = {
        timestamp: new Date().toISOString().replace('T', ' ').substring(0, 19),
        program: 'PORTTRAN',
        type: 'TRANSACTION',
        action: auditAction,
        status: 'SUCC',
        portfolioId: '',
        accountNo: params.accountNo,
        message: `Transaction: ${params.transType} Amount: ${params.amount.toFixed(2)} Units: ${params.shareQty.toFixed(3)}`,
      };

      setState((prev) => {
        const updatedPositions = prev.positions.map((p) => {
          if (p.accountNo === params.accountNo && p.fundId === params.fundId && p.status === 'A') {
            const updated = { ...p, lastDate: params.transDate, lastTrans: params.transType };
            if (params.transType === 'BY') {
              updated.shareBalance = p.shareBalance + params.shareQty;
              updated.costBasis = p.costBasis + params.amount;
            } else if (params.transType === 'SL') {
              updated.shareBalance = p.shareBalance - params.shareQty;
              updated.costBasis = p.costBasis - params.amount;
            } else if (params.transType === 'FE') {
              updated.costBasis = p.costBasis - params.amount;
            }
            return updated;
          }
          return p;
        });

        return {
          transactions: [newTransaction, ...prev.transactions],
          positions: updatedPositions,
          auditEntries: [newAudit, ...prev.auditEntries],
        };
      });

      return { transId, beforeBalance, afterBalance };
    },
    [state.positions]
  );

  return (
    <TransactionContext.Provider
      value={{
        ...state,
        submitTransaction,
        getPositionBalance,
        getPortfolioName,
        getAccountNoForPortfolio,
      }}
    >
      {children}
    </TransactionContext.Provider>
  );
}

export function useTransactions(): TransactionContextType {
  const context = useContext(TransactionContext);
  if (!context) {
    throw new Error('useTransactions must be used within a TransactionProvider');
  }
  return context;
}

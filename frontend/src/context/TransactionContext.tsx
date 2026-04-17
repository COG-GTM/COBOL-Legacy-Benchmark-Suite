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
  // Explicit mapping: each portfolio ID maps to a known account number.
  // PORT0012 gets its own account (100000012) since mock positions don't
  // yet include an entry for it, but we still need it selectable.
  const explicit: Record<string, string> = {
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
  return new Map(Object.entries(explicit));
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

      // Computed inside setState updater to avoid stale closure reads
      let computedBefore = 0;
      let computedAfter = 0;

      setState((prev) => {
        const existingPosition = prev.positions.find(
          (p) => p.accountNo === params.accountNo && p.fundId === params.fundId && p.status === 'A'
        );
        computedBefore = existingPosition?.shareBalance ?? 0;
        computedAfter = computedBefore;

        if (params.transType === 'BY') {
          computedAfter = computedBefore + params.shareQty;
        } else if (params.transType === 'SL') {
          computedAfter = computedBefore - params.shareQty;
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
          beforeBalance: computedBefore,
          afterBalance: computedAfter,
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

        let positionUpdated = false;
        const updatedPositions = prev.positions.map((p) => {
          if (p.accountNo === params.accountNo && p.fundId === params.fundId && p.status === 'A') {
            positionUpdated = true;
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

        // Create new position record on first Buy (mirrors COBOL PORTTRAN behavior)
        if (!positionUpdated && params.transType === 'BY') {
          const newPosition: Position = {
            accountNo: params.accountNo,
            fundId: params.fundId,
            cusip: '000000000',
            shareBalance: params.shareQty,
            avgCost: params.price,
            costBasis: params.amount,
            lastDate: params.transDate,
            lastTrans: 'BY',
            status: 'A',
          };
          updatedPositions.push(newPosition);
        }

        return {
          transactions: [newTransaction, ...prev.transactions],
          positions: updatedPositions,
          auditEntries: [newAudit, ...prev.auditEntries],
        };
      });

      return { transId, beforeBalance: computedBefore, afterBalance: computedAfter };
    },
    []
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

import { useSyncExternalStore } from 'react';
import {
  portfolios,
  positions as seedPositions,
  transactions as seedTransactions,
  auditEntries as seedAuditEntries,
} from '@/data/mockData';
import type { AuditEntry, Position, Transaction } from '@/data/types';

export interface TransactionState {
  transactions: Transaction[];
  positions: Position[];
  auditEntries: AuditEntry[];
}

// Portfolio PORT00NN maps to account 1000000NN in the mock data layer
export function accountNoForPortfolio(portfolioId: string): string {
  const n = Number.parseInt(portfolioId.slice(4), 10);
  return String(100000000 + n);
}

export function portfolioIdForAccount(accountNo: string): string {
  const n = Number.parseInt(accountNo, 10) - 100000000;
  return `PORT${String(n).padStart(4, '0')}`;
}

export function portfolioNameForAccount(accountNo: string): string {
  const id = portfolioIdForAccount(accountNo);
  return portfolios.find((p) => p.id === id)?.name ?? id;
}

let state: TransactionState = {
  transactions: [...seedTransactions],
  positions: [...seedPositions],
  auditEntries: [...seedAuditEntries],
};

let nextTransNo = seedTransactions.length + 1;

const listeners = new Set<() => void>();

function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

function getSnapshot(): TransactionState {
  return state;
}

// All mutations go through an updater function reading the latest state,
// mirroring React setState updater semantics to avoid stale closures.
function setState(updater: (prev: TransactionState) => TransactionState): void {
  state = updater(state);
  listeners.forEach((listener) => listener());
}

export function useTransactionStore(): TransactionState {
  return useSyncExternalStore(subscribe, getSnapshot);
}

export interface NewTransactionInput {
  portfolioId: string;
  fundId: string;
  transType: 'BU' | 'SL' | 'FE';
  shareQty: number;
  price: number;
  amount: number;
}

export interface ProcessResult {
  ok: boolean;
  transId: string;
  error?: string;
}

function nowTimestamp(): string {
  return new Date().toISOString().slice(0, 19).replace('T', ' ');
}

function todayDate(): string {
  return new Date().toISOString().slice(0, 10);
}

// 2300-UPDATE-AUDIT-TRAIL action codes per transaction type
function auditAction(transType: 'BU' | 'SL' | 'FE'): string {
  switch (transType) {
    case 'BU': return 'CREATE';
    case 'SL': return 'DELETE';
    case 'FE': return 'UPDATE';
  }
}

function makeAuditEntry(
  input: NewTransactionInput,
  status: 'SUCC' | 'FAIL',
  message: string,
): AuditEntry {
  return {
    timestamp: nowTimestamp(),
    program: 'PORTTRAN',
    type: 'TRAN',
    action: auditAction(input.transType),
    status,
    portfolioId: input.portfolioId,
    accountNo: accountNoForPortfolio(input.portfolioId),
    message,
  };
}

// Mirrors PORTTRAN 2200-UPDATE-POSITIONS: Buy adds units and cost basis,
// Sell validates sufficient units then subtracts, Fee subtracts cost basis
// only. Transfer (2230) is not implemented in the COBOL program.
export function processTransaction(input: NewTransactionInput): ProcessResult {
  const transId = `TXN${String(nextTransNo).padStart(6, '0')}`;
  nextTransNo += 1;

  const accountNo = accountNoForPortfolio(input.portfolioId);
  let result: ProcessResult = { ok: true, transId };

  setState((prev) => {
    const positionIdx = prev.positions.findIndex(
      (p) => p.accountNo === accountNo && p.fundId === input.fundId,
    );
    const existing = positionIdx >= 0 ? prev.positions[positionIdx] : null;
    const beforeBalance = existing?.shareBalance ?? 0;

    if (input.transType === 'SL' && beforeBalance < input.shareQty) {
      result = { ok: false, transId, error: 'Insufficient units for sale' };
      const failedTxn: Transaction = {
        transId,
        accountNo,
        fundId: input.fundId,
        transType: input.transType,
        transDate: todayDate(),
        shareQty: input.shareQty,
        price: input.price,
        amount: input.amount,
        status: 'F',
        beforeBalance,
        afterBalance: beforeBalance,
      };
      return {
        ...prev,
        transactions: [failedTxn, ...prev.transactions],
        auditEntries: [
          makeAuditEntry(input, 'FAIL', 'Insufficient units for sale'),
          ...prev.auditEntries,
        ],
      };
    }

    let positions = prev.positions;
    let afterBalance = beforeBalance;

    if (input.transType === 'BU') {
      afterBalance = beforeBalance + input.shareQty;
      const newCostBasis = (existing?.costBasis ?? 0) + input.amount;
      const updated: Position = {
        accountNo,
        fundId: input.fundId,
        cusip: existing?.cusip ?? '',
        shareBalance: afterBalance,
        avgCost: afterBalance > 0 ? newCostBasis / afterBalance : 0,
        costBasis: newCostBasis,
        lastDate: todayDate(),
        lastTrans: 'BU',
        status: 'A',
      };
      positions =
        positionIdx >= 0
          ? prev.positions.map((p, i) => (i === positionIdx ? updated : p))
          : [...prev.positions, updated];
    } else if (input.transType === 'SL' && existing) {
      afterBalance = beforeBalance - input.shareQty;
      const newCostBasis = existing.costBasis - input.amount;
      const updated: Position = {
        ...existing,
        shareBalance: afterBalance,
        avgCost: afterBalance > 0 ? newCostBasis / afterBalance : 0,
        costBasis: newCostBasis,
        lastDate: todayDate(),
        lastTrans: 'SL',
        status: afterBalance > 0 ? 'A' : 'C',
      };
      positions = prev.positions.map((p, i) => (i === positionIdx ? updated : p));
    } else if (input.transType === 'FE' && existing) {
      const updated: Position = {
        ...existing,
        costBasis: existing.costBasis - input.amount,
        lastDate: todayDate(),
        lastTrans: 'FE',
      };
      positions = prev.positions.map((p, i) => (i === positionIdx ? updated : p));
    }

    const newTxn: Transaction = {
      transId,
      accountNo,
      fundId: input.fundId,
      transType: input.transType,
      transDate: todayDate(),
      shareQty: input.shareQty,
      price: input.price,
      amount: input.amount,
      status: 'D',
      beforeBalance,
      afterBalance,
    };

    return {
      transactions: [newTxn, ...prev.transactions],
      positions,
      auditEntries: [
        makeAuditEntry(
          input,
          'SUCC',
          `Transaction: ${input.transType} Amount: ${input.amount.toFixed(2)} Units: ${input.shareQty.toFixed(3)}`,
        ),
        ...prev.auditEntries,
      ],
    };
  });

  return result;
}

/**
 * TypeScript types for the Investment Portfolio Management System.
 * Translated from COBOL copybooks: PORTFLIO.cpy, POSREC.cpy, TRNREC.cpy,
 * HISTREC.cpy, BCHCTL.cpy, CKPRST.cpy, COMMON.cpy, RTNCODE.cpy
 */

import { Decimal } from "decimal.js";

// --- Enums from COMMON.cpy ---

export const TransactionType = {
  BUY: "BU",
  SELL: "SL",
  TRANSFER: "TR",
  FEE: "FE",
} as const;
export type TransactionType =
  (typeof TransactionType)[keyof typeof TransactionType];

export const TransactionStatus = {
  PENDING: "P",
  DONE: "D",
  FAILED: "F",
  REVERSED: "R",
} as const;
export type TransactionStatus =
  (typeof TransactionStatus)[keyof typeof TransactionStatus];

export const PortfolioStatus = {
  ACTIVE: "A",
  CLOSED: "C",
  SUSPENDED: "S",
} as const;
export type PortfolioStatus =
  (typeof PortfolioStatus)[keyof typeof PortfolioStatus];

export const PositionStatus = {
  ACTIVE: "A",
  CLOSED: "C",
  PENDING: "P",
} as const;
export type PositionStatus =
  (typeof PositionStatus)[keyof typeof PositionStatus];

export const ClientType = {
  INDIVIDUAL: "I",
  CORPORATE: "C",
  TRUST: "T",
} as const;
export type ClientType = (typeof ClientType)[keyof typeof ClientType];

export const HistoryRecordType = {
  PORTFOLIO: "PT",
  POSITION: "PS",
  TRANSACTION: "TR",
} as const;
export type HistoryRecordType =
  (typeof HistoryRecordType)[keyof typeof HistoryRecordType];

export const HistoryActionCode = {
  ADD: "A",
  CHANGE: "C",
  DELETE: "D",
} as const;
export type HistoryActionCode =
  (typeof HistoryActionCode)[keyof typeof HistoryActionCode];

export const BatchJobStatus = {
  READY: "R",
  ACTIVE: "A",
  WAITING: "W",
  DONE: "D",
  ERROR: "E",
} as const;
export type BatchJobStatus =
  (typeof BatchJobStatus)[keyof typeof BatchJobStatus];

export const CheckpointStatus = {
  INITIAL: "I",
  ACTIVE: "A",
  COMPLETE: "C",
  FAILED: "F",
  RESTARTED: "R",
} as const;
export type CheckpointStatus =
  (typeof CheckpointStatus)[keyof typeof CheckpointStatus];

export const CheckpointPhase = {
  INIT: "00",
  READ: "10",
  PROCESS: "20",
  UPDATE: "30",
  TERMINATE: "40",
} as const;
export type CheckpointPhase =
  (typeof CheckpointPhase)[keyof typeof CheckpointPhase];

// --- Return codes from COMMON.cpy / RTNCODE.cpy ---

export const ReturnCode = {
  SUCCESS: 0,
  WARNING: 4,
  ERROR: 8,
  SEVERE: 12,
  CRITICAL: 16,
} as const;
export type ReturnCode = (typeof ReturnCode)[keyof typeof ReturnCode];

// --- Domain types ---

export interface TransactionInput {
  portfolioId: string;
  investmentId: string;
  type: TransactionType;
  quantity: Decimal;
  price: Decimal;
  amount: Decimal;
  currency?: string;
  targetPortfolioId?: string; // for transfers
}

export interface TransactionResult {
  transactionId: string;
  status: TransactionStatus;
  returnCode: ReturnCode;
  message: string;
  gainLoss?: Decimal;
}

export interface ValidationError {
  field: string;
  message: string;
  transactionIndex?: number;
}

export interface ValidationResult {
  valid: boolean;
  returnCode: ReturnCode;
  errors: ValidationError[];
  validCount: number;
  errorCount: number;
}

export interface PositionUpdate {
  portfolioId: string;
  investmentId: string;
  quantityDelta: Decimal;
  costBasisDelta: Decimal;
  marketValueDelta: Decimal;
  actionCode: HistoryActionCode;
}

export interface HistoryEntry {
  portfolioId: string;
  recordType: HistoryRecordType;
  actionCode: HistoryActionCode;
  beforeImage: Record<string, unknown> | null;
  afterImage: Record<string, unknown> | null;
  reasonCode?: string;
}

export interface BatchPipelineResult {
  jobId: string;
  status: BatchJobStatus;
  steps: BatchStepResult[];
  startTime: Date;
  endTime?: Date;
}

export interface BatchStepResult {
  stepName: string;
  programName: string;
  returnCode: number;
  recordsProcessed: number;
  errorsEncountered: number;
  startTime: Date;
  endTime?: Date;
}

export interface CheckpointState {
  programId: string;
  recordsRead: number;
  recordsProcessed: number;
  recordsError: number;
  lastKey: string | null;
  phase: CheckpointPhase;
}

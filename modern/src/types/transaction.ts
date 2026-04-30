/**
 * Transaction types and Zod schemas
 * Translated from: src/copybook/common/TRNREC.cpy
 *
 * Transaction types: BU=Buy, SL=Sell, TR=Transfer, FE=Fee
 * Status: P=Pending, D=Done, F=Failed, R=Reversed
 */

import { z } from "zod";

// ---------------------------------------------------------------------------
// Enums (from COBOL Level 88 conditionals)
// ---------------------------------------------------------------------------

export const TransactionType = {
  Buy: "BU",
  Sell: "SL",
  Transfer: "TR",
  Fee: "FE",
} as const;
export type TransactionType =
  (typeof TransactionType)[keyof typeof TransactionType];

export const TransactionStatus = {
  Pending: "P",
  Done: "D",
  Failed: "F",
  Reversed: "R",
} as const;
export type TransactionStatus =
  (typeof TransactionStatus)[keyof typeof TransactionStatus];

// ---------------------------------------------------------------------------
// Zod schemas
// ---------------------------------------------------------------------------

export const transactionTypeSchema = z.enum(["BU", "SL", "TR", "FE"]);

export const transactionStatusSchema = z.enum(["P", "D", "F", "R"]);

/**
 * Maps to TRN-KEY group
 * TRN-DATE (YYYYMMDD) + TRN-TIME (HHMMSS) + TRN-PORTFOLIO-ID + TRN-SEQUENCE-NO
 */
export const transactionKeySchema = z.object({
  date: z.string().length(8),
  time: z.string().length(6),
  portfolioId: z.string().length(8),
  sequenceNo: z.string().length(6),
});

/** Maps to TRN-DATA group */
export const transactionDataSchema = z.object({
  investmentId: z.string().length(10),
  type: transactionTypeSchema,
  quantity: z.number(),
  price: z.number(),
  amount: z.number(),
  currency: z.string().length(3),
  status: transactionStatusSchema,
});

/** Maps to TRN-AUDIT group */
export const transactionAuditSchema = z.object({
  processDate: z.string().max(26),
  processUser: z.string().max(8),
});

/** Full TRANSACTION-RECORD from TRNREC.cpy */
export const transactionRecordSchema = z.object({
  key: transactionKeySchema,
  data: transactionDataSchema,
  audit: transactionAuditSchema,
});

// ---------------------------------------------------------------------------
// TypeScript interfaces
// ---------------------------------------------------------------------------

export interface TransactionKey {
  date: string;
  time: string;
  portfolioId: string;
  sequenceNo: string;
}

export interface TransactionData {
  investmentId: string;
  type: TransactionType;
  quantity: number;
  price: number;
  amount: number;
  currency: string;
  status: TransactionStatus;
}

export interface TransactionAudit {
  processDate: string;
  processUser: string;
}

export interface TransactionRecord {
  key: TransactionKey;
  data: TransactionData;
  audit: TransactionAudit;
}

/**
 * History / Audit types and Zod schemas
 * Translated from: src/copybook/common/HISTREC.cpy
 *
 * Record types: PT=Portfolio, PS=Position, TR=Transaction
 * Action codes: A=Add, C=Change, D=Delete
 */

import { z } from "zod";

// ---------------------------------------------------------------------------
// Enums (from COBOL Level 88 conditionals)
// ---------------------------------------------------------------------------

export const HistoryRecordType = {
  Portfolio: "PT",
  Position: "PS",
  Transaction: "TR",
} as const;
export type HistoryRecordType =
  (typeof HistoryRecordType)[keyof typeof HistoryRecordType];

export const HistoryActionCode = {
  Add: "A",
  Change: "C",
  Delete: "D",
} as const;
export type HistoryActionCode =
  (typeof HistoryActionCode)[keyof typeof HistoryActionCode];

// ---------------------------------------------------------------------------
// Zod schemas
// ---------------------------------------------------------------------------

export const historyRecordTypeSchema = z.enum(["PT", "PS", "TR"]);

export const historyActionCodeSchema = z.enum(["A", "C", "D"]);

/**
 * Maps to HIST-KEY group
 * HIST-PORTFOLIO-ID + HIST-DATE (YYYYMMDD) + HIST-TIME (HHMMSS) + HIST-SEQ-NO
 */
export const historyKeySchema = z.object({
  portfolioId: z.string().length(8),
  date: z.string().length(8),
  time: z.string().length(6),
  seqNo: z.string().length(4),
});

/** Maps to HIST-DATA group */
export const historyDataSchema = z.object({
  recordType: historyRecordTypeSchema,
  actionCode: historyActionCodeSchema,
  beforeImage: z.string().max(400).optional(),
  afterImage: z.string().max(400).optional(),
  reasonCode: z.string().length(4).optional(),
});

/** Maps to HIST-AUDIT group */
export const historyAuditSchema = z.object({
  processDate: z.string().max(26),
  processUser: z.string().max(8),
});

/** Full HISTORY-RECORD from HISTREC.cpy */
export const historyRecordSchema = z.object({
  key: historyKeySchema,
  data: historyDataSchema,
  audit: historyAuditSchema,
});

// ---------------------------------------------------------------------------
// TypeScript interfaces
// ---------------------------------------------------------------------------

export interface HistoryKey {
  portfolioId: string;
  date: string;
  time: string;
  seqNo: string;
}

export interface HistoryData {
  recordType: HistoryRecordType;
  actionCode: HistoryActionCode;
  beforeImage?: string;
  afterImage?: string;
  reasonCode?: string;
}

export interface HistoryAudit {
  processDate: string;
  processUser: string;
}

export interface HistoryRecord {
  key: HistoryKey;
  data: HistoryData;
  audit: HistoryAudit;
}

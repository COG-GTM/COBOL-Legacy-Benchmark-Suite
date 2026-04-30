/**
 * Position types and Zod schemas
 * Translated from: src/copybook/common/POSREC.cpy
 *
 * Status: A=Active, C=Closed, P=Pending
 */

import { z } from "zod";

// ---------------------------------------------------------------------------
// Enums (from COBOL Level 88 conditionals)
// ---------------------------------------------------------------------------

export const PositionStatus = {
  Active: "A",
  Closed: "C",
  Pending: "P",
} as const;
export type PositionStatus =
  (typeof PositionStatus)[keyof typeof PositionStatus];

// ---------------------------------------------------------------------------
// Zod schemas
// ---------------------------------------------------------------------------

export const positionStatusSchema = z.enum(["A", "C", "P"]);

/** Maps to POS-KEY group (POS-PORTFOLIO-ID + POS-DATE + POS-INVESTMENT-ID) */
export const positionKeySchema = z.object({
  portfolioId: z.string().length(8),
  date: z.string().length(8),
  investmentId: z.string().length(10),
});

/** Maps to POS-DATA group */
export const positionDataSchema = z.object({
  quantity: z.number(),
  costBasis: z.number(),
  marketValue: z.number(),
  currency: z.string().length(3),
  status: positionStatusSchema,
});

/** Maps to POS-AUDIT group */
export const positionAuditSchema = z.object({
  lastMaintDate: z.string().max(26),
  lastMaintUser: z.string().max(8),
});

/** Full POSITION-RECORD from POSREC.cpy */
export const positionRecordSchema = z.object({
  key: positionKeySchema,
  data: positionDataSchema,
  audit: positionAuditSchema,
});

// ---------------------------------------------------------------------------
// TypeScript interfaces
// ---------------------------------------------------------------------------

export interface PositionKey {
  portfolioId: string;
  date: string;
  investmentId: string;
}

export interface PositionData {
  quantity: number;
  costBasis: number;
  marketValue: number;
  currency: string;
  status: PositionStatus;
}

export interface PositionAudit {
  lastMaintDate: string;
  lastMaintUser: string;
}

export interface PositionRecord {
  key: PositionKey;
  data: PositionData;
  audit: PositionAudit;
}

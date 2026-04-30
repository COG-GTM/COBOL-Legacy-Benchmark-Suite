/**
 * Portfolio types and Zod schemas
 * Translated from: src/copybook/common/PORTFLIO.cpy
 *
 * Status enums: A=Active, C=Closed, S=Suspended
 * Client types: I=Individual, C=Corporate, T=Trust
 */

import { z } from "zod";

// ---------------------------------------------------------------------------
// Enums (from COBOL Level 88 conditionals)
// ---------------------------------------------------------------------------

export const PortfolioStatus = {
  Active: "A",
  Closed: "C",
  Suspended: "S",
} as const;
export type PortfolioStatus =
  (typeof PortfolioStatus)[keyof typeof PortfolioStatus];

export const ClientType = {
  Individual: "I",
  Corporate: "C",
  Trust: "T",
} as const;
export type ClientType = (typeof ClientType)[keyof typeof ClientType];

// ---------------------------------------------------------------------------
// Zod schemas
// ---------------------------------------------------------------------------

export const portfolioStatusSchema = z.enum(["A", "C", "S"]);

export const clientTypeSchema = z.enum(["I", "C", "T"]);

/** Maps to PORT-KEY group (PORT-ID + PORT-ACCOUNT-NO) */
export const portfolioKeySchema = z.object({
  portfolioId: z.string().length(8),
  accountNo: z.string().length(10),
});

/** Maps to PORT-CLIENT-INFO group */
export const portfolioClientInfoSchema = z.object({
  clientName: z.string().max(30),
  clientType: clientTypeSchema,
});

/** Maps to PORT-PORTFOLIO-INFO group */
export const portfolioInfoSchema = z.object({
  createDate: z.string().length(8),
  lastMaint: z.string().length(8),
  status: portfolioStatusSchema,
});

/** Maps to PORT-FINANCIAL-INFO group (PIC S9(13)V99 COMP-3) */
export const portfolioFinancialInfoSchema = z.object({
  totalValue: z.number(),
  cashBalance: z.number(),
});

/** Maps to PORT-AUDIT-INFO group */
export const portfolioAuditInfoSchema = z.object({
  lastUser: z.string().max(8),
  lastTrans: z.string().length(8),
});

/** Full PORT-RECORD from PORTFLIO.cpy */
export const portfolioRecordSchema = z.object({
  key: portfolioKeySchema,
  clientInfo: portfolioClientInfoSchema,
  portfolioInfo: portfolioInfoSchema,
  financialInfo: portfolioFinancialInfoSchema,
  auditInfo: portfolioAuditInfoSchema,
});

// ---------------------------------------------------------------------------
// TypeScript interfaces
// ---------------------------------------------------------------------------

export interface PortfolioKey {
  portfolioId: string;
  accountNo: string;
}

export interface PortfolioClientInfo {
  clientName: string;
  clientType: ClientType;
}

export interface PortfolioInfo {
  createDate: string;
  lastMaint: string;
  status: PortfolioStatus;
}

export interface PortfolioFinancialInfo {
  totalValue: number;
  cashBalance: number;
}

export interface PortfolioAuditInfo {
  lastUser: string;
  lastTrans: string;
}

export interface PortfolioRecord {
  key: PortfolioKey;
  clientInfo: PortfolioClientInfo;
  portfolioInfo: PortfolioInfo;
  financialInfo: PortfolioFinancialInfo;
  auditInfo: PortfolioAuditInfo;
}

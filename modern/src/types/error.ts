/**
 * Error types and Zod schemas
 * Translated from: src/copybook/common/ERRHAND.cpy
 * DB table definition: src/database/db2/ERRLOG.sql
 *
 * Error categories: VS=VSAM, VL=Validation, PR=Process, SY=System
 * Error types (DB): S=System, A=Application, D=Data
 * Severity levels: 1=Info, 2=Warning, 3=Error, 4=Severe
 * Return codes: 0=Success, 4=Warning, 8=Error, 12=Severe, 16=Terminal
 */

import { z } from "zod";

// ---------------------------------------------------------------------------
// Enums
// ---------------------------------------------------------------------------

export const ErrorCategory = {
  VSAM: "VS",
  Validation: "VL",
  Process: "PR",
  System: "SY",
} as const;
export type ErrorCategory =
  (typeof ErrorCategory)[keyof typeof ErrorCategory];

export const ErrorType = {
  System: "S",
  Application: "A",
  Data: "D",
} as const;
export type ErrorType = (typeof ErrorType)[keyof typeof ErrorType];

export const ErrorSeverity = {
  Info: 1,
  Warning: 2,
  Error: 3,
  Severe: 4,
} as const;
export type ErrorSeverity =
  (typeof ErrorSeverity)[keyof typeof ErrorSeverity];

export const ReturnCode = {
  Success: 0,
  Warning: 4,
  Error: 8,
  Severe: 12,
  Terminal: 16,
} as const;
export type ReturnCode = (typeof ReturnCode)[keyof typeof ReturnCode];

export const VsamStatus = {
  Success: "00",
  DuplicateKey: "22",
  NotFound: "23",
  EndOfFile: "10",
} as const;
export type VsamStatus = (typeof VsamStatus)[keyof typeof VsamStatus];

// ---------------------------------------------------------------------------
// Zod schemas
// ---------------------------------------------------------------------------

export const errorCategorySchema = z.enum(["VS", "VL", "PR", "SY"]);

export const errorTypeSchema = z.enum(["S", "A", "D"]);

export const errorSeveritySchema = z.union([
  z.literal(1),
  z.literal(2),
  z.literal(3),
  z.literal(4),
]);

export const returnCodeSchema = z.union([
  z.literal(0),
  z.literal(4),
  z.literal(8),
  z.literal(12),
  z.literal(16),
]);

/** Maps to ERR-TIMESTAMP in ERR-MESSAGE */
export const errorTimestampSchema = z.object({
  date: z.string().max(10),
  time: z.string().max(8),
});

/** Maps to ERR-MESSAGE structure from ERRHAND.cpy */
export const errorMessageSchema = z.object({
  timestamp: errorTimestampSchema,
  program: z.string().max(8),
  category: errorCategorySchema,
  code: z.string().max(4),
  severity: errorSeveritySchema,
  text: z.string().max(80),
  details: z.string().max(256).optional(),
});

/** Maps to ERRLOG table from ERRLOG.sql */
export const errorLogSchema = z.object({
  errorTimestamp: z.date(),
  programId: z.string().max(8),
  errorType: errorTypeSchema,
  errorSeverity: errorSeveritySchema,
  errorCode: z.string().max(8),
  errorMessage: z.string().max(200),
  processDate: z.date(),
  processTime: z.string(),
  userId: z.string().max(8),
  additionalInfo: z.string().max(500).optional(),
});

// ---------------------------------------------------------------------------
// TypeScript interfaces
// ---------------------------------------------------------------------------

export interface ErrorTimestamp {
  date: string;
  time: string;
}

export interface ErrorMessage {
  timestamp: ErrorTimestamp;
  program: string;
  category: ErrorCategory;
  code: string;
  severity: ErrorSeverity;
  text: string;
  details?: string;
}

export interface ErrorLogEntry {
  errorTimestamp: Date;
  programId: string;
  errorType: ErrorType;
  errorSeverity: ErrorSeverity;
  errorCode: string;
  errorMessage: string;
  processDate: Date;
  processTime: string;
  userId: string;
  additionalInfo?: string;
}

// Standardized error handling - migrated from ERRPROC.cbl and DB2ERR.cbl
//
// ERRPROC.cbl: Standard error processing subroutine that receives error info
// via LINKAGE SECTION, formats timestamps, writes to ERROR-LOG file, displays
// errors, and sets return codes based on severity.
//
// DB2ERR.cbl: DB2-specific error handler that logs SQL errors to ERRLOG table,
// diagnoses error categories (deadlock, timeout, connection, dup-key, not-found),
// sets retry flags, and maps SQLCODE values to severity levels.
//
// This module merges both into a unified error handler that:
// - Maps COBOL return codes (0/4/8/12/16) to HTTP status codes (200/200/400/500/500)
// - Logs errors to the ErrorLog table (from ERRLOG.sql schema)
// - Provides structured error responses matching ERRREC.cpy fields
// - Handles database-specific errors with retry semantics from DB2ERR

import { NextResponse } from "next/server";
import { prisma } from "./db";
import type {
  ApiResponse,
  CobolReturnCode,
  ErrorSeverity,
  ErrorType,
} from "../types";
import { COBOL_RC_TO_HTTP, COBOL_RC_TO_SEVERITY } from "../types";

// DB2ERR.cbl error categories mapped to Prisma/PostgreSQL error codes
const DB_ERROR_MAP: Record<string, { severity: ErrorSeverity; retry: boolean; message: string }> = {
  P2002: { severity: 1, retry: false, message: "Duplicate key violation" },        // WS-DUP-KEY (-803)
  P2025: { severity: 1, retry: false, message: "Record not found" },               // WS-NOT-FOUND (+100)
  P2024: { severity: 2, retry: true, message: "Timeout - retry transaction" },      // WS-TIMEOUT (-913)
  P2034: { severity: 2, retry: true, message: "Transaction conflict - retry" },     // WS-DEADLOCK (-911)
  P1001: { severity: 4, retry: false, message: "Database connection error" },       // WS-CONNECTION-ERROR (-30081)
};

export class AppError extends Error {
  public readonly httpStatus: number;
  public readonly severity: ErrorSeverity;
  public readonly errorCode: string;
  public readonly errorType: ErrorType;
  public readonly details?: string;
  public readonly shouldRetry: boolean;

  constructor(params: {
    message: string;
    httpStatus?: number;
    severity?: ErrorSeverity;
    errorCode?: string;
    errorType?: ErrorType;
    details?: string;
    shouldRetry?: boolean;
  }) {
    super(params.message);
    this.name = "AppError";
    this.httpStatus = params.httpStatus ?? 500;
    this.severity = params.severity ?? 3;
    this.errorCode = params.errorCode ?? "GENERR";
    this.errorType = params.errorType ?? "A";
    this.details = params.details;
    this.shouldRetry = params.shouldRetry ?? false;
  }
}

/**
 * Map a COBOL return code to an AppError.
 * COBOL return codes: 0=success, 4=warning, 8=error, 12=severe, 16=terminal
 * HTTP mapping: 0->200, 4->200, 8->400, 12->500, 16->500
 */
export function fromCobolReturnCode(
  returnCode: CobolReturnCode,
  message: string,
  details?: string
): AppError {
  return new AppError({
    message,
    httpStatus: COBOL_RC_TO_HTTP[returnCode],
    severity: COBOL_RC_TO_SEVERITY[returnCode],
    errorCode: `RC${String(returnCode).padStart(4, "0")}`,
    errorType: returnCode >= 12 ? "S" : "A",
    details,
  });
}

/**
 * Diagnose database errors, mirroring DB2ERR 2000-DIAGNOSE-ERROR.
 * Maps Prisma error codes to severity levels and retry flags.
 */
export function diagnoseDatabaseError(error: unknown): AppError {
  if (error instanceof AppError) return error;

  const prismaCode = (error as { code?: string })?.code;
  const mapped = prismaCode ? DB_ERROR_MAP[prismaCode] : undefined;

  if (mapped) {
    return new AppError({
      message: mapped.message,
      httpStatus: mapped.severity >= 3 ? 500 : 400,
      severity: mapped.severity,
      errorCode: `DB${prismaCode}`,
      errorType: "D",
      details: error instanceof Error ? error.message : undefined,
      shouldRetry: mapped.retry,
    });
  }

  return new AppError({
    message: "Unhandled database error",
    httpStatus: 500,
    severity: 3,
    errorCode: "DBERR",
    errorType: "D",
    details: error instanceof Error ? error.message : String(error),
  });
}

/**
 * Log error to ErrorLog table, mirroring ERRPROC 2000-PROCESS-ERROR
 * and DB2ERR 1000-LOG-ERROR / 1200-INSERT-ERROR paragraphs.
 *
 * ERRPROC built the error record from linkage section fields:
 *   ERR-TIMESTAMP, ERR-PROGRAM, ERR-CATEGORY, ERR-CODE, ERR-SEVERITY,
 *   ERR-TEXT, ERR-DETAILS
 *
 * DB2ERR logged to ERRLOG table with INSERT INTO ERRLOG VALUES.
 */
export async function logError(
  error: AppError,
  programId: string = "API",
  userId: string = "SYSTEM"
): Promise<void> {
  try {
    const now = new Date();
    await prisma.errorLog.create({
      data: {
        errorTimestamp: now,
        programId: programId.padEnd(8).substring(0, 8),
        errorType: error.errorType,
        errorSeverity: error.severity,
        errorCode: error.errorCode.padEnd(8).substring(0, 8),
        errorMessage: error.message.substring(0, 200),
        processDate: now,
        processTime: now,
        userId: userId.padEnd(8).substring(0, 8),
        additionalInfo: error.details?.substring(0, 500),
      },
    });
  } catch (logErr) {
    // Mirrors ERRPROC fallback: DISPLAY 'Error writing to log: ' WS-LOG-STATUS
    console.error("Failed to write error log:", logErr);
  }
}

/**
 * Build a structured error response matching ERRHAND.cpy ERR-MESSAGE fields.
 */
function buildErrorResponse(error: AppError): ApiResponse<never> {
  return {
    success: false,
    error: {
      code: error.errorCode,
      message: error.message,
      details: error.details,
      severity: error.severity,
      httpStatus: error.httpStatus,
    },
    metadata: {
      generatedAt: new Date().toISOString(),
      processingTimeMs: 0,
    },
  };
}

/**
 * API route error handler middleware.
 * Wraps an async route handler, catches errors, logs them, and returns
 * structured JSON responses.
 */
export function withErrorHandler(
  handler: (request: Request) => Promise<NextResponse>
): (request: Request) => Promise<NextResponse> {
  return async (request: Request): Promise<NextResponse> => {
    const start = Date.now();
    try {
      return await handler(request);
    } catch (error) {
      const appError =
        error instanceof AppError
          ? error
          : diagnoseDatabaseError(error);

      await logError(appError);

      const response = buildErrorResponse(appError);
      response.metadata = {
        generatedAt: new Date().toISOString(),
        processingTimeMs: Date.now() - start,
      };

      return NextResponse.json(response, { status: appError.httpStatus });
    }
  };
}

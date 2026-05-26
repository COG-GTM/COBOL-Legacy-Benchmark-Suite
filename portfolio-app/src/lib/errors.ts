/**
 * Error handling — maps Redis and PostgreSQL errors to VSAM/DB2 semantics from ERRHAND.cpy.
 */

export type VsamErrorCode = "00" | "22" | "23" | "10" | "99";

export class PortfolioError extends Error {
  public readonly code: VsamErrorCode;

  constructor(code: VsamErrorCode, message: string) {
    super(message);
    this.name = "PortfolioError";
    this.code = code;
  }
}

/** VSAM status '22' — Duplicate record key (ERRHAND.cpy line 46, ERR-VSAM-22) */
export class DuplicateKeyError extends PortfolioError {
  constructor(detail?: string) {
    super("22", detail ?? "Duplicate record key");
  }
}

/** VSAM status '23' — Record not found (ERRHAND.cpy line 47, ERR-VSAM-23) */
export class RecordNotFoundError extends PortfolioError {
  constructor(detail?: string) {
    super("23", detail ?? "Record not found");
  }
}

/**
 * Maps a database/Redis error into a PortfolioError with VSAM-equivalent codes.
 * PostgreSQL 23505 = unique_violation → duplicate key.
 */
export function mapDbError(err: unknown): PortfolioError {
  if (err instanceof PortfolioError) {
    return err;
  }

  const pgErr = err as { code?: string; detail?: string; message?: string };
  if (pgErr.code === "23505") {
    return new DuplicateKeyError(pgErr.detail ?? "Duplicate record key");
  }

  const msg =
    pgErr.message ?? (err instanceof Error ? err.message : "Unexpected error");
  return new PortfolioError("99", msg);
}

export const REASON_CODE_LABELS: Record<string, string> = {
  "01": "Account Closed",
  "02": "Transferred",
  "03": "Client Requested",
};

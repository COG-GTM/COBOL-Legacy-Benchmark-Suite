import type {
  Transaction,
  TransactionInput,
  TransactionQuery,
} from '../types/transaction';

/**
 * Data-access boundary for portfolio transactions — the modern replacement for
 * the legacy PORTTRAN batch program (TRANFILE input) and its PORTVALD
 * validation subroutine.
 *
 * The UI depends only on this interface; {@link MockTransactionService}
 * implements it against in-memory fixtures today, and a REST implementation can
 * be dropped in later without touching the components:
 *
 *   list           -> GET  /api/transactions           (TRANFILE browse)
 *   availableUnits -> GET  /api/positions/:id/units    (POSFILE)
 *   submit         -> POST /api/transactions           (PORTTRAN)
 */
export interface TransactionService {
  /** Returns transactions ordered newest first, optionally filtered. */
  list(query?: TransactionQuery): Promise<Transaction[]>;
  /**
   * Units currently held for a portfolio / investment pair, as a decimal
   * string, or `null` when the portfolio holds no such investment. Used by the
   * form to check that a SELL is covered before it is submitted, mirroring the
   * `PORT-TOTAL-UNITS < TRN-QUANTITY` guard in PORTTRAN 2220-PROCESS-SELL.
   */
  availableUnits(
    portfolioId: string,
    investmentId: string,
  ): Promise<string | null>;
  /**
   * Submits a transaction. The service stamps TRN-KEY (date, time, sequence
   * number), TRN-AMOUNT and the TRN-AUDIT fields, and returns the resulting
   * pending record.
   */
  submit(input: TransactionInput): Promise<Transaction>;
}

/** Thrown when TRN-PORTFOLIO-ID does not exist (PORTTRAN 2110-CHECK-PORTFOLIO). */
export class UnknownPortfolioError extends Error {
  constructor(portfolioId: string) {
    super(`Invalid Portfolio ID: ${portfolioId}`);
    this.name = 'UnknownPortfolioError';
  }
}

/** Thrown when a SELL exceeds the units held (PORTTRAN 2220-PROCESS-SELL). */
export class InsufficientUnitsError extends Error {
  constructor(
    readonly requested: string,
    readonly available: string,
  ) {
    super(`Insufficient units for sale: ${available} available.`);
    this.name = 'InsufficientUnitsError';
  }
}

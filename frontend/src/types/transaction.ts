/**
 * Domain types for the Transaction Record.
 *
 * Mirrors `src/copybook/common/TRNREC.cpy` (01 TRANSACTION-RECORD). Only the
 * pieces referenced by the history inquiry are modelled here; the full record
 * arrives with the transaction-processing screens.
 */

/** TRN-TYPE (X(2)) — 88-level values from TRNREC.cpy. */
export type TransactionType = 'BU' | 'SL' | 'TR' | 'FE';

export const TRANSACTION_TYPE_LABELS: Record<TransactionType, string> = {
  BU: 'Buy',
  SL: 'Sell',
  TR: 'Transfer',
  FE: 'Fee',
};

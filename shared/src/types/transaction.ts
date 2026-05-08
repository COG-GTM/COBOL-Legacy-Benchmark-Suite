/**
 * Transaction types derived from COBOL copybook TRNREC.cpy (lines 6-31)
 * and DB2 table TRANSACTION_HISTORY
 */

export type TransactionType = 'BUY' | 'SELL' | 'TRANSFER' | 'FEE';

export type TransactionStatus = 'PENDING' | 'DONE' | 'FAILED' | 'REVERSED';

export interface Transaction {
  date: string;                  // TRN-DATE (ISO 8601)
  time: string;                  // TRN-TIME (HH:MM:SS)
  portfolioId: string;           // TRN-PORTFOLIO-ID (8 chars)
  sequenceNo: string;            // TRN-SEQUENCE-NO (6 chars)
  investmentId: string;          // TRN-INVESTMENT-ID (10 chars)
  type: TransactionType;         // TRN-TYPE BU/SL/TR/FE
  quantity: number;              // TRN-QUANTITY (COMP-3 S9(11)V9(4))
  price: number;                 // TRN-PRICE (COMP-3 S9(11)V9(4))
  amount: number;                // TRN-AMOUNT (COMP-3 S9(13)V9(2))
  currency: string;              // TRN-CURRENCY (3 chars)
  status: TransactionStatus;     // TRN-STATUS P/D/F/R
  processDate: string;           // TRN-PROCESS-DATE (ISO 8601)
  processUser: string;           // TRN-PROCESS-USER (8 chars)
}

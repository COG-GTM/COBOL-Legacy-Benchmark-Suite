/** Maps to TRNREC.cpy - TRANSACTION-RECORD */
export interface Transaction {
  date: string;              // TRN-DATE PIC X(08) YYYYMMDD
  time: string;              // TRN-TIME PIC X(06) HHMMSS
  portfolioId: string;       // TRN-PORTFOLIO-ID PIC X(08)
  sequenceNo: string;        // TRN-SEQUENCE-NO PIC X(06)
  investmentId: string;      // TRN-INVESTMENT-ID PIC X(10)
  type: TransactionType;     // TRN-TYPE PIC X(02)
  quantity: number;          // TRN-QUANTITY PIC S9(11)V9(4)
  price: number;             // TRN-PRICE PIC S9(11)V9(4)
  amount: number;            // TRN-AMOUNT PIC S9(13)V9(2)
  currency: string;          // TRN-CURRENCY PIC X(03)
  status: TransactionStatus; // TRN-STATUS PIC X(01)
  processDate: string;       // TRN-PROCESS-DATE PIC X(26)
  processUser: string;       // TRN-PROCESS-USER PIC X(08)
}

export type TransactionType = 'BU' | 'SL' | 'TR' | 'FE';
export const TRANSACTION_TYPE_LABELS: Record<TransactionType, string> = {
  BU: 'Buy',
  SL: 'Sell',
  TR: 'Transfer',
  FE: 'Fee',
};

export type TransactionStatus = 'P' | 'D' | 'F' | 'R';
export const TRANSACTION_STATUS_LABELS: Record<TransactionStatus, string> = {
  P: 'Pending',
  D: 'Done',
  F: 'Failed',
  R: 'Reversed',
};

/** Maps to INQHIST.cbl WS-HISTORY-TABLE entry (display format) */
export interface HistoryEntry {
  date: string;     // WS-TRANS-DATE X(10)
  type: string;     // WS-TRANS-TYPE X(4) - Buy/Sell/Transfer/Fee
  units: number;    // WS-TRANS-UNITS S9(9)V99
  price: number;    // WS-TRANS-PRICE S9(9)V99
  amount: number;   // WS-TRANS-AMOUNT S9(9)V99
}

export type InvestmentType = 'STK' | 'BND' | 'MMF' | 'ETF';
export const INVESTMENT_TYPE_LABELS: Record<InvestmentType, string> = {
  STK: 'Stock',
  BND: 'Bond',
  MMF: 'Money Market Fund',
  ETF: 'Exchange Traded Fund',
};

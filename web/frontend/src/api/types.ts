// Response shapes returned by the CLBS Portfolio API (web/api). Kept in sync
// with web/api/src/types.ts.

export type TransactionType = "BU" | "SL" | "TR" | "FE";

export interface PositionResponse {
  accountNo: string;
  portfolioId: string;
  fundId: string;
  fundName: string;
  units: number;
  costBasis: number;
  marketValue: number;
  currencyCode: string;
  positionDate: string;
}

export interface HistoryRow {
  date: string;
  type: TransactionType;
  units: number;
  price: number;
  amount: number;
}

export interface HistoryResponse {
  accountNo: string;
  portfolioId: string;
  transactions: HistoryRow[];
}

export interface ApiError {
  error: string;
  message: string;
}

// Human-readable labels for the COBOL transaction type codes (TRNREC.cpy).
export const TRANSACTION_TYPE_LABELS: Record<TransactionType, string> = {
  BU: "Buy",
  SL: "Sell",
  TR: "Transfer",
  FE: "Fee",
};

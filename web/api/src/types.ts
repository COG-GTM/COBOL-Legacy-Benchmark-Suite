/**
 * API data shapes.
 *
 * These interfaces are derived directly from the DB2 schema in
 * `src/database/db2/db2-definitions.sql` (PORTFOLIO_MASTER, INVESTMENT_POSITIONS,
 * TRANSACTION_HISTORY) and the COBOL copybooks (PORTFLIO.cpy, POSREC.cpy,
 * TRNREC.cpy). DB2 column names are mapped to camelCase JSON fields.
 *
 * Note on keys: the online CICS programs (INQPORT / INQHIST) look records up by
 * *account number* (INQCOM-ACCOUNT-NO / PORT-ACCOUNT-NO, PIC X(10)), while the
 * DB2 tables are keyed by PORTFOLIO_ID (CHAR(8)). Each portfolio therefore
 * carries both identifiers so the account-keyed inquiry endpoints and the
 * portfolio-id master endpoint resolve to the same record.
 */

/** Portfolio status codes (PORTFOLIO_MASTER.STATUS). */
export type PortfolioStatus = "A" | "C" | "S"; // Active / Closed / Suspended

/** Transaction type codes (TRANSACTION_HISTORY.TRANSACTION_TYPE). */
export type TransactionType = "BU" | "SL" | "TR" | "FE"; // Buy / Sell / Transfer / Fee

/** Transaction status codes (TRANSACTION_HISTORY.STATUS). */
export type TransactionStatus = "P" | "F" | "R"; // Processed / Failed / Reversed

/** Mirrors PORTFOLIO_MASTER. */
export interface PortfolioMaster {
  portfolioId: string; // PORTFOLIO_ID   CHAR(8)
  accountNo: string; // account key used by the online inquiries (PORT-ACCOUNT-NO)
  accountType: string; // ACCOUNT_TYPE   CHAR(2)
  branchId: string; // BRANCH_ID      CHAR(2)
  clientId: string; // CLIENT_ID      CHAR(10)
  portfolioName: string; // PORTFOLIO_NAME VARCHAR(50)
  currencyCode: string; // CURRENCY_CODE  CHAR(3)
  riskLevel: string; // RISK_LEVEL     CHAR(1)
  status: PortfolioStatus; // STATUS        CHAR(1)
  openDate: string; // OPEN_DATE      DATE (YYYY-MM-DD)
  closeDate: string | null; // CLOSE_DATE     DATE
  lastMaintDate: string; // LAST_MAINT_DATE TIMESTAMP
  lastMaintUser: string; // LAST_MAINT_USER VARCHAR(8)
}

/** Mirrors INVESTMENT_POSITIONS (one holding within a portfolio). */
export interface InvestmentPosition {
  portfolioId: string; // PORTFOLIO_ID   CHAR(8)
  investmentId: string; // INVESTMENT_ID  CHAR(10)  -> "Fund ID" on POSMAP
  investmentName: string; // "Fund Name" on POSMAP (from investment reference)
  positionDate: string; // POSITION_DATE  DATE
  quantity: number; // QUANTITY       DECIMAL(18,4) -> "Units" on POSMAP
  costBasis: number; // COST_BASIS     DECIMAL(18,2)
  marketValue: number; // MARKET_VALUE   DECIMAL(18,2)
  currencyCode: string; // CURRENCY_CODE  CHAR(3)
}

/** Mirrors TRANSACTION_HISTORY (one dated transaction). */
export interface TransactionRecord {
  transactionId: string; // TRANSACTION_ID   CHAR(20)
  portfolioId: string; // PORTFOLIO_ID     CHAR(8)
  transactionDate: string; // TRANSACTION_DATE DATE  -> "Date" on HISMAP
  transactionTime: string; // TRANSACTION_TIME TIME
  investmentId: string; // INVESTMENT_ID    CHAR(10)
  transactionType: TransactionType; // TRANSACTION_TYPE CHAR(2) -> "Type" on HISMAP
  quantity: number; // QUANTITY DECIMAL(18,4) -> "Units" on HISMAP
  price: number; // PRICE    DECIMAL(18,4) -> "Price" on HISMAP
  amount: number; // AMOUNT   DECIMAL(18,2) -> "Amount" on HISMAP
  currencyCode: string; // CURRENCY_CODE CHAR(3)
  status: TransactionStatus; // STATUS CHAR(1)
}

/** Response body for GET /api/portfolios/:accountNo/position (mirrors POSMAP). */
export interface PositionResponse {
  accountNo: string;
  portfolioId: string;
  fundId: string; // investmentId
  fundName: string; // investmentName
  units: number; // quantity
  costBasis: number;
  marketValue: number;
  currencyCode: string;
  positionDate: string;
}

/** One row of GET /api/portfolios/:accountNo/history (mirrors a HISMAP row). */
export interface HistoryRow {
  date: string; // transactionDate
  type: TransactionType; // transactionType
  units: number; // quantity
  price: number;
  amount: number;
}

/** Response body for GET /api/portfolios/:accountNo/history. */
export interface HistoryResponse {
  accountNo: string;
  portfolioId: string;
  transactions: HistoryRow[];
}

/** Standard error envelope. Mirrors the COBOL INQCOM-ERROR-MSG path. */
export interface ApiError {
  error: string;
  message: string;
}

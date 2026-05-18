// Domain types derived from COBOL copybooks:
//   PORTFLIO.cpy — Portfolio Master Record
//   POSREC.cpy   — Position Record
//   TRNREC.cpy   — Transaction Record

// ---------------------------------------------------------------------------
// Enums matching COBOL level-88 condition names
// ---------------------------------------------------------------------------

export type ClientType = "I" | "C" | "T";
export type PortfolioStatus = "A" | "C" | "S";
export type PositionStatus = "A" | "C" | "P";
export type TransactionType = "BU" | "SL" | "TR" | "FE";
export type TransactionStatus = "P" | "D" | "F" | "R";

// ---------------------------------------------------------------------------
// Portfolio  (from PORTFLIO.cpy — PORT-RECORD)
// ---------------------------------------------------------------------------

export interface Portfolio {
  id: string;            // PORT-ID          PIC X(8)
  accountNo: string;     // PORT-ACCOUNT-NO  PIC X(10)
  clientName: string;    // PORT-CLIENT-NAME PIC X(30)
  clientType: ClientType; // PORT-CLIENT-TYPE PIC X(1)
  createDate: string | null;    // PORT-CREATE-DATE (ISO 8601)
  lastMaint: string | null;     // PORT-LAST-MAINT  (ISO 8601)
  status: PortfolioStatus; // PORT-STATUS    PIC X(1)
  totalValue: number;    // PORT-TOTAL-VALUE  S9(13)V99
  cashBalance: number;   // PORT-CASH-BALANCE S9(13)V99
  lastUser: string;      // PORT-LAST-USER   PIC X(8)
  lastTrans: string | null;     // PORT-LAST-TRANS  (ISO 8601)
}

// ---------------------------------------------------------------------------
// Position  (from POSREC.cpy — POSITION-RECORD)
// ---------------------------------------------------------------------------

export interface Position {
  portfolioId: string;     // POS-PORTFOLIO-ID  PIC X(8)
  date: string;            // POS-DATE          (ISO 8601)
  investmentId: string;    // POS-INVESTMENT-ID PIC X(10)
  quantity: number;        // POS-QUANTITY      S9(11)V9(4)
  costBasis: number;       // POS-COST-BASIS    S9(13)V9(2)
  marketValue: number;     // POS-MARKET-VALUE  S9(13)V9(2)
  currency: string;        // POS-CURRENCY      PIC X(3)
  status: PositionStatus;  // POS-STATUS        PIC X(1)
  lastMaintDate: string;   // POS-LAST-MAINT-DATE (ISO 8601)
  lastMaintUser: string;   // POS-LAST-MAINT-USER PIC X(8)
}

// ---------------------------------------------------------------------------
// Transaction  (from TRNREC.cpy — TRANSACTION-RECORD)
// ---------------------------------------------------------------------------

export interface Transaction {
  date: string;            // TRN-DATE         (ISO 8601)
  time: string;            // TRN-TIME         HHMMSS
  portfolioId: string;     // TRN-PORTFOLIO-ID PIC X(8)
  sequenceNo: string;      // TRN-SEQUENCE-NO  PIC X(6)
  investmentId: string;    // TRN-INVESTMENT-ID PIC X(10)
  type: TransactionType;   // TRN-TYPE         PIC X(2)
  quantity: number;        // TRN-QUANTITY     S9(11)V9(4)
  price: number;           // TRN-PRICE        S9(11)V9(4)
  amount: number;          // TRN-AMOUNT       S9(13)V9(2)
  currency: string;        // TRN-CURRENCY     PIC X(3)
  status: TransactionStatus; // TRN-STATUS     PIC X(1)
  processDate: string;     // TRN-PROCESS-DATE (ISO 8601)
  processUser: string;     // TRN-PROCESS-USER PIC X(8)
}

// ---------------------------------------------------------------------------
// Audit & Error records (supplementary)
// ---------------------------------------------------------------------------

export interface AuditRecord {
  id: string;
  entityType: "portfolio" | "position" | "transaction";
  entityId: string;
  action: string;
  timestamp: string;       // ISO 8601
  user: string;
  details: string;
}

export interface ErrorRecord {
  id: string;
  code: string;
  message: string;
  severity: "info" | "warning" | "error" | "critical";
  timestamp: string;       // ISO 8601
  source: string;
  details: string;
}

// ---------------------------------------------------------------------------
// Type guards
// ---------------------------------------------------------------------------

export function isClientType(v: unknown): v is ClientType {
  return v === "I" || v === "C" || v === "T";
}

export function isPortfolioStatus(v: unknown): v is PortfolioStatus {
  return v === "A" || v === "C" || v === "S";
}

export function isPositionStatus(v: unknown): v is PositionStatus {
  return v === "A" || v === "C" || v === "P";
}

export function isTransactionType(v: unknown): v is TransactionType {
  return v === "BU" || v === "SL" || v === "TR" || v === "FE";
}

export function isTransactionStatus(v: unknown): v is TransactionStatus {
  return v === "P" || v === "D" || v === "F" || v === "R";
}

// ---------------------------------------------------------------------------
// Validation helpers
// ---------------------------------------------------------------------------

export function isValidPortfolio(o: unknown): o is Portfolio {
  if (typeof o !== "object" || o === null) return false;
  const p = o as Record<string, unknown>;
  return (
    typeof p.id === "string" &&
    typeof p.accountNo === "string" &&
    typeof p.clientName === "string" &&
    isClientType(p.clientType) &&
    typeof p.totalValue === "number" &&
    isPortfolioStatus(p.status)
  );
}

export function isValidPosition(o: unknown): o is Position {
  if (typeof o !== "object" || o === null) return false;
  const p = o as Record<string, unknown>;
  return (
    typeof p.portfolioId === "string" &&
    typeof p.investmentId === "string" &&
    typeof p.quantity === "number" &&
    typeof p.marketValue === "number" &&
    isPositionStatus(p.status)
  );
}

export function isValidTransaction(o: unknown): o is Transaction {
  if (typeof o !== "object" || o === null) return false;
  const t = o as Record<string, unknown>;
  return (
    typeof t.portfolioId === "string" &&
    typeof t.investmentId === "string" &&
    isTransactionType(t.type) &&
    typeof t.amount === "number" &&
    isTransactionStatus(t.status)
  );
}

// ---------------------------------------------------------------------------
// Display helpers
// ---------------------------------------------------------------------------

const CLIENT_TYPE_LABELS: Record<ClientType, string> = {
  I: "Individual",
  C: "Corporate",
  T: "Trust",
};

const PORTFOLIO_STATUS_LABELS: Record<PortfolioStatus, string> = {
  A: "Active",
  C: "Closed",
  S: "Suspended",
};

const TRANSACTION_TYPE_LABELS: Record<TransactionType, string> = {
  BU: "Buy",
  SL: "Sell",
  TR: "Transfer",
  FE: "Fee",
};

const TRANSACTION_STATUS_LABELS: Record<TransactionStatus, string> = {
  P: "Pending",
  D: "Done",
  F: "Failed",
  R: "Reversed",
};

export function clientTypeLabel(t: ClientType): string {
  return CLIENT_TYPE_LABELS[t];
}

export function portfolioStatusLabel(s: PortfolioStatus): string {
  return PORTFOLIO_STATUS_LABELS[s];
}

export function transactionTypeLabel(t: TransactionType): string {
  return TRANSACTION_TYPE_LABELS[t];
}

export function transactionStatusLabel(s: TransactionStatus): string {
  return TRANSACTION_STATUS_LABELS[s];
}

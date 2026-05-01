/** Maps to PORTFLIO.cpy — PORT-RECORD */
export interface Portfolio {
  id: string;               // PORT-ID PIC X(8)
  accountNo: string;         // PORT-ACCOUNT-NO PIC X(10)
  clientName: string;        // PORT-CLIENT-NAME PIC X(30)
  clientType: ClientType;    // PORT-CLIENT-TYPE PIC X(1)
  createDate: string;        // PORT-CREATE-DATE PIC 9(8)
  lastMaint: string;         // PORT-LAST-MAINT PIC 9(8)
  status: PortfolioStatus;   // PORT-STATUS PIC X(1)
  totalValue: number;        // PORT-TOTAL-VALUE PIC S9(13)V99
  cashBalance: number;       // PORT-CASH-BALANCE PIC S9(13)V99
  lastUser: string;          // PORT-LAST-USER PIC X(8)
  lastTrans: string;         // PORT-LAST-TRANS PIC 9(8)
}

export type ClientType = "I" | "C" | "T";
export type PortfolioStatus = "A" | "C" | "S" | "P";

export const CLIENT_TYPE_LABELS: Record<ClientType, string> = {
  I: "Individual",
  C: "Corporate",
  T: "Trust",
};

export const PORTFOLIO_STATUS_LABELS: Record<PortfolioStatus, string> = {
  A: "Active",
  C: "Closed",
  S: "Suspended",
  P: "Pending",
};

/** Maps to POSREC.cpy — POSITION-RECORD */
export interface Position {
  portfolioId: string;       // POS-PORTFOLIO-ID PIC X(08)
  date: string;              // POS-DATE PIC X(08)
  investmentId: string;      // POS-INVESTMENT-ID PIC X(10)
  fundName: string;          // Derived — matches NAMEOUT on POSMAP
  quantity: number;          // POS-QUANTITY PIC S9(11)V9(4)
  costBasis: number;         // POS-COST-BASIS PIC S9(13)V9(2)
  marketValue: number;       // POS-MARKET-VALUE PIC S9(13)V9(2)
  currency: string;          // POS-CURRENCY PIC X(03)
  status: PositionStatus;    // POS-STATUS PIC X(01)
  lastMaintDate: string;     // POS-LAST-MAINT-DATE PIC X(26)
  lastMaintUser: string;     // POS-LAST-MAINT-USER PIC X(08)
}

export type PositionStatus = "A" | "C" | "P";

export const POSITION_STATUS_LABELS: Record<PositionStatus, string> = {
  A: "Active",
  C: "Closed",
  P: "Pending",
};

/** Maps to TRNREC.cpy — TRANSACTION-RECORD */
export interface Transaction {
  date: string;              // TRN-DATE PIC X(08)
  time: string;              // TRN-TIME PIC X(06)
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

export type TransactionType = "BU" | "SL" | "TR" | "FE";
export type TransactionStatus = "P" | "D" | "F" | "R";

export const TRANSACTION_TYPE_LABELS: Record<TransactionType, string> = {
  BU: "Buy",
  SL: "Sell",
  TR: "Transfer",
  FE: "Fee",
};

export const TRANSACTION_STATUS_LABELS: Record<TransactionStatus, string> = {
  P: "Pending",
  D: "Done",
  F: "Failed",
  R: "Reversed",
};

/** Maps to ERRHND.cpy — ERROR-HANDLING */
export interface AppError {
  program: string;           // ERR-PROGRAM PIC X(8)
  paragraph: string;         // ERR-PARAGRAPH PIC X(30)
  severity: ErrorSeverity;   // ERR-SEVERITY PIC X
  message: string;           // ERR-MESSAGE PIC X(80)
  action: ErrorAction;       // ERR-ACTION PIC X
  traceId: string;           // ERR-TRACE-ID PIC X(16)
  timestamp: string;         // ERR-TIMESTAMP PIC X(26)
}

export type ErrorSeverity = "F" | "W" | "I";
export type ErrorAction = "R" | "C" | "A";

export const ERROR_SEVERITY_LABELS: Record<ErrorSeverity, string> = {
  F: "Fatal",
  W: "Warning",
  I: "Info",
};

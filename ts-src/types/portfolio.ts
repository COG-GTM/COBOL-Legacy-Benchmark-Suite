/**
 * Portfolio Master Record types.
 * Migrated from: src/copybook/common/PORTFLIO.cpy
 *
 * Original COBOL record layout for the VSAM KSDS Portfolio Master file.
 * Key structure: PORT-ID (8) + PORT-ACCOUNT-NO (10) = 18-byte composite key.
 */

/** Composite primary key for a portfolio record. */
export interface PortfolioKey {
  /** PIC X(8) – Portfolio identifier (e.g., "PORT0001"). */
  portId: string;
  /** PIC X(10) – Client account number. */
  portAccountNo: string;
  /** PIC X(2) – Account type code. */
  portAccountType: string;
}

/** Client-level information embedded in the portfolio record. */
export interface PortfolioClientInfo {
  /** PIC X(30) – Client full name. */
  portClientName: string;
  /** PIC X(1) – Client type: I=Individual, C=Corporate, T=Trust. */
  portClientType: string;
  /** PIC X(3) – Branch identifier. */
  portBranchId: string;
}

/** Financial summary fields stored as fixed-point decimals. */
export interface PortfolioFinancialInfo {
  /** PIC S9(13)V99 COMP-3 – Total portfolio value. */
  portTotalValue: number;
  /** PIC S9(13)V99 COMP-3 – Cash balance. */
  portCashBalance: number;
  /** PIC S9(13)V99 COMP-3 – Total cost basis. */
  portTotalCost: number;
  /** PIC S9(8) COMP – Total number of units/shares. */
  portTotalUnits: number;
}

/** Audit / maintenance fields. */
export interface PortfolioAuditInfo {
  /** PIC X(8) – Record creation date (YYYYMMDD). */
  portCreateDate: string;
  /** PIC X(8) – Last maintenance date (YYYYMMDD). */
  portLastMaint: string;
  /** PIC X(8) – Last maintenance user ID. */
  portMaintUser: string;
}

/** Portfolio status codes (level-88 condition names). */
export enum PortfolioStatus {
  Active = 'A',
  Closed = 'C',
  Suspended = 'S',
  Inactive = 'I',
}

/** Full portfolio master record. */
export interface PortfolioRecord {
  portKey: PortfolioKey;
  portStatus: PortfolioStatus | string;
  portClientInfo: PortfolioClientInfo;
  portFinancialInfo: PortfolioFinancialInfo;
  portAuditInfo: PortfolioAuditInfo;
}

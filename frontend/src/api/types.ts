// Data contract from section 7.2 of the frontend functional requirements.
// Decimal values are strings to avoid float rounding (BR-09).
export type DecimalString = string;
export type IsoDate = string;
export type IsoTime = string;
export type IsoDateTime = string;

/** INQCOM-ACCOUNT-NO PIC X(10); digits only (BR-01) */
export type AccountNo = string;

/** POS-STATUS: Active | Closed | Pending */
export type PositionStatus = 'A' | 'C' | 'P';

/** TRN-TYPE / POSHIST.TRANS_TYPE */
export type TransactionType = 'BU' | 'SL' | 'TR' | 'FE';

export type CurrencyCode = 'USD' | 'EUR' | 'GBP' | 'JPY' | 'CAD' | string;

export interface Permission {
  resource: string;
  accessType: string;
}

export interface SessionInfo {
  userId: string;
  displayName?: string;
  permissions: Permission[];
}

export interface Position {
  accountNo: AccountNo;
  portfolioId: string;
  positionDate: IsoDate;
  investmentId: string;
  fundName: string | null;
  units: DecimalString;
  costBasis: DecimalString;
  marketValue: DecimalString;
  currency: CurrencyCode;
  status: PositionStatus;
  lastMaintainedAt: IsoDateTime | null;
  lastMaintainedBy: string | null;
}

export interface Transaction {
  accountNo: AccountNo;
  portfolioId: string;
  tradeDate: IsoDate;
  tradeTime: IsoTime;
  type: TransactionType;
  securityId: string;
  units: DecimalString;
  price: DecimalString;
  amount: DecimalString;
  fees: DecimalString;
  totalAmount: DecimalString;
  costBasis: DecimalString;
  gainLoss: DecimalString;
}

export interface Page<T> {
  items: T[];
  page: number;
  pageSize: number;
  totalCount: number;
  hasNext: boolean;
  hasPrevious: boolean;
}
export type PositionPage = Page<Position>;
export type TransactionPage = Page<Transaction>;

export type ErrorSeverity = 'INFO' | 'WARNING' | 'ERROR' | 'FATAL';
export type ErrorAction = 'CONTINUE' | 'RETURN' | 'ABEND';

export type ApiErrorCode =
  | 'INVALID_ACCOUNT_FORMAT'
  | 'POSITION_NOT_FOUND'
  | 'POSITION_READ_ERROR'
  | 'HISTORY_READ_ERROR'
  | 'DB_UNAVAILABLE'
  | 'UNAUTHENTICATED'
  | 'ACCESS_DENIED'
  | 'AUTH_CHECK_FAILED'
  | 'AUDIT_FAILED'
  | 'SYSTEM_ERROR';

export interface ApiError {
  code: ApiErrorCode;
  message: string;
  severity: ErrorSeverity;
  action: ErrorAction;
  traceId: string;
  legacyCode?: string;
  field?: 'accountNo';
  timestamp: IsoDateTime;
}

/** Permission required for the menu and both inquiries (AUTHFILE resource + access type). */
export const INQUIRY_PERMISSION: Permission = { resource: 'INQONLN', accessType: 'READ' };

export const HISTORY_PAGE_SIZE = 10;
export const POSITION_PAGE_SIZE = 10;

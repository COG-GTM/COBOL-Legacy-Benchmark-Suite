/** Position status, from POSREC POS-STATUS (A/P/C). */
export type PositionStatus = 'Active' | 'Pending' | 'Closed';
/** Transaction type, from TRNREC TRN-TYPE (BU/SL/TR/FE). */
export type TransactionType = 'Buy' | 'Sell' | 'Transfer' | 'Fee';
/** Transaction status, from TRNREC TRN-STATUS (P/D/F/R). */
export type TransactionStatus = 'Pending' | 'Done' | 'Failed' | 'Reversed';
export type AssetClass = 'us' | 'intl' | 'bond' | 're' | 'cash';

export interface AccountSummary {
  accountNo: string;
  name: string;
}

export interface Position {
  fundCode: string;
  fundName: string;
  assetClass: AssetClass;
  status: PositionStatus;
  units: number;
  nav: number;
  costBasis: number;
  marketValue: number;
  /** Market value at the previous business day's close, for the day-change KPI. */
  previousValue: number;
  realizedGain: number;
  settlingUnits: number;
}

export interface Transaction {
  id: string;
  date: string;
  type: TransactionType;
  fundCode: string;
  fundName: string;
  units: number;
  price: number;
  amount: number;
  status: TransactionStatus;
  /** +1 adds units to the position (buy, transfer in), -1 removes them. */
  direction: 1 | -1;
}

export interface ValuePoint {
  date: string;
  marketValue: number;
  costBasis: number;
}

export interface AccountDetail {
  accountNo: string;
  name: string;
  accountType: string;
  openedDate: string;
  asOfDate: string;
  positions: Position[];
  transactions: Transaction[];
  valueHistory: ValuePoint[];
}

export type ApiErrorKind = 'NOT_FOUND' | 'ACCESS_DENIED' | 'SYSTEM';

export class ApiError extends Error {
  constructor(
    public readonly kind: ApiErrorKind,
    message: string,
    public readonly code?: string,
    public readonly traceId?: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export interface PortfolioApi {
  listAccounts(): Promise<AccountSummary[]>;
  getAccount(accountNo: string): Promise<AccountDetail>;
}

export const ASSET_CLASS_LABELS: Record<AssetClass, string> = {
  us: 'US equity',
  intl: 'International equity',
  bond: 'Bonds',
  re: 'Real estate',
  cash: 'Short-term treasury',
};

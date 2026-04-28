/** Maps to PORTFLIO.cpy - PORT-RECORD */
export interface Portfolio {
  portfolioId: string;       // PORT-ID PIC X(8)
  accountNumber: string;     // PORT-ACCOUNT-NO PIC X(10)
  clientName: string;        // PORT-CLIENT-NAME PIC X(30)
  clientType: ClientType;    // PORT-CLIENT-TYPE PIC X(1)
  createDate: string;        // PORT-CREATE-DATE PIC 9(8) YYYYMMDD
  lastMaintDate: string;     // PORT-LAST-MAINT PIC 9(8)
  status: PortfolioStatus;   // PORT-STATUS PIC X(1)
  totalValue: number;        // PORT-TOTAL-VALUE PIC S9(13)V99
  cashBalance: number;       // PORT-CASH-BALANCE PIC S9(13)V99
  lastUser: string;          // PORT-LAST-USER PIC X(8)
  lastTransDate: string;     // PORT-LAST-TRANS PIC 9(8)
}

export type ClientType = 'I' | 'C' | 'T';
export const CLIENT_TYPE_LABELS: Record<ClientType, string> = {
  I: 'Individual',
  C: 'Corporate',
  T: 'Trust',
};

export type PortfolioStatus = 'A' | 'C' | 'S';
export const PORTFOLIO_STATUS_LABELS: Record<PortfolioStatus, string> = {
  A: 'Active',
  C: 'Closed',
  S: 'Suspended',
};

export type UpdateActionCode = 'S' | 'V' | 'N';
export const UPDATE_ACTION_LABELS: Record<UpdateActionCode, string> = {
  S: 'Status Change',
  V: 'Value Update',
  N: 'Name Change',
};

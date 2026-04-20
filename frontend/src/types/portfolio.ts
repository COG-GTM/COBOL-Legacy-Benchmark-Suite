/**
 * TypeScript interfaces mirroring COBOL data structures
 * Derived from src/copybook/common/PORTFLIO.cpy
 */

export type ClientType = 'I' | 'C' | 'T';
export type PortfolioStatus = 'A' | 'C' | 'S';

export const CLIENT_TYPE_LABELS: Record<ClientType, string> = {
  I: 'Individual',
  C: 'Corporate',
  T: 'Trust',
};

export const PORTFOLIO_STATUS_LABELS: Record<PortfolioStatus, string> = {
  A: 'Active',
  C: 'Closed',
  S: 'Suspended',
};

export interface Portfolio {
  id: string;
  accountNo: string;
  clientName: string;
  clientType: ClientType;
  createDate: string;
  lastMaint: string;
  status: PortfolioStatus;
  totalValue: number;
  cashBalance: number;
  lastUser: string;
  lastTrans: string;
}

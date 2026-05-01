export type PortfolioStatus = 'A' | 'C' | 'S';
export type RiskLevel = '1' | '2' | '3' | '4' | '5';

export interface Portfolio {
  portfolioId: string;
  accountType: string;
  branchId: string;
  clientId: string;
  name: string;
  currencyCode: string;
  riskLevel: RiskLevel;
  status: PortfolioStatus;
  openDate: string;
  closeDate?: string;
  lastMaintDate: string;
  lastMaintUser: string;
}

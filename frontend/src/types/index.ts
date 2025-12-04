export interface PortfolioPosition {
  accountId: string;
  fundId: string;
  fundName: string;
  units: number;
  costBasis: number;
  marketValue: number;
}

export interface Transaction {
  date: string;
  type: string;
  units: number;
  price: number;
  amount: number;
}

export interface TransactionHistory {
  accountId: string;
  transactions: Transaction[];
}

export interface MenuItem {
  id: number;
  label: string;
  path: string;
}

export interface ErrorInfo {
  code: string;
  message: string;
  details?: string;
}

export interface NavigationState {
  currentScreen: string;
  previousScreen?: string;
}

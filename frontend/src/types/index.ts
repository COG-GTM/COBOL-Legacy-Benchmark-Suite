export interface Portfolio {
  portfolioId: string;
  portfolioName: string;
  accountType: string;
  branchId: string;
  clientId: string;
  currencyCode: string;
  riskLevel: string;
  status: string;
  openDate: string;
  closeDate: string | null;
  lastMaintDate: string;
  totalMarketValue: number | null;
  totalCostBasis: number | null;
  totalGainLoss: number | null;
  positions: Position[] | null;
}

export interface Position {
  portfolioId: string;
  investmentId: string;
  positionDate: string;
  quantity: number;
  costBasis: number;
  marketValue: number;
  unrealizedGainLoss: number;
  currencyCode: string;
  status: string;
}

export interface Transaction {
  transactionId: string;
  portfolioId: string;
  transactionDate: string;
  transactionTime: string;
  investmentId: string;
  transactionType: string;
  transactionTypeLabel: string;
  quantity: number;
  price: number;
  amount: number;
  currencyCode: string;
  status: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface AuthResponse {
  token: string;
  username: string;
  role: string;
}

export interface AuthState {
  token: string | null;
  username: string | null;
  role: string | null;
  isAuthenticated: boolean;
}

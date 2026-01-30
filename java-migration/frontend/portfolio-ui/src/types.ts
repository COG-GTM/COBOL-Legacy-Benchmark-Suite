export interface Portfolio {
  id: number;
  portfolioId: string;
  accountNo: string;
  clientName: string;
  clientType: 'INDIVIDUAL' | 'CORPORATE' | 'TRUST';
  createDate: string;
  lastMaintDate: string | null;
  status: 'ACTIVE' | 'CLOSED' | 'SUSPENDED';
  totalValue: number;
  cashBalance: number;
  totalUnits: number;
  totalCost: number;
  lastUser: string | null;
  lastTransDate: string | null;
}

export interface Transaction {
  id: number;
  transactionDate: string;
  transactionTime: string;
  portfolioId: string;
  sequenceNo: string | null;
  investmentId: string | null;
  type: 'BUY' | 'SELL' | 'TRANSFER' | 'FEE';
  quantity: number;
  price: number | null;
  amount: number | null;
  currency: string;
  status: 'PENDING' | 'DONE' | 'FAILED' | 'REVERSED';
  processDate: string | null;
  processUser: string | null;
  message: string | null;
}

export interface TransactionRequest {
  portfolioId: string;
  investmentId?: string;
  type: 'BUY' | 'SELL' | 'TRANSFER' | 'FEE';
  quantity: number;
  price?: number;
  amount?: number;
  currency?: string;
  userId?: string;
}

export interface Position {
  id: number;
  portfolioId: string;
  positionDate: string;
  investmentId: string | null;
  quantity: number;
  costBasis: number;
  marketValue: number;
  currency: string;
  status: 'ACTIVE' | 'CLOSED' | 'PENDING';
  lastMaintDate: string | null;
  lastMaintUser: string | null;
}

export interface AuditLog {
  id: number;
  timestamp: string;
  systemId: string;
  userId: string;
  program: string;
  terminal: string | null;
  type: 'TRANSACTION' | 'USER_ACTION' | 'SYSTEM_EVENT';
  action: 'CREATE' | 'UPDATE' | 'DELETE' | 'INQUIRE' | 'LOGIN' | 'LOGOUT' | 'STARTUP' | 'SHUTDOWN';
  status: 'SUCCESS' | 'FAILURE' | 'WARNING';
  portfolioId: string;
  accountNo: string | null;
  beforeImage: string | null;
  afterImage: string | null;
  message: string | null;
}

export interface BatchProcessingResult {
  recordsRead: number;
  recordsProcessed: number;
  recordsWritten: number;
  errorCount: number;
  status: string;
  message: string;
  errors: string[];
  processedTransactions: Transaction[];
}

export interface PortfolioUpdateRequest {
  status?: 'ACTIVE' | 'CLOSED' | 'SUSPENDED';
  clientName?: string;
  totalValue?: number;
  userId?: string;
}

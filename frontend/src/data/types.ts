export interface Portfolio {
  id: string;
  name: string;
  accountNo: string;
  clientType: 'I' | 'C' | 'T';
  cashBalance: number;
  createDate: string;
  status: 'A' | 'I' | 'C';
  totalValue: number;
  lastUser?: string;
  lastTransDate?: string;
}

export interface Position {
  accountNo: string;
  fundId: string;
  cusip: string;
  shareBalance: number;
  avgCost: number;
  costBasis: number;
  lastDate: string;
  lastTrans: string;
  status: 'A' | 'C';
}

export interface Transaction {
  transId: string;
  accountNo: string;
  fundId: string;
  transType: 'BY' | 'SL' | 'FE';
  transDate: string;
  shareQty: number;
  price: number;
  amount: number;
  status: 'P' | 'C' | 'E';
  beforeBalance: number;
  afterBalance: number;
}

export interface BatchJob {
  processDate: string;
  processId: string;
  status: 'W' | 'P' | 'C' | 'E';
  startTime: string;
  endTime: string;
  recordCount: number;
  errorCount: number;
  returnCode: string;
  message: string;
}

export interface AuditEntry {
  timestamp: string;
  program: string;
  type: string;
  action: string;
  status: 'SUCC' | 'FAIL';
  portfolioId: string;
  accountNo: string;
  message: string;
}

export interface ErrorEntry {
  code: string;
  description: string;
  severity: 'Error' | 'Warning';
  action: string;
  timestamp: string;
  program: string;
}

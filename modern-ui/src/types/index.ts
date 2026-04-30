// Portfolio Master Record — from PORTFLIO.cpy
export interface Portfolio {
  id: string;
  accountNo: string;
  clientName: string;
  clientType: 'I' | 'C' | 'T';
  createDate: string;
  lastMaintDate: string;
  status: 'A' | 'C' | 'S';
  totalValue: number;
  cashBalance: number;
  lastUser: string;
  lastTransDate: string;
}

// Transaction Record — from TRNREC.cpy
export interface Transaction {
  id: string;
  date: string;
  time: string;
  portfolioId: string;
  sequenceNo: string;
  investmentId: string;
  type: 'BU' | 'SL' | 'TR' | 'FE';
  quantity: number;
  price: number;
  amount: number;
  currency: string;
  status: 'P' | 'D' | 'F' | 'R';
  processDate: string;
  processUser: string;
}

// Position Record — from POSREC.cpy
export interface Position {
  portfolioId: string;
  date: string;
  investmentId: string;
  investmentName: string;
  quantity: number;
  costBasis: number;
  marketValue: number;
  currency: string;
  status: 'A' | 'C' | 'P';
  lastMaintDate: string;
  lastMaintUser: string;
}

// History Record — from HISTREC.cpy
export interface HistoryRecord {
  portfolioId: string;
  date: string;
  time: string;
  seqNo: string;
  recordType: 'PT' | 'PS' | 'TR';
  actionCode: 'A' | 'C' | 'D';
  reasonCode: string;
  processDate: string;
  processUser: string;
  description: string;
}

// Audit Record — from AUDITLOG.cpy
export interface AuditRecord {
  id: string;
  timestamp: string;
  systemId: string;
  userId: string;
  program: string;
  terminal: string;
  type: 'TRAN' | 'USER' | 'SYST';
  action: string;
  status: 'SUCC' | 'FAIL' | 'WARN';
  portfolioId: string;
  accountNo: string;
  message: string;
}

// Batch Control Record — from BCHCTL.cpy
export interface BatchJob {
  id: string;
  jobName: string;
  processDate: string;
  sequenceNo: number;
  status: 'R' | 'A' | 'W' | 'D' | 'E';
  stepName: string;
  programName: string;
  startTime: string;
  endTime: string;
  prereqCount: number;
  prereqs: BatchPrereq[];
  returnCode: number;
  errorDesc: string;
  restartCount: number;
  attemptTs: string;
  completeTs: string;
}

export interface BatchPrereq {
  name: string;
  sequenceNo: number;
  returnCode: number;
}

// Lookup maps
export const CLIENT_TYPE_LABELS: Record<Portfolio['clientType'], string> = {
  I: 'Individual',
  C: 'Corporate',
  T: 'Trust',
};

export const PORTFOLIO_STATUS_LABELS: Record<Portfolio['status'], string> = {
  A: 'Active',
  C: 'Closed',
  S: 'Suspended',
};

export const TXN_TYPE_LABELS: Record<Transaction['type'], string> = {
  BU: 'Buy',
  SL: 'Sell',
  TR: 'Transfer',
  FE: 'Fee',
};

export const TXN_STATUS_LABELS: Record<Transaction['status'], string> = {
  P: 'Pending',
  D: 'Done',
  F: 'Failed',
  R: 'Reversed',
};

export const POSITION_STATUS_LABELS: Record<Position['status'], string> = {
  A: 'Active',
  C: 'Closed',
  P: 'Pending',
};

export const BATCH_STATUS_LABELS: Record<BatchJob['status'], string> = {
  R: 'Ready',
  A: 'Active',
  W: 'Waiting',
  D: 'Done',
  E: 'Error',
};

export const AUDIT_TYPE_LABELS: Record<AuditRecord['type'], string> = {
  TRAN: 'Transaction',
  USER: 'User Action',
  SYST: 'System Event',
};

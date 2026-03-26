export interface PositionReportEntry {
  portfolioId: string;
  portfolioName: string;
  accountNo: string;
  investmentId: string;
  investmentName: string;
  positionDate: string;
  quantity: number;
  costBasis: number;
  marketValue: number;
  unrealizedGainLoss: number;
  gainLossPercent: number;
  currency: string;
  status: 'Active' | 'Closed' | 'Pending';
}

export interface PositionReportSummary {
  reportDate: string;
  totalPortfolios: number;
  totalPositions: number;
  totalCostBasis: number;
  totalMarketValue: number;
  totalUnrealizedGainLoss: number;
  overallGainLossPercent: number;
}

export type AuditType = 'TRAN' | 'USER' | 'SYST';
export type AuditAction =
  | 'CREATE'
  | 'UPDATE'
  | 'DELETE'
  | 'INQUIRE'
  | 'LOGIN'
  | 'LOGOUT'
  | 'STARTUP'
  | 'SHUTDOWN';
export type AuditStatus = 'SUCC' | 'FAIL' | 'WARN';

export interface AuditReportEntry {
  timestamp: string;
  systemId: string;
  userId: string;
  program: string;
  terminal: string;
  type: AuditType;
  action: AuditAction;
  status: AuditStatus;
  portfolioId: string;
  accountNo: string;
  message: string;
}

export interface AuditReportSummary {
  reportDate: string;
  totalEvents: number;
  successCount: number;
  failureCount: number;
  warningCount: number;
  byType: { type: string; count: number }[];
  byAction: { action: string; count: number }[];
  byProgram: { program: string; count: number }[];
}

export interface StatisticsReportEntry {
  metricName: string;
  category: 'Performance' | 'Error' | 'Volume' | 'System';
  currentValue: number;
  previousValue: number;
  changePercent: number;
  unit: string;
  trend: 'up' | 'down' | 'flat';
  status: 'normal' | 'warning' | 'critical';
}

export interface ErrorSummaryEntry {
  errorCode: string;
  errorDescription: string;
  count: number;
  severity: 'Info' | 'Warning' | 'Error' | 'Severe';
  lastOccurrence: string;
  program: string;
}

export interface ProcessingThroughput {
  stepName: string;
  recordsProcessed: number;
  elapsedTime: string;
  recordsPerSecond: number;
  returnCode: number;
  date: string;
}

export interface StatisticsReportSummary {
  reportDate: string;
  metrics: StatisticsReportEntry[];
  errorSummary: ErrorSummaryEntry[];
  throughput: ProcessingThroughput[];
  dailyVolumes: {
    date: string;
    transactions: number;
    positions: number;
    errors: number;
  }[];
}

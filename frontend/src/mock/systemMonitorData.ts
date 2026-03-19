import type {
  ResourceMetric,
  ConnectionPoolStats,
  ErrorRateData,
  BatchPipelineData,
  ErrorLogEntry,
  SystemHealth,
} from '@/types';

export const resourceMetrics: ResourceMetric[] = [
  {
    name: 'CPU Utilization',
    current: 62,
    threshold_warning: 80,
    threshold_critical: 90,
    unit: '%',
    trend: [45, 48, 52, 55, 50, 47, 43, 41, 44, 53, 58, 65, 72, 75, 70, 68, 63, 60, 58, 55, 52, 57, 60, 62],
  },
  {
    name: 'Memory Usage',
    current: 78,
    threshold_warning: 80,
    threshold_critical: 90,
    unit: '%',
    trend: [68, 70, 72, 69, 71, 73, 75, 74, 76, 78, 80, 82, 79, 77, 75, 73, 74, 76, 78, 80, 79, 77, 76, 78],
  },
  {
    name: 'DASD Utilization',
    current: 45,
    threshold_warning: 80,
    threshold_critical: 90,
    unit: '%',
    trend: [42, 43, 41, 42, 44, 43, 45, 44, 46, 45, 47, 48, 46, 45, 44, 43, 44, 45, 46, 47, 48, 46, 45, 45],
  },
];

export const connectionPoolStats: ConnectionPoolStats = {
  active: 12,
  idle: 38,
  total: 50,
  maxTotal: 100,
  waitCount: 0,
  avgResponseMs: 23,
  trend: [8, 7, 6, 5, 5, 4, 6, 8, 10, 14, 18, 20, 22, 19, 16, 14, 12, 10, 9, 8, 10, 11, 13, 12],
};

export const errorRateData: ErrorRateData = {
  currentRate: 0.3,
  totalToday: 47,
  totalYesterday: 52,
  byCategory: [
    { category: 'DB2 Connection Timeout', count: 3, severity: 'critical' },
    { category: 'VSAM Read Error', count: 5, severity: 'warning' },
    { category: 'Transaction Validation', count: 22, severity: 'info' },
    { category: 'Security Auth Failure', count: 8, severity: 'warning' },
    { category: 'Batch Step Failure', count: 2, severity: 'critical' },
    { category: 'Data Format Error', count: 7, severity: 'info' },
  ],
  hourlyErrors: [1, 0, 1, 0, 0, 1, 2, 3, 4, 3, 2, 2, 3, 4, 3, 2, 1, 2, 3, 2, 1, 1, 2, 1],
};

export const batchPipelineData: BatchPipelineData = {
  steps: [
    {
      name: 'Transaction Validation',
      status: 'complete',
      startTime: '2024-01-15 02:00:00',
      endTime: '2024-01-15 02:12:34',
      returnCode: 0,
      recordsProcessed: 15420,
    },
    {
      name: 'Position Update',
      status: 'complete',
      startTime: '2024-01-15 02:12:35',
      endTime: '2024-01-15 02:28:17',
      returnCode: 0,
      recordsProcessed: 8930,
    },
    {
      name: 'History Load',
      status: 'complete',
      startTime: '2024-01-15 02:28:18',
      endTime: '2024-01-15 02:45:02',
      returnCode: 0,
      recordsProcessed: 24100,
    },
    {
      name: 'Price Reconciliation',
      status: 'complete',
      startTime: '2024-01-15 02:45:03',
      endTime: '2024-01-15 03:01:45',
      returnCode: 0,
      recordsProcessed: 12350,
    },
    {
      name: 'Position Report',
      status: 'complete',
      startTime: '2024-01-15 03:01:46',
      endTime: '2024-01-15 03:10:22',
      returnCode: 0,
      recordsProcessed: 8930,
    },
    {
      name: 'Audit Report',
      status: 'complete',
      startTime: '2024-01-15 03:10:23',
      endTime: '2024-01-15 03:18:55',
      returnCode: 0,
      recordsProcessed: 47200,
    },
    {
      name: 'Statistics Report',
      status: 'complete',
      startTime: '2024-01-15 03:18:56',
      endTime: '2024-01-15 03:25:10',
      returnCode: 0,
      recordsProcessed: 15420,
    },
  ],
  lastRun: '2024-01-15 02:00:00',
  nextScheduled: '2024-01-16 02:00:00',
};

export const errorLogEntries: ErrorLogEntry[] = [
  {
    timestamp: '2024-01-15 14:32:15',
    errorCode: 'E005',
    program: 'INQPORT',
    severity: 'critical',
    message: 'DB2 connection pool exhausted during portfolio inquiry — retry limit exceeded',
  },
  {
    timestamp: '2024-01-15 14:28:42',
    errorCode: 'E010',
    program: 'HISTLD00',
    severity: 'warning',
    message: 'History load operation timed out after 30s — batch step retried successfully',
  },
  {
    timestamp: '2024-01-15 14:15:03',
    errorCode: 'E006',
    program: 'SECMGR',
    severity: 'warning',
    message: 'Authentication failure for user BATCH02 — invalid credentials on third attempt',
  },
  {
    timestamp: '2024-01-15 13:58:21',
    errorCode: 'E001',
    program: 'TRNVAL00',
    severity: 'info',
    message: 'Invalid data format in transaction field ACCT-NUM — expected numeric, got alphanumeric',
  },
  {
    timestamp: '2024-01-15 13:45:10',
    errorCode: 'E008',
    program: 'TRNVAL00',
    severity: 'info',
    message: 'Validation warning: transaction amount exceeds soft limit of $1,000,000',
  },
  {
    timestamp: '2024-01-15 13:30:55',
    errorCode: 'E004',
    program: 'INQHIST',
    severity: 'warning',
    message: 'VSAM file read error on HIST-MASTER — record key not found for account 78432',
  },
  {
    timestamp: '2024-01-15 13:22:18',
    errorCode: 'E003',
    program: 'PRCSEQ00',
    severity: 'info',
    message: 'Duplicate batch sequence detected — step POSUPD00 already processed for run 2024-01-15',
  },
  {
    timestamp: '2024-01-15 13:10:44',
    errorCode: 'E007',
    program: 'RPTGEN00',
    severity: 'warning',
    message: 'Processing delay in report generation — queue depth exceeded threshold of 500',
  },
  {
    timestamp: '2024-01-15 12:58:33',
    errorCode: 'E002',
    program: 'INQPORT',
    severity: 'info',
    message: 'Portfolio record not found for account 99102 — account may be closed or transferred',
  },
  {
    timestamp: '2024-01-15 12:45:07',
    errorCode: 'E009',
    program: 'BCKLOD00',
    severity: 'critical',
    message: 'Version conflict on backup load — expected v3.2, found v3.1 in staging area',
  },
];

export const systemHealth: SystemHealth = {
  overallStatus: 'healthy',
  uptime: '14d 7h 23m',
  lastIncident: '2024-01-12 14:32:00',
  cicsRegionStatus: 'active',
  db2Status: 'active',
  vsamStatus: 'active',
  mqStatus: 'active',
};

import type {
  ResourceMetric,
  ConnectionPoolStats,
  ErrorRateData,
  BatchPipelineStep,
  ErrorLogEntry,
  SystemHealth,
} from "../types";

export const resourceMetrics: ResourceMetric[] = [
  {
    name: "CPU Utilization",
    current: 62,
    threshold_warning: 80,
    threshold_critical: 90,
    unit: "%",
    trend: [
      45, 48, 42, 40, 43, 47, 52, 58, 63, 68, 72, 75, 70, 65, 60, 58, 55,
      53, 57, 62, 66, 64, 60, 62,
    ],
  },
  {
    name: "Memory Usage",
    current: 78,
    threshold_warning: 80,
    threshold_critical: 90,
    unit: "%",
    trend: [
      68, 70, 65, 67, 69, 72, 74, 76, 78, 80, 82, 80, 79, 77, 75, 73, 71,
      70, 72, 74, 76, 78, 77, 78,
    ],
  },
  {
    name: "DASD Utilization",
    current: 45,
    threshold_warning: 80,
    threshold_critical: 90,
    unit: "%",
    trend: [
      42, 42, 43, 43, 44, 44, 45, 45, 46, 47, 48, 47, 46, 45, 44, 43, 44,
      45, 46, 47, 48, 46, 45, 45,
    ],
  },
];

export const connectionPool: ConnectionPoolStats = {
  active: 12,
  idle: 38,
  total: 50,
  maxTotal: 100,
  waitCount: 0,
  avgResponseMs: 23,
  trend: [
    8, 6, 4, 3, 3, 5, 8, 12, 18, 22, 25, 20, 18, 15, 14, 16, 19, 22, 20,
    17, 14, 12, 10, 12,
  ],
};

export const errorRate: ErrorRateData = {
  currentRate: 0.3,
  totalToday: 47,
  totalYesterday: 52,
  byCategory: [
    { category: "DB2 Connection Timeout", count: 3, severity: "critical" },
    { category: "VSAM Read Error", count: 5, severity: "warning" },
    { category: "Transaction Validation", count: 22, severity: "info" },
    { category: "Security Auth Failure", count: 8, severity: "warning" },
    { category: "Batch Step Failure", count: 2, severity: "critical" },
    { category: "Data Format Error", count: 7, severity: "info" },
  ],
  hourlyErrors: [
    1, 0, 0, 1, 0, 1, 2, 3, 4, 5, 3, 2, 3, 4, 2, 1, 2, 3, 2, 2, 1, 1, 2,
    1,
  ],
};

export const batchPipeline: {
  lastRun: string;
  nextScheduled: string;
  steps: BatchPipelineStep[];
} = {
  lastRun: "2024-01-15 02:00:00",
  nextScheduled: "2024-01-16 02:00:00",
  steps: [
    {
      name: "Transaction Validation",
      status: "complete",
      startTime: "2024-01-15 02:00:00",
      endTime: "2024-01-15 02:12:34",
      returnCode: 0,
      recordsProcessed: 15234,
    },
    {
      name: "Position Update",
      status: "complete",
      startTime: "2024-01-15 02:12:35",
      endTime: "2024-01-15 02:28:12",
      returnCode: 0,
      recordsProcessed: 8432,
    },
    {
      name: "History Load",
      status: "complete",
      startTime: "2024-01-15 02:28:13",
      endTime: "2024-01-15 02:45:50",
      returnCode: 0,
      recordsProcessed: 23567,
    },
    {
      name: "Price Reconciliation",
      status: "complete",
      startTime: "2024-01-15 02:45:51",
      endTime: "2024-01-15 02:58:20",
      returnCode: 0,
      recordsProcessed: 4521,
    },
    {
      name: "Position Report",
      status: "complete",
      startTime: "2024-01-15 02:58:21",
      endTime: "2024-01-15 03:10:45",
      returnCode: 0,
      recordsProcessed: 8432,
    },
    {
      name: "Audit Report",
      status: "complete",
      startTime: "2024-01-15 03:10:46",
      endTime: "2024-01-15 03:22:10",
      returnCode: 0,
      recordsProcessed: 31205,
    },
    {
      name: "Statistics Report",
      status: "complete",
      startTime: "2024-01-15 03:22:11",
      endTime: "2024-01-15 03:30:00",
      returnCode: 0,
      recordsProcessed: 1247,
    },
  ],
};

export const errorLog: ErrorLogEntry[] = [
  {
    timestamp: "2024-01-15 14:32:10",
    errorCode: "DB2-SQLCODE-911",
    program: "PRCSEQ00",
    severity: "critical",
    message: "DB2 connection timeout during sequential price update batch",
  },
  {
    timestamp: "2024-01-15 14:28:45",
    errorCode: "VSAM-ERR-035",
    program: "INQPORT",
    severity: "warning",
    message: "VSAM read error on PORTFOLIO-MASTER, record key not found",
  },
  {
    timestamp: "2024-01-15 14:25:12",
    errorCode: "SEC-AUTH-401",
    program: "SECMGR",
    severity: "warning",
    message: "Authentication failure for user OPER003, invalid credentials",
  },
  {
    timestamp: "2024-01-15 14:20:33",
    errorCode: "TRN-VAL-100",
    program: "TRNVAL00",
    severity: "info",
    message: "Transaction validation: duplicate trade reference TRD-20240115-8821",
  },
  {
    timestamp: "2024-01-15 14:15:08",
    errorCode: "DB2-SQLCODE-803",
    program: "INQHIST",
    severity: "critical",
    message: "Duplicate key on insert into TRANSACTION-HISTORY table",
  },
  {
    timestamp: "2024-01-15 14:10:22",
    errorCode: "BATCH-RC-008",
    program: "PRCSEQ00",
    severity: "critical",
    message: "Batch step failed: Price Reconciliation abended with RC=8",
  },
  {
    timestamp: "2024-01-15 14:05:44",
    errorCode: "TRN-VAL-200",
    program: "TRNVAL00",
    severity: "info",
    message: "Data format error: invalid date format in transaction record",
  },
  {
    timestamp: "2024-01-15 13:58:19",
    errorCode: "SEC-AUTH-403",
    program: "SECMGR",
    severity: "warning",
    message: "Unauthorized access attempt to ADMIN-PANEL by user CLERK007",
  },
  {
    timestamp: "2024-01-15 13:50:55",
    errorCode: "VSAM-ERR-028",
    program: "INQPORT",
    severity: "warning",
    message: "VSAM file status 35: file not found during portfolio inquiry",
  },
  {
    timestamp: "2024-01-15 13:45:30",
    errorCode: "TRN-VAL-150",
    program: "TRNVAL00",
    severity: "info",
    message: "Validation warning: trade amount exceeds soft limit threshold",
  },
];

export const systemHealth: SystemHealth = {
  overallStatus: "healthy",
  uptime: "14d 7h 23m",
  lastIncident: "2024-01-12 14:32:00",
  cicsRegionStatus: "active",
  db2Status: "active",
  vsamStatus: "active",
  mqStatus: "active",
};

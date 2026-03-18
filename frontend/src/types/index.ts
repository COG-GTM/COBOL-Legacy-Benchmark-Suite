export interface SummaryMetrics {
  totalAccounts: number;
  totalPositions: number;
  todayTransactions: number;
  systemStatus: string;
}

export interface Transaction {
  date: string;
  account: string;
  type: "BUY" | "SELL" | "XFER" | "FEE";
  fund: string;
  units: number;
  price: number;
  amount: number;
}

export interface SystemStatus {
  status: "operational" | "degraded" | "down";
  lastBatchRun: string;
}

export interface ErrorBannerProps {
  message: string;
  severity: "error" | "warning" | "info" | "success";
  onDismiss: () => void;
}

export interface NavItem {
  label: string;
  path: string;
  icon: string;
  badge?: string;
}

export interface ResourceMetric {
  name: string;
  current: number;
  threshold_warning: number;
  threshold_critical: number;
  unit: string;
  trend: number[];
}

export interface ConnectionPoolStats {
  active: number;
  idle: number;
  total: number;
  maxTotal: number;
  waitCount: number;
  avgResponseMs: number;
  trend: number[];
}

export interface ErrorRateData {
  currentRate: number;
  totalToday: number;
  totalYesterday: number;
  byCategory: {
    category: string;
    count: number;
    severity: "critical" | "warning" | "info";
  }[];
  hourlyErrors: number[];
}

export interface BatchPipelineStep {
  name: string;
  status: "complete" | "running" | "pending" | "error" | "suspended";
  startTime: string | null;
  endTime: string | null;
  returnCode: number | null;
  recordsProcessed: number | null;
}

export interface ErrorLogEntry {
  timestamp: string;
  errorCode: string;
  program: string;
  severity: "critical" | "warning" | "info";
  message: string;
}

export interface SystemHealth {
  overallStatus: "healthy" | "degraded" | "critical";
  uptime: string;
  lastIncident: string;
  cicsRegionStatus: "active" | "inactive";
  db2Status: "active" | "inactive";
  vsamStatus: "active" | "inactive";
  mqStatus: "active" | "inactive";
}

export interface MetricCardProps {
  title: string;
  value: string | number;
  unit?: string;
  trend?: "up" | "down" | "flat";
  trendValue?: string;
  color?: string;
}

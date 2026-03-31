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

// ---------------------------------------------------------------------------
// Batch Job Monitoring types
// ---------------------------------------------------------------------------

export interface BatchStep {
  stepId: string;
  stepName: string;
  phase: "start-of-day" | "main-process" | "end-of-day";
  sequence: number;
  status: "complete" | "running" | "pending" | "failed" | "skipped" | "waiting";
  prerequisite: string | null;
  requiredMaxRC: number | null;
  scheduledStart: string;
  scheduledEnd: string;
  actualStart: string | null;
  actualEnd: string | null;
  returnCode: number | null;
  recordsRead: number | null;
  recordsProcessed: number | null;
  recordsRejected: number | null;
  checkpointCount: number | null;
  lastCheckpoint: string | null;
  errorMessage: string | null;
}

export interface BatchRun {
  runId: string;
  runDate: string;
  scheduledStart: string;
  actualStart: string | null;
  actualEnd: string | null;
  overallStatus: "complete" | "running" | "failed" | "scheduled" | "partial";
  totalRecordsProcessed: number;
  totalErrors: number;
  steps: BatchStep[];
}

export interface PipelineStepDefinition {
  stepId: string;
  stepName: string;
  program: string;
  description: string;
  prerequisite: string | null;
  requiredMaxRC: number | null;
  timeWindow: string;
  checkpointFrequency: string;
  estimatedDuration: string;
}

export interface PipelinePhase {
  name: string;
  description: string;
  steps: PipelineStepDefinition[];
}

export interface PipelineDefinition {
  phases: PipelinePhase[];
}

export interface DependencyEdge {
  from: string;
  to: string;
  condition: string;
}

export interface CheckpointRecord {
  stepId: string;
  checkpointNumber: number;
  timestamp: string;
  recordsProcessedAtCheckpoint: number;
  totalRecords: number;
  status: "saved" | "cleared";
}

export interface ScheduleEntry {
  dayOfWeek: string;
  scheduledTime: string;
  status: "active" | "suspended" | "holiday";
  note: string | null;
}

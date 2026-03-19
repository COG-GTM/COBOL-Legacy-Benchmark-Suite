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
  byCategory: { category: string; count: number; severity: 'critical' | 'warning' | 'info' }[];
  hourlyErrors: number[];
}

export interface BatchPipelineStep {
  name: string;
  status: 'complete' | 'running' | 'pending' | 'error' | 'suspended';
  startTime: string | null;
  endTime: string | null;
  returnCode: number | null;
  recordsProcessed: number | null;
}

export interface ErrorLogEntry {
  timestamp: string;
  errorCode: string;
  program: string;
  severity: 'critical' | 'warning' | 'info';
  message: string;
}

export interface SystemHealth {
  overallStatus: 'healthy' | 'degraded' | 'critical';
  uptime: string;
  lastIncident: string;
  cicsRegionStatus: 'active' | 'inactive';
  db2Status: 'active' | 'inactive';
  vsamStatus: 'active' | 'inactive';
  mqStatus: 'active' | 'inactive';
}

export interface BatchPipelineData {
  steps: BatchPipelineStep[];
  lastRun: string;
  nextScheduled: string;
}

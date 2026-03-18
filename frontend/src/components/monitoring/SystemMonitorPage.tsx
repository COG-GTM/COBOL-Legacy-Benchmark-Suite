import { useState, useEffect, useCallback } from "react";
import { RefreshCw } from "lucide-react";
import type {
  ResourceMetric,
  ConnectionPoolStats,
  ErrorRateData,
  SystemHealth,
  BatchPipelineStep,
  ErrorLogEntry,
} from "../../types";
import {
  resourceMetrics as initialMetrics,
  connectionPool as initialPool,
  errorRate as initialErrorRate,
  systemHealth as initialHealth,
  batchPipeline,
  errorLog,
} from "../../mock/systemMonitorData";
import SystemHealthBanner from "./SystemHealthBanner";
import ResourceGauges from "./ResourceGauges";
import ConnectionPoolCard from "./ConnectionPoolCard";
import ErrorRatePanel from "./ErrorRatePanel";
import BatchPipelineStatus from "./BatchPipelineStatus";
import ErrorLogTable from "./ErrorLogTable";

function clamp(val: number, min: number, max: number) {
  return Math.min(max, Math.max(min, val));
}

function jitter(val: number, range: number, min = 0, max = 100) {
  return clamp(val + (Math.random() * 2 - 1) * range, min, max);
}

function randomizeMetrics(metrics: ResourceMetric[]): ResourceMetric[] {
  return metrics.map((m) => ({
    ...m,
    current: Math.round(jitter(m.current, 3)),
  }));
}

function randomizePool(pool: ConnectionPoolStats): ConnectionPoolStats {
  const active = clamp(
    pool.active + Math.round((Math.random() * 2 - 1) * 2),
    1,
    pool.maxTotal
  );
  const idle = clamp(pool.total - active, 0, pool.maxTotal);
  return {
    ...pool,
    active,
    idle,
    avgResponseMs: Math.max(1, pool.avgResponseMs + Math.round((Math.random() * 2 - 1) * 3)),
  };
}

function randomizeErrorRate(data: ErrorRateData): ErrorRateData {
  return {
    ...data,
    currentRate: Math.round(jitter(data.currentRate, 0.1, 0, 10) * 10) / 10,
    totalToday: Math.max(0, data.totalToday + Math.round((Math.random() * 2 - 1) * 2)),
  };
}

export default function SystemMonitorPage() {
  const [metrics, setMetrics] = useState<ResourceMetric[]>(initialMetrics);
  const [pool, setPool] = useState<ConnectionPoolStats>(initialPool);
  const [errors, setErrors] = useState<ErrorRateData>(initialErrorRate);
  const [health] = useState<SystemHealth>(initialHealth);
  const [steps] = useState<BatchPipelineStep[]>(batchPipeline.steps);
  const [logEntries] = useState<ErrorLogEntry[]>(errorLog);
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [lastUpdated, setLastUpdated] = useState(new Date());

  const refresh = useCallback(() => {
    setMetrics((m) => randomizeMetrics(m));
    setPool((p) => randomizePool(p));
    setErrors((e) => randomizeErrorRate(e));
    setLastUpdated(new Date());
  }, []);

  useEffect(() => {
    if (!autoRefresh) return;
    const id = setInterval(refresh, 30000);
    return () => clearInterval(id);
  }, [autoRefresh, refresh]);

  return (
    <div className="space-y-6">
      {/* Page header */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            System Monitoring Dashboard
          </h1>
          <p className="mt-1 text-sm text-gray-500">
            Last updated: {lastUpdated.toLocaleTimeString()}
          </p>
        </div>
        <div className="flex items-center gap-3">
          <label className="flex cursor-pointer items-center gap-2 text-sm text-gray-600">
            <div className="relative">
              <input
                type="checkbox"
                checked={autoRefresh}
                onChange={(e) => setAutoRefresh(e.target.checked)}
                className="peer sr-only"
              />
              <div className="h-5 w-9 rounded-full bg-gray-300 transition-colors peer-checked:bg-blue-600" />
              <div className="absolute left-0.5 top-0.5 h-4 w-4 rounded-full bg-white shadow transition-transform peer-checked:translate-x-4" />
            </div>
            Auto-refresh
          </label>
          <button
            onClick={refresh}
            className="flex items-center gap-1.5 rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm font-medium text-gray-700 shadow-sm transition-colors hover:bg-gray-50"
          >
            <RefreshCw size={14} />
            Refresh Now
          </button>
        </div>
      </div>

      {/* 1. System Health Banner */}
      <SystemHealthBanner health={health} />

      {/* 2. Resource Gauges */}
      <ResourceGauges metrics={metrics} />

      {/* 3. Connection Pool + Error Rate */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <ConnectionPoolCard pool={pool} />
        <ErrorRatePanel errorData={errors} />
      </div>

      {/* 4. Batch Pipeline Status */}
      <BatchPipelineStatus
        steps={steps}
        lastRun={batchPipeline.lastRun}
        nextScheduled={batchPipeline.nextScheduled}
      />

      {/* 5. Error Log Table */}
      <ErrorLogTable entries={logEntries} />
    </div>
  );
}

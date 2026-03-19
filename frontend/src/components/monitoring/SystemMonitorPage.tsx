import { useState, useEffect, useCallback } from 'react';
import type { ResourceMetric, ConnectionPoolStats, ErrorRateData } from '@/types';
import {
  resourceMetrics as initialMetrics,
  connectionPoolStats as initialPool,
  errorRateData as initialErrors,
  batchPipelineData,
  errorLogEntries,
  systemHealth,
} from '@/mock/systemMonitorData';
import SystemHealthBanner from './SystemHealthBanner';
import ResourceGauges from './ResourceGauges';
import ConnectionPoolCard from './ConnectionPoolCard';
import ErrorRatePanel from './ErrorRatePanel';
import BatchPipelineStatus from './BatchPipelineStatus';
import ErrorLogTable from './ErrorLogTable';
import { RefreshCw } from 'lucide-react';

function randomize(val: number, range: number, min = 0, max = 100): number {
  const delta = (Math.random() - 0.5) * 2 * range;
  return Math.max(min, Math.min(max, Math.round(val + delta)));
}

export default function SystemMonitorPage() {
  const [metrics, setMetrics] = useState<ResourceMetric[]>(initialMetrics);
  const [pool, setPool] = useState<ConnectionPoolStats>(initialPool);
  const [errors, setErrors] = useState<ErrorRateData>(initialErrors);
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [lastUpdated, setLastUpdated] = useState(new Date());

  const refresh = useCallback(() => {
    setMetrics((prev) =>
      prev.map((m) => ({
        ...m,
        current: randomize(m.current, 3, 0, 100),
      }))
    );
    setPool((prev) => {
      const newActive = randomize(prev.active, 1, 0, prev.maxTotal);
      const newIdle = Math.max(0, prev.total - newActive);
      return {
        ...prev,
        active: newActive,
        idle: newIdle,
        avgResponseMs: randomize(prev.avgResponseMs, 2, 5, 200),
      };
    });
    setErrors((prev) => ({
      ...prev,
      currentRate: Math.max(0, +(prev.currentRate + (Math.random() - 0.5) * 0.1).toFixed(1)),
    }));
    setLastUpdated(new Date());
  }, []);

  useEffect(() => {
    if (!autoRefresh) return;
    const id = setInterval(refresh, 30000);
    return () => clearInterval(id);
  }, [autoRefresh, refresh]);

  return (
    <div className="p-4 md:p-6 lg:p-8 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">System Monitoring Dashboard</h1>
          <p className="text-sm text-gray-500 mt-1">
            Last updated: {lastUpdated.toLocaleTimeString()}
          </p>
        </div>
        <div className="flex items-center gap-3 mt-3 sm:mt-0">
          <label className="inline-flex items-center gap-2 text-sm text-gray-600">
            <button
              role="switch"
              aria-checked={autoRefresh}
              aria-label="Toggle auto-refresh"
              onClick={() => setAutoRefresh(!autoRefresh)}
              className={`relative inline-flex h-5 w-9 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 ${
                autoRefresh ? 'bg-blue-600' : 'bg-gray-200'
              }`}
            >
              <span
                className={`pointer-events-none inline-block h-4 w-4 rounded-full bg-white shadow transform ring-0 transition duration-200 ease-in-out ${
                  autoRefresh ? 'translate-x-4' : 'translate-x-0'
                }`}
              />
            </button>
            Auto-refresh
          </label>
          <button
            onClick={refresh}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <RefreshCw className="h-3.5 w-3.5" />
            Refresh Now
          </button>
        </div>
      </div>

      <div className="space-y-6">
        {/* System Health Banner */}
        <SystemHealthBanner health={systemHealth} />

        {/* Resource Gauges */}
        <ResourceGauges metrics={metrics} />

        {/* Connection Pool + Error Rate */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <ConnectionPoolCard stats={pool} />
          <ErrorRatePanel data={errors} />
        </div>

        {/* Batch Pipeline Status */}
        <BatchPipelineStatus data={batchPipelineData} />

        {/* Error Log Table */}
        <ErrorLogTable entries={errorLogEntries} />
      </div>
    </div>
  );
}

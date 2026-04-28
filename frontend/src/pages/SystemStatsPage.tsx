import { mockSystemStats } from '../mocks/mockData';
import { formatNumber } from '../utils/validation';

/**
 * Maps to RPTSTA00 from RPTSTA00.cbl lines 169-170
 * DB2 metrics and batch processing metrics
 */
export function SystemStatsPage() {
  const { db2Metrics, batchMetrics } = mockSystemStats;

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-2">System Statistics</h1>
      <p className="text-sm text-gray-500 mb-6">DB2 and Batch Processing Metrics (RPTSTA00)</p>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 max-w-4xl">
        {/* DB2 Metrics */}
        <div className="bg-white border border-gray-200 rounded-lg p-6">
          <h2 className="text-lg font-semibold mb-4">DB2 Metrics</h2>
          <div className="space-y-3">
            <Stat label="Total Queries" value={formatNumber(db2Metrics.totalQueries, 0)} />
            <Stat label="Avg Response Time" value={`${db2Metrics.avgResponseTimeMs} ms`} />
            <Stat label="Peak Response Time" value={`${db2Metrics.peakResponseTimeMs} ms`} />
            <Stat label="Active Connections" value={db2Metrics.activeConnections.toString()} />
            <Stat label="Deadlock Count" value={db2Metrics.deadlockCount.toString()} />
            <Stat label="Buffer Pool Hit Ratio" value={`${db2Metrics.bufferPoolHitRatio}%`} />
          </div>
        </div>

        {/* Batch Metrics */}
        <div className="bg-white border border-gray-200 rounded-lg p-6">
          <h2 className="text-lg font-semibold mb-4">Batch Processing Metrics</h2>
          <div className="space-y-3">
            <Stat label="Total Jobs Run" value={formatNumber(batchMetrics.totalJobsRun, 0)} />
            <Stat label="Successful Jobs" value={formatNumber(batchMetrics.successfulJobs, 0)} />
            <Stat label="Failed Jobs" value={batchMetrics.failedJobs.toString()} highlight={batchMetrics.failedJobs > 0} />
            <Stat label="Avg Duration" value={`${batchMetrics.avgDurationMinutes} min`} />
            <Stat label="Records Processed" value={formatNumber(batchMetrics.recordsProcessed, 0)} />
            <Stat label="Last Run Date" value={batchMetrics.lastRunDate} />
          </div>
        </div>
      </div>
    </div>
  );
}

function Stat({ label, value, highlight = false }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div className="flex justify-between items-center">
      <span className="text-sm text-gray-600">{label}</span>
      <span className={`text-sm font-mono font-medium ${highlight ? 'text-red-600' : 'text-gray-900'}`}>
        {value}
      </span>
    </div>
  );
}

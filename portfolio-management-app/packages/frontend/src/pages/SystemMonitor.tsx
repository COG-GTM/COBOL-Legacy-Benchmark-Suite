import { useQuery, useMutation } from '@tanstack/react-query';
import { getSystemHealth, validateSystem, runMaintenance } from '../lib/api';
import StatusBadge from '../components/StatusBadge';
import toast from 'react-hot-toast';
import { Activity, Database, Wifi, Clock, AlertTriangle, CheckCircle } from 'lucide-react';

export default function SystemMonitor() {
  const { data: healthData, isLoading } = useQuery({
    queryKey: ['health'],
    queryFn: getSystemHealth,
    refetchInterval: 10000,
  });

  const validateMutation = useMutation({
    mutationFn: validateSystem,
    onSuccess: (data) => {
      const result = data.data;
      if (result?.issueCount === 0) {
        toast.success('Data validation passed — no issues found');
      } else {
        toast.error(`Found ${result?.issueCount} issue(s)`);
      }
    },
    onError: () => toast.error('Validation failed'),
  });

  const maintenanceMutation = useMutation({
    mutationFn: (op: string) => runMaintenance(op),
    onSuccess: () => toast.success('Maintenance operation completed'),
    onError: () => toast.error('Maintenance failed'),
  });

  const health = healthData?.data;

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">System Monitor</h1>

      {/* Health Status — UTLMON00 */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
        <HealthCard
          icon={<Activity className={health?.status === 'healthy' ? 'text-green-500' : 'text-red-500'} />}
          label="System Status"
          value={health?.status || 'unknown'}
          status={health?.status === 'healthy'}
        />
        <HealthCard
          icon={<Database className={health?.database === 'connected' ? 'text-green-500' : 'text-red-500'} />}
          label="Database"
          value={health?.database || 'unknown'}
          status={health?.database === 'connected'}
        />
        <HealthCard
          icon={<Wifi className="text-green-500" />}
          label="WebSocket"
          value={health?.websocket || 'unknown'}
          status={health?.websocket === 'active'}
        />
        <HealthCard
          icon={<Clock className="text-indigo-500" />}
          label="Uptime"
          value={health?.uptime ? formatUptime(health.uptime) : '--'}
          status={true}
        />
      </div>

      {/* System Metrics */}
      {health?.metrics && (
        <div className="bg-white rounded-xl shadow-sm border p-6 mb-6">
          <h2 className="text-lg font-semibold mb-4">System Metrics</h2>
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
            <MetricItem label="Total Portfolios" value={health.metrics.totalPortfolios} />
            <MetricItem label="Active Portfolios" value={health.metrics.activePortfolios} />
            <MetricItem label="Pending Transactions" value={health.metrics.pendingTransactions} highlight={health.metrics.pendingTransactions > 0} />
            <MetricItem label="Total Transactions" value={health.metrics.totalTransactions} />
            <MetricItem label="Batch Jobs Today" value={health.metrics.batchJobsToday} />
            <MetricItem
              label="Last Batch Run"
              value={health.metrics.lastBatchRun ? new Date(health.metrics.lastBatchRun).toLocaleString() : 'Never'}
            />
          </div>
        </div>
      )}

      {/* Actions */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Data Validation — UTLVAL00 */}
        <div className="bg-white rounded-xl shadow-sm border p-6">
          <h2 className="text-lg font-semibold mb-4">Data Validation</h2>
          <p className="text-sm text-gray-500 mb-4">
            Check portfolio value integrity, orphaned transactions, and stale pending records.
          </p>
          <button
            onClick={() => validateMutation.mutate()}
            disabled={validateMutation.isPending}
            className="w-full py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50"
          >
            {validateMutation.isPending ? 'Validating...' : 'Run Validation'}
          </button>

          {validateMutation.data?.data && (
            <div className="mt-4 space-y-2">
              <div className="flex items-center gap-2">
                {validateMutation.data.data.status === 'VALID' ? (
                  <CheckCircle className="text-green-500" size={16} />
                ) : (
                  <AlertTriangle className="text-yellow-500" size={16} />
                )}
                <StatusBadge status={validateMutation.data.data.status === 'VALID' ? 'D' : 'E'} />
              </div>
              {validateMutation.data.data.issues.map((issue, i) => (
                <div key={i} className="text-sm p-3 bg-yellow-50 rounded-lg border border-yellow-200">
                  <span className="font-medium">[{issue.severity}]</span> {issue.message}
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Maintenance — UTLMNT00 */}
        <div className="bg-white rounded-xl shadow-sm border p-6">
          <h2 className="text-lg font-semibold mb-4">Maintenance Operations</h2>
          <div className="space-y-3">
            {[
              { op: 'ARCHIVE', label: 'Archive Transactions', desc: 'Archive completed transactions older than 30 days' },
              { op: 'CLEANUP', label: 'Cleanup Jobs', desc: 'Remove failed batch jobs older than 30 days' },
              { op: 'ANALYZE', label: 'Analyze Database', desc: 'Collect table counts and statistics' },
            ].map(({ op, label, desc }) => (
              <button
                key={op}
                onClick={() => maintenanceMutation.mutate(op)}
                disabled={maintenanceMutation.isPending}
                className="w-full text-left p-3 border rounded-lg hover:bg-gray-50 transition-colors disabled:opacity-50"
              >
                <span className="font-medium text-sm">{label}</span>
                <p className="text-xs text-gray-500">{desc}</p>
              </button>
            ))}
          </div>
        </div>
      </div>

      {isLoading && (
        <div className="text-center py-12 text-gray-500">Loading system health...</div>
      )}
    </div>
  );
}

function HealthCard({ icon, label, value, status }: {
  icon: React.ReactNode; label: string; value: string; status: boolean;
}) {
  return (
    <div className={`bg-white rounded-xl shadow-sm border p-4 ${status ? '' : 'ring-2 ring-red-200'}`}>
      <div className="flex items-center gap-3 mb-2">{icon}<span className="text-sm text-gray-500">{label}</span></div>
      <p className="text-lg font-bold capitalize">{value}</p>
    </div>
  );
}

function MetricItem({ label, value, highlight }: { label: string; value: number | string; highlight?: boolean }) {
  return (
    <div className={`p-3 rounded-lg ${highlight ? 'bg-yellow-50 ring-1 ring-yellow-200' : 'bg-gray-50'}`}>
      <p className="text-xs text-gray-500">{label}</p>
      <p className="text-lg font-bold mt-1">{typeof value === 'number' ? value.toLocaleString() : value}</p>
    </div>
  );
}

function formatUptime(seconds: number): string {
  const d = Math.floor(seconds / 86400);
  const h = Math.floor((seconds % 86400) / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  if (d > 0) return `${d}d ${h}h ${m}m`;
  if (h > 0) return `${h}h ${m}m`;
  return `${m}m`;
}

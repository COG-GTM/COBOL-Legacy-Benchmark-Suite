import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { runBatch, getBatchStatus } from '../lib/api';
import StatusBadge from '../components/StatusBadge';
import toast from 'react-hot-toast';

export default function BatchOperations() {
  const queryClient = useQueryClient();
  const [jobName, setJobName] = useState('BATCHRUN');

  const { data: statusData, isLoading } = useQuery({
    queryKey: ['batch', 'status'],
    queryFn: getBatchStatus,
    refetchInterval: 5000,
  });

  const batchMutation = useMutation({
    mutationFn: () => runBatch({ jobName }),
    onSuccess: (data) => {
      toast.success(
        `Batch complete: ${data.data?.recordsWritten} processed, ${data.data?.errorCount} errors`
      );
      queryClient.invalidateQueries({ queryKey: ['batch'] });
      queryClient.invalidateQueries({ queryKey: ['portfolios'] });
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
    },
    onError: (err: { response?: { data?: { error?: { message?: string } } } }) => {
      toast.error(err.response?.data?.error?.message || 'Batch failed');
    },
  });

  const jobs = statusData?.data ?? [];

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">Batch Operations</h1>

      {/* Batch Control Panel — BCHCTL00 */}
      <div className="bg-white rounded-xl shadow-sm border p-6 mb-6">
        <h2 className="text-lg font-semibold mb-4">Run Batch Processing</h2>
        <p className="text-sm text-gray-500 mb-4">
          Executes the full batch cycle: validate pending transactions, update positions, and load history.
        </p>
        <div className="flex gap-4 items-end">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Job Name</label>
            <input
              type="text"
              value={jobName}
              onChange={(e) => setJobName(e.target.value.toUpperCase())}
              maxLength={8}
              className="px-3 py-2 border rounded-lg focus:ring-2 focus:ring-indigo-500"
            />
          </div>
          <button
            onClick={() => batchMutation.mutate()}
            disabled={batchMutation.isPending}
            className="px-6 py-2 bg-indigo-600 text-white rounded-lg font-medium hover:bg-indigo-700 disabled:opacity-50"
          >
            {batchMutation.isPending ? 'Running...' : 'Start Batch'}
          </button>
        </div>

        {batchMutation.isPending && (
          <div className="mt-4">
            <div className="flex items-center gap-3">
              <div className="animate-spin h-5 w-5 border-2 border-indigo-600 border-t-transparent rounded-full" />
              <span className="text-sm text-gray-600">Batch processing in progress...</span>
            </div>
            <div className="mt-2 w-full bg-gray-200 rounded-full h-2">
              <div className="bg-indigo-600 h-2 rounded-full animate-pulse" style={{ width: '60%' }} />
            </div>
          </div>
        )}
      </div>

      {/* Batch Job History */}
      <div className="bg-white rounded-xl shadow-sm border overflow-hidden">
        <div className="px-6 py-4 border-b">
          <h2 className="text-lg font-semibold">Recent Batch Jobs</h2>
        </div>
        <table className="w-full">
          <thead className="bg-gray-50 border-b">
            <tr>
              <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Job Name</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Date</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Step</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Status</th>
              <th className="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase">Read</th>
              <th className="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase">Written</th>
              <th className="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase">Errors</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Duration</th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {isLoading ? (
              <tr><td colSpan={8} className="px-4 py-8 text-center text-gray-500">Loading...</td></tr>
            ) : jobs.length === 0 ? (
              <tr><td colSpan={8} className="px-4 py-8 text-center text-gray-500">No batch jobs found</td></tr>
            ) : jobs.map((job) => (
              <tr key={job.id} className="hover:bg-gray-50">
                <td className="px-4 py-3 font-medium">{job.jobName}</td>
                <td className="px-4 py-3 text-sm">{new Date(job.processDate).toLocaleDateString()}</td>
                <td className="px-4 py-3 text-sm text-gray-500">{job.stepName || '-'}</td>
                <td className="px-4 py-3"><StatusBadge status={job.status} /></td>
                <td className="px-4 py-3 text-right">{job.recordsRead}</td>
                <td className="px-4 py-3 text-right">{job.recordsWritten}</td>
                <td className={`px-4 py-3 text-right ${job.errorCount > 0 ? 'text-red-600 font-medium' : ''}`}>{job.errorCount}</td>
                <td className="px-4 py-3 text-sm text-gray-500">{formatDuration(job.startTime, job.endTime)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function formatDuration(start: string | null, end: string | null): string {
  if (!start) return '-';
  if (!end) return 'Running...';
  const ms = new Date(end).getTime() - new Date(start).getTime();
  if (ms < 1000) return `${ms}ms`;
  return `${(ms / 1000).toFixed(1)}s`;
}

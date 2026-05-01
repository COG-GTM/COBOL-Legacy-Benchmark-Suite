// Admin Panel (replaces UTLMNT00, UTLMON00, UTLVAL00)
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../lib/api';

interface Job {
  id: string;
  type: string;
  status: string;
  progress: number;
  result: unknown;
  error: string | null;
  startedAt: string;
  completedAt: string | null;
}

export default function Admin() {
  const queryClient = useQueryClient();
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const { data: jobsData, isLoading } = useQuery({
    queryKey: ['jobs-status'],
    queryFn: () => api.getJobStatus() as Promise<{ data: Job[] }>,
    refetchInterval: 3000,
  });

  const processMutation = useMutation({
    mutationFn: () => api.processTransactions(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jobs-status'] });
      setMessage({ type: 'success', text: 'Transaction processing job started' });
    },
    onError: (err: Error) => setMessage({ type: 'error', text: err.message }),
  });

  const reportMutation = useMutation({
    mutationFn: () => api.generateReports(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jobs-status'] });
      setMessage({ type: 'success', text: 'Report generation job started' });
    },
    onError: (err: Error) => setMessage({ type: 'error', text: err.message }),
  });

  const jobs = jobsData?.data || [];

  const statusColor = (s: string) => {
    const colors: Record<string, string> = {
      RUNNING: 'bg-blue-100 text-blue-800',
      COMPLETED: 'bg-green-100 text-green-800',
      FAILED: 'bg-red-100 text-red-800',
      QUEUED: 'bg-yellow-100 text-yellow-800',
    };
    return colors[s] || 'bg-gray-100 text-gray-800';
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-800">Admin Panel</h1>
        <p className="text-gray-500 mt-1">System monitoring and job management</p>
      </div>

      {message && (
        <div className={`px-4 py-3 rounded-lg text-sm ${message.type === 'success' ? 'bg-green-50 text-green-700 border border-green-200' : 'bg-red-50 text-red-700 border border-red-200'}`}>
          {message.text}
        </div>
      )}

      {/* Job Actions */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="bg-white rounded-lg shadow-sm border p-5">
          <h3 className="font-semibold text-gray-800 mb-2">Process Transactions</h3>
          <p className="text-sm text-gray-500 mb-4">
            Execute the batch pipeline: Validate (TRNVAL00) &rarr; Update Positions (POSUPD00) &rarr; Load History (HISTLD00)
          </p>
          <button
            onClick={() => processMutation.mutate()}
            disabled={processMutation.isPending}
            className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
          >
            {processMutation.isPending ? 'Starting...' : 'Run Transaction Processing'}
          </button>
        </div>

        <div className="bg-white rounded-lg shadow-sm border p-5">
          <h3 className="font-semibold text-gray-800 mb-2">Generate Reports</h3>
          <p className="text-sm text-gray-500 mb-4">
            Generate position reports (RPTPOS00), audit reports (RPTAUD00), and statistics (RPTSTA00)
          </p>
          <button
            onClick={() => reportMutation.mutate()}
            disabled={reportMutation.isPending}
            className="bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 transition-colors disabled:opacity-50"
          >
            {reportMutation.isPending ? 'Starting...' : 'Generate Reports'}
          </button>
        </div>
      </div>

      {/* System Information */}
      <div className="bg-white rounded-lg shadow-sm border p-5">
        <h3 className="font-semibold text-gray-800 mb-4">System Information</h3>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
          <div>
            <p className="text-gray-500">Platform</p>
            <p className="font-medium">Node.js + Express</p>
          </div>
          <div>
            <p className="text-gray-500">Database</p>
            <p className="font-medium">PostgreSQL + Prisma</p>
          </div>
          <div>
            <p className="text-gray-500">Frontend</p>
            <p className="font-medium">React + TypeScript</p>
          </div>
          <div>
            <p className="text-gray-500">Original System</p>
            <p className="font-medium">COBOL / CICS / DB2</p>
          </div>
        </div>
      </div>

      {/* Job History */}
      <div className="bg-white rounded-lg shadow-sm border">
        <div className="p-4 border-b">
          <h3 className="font-semibold text-gray-800">Job History</h3>
        </div>
        {isLoading ? (
          <div className="flex justify-center py-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600" />
          </div>
        ) : jobs.length === 0 ? (
          <div className="p-8 text-center text-gray-500">No jobs have been run yet</div>
        ) : (
          <div className="divide-y">
            {jobs.map((job) => (
              <div key={job.id} className="p-4 flex items-center justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-medium text-gray-800">{job.type}</span>
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${statusColor(job.status)}`}>
                      {job.status}
                    </span>
                  </div>
                  <p className="text-xs text-gray-500 mt-1">
                    Started: {new Date(job.startedAt).toLocaleString()}
                    {job.completedAt && ` | Completed: ${new Date(job.completedAt).toLocaleString()}`}
                  </p>
                  {job.error && <p className="text-xs text-red-600 mt-1">{job.error}</p>}
                  {job.result != null && <p className="text-xs text-gray-600 mt-1">{String(JSON.stringify(job.result))}</p>}
                </div>
                {job.status === 'RUNNING' && (
                  <div className="w-32">
                    <div className="bg-gray-200 rounded-full h-2">
                      <div className="bg-blue-600 h-2 rounded-full transition-all" style={{ width: `${job.progress}%` }} />
                    </div>
                    <p className="text-xs text-gray-500 text-center mt-1">{job.progress}%</p>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

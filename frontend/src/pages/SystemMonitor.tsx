import { Activity } from 'lucide-react';

export default function SystemMonitor() {
  return (
    <div className="flex flex-col items-center justify-center py-20 text-center">
      <div className="h-16 w-16 rounded-2xl bg-cyan-50 flex items-center justify-center mb-6">
        <Activity className="h-8 w-8 text-cyan-600" />
      </div>
      <h1 className="text-2xl font-bold text-gray-900 mb-3">System Monitoring</h1>
      <p className="text-gray-500 max-w-md">
        Coming soon &mdash; this page will display real-time system health metrics and monitoring dashboards.
      </p>
    </div>
  );
}

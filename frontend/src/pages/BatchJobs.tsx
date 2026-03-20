import { Server } from 'lucide-react';

export default function BatchJobs() {
  return (
    <div className="flex flex-col items-center justify-center py-20 text-center">
      <div className="h-16 w-16 rounded-2xl bg-orange-50 flex items-center justify-center mb-6">
        <Server className="h-8 w-8 text-orange-600" />
      </div>
      <h1 className="text-2xl font-bold text-gray-900 mb-3">Batch Job Monitoring</h1>
      <p className="text-gray-500 max-w-md">
        Coming soon &mdash; this page will allow you to monitor and manage batch processing jobs.
      </p>
    </div>
  );
}

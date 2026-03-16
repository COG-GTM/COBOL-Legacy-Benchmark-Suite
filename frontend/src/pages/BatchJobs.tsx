import { Cog } from "lucide-react";

export default function BatchJobs() {
  return (
    <div className="flex flex-col items-center justify-center py-20">
      <div className="mb-6 flex h-16 w-16 items-center justify-center rounded-full bg-orange-100">
        <Cog size={32} className="text-orange-600" />
      </div>
      <h1 className="mb-2 text-2xl font-bold text-gray-900">
        Batch Jobs
      </h1>
      <p className="mb-4 text-gray-500">View and manage batch processing jobs</p>
      <span className="rounded-full bg-gray-100 px-4 py-2 text-sm font-medium text-gray-700">
        Coming Soon
      </span>
    </div>
  );
}

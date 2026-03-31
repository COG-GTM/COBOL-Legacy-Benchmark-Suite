import { Clock } from "lucide-react";

export default function TransactionHistory() {
  return (
    <div className="flex flex-col items-center justify-center py-20">
      <div className="mb-6 flex h-16 w-16 items-center justify-center rounded-full bg-emerald-100">
        <Clock size={32} className="text-emerald-600" />
      </div>
      <h1 className="mb-2 text-2xl font-bold text-gray-900">
        Transaction History
      </h1>
      <p className="mb-4 text-gray-500">
        View transaction history for an account
      </p>
      <span className="rounded-full bg-emerald-100 px-4 py-2 text-sm font-medium text-emerald-700">
        Coming Soon
      </span>
      <p className="mt-6 max-w-md text-center text-sm text-gray-400">
        This page will replace the HISMAP screen from the original CICS application,
        providing a searchable and sortable transaction history view.
      </p>
    </div>
  );
}

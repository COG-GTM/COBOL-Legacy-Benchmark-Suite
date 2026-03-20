import { Clock } from 'lucide-react';

export default function TransactionHistory() {
  return (
    <div className="flex flex-col items-center justify-center py-20 text-center">
      <div className="h-16 w-16 rounded-2xl bg-indigo-50 flex items-center justify-center mb-6">
        <Clock className="h-8 w-8 text-indigo-600" />
      </div>
      <h1 className="text-2xl font-bold text-gray-900 mb-3">Transaction History</h1>
      <p className="text-gray-500 max-w-md">
        Coming soon &mdash; this page will display transaction history with filtering by account and date range.
      </p>
    </div>
  );
}

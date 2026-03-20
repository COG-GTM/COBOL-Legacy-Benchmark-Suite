import { recentTransactions } from '@/mock/dashboardData';
import { formatCurrency } from '@/lib/utils';

const typeBadgeStyles: Record<string, string> = {
  BUY: 'bg-blue-100 text-blue-800',
  SELL: 'bg-red-100 text-red-800',
  XFER: 'bg-purple-100 text-purple-800',
  FEE: 'bg-gray-100 text-gray-800',
};

export default function RecentActivity() {
  return (
    <section>
      <h2 className="text-lg font-semibold text-gray-900 mb-4">Recent Activity</h2>
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-200 bg-gray-50">
                <th className="text-left px-4 py-3 font-medium text-gray-500">Date</th>
                <th className="text-left px-4 py-3 font-medium text-gray-500">Account</th>
                <th className="text-left px-4 py-3 font-medium text-gray-500">Type</th>
                <th className="text-left px-4 py-3 font-medium text-gray-500">Fund</th>
                <th className="text-right px-4 py-3 font-medium text-gray-500">Amount</th>
              </tr>
            </thead>
            <tbody>
              {recentTransactions.map((tx, idx) => (
                <tr
                  key={`${tx.date}-${tx.account}-${tx.fund}-${idx}`}
                  className="border-b border-gray-100 last:border-b-0 hover:bg-gray-50 transition-colors"
                >
                  <td className="px-4 py-3 text-gray-600 whitespace-nowrap">{tx.date}</td>
                  <td className="px-4 py-3 font-mono text-gray-700 whitespace-nowrap">{tx.account}</td>
                  <td className="px-4 py-3">
                    <span
                      className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${typeBadgeStyles[tx.type]}`}
                    >
                      {tx.type}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-700">
                    <span className="font-mono text-xs text-gray-500 mr-2">{tx.fund}</span>
                    <span className="hidden sm:inline">{tx.fundName}</span>
                  </td>
                  <td className="px-4 py-3 text-right font-medium text-gray-900 whitespace-nowrap">
                    {formatCurrency(tx.amount)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  );
}

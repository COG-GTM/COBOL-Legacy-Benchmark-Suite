import { recentTransactions } from "../../mock/dashboardData";

const typeBadgeColors: Record<string, string> = {
  BUY: "bg-green-100 text-green-700",
  SELL: "bg-red-100 text-red-700",
  XFER: "bg-blue-100 text-blue-700",
  FEE: "bg-gray-100 text-gray-700",
};

export default function RecentActivity() {
  return (
    <div>
      <h2 className="mb-4 text-lg font-semibold text-gray-900">
        Recent Activity
      </h2>
      <div className="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-gray-200 bg-gray-50">
                <th className="px-4 py-3 font-medium text-gray-600">Date</th>
                <th className="px-4 py-3 font-medium text-gray-600">Account</th>
                <th className="px-4 py-3 font-medium text-gray-600">Type</th>
                <th className="px-4 py-3 font-medium text-gray-600">Fund</th>
                <th className="px-4 py-3 text-right font-medium text-gray-600">Units</th>
                <th className="px-4 py-3 text-right font-medium text-gray-600">Price</th>
                <th className="px-4 py-3 text-right font-medium text-gray-600">Amount</th>
              </tr>
            </thead>
            <tbody>
              {recentTransactions.map((tx, index) => (
                <tr
                  key={`${tx.account}-${tx.date}-${index}`}
                  className="border-b border-gray-100 transition-colors last:border-0 hover:bg-gray-50"
                >
                  <td className="whitespace-nowrap px-4 py-3 text-gray-700">{tx.date}</td>
                  <td className="whitespace-nowrap px-4 py-3 font-mono text-gray-700">{tx.account}</td>
                  <td className="whitespace-nowrap px-4 py-3">
                    <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium ${typeBadgeColors[tx.type]}`}>
                      {tx.type}
                    </span>
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 font-mono text-gray-700">{tx.fund}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-right text-gray-700">
                    {tx.units > 0 ? tx.units.toLocaleString() : "\u2014"}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-right text-gray-700">
                    {tx.price > 0 ? `$${tx.price.toFixed(2)}` : "\u2014"}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-right font-medium text-gray-900">
                    ${tx.amount.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

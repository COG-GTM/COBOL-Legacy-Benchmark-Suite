import { Users, TrendingUp, ArrowRightLeft, CheckCircle } from "lucide-react";
import { summaryMetrics } from "../../mock/dashboardData";

const metrics = [
  {
    label: "Total Accounts",
    value: summaryMetrics.totalAccounts.toLocaleString(),
    icon: <Users size={24} className="text-blue-600" />,
    bgColor: "bg-blue-50",
  },
  {
    label: "Total Positions",
    value: summaryMetrics.totalPositions.toLocaleString(),
    icon: <TrendingUp size={24} className="text-purple-600" />,
    bgColor: "bg-purple-50",
  },
  {
    label: "Today's Transactions",
    value: summaryMetrics.todayTransactions.toLocaleString(),
    icon: <ArrowRightLeft size={24} className="text-amber-600" />,
    bgColor: "bg-amber-50",
  },
  {
    label: "System Status",
    value: summaryMetrics.systemStatus,
    icon: <CheckCircle size={24} className="text-green-600" />,
    bgColor: "bg-green-50",
    isStatus: true,
  },
];

export default function SummaryCards() {
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
      {metrics.map((metric) => (
        <div
          key={metric.label}
          className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm transition-shadow hover:shadow-md"
        >
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500">
                {metric.label}
              </p>
              <div className="mt-1 flex items-center gap-2">
                <p className="text-2xl font-semibold text-gray-900">
                  {metric.value}
                </p>
                {metric.isStatus && (
                  <span className="inline-flex items-center rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-700">
                    <span className="mr-1 h-1.5 w-1.5 rounded-full bg-green-500" />
                    Active
                  </span>
                )}
              </div>
            </div>
            <div
              className={`${metric.bgColor} flex h-12 w-12 items-center justify-center rounded-lg`}
            >
              {metric.icon}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

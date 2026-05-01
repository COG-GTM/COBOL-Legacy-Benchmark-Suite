import type { Portfolio } from "@/types/portfolio";
import { CLIENT_TYPE_LABELS } from "@/types/portfolio";
import CurrencyDisplay from "@/components/common/CurrencyDisplay";
import StatusBadge from "@/components/common/StatusBadge";

interface PortfolioOverviewCardProps {
  portfolio: Portfolio;
}

export default function PortfolioOverviewCard({
  portfolio,
}: PortfolioOverviewCardProps) {
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
      <div className="flex items-start justify-between">
        <div>
          <h3 className="text-sm font-medium text-gray-500">
            {CLIENT_TYPE_LABELS[portfolio.clientType]}
          </h3>
          <p className="mt-1 text-lg font-semibold text-gray-900">
            {portfolio.clientName}
          </p>
        </div>
        <StatusBadge code={portfolio.status} />
      </div>

      <div className="mt-6 grid grid-cols-2 gap-4">
        <div>
          <p className="text-xs font-medium text-gray-500">Total Value</p>
          <CurrencyDisplay
            amount={portfolio.totalValue}
            className="text-xl font-bold text-gray-900"
          />
        </div>
        <div>
          <p className="text-xs font-medium text-gray-500">Cash Balance</p>
          <CurrencyDisplay
            amount={portfolio.cashBalance}
            className="text-xl font-bold text-gray-900"
          />
        </div>
      </div>

      <div className="mt-4 flex items-center gap-4 border-t border-gray-100 pt-4 text-xs text-gray-500">
        <span>
          Account: <span className="font-mono">{portfolio.accountNo}</span>
        </span>
        <span>
          ID: <span className="font-mono">{portfolio.id}</span>
        </span>
      </div>
    </div>
  );
}

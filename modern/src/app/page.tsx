import { MOCK_PORTFOLIOS } from "@/data/mock";
import PortfolioOverviewCard from "@/components/dashboard/PortfolioOverviewCard";
import { Briefcase, DollarSign, TrendingUp, AlertCircle } from "lucide-react";

function SummaryCard({
  label,
  value,
  icon: Icon,
}: {
  label: string;
  value: string;
  icon: React.ComponentType<{ className?: string }>;
}) {
  return (
    <div className="flex items-center gap-4 rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
      <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-indigo-50">
        <Icon className="h-6 w-6 text-indigo-600" />
      </div>
      <div>
        <p className="text-sm text-gray-500">{label}</p>
        <p className="text-xl font-bold text-gray-900">{value}</p>
      </div>
    </div>
  );
}

export default function DashboardPage() {
  const activePortfolios = MOCK_PORTFOLIOS.filter((p) => p.status === "A");
  const totalValue = MOCK_PORTFOLIOS.reduce((sum, p) => sum + p.totalValue, 0);
  const totalCash = MOCK_PORTFOLIOS.reduce((sum, p) => sum + p.cashBalance, 0);
  const suspendedCount = MOCK_PORTFOLIOS.filter((p) => p.status === "S").length;

  const fmt = new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 0,
  });

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">
          Portfolio Management System
        </h1>
        <p className="mt-1 text-sm text-gray-500">
          Dashboard overview of all managed portfolios
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <SummaryCard
          label="Active Portfolios"
          value={String(activePortfolios.length)}
          icon={Briefcase}
        />
        <SummaryCard
          label="Total Value"
          value={fmt.format(totalValue)}
          icon={DollarSign}
        />
        <SummaryCard
          label="Total Cash"
          value={fmt.format(totalCash)}
          icon={TrendingUp}
        />
        <SummaryCard
          label="Suspended"
          value={String(suspendedCount)}
          icon={AlertCircle}
        />
      </div>

      <div>
        <h2 className="mb-4 text-lg font-semibold text-gray-900">
          Portfolio Overview
        </h2>
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2 xl:grid-cols-3">
          {MOCK_PORTFOLIOS.map((portfolio) => (
            <PortfolioOverviewCard key={portfolio.id} portfolio={portfolio} />
          ))}
        </div>
      </div>
    </div>
  );
}

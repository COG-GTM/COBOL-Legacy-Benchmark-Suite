"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { getDashboardSummary } from "@/lib/api";
import { transactionTypeLabel } from "@/types/domain";

function formatCurrency(value: number): string {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 2,
  }).format(value);
}

export default function Dashboard() {
  const { data: summary, isLoading, error } = useQuery({
    queryKey: ["dashboardSummary"],
    queryFn: getDashboardSummary,
  });

  if (isLoading) {
    return (
      <div className="space-y-6">
        <h1 className="text-2xl font-bold text-zinc-900 dark:text-zinc-100">
          Dashboard
        </h1>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {[1, 2, 3].map((i) => (
            <div
              key={i}
              className="h-28 animate-pulse rounded-lg border border-zinc-200 bg-zinc-100 dark:border-zinc-700 dark:bg-zinc-800"
            />
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-lg border border-red-200 bg-red-50 p-4 dark:border-red-800 dark:bg-red-950">
        <p className="text-sm text-red-700 dark:text-red-300">
          Failed to load dashboard data.
        </p>
      </div>
    );
  }

  const totalPortfolios = summary?.totalPortfolios ?? 0;
  const totalValue = summary?.totalValue ?? 0;
  const recentTransactions = summary?.recentTransactions ?? [];

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-zinc-900 dark:text-zinc-100">
        Dashboard
      </h1>

      <p className="text-zinc-600 dark:text-zinc-400">
        Portfolio Management System — Investment positions and transaction
        history.
      </p>

      {/* Summary Cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <SummaryCard
          title="Total Portfolios"
          value={totalPortfolios.toString()}
          href="/portfolios"
        />
        <SummaryCard
          title="Total Market Value"
          value={formatCurrency(totalValue)}
        />
        <SummaryCard
          title="Recent Transactions"
          value={recentTransactions.length.toString()}
        />
      </div>

      {/* Recent Transactions Table */}
      {recentTransactions.length > 0 && (
        <section className="space-y-3">
          <h2 className="text-lg font-semibold text-zinc-900 dark:text-zinc-100">
            Recent Transactions
          </h2>
          <div className="overflow-hidden rounded-lg border border-zinc-200 dark:border-zinc-700">
            <table className="min-w-full text-left text-sm">
              <thead className="bg-zinc-100 dark:bg-zinc-800">
                <tr>
                  <th className="px-4 py-3 font-medium text-zinc-700 dark:text-zinc-300">
                    Date
                  </th>
                  <th className="px-4 py-3 font-medium text-zinc-700 dark:text-zinc-300">
                    Type
                  </th>
                  <th className="px-4 py-3 font-medium text-zinc-700 dark:text-zinc-300">
                    Investment
                  </th>
                  <th className="px-4 py-3 font-medium text-zinc-700 dark:text-zinc-300">
                    Amount
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-100 dark:divide-zinc-700">
                {recentTransactions.map((tx, idx) => (
                  <tr key={`${tx.portfolioId}-${tx.sequenceNo}-${idx}`}>
                    <td className="px-4 py-3 text-zinc-900 dark:text-zinc-100">
                      {tx.date}
                    </td>
                    <td className="px-4 py-3 text-zinc-600 dark:text-zinc-400">
                      {transactionTypeLabel(tx.type)}
                    </td>
                    <td className="px-4 py-3 text-zinc-600 dark:text-zinc-400">
                      {tx.investmentId}
                    </td>
                    <td className="px-4 py-3 font-mono text-zinc-900 dark:text-zinc-100">
                      {formatCurrency(tx.amount)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {/* Quick Navigation */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <NavCard
          title="Portfolios"
          description="View and manage investment portfolios"
          href="/portfolios"
        />
        <NavCard
          title="Position Inquiry"
          description="Search positions within a portfolio"
          href="/portfolios"
        />
        <NavCard
          title="Transaction History"
          description="Review transaction history and audit trail"
          href="/portfolios"
        />
      </div>
    </div>
  );
}

function SummaryCard({
  title,
  value,
  href,
}: {
  title: string;
  value: string;
  href?: string;
}) {
  const content = (
    <div className="rounded-lg border border-zinc-200 bg-white p-6 shadow-sm dark:border-zinc-700 dark:bg-zinc-800">
      <p className="text-sm font-medium text-zinc-500 dark:text-zinc-400">
        {title}
      </p>
      <p className="mt-2 text-2xl font-bold text-zinc-900 dark:text-zinc-100">
        {value}
      </p>
    </div>
  );

  if (href) {
    return (
      <Link href={href} className="transition-shadow hover:shadow-md">
        {content}
      </Link>
    );
  }
  return content;
}

function NavCard({
  title,
  description,
  href,
}: {
  title: string;
  description: string;
  href: string;
}) {
  return (
    <Link
      href={href}
      className="block rounded-lg border border-zinc-200 bg-white p-6 shadow-sm transition-shadow hover:shadow-md dark:border-zinc-700 dark:bg-zinc-800"
    >
      <h3 className="text-lg font-semibold text-zinc-900 dark:text-zinc-100">
        {title}
      </h3>
      <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
        {description}
      </p>
    </Link>
  );
}

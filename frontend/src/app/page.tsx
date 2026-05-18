import Link from "next/link";

export default function DashboardPage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-zinc-900 dark:text-zinc-100">
        Dashboard
      </h1>

      <p className="text-zinc-600 dark:text-zinc-400">
        Welcome to the Portfolio Management System. This application manages
        investment portfolios, positions, and transaction histories.
      </p>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <DashboardCard
          title="Portfolios"
          description="View and manage investment portfolios"
          href="/portfolios"
        />
        <DashboardCard
          title="Positions"
          description="Track holdings across portfolios"
          href="/portfolios"
        />
        <DashboardCard
          title="Transactions"
          description="Review transaction history"
          href="/portfolios"
        />
      </div>
    </div>
  );
}

function DashboardCard({
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
      <h2 className="text-lg font-semibold text-zinc-900 dark:text-zinc-100">
        {title}
      </h2>
      <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
        {description}
      </p>
    </Link>
  );
}

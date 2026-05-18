import Link from "next/link";

export default async function PortfolioDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  return (
    <div className="space-y-6">
      <nav className="text-sm text-zinc-500 dark:text-zinc-400">
        <Link href="/portfolios" className="hover:underline">
          Portfolios
        </Link>{" "}
        / {id}
      </nav>

      <h1 className="text-2xl font-bold text-zinc-900 dark:text-zinc-100">
        Portfolio {id}
      </h1>

      <p className="text-sm text-zinc-500 dark:text-zinc-400">
        Portfolio detail data will be available once the API is connected
        (Wave&nbsp;3).
      </p>

      {/* Positions section */}
      <section className="space-y-3">
        <h2 className="text-lg font-semibold text-zinc-900 dark:text-zinc-100">
          Positions
        </h2>
        <div className="overflow-hidden rounded-lg border border-zinc-200 dark:border-zinc-700">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-zinc-100 dark:bg-zinc-800">
              <tr>
                <th className="px-4 py-3 font-medium text-zinc-700 dark:text-zinc-300">
                  Investment
                </th>
                <th className="px-4 py-3 font-medium text-zinc-700 dark:text-zinc-300">
                  Quantity
                </th>
                <th className="px-4 py-3 font-medium text-zinc-700 dark:text-zinc-300">
                  Cost Basis
                </th>
                <th className="px-4 py-3 font-medium text-zinc-700 dark:text-zinc-300">
                  Market Value
                </th>
                <th className="px-4 py-3 font-medium text-zinc-700 dark:text-zinc-300">
                  Status
                </th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td
                  colSpan={5}
                  className="px-4 py-8 text-center text-zinc-400 dark:text-zinc-500"
                >
                  No positions loaded.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      {/* Transactions section */}
      <section className="space-y-3">
        <h2 className="text-lg font-semibold text-zinc-900 dark:text-zinc-100">
          Transactions
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
                  Units
                </th>
                <th className="px-4 py-3 font-medium text-zinc-700 dark:text-zinc-300">
                  Price
                </th>
                <th className="px-4 py-3 font-medium text-zinc-700 dark:text-zinc-300">
                  Amount
                </th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td
                  colSpan={5}
                  className="px-4 py-8 text-center text-zinc-400 dark:text-zinc-500"
                >
                  No transactions loaded.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

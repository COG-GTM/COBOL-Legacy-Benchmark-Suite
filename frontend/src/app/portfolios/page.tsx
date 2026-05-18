import Link from "next/link";

export default function PortfolioListPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-zinc-900 dark:text-zinc-100">
          Portfolios
        </h1>
        <button
          type="button"
          disabled
          className="rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white opacity-50 dark:bg-zinc-100 dark:text-zinc-900"
        >
          New Portfolio
        </button>
      </div>

      <p className="text-sm text-zinc-500 dark:text-zinc-400">
        Portfolio data will be available once the API is connected (Wave&nbsp;3).
      </p>

      <div className="overflow-hidden rounded-lg border border-zinc-200 dark:border-zinc-700">
        <table className="min-w-full text-left text-sm">
          <thead className="bg-zinc-100 dark:bg-zinc-800">
            <tr>
              <th className="px-4 py-3 font-medium text-zinc-700 dark:text-zinc-300">
                ID
              </th>
              <th className="px-4 py-3 font-medium text-zinc-700 dark:text-zinc-300">
                Account
              </th>
              <th className="px-4 py-3 font-medium text-zinc-700 dark:text-zinc-300">
                Client
              </th>
              <th className="px-4 py-3 font-medium text-zinc-700 dark:text-zinc-300">
                Status
              </th>
              <th className="px-4 py-3 font-medium text-zinc-700 dark:text-zinc-300">
                Total Value
              </th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td
                colSpan={5}
                className="px-4 py-8 text-center text-zinc-400 dark:text-zinc-500"
              >
                No portfolios loaded.{" "}
                <Link href="/" className="underline">
                  Return to dashboard
                </Link>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  );
}

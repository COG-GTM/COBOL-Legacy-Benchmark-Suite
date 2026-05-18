import Link from "next/link";
import PortfolioDetail from "./PortfolioDetail";

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

      <PortfolioDetail portfolioId={id} />
    </div>
  );
}

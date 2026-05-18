import Link from "next/link";

export default function Header() {
  return (
    <header className="flex items-center justify-between border-b border-zinc-200 bg-white px-6 py-3 dark:border-zinc-700 dark:bg-zinc-800">
      <Link href="/" className="text-lg font-semibold text-zinc-900 dark:text-zinc-100">
        Portfolio Management System
      </Link>

      <nav className="flex gap-4 text-sm">
        <Link
          href="/"
          className="text-zinc-600 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-100"
        >
          Dashboard
        </Link>
        <Link
          href="/portfolios"
          className="text-zinc-600 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-100"
        >
          Portfolios
        </Link>
      </nav>
    </header>
  );
}

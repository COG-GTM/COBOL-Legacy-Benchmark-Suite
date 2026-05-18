import Link from "next/link";

const navItems = [
  { href: "/", label: "Dashboard" },
  { href: "/portfolios", label: "Portfolios" },
] as const;

export default function Sidebar() {
  return (
    <aside className="hidden w-56 shrink-0 border-r border-zinc-200 bg-white p-4 dark:border-zinc-700 dark:bg-zinc-800 md:block">
      <nav className="flex flex-col gap-1">
        {navItems.map((item) => (
          <Link
            key={item.href}
            href={item.href}
            className="rounded-md px-3 py-2 text-sm text-zinc-700 hover:bg-zinc-100 dark:text-zinc-300 dark:hover:bg-zinc-700"
          >
            {item.label}
          </Link>
        ))}
      </nav>
    </aside>
  );
}

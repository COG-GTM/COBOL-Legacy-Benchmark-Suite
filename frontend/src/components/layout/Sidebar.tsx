import { useState } from "react";
import { NavLink } from "react-router-dom";
import {
  LayoutDashboard,
  Briefcase,
  Clock,
  BarChart3,
  Cog,
  Activity,
  ChevronLeft,
  ChevronRight,
  Menu,
  X,
} from "lucide-react";

interface NavItemConfig {
  label: string;
  path: string;
  icon: React.ReactNode;
  badge?: string;
}

const navItems: NavItemConfig[] = [
  {
    label: "Dashboard",
    path: "/",
    icon: <LayoutDashboard size={20} />,
  },
  {
    label: "Portfolio Inquiry",
    path: "/portfolio-inquiry",
    icon: <Briefcase size={20} />,
  },
  {
    label: "Transaction History",
    path: "/transaction-history",
    icon: <Clock size={20} />,
  },
  {
    label: "Reports",
    path: "/reports",
    icon: <BarChart3 size={20} />,
    badge: "Coming Soon",
  },
  {
    label: "Batch Jobs",
    path: "/batch-jobs",
    icon: <Cog size={20} />,
    badge: "Coming Soon",
  },
  {
    label: "System Monitor",
    path: "/system-monitor",
    icon: <Activity size={20} />,
  },
];

interface SidebarProps {
  collapsed: boolean;
  onToggle: () => void;
}

export default function Sidebar({ collapsed, onToggle }: SidebarProps) {
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <>
      {/* Mobile hamburger button */}
      <button
        className="fixed top-4 left-4 z-50 rounded-md bg-slate-800 p-2 text-white lg:hidden"
        onClick={() => setMobileOpen(!mobileOpen)}
        aria-label="Toggle navigation"
      >
        {mobileOpen ? <X size={20} /> : <Menu size={20} />}
      </button>

      {/* Mobile overlay */}
      {mobileOpen && (
        <div
          className="fixed inset-0 z-30 bg-black/50 lg:hidden"
          onClick={() => setMobileOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside
        className={`fixed top-0 left-0 z-40 flex h-full flex-col bg-slate-800 text-slate-200 transition-all duration-300 ${
          collapsed ? "w-16" : "w-64"
        } ${mobileOpen ? "translate-x-0" : "-translate-x-full"} lg:translate-x-0`}
      >
        {/* Logo/Title */}
        <div className="flex h-16 items-center gap-3 border-b border-slate-700 px-4">
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-blue-600 text-sm font-bold text-white">
            PM
          </div>
          {!collapsed && (
            <div className="overflow-hidden">
              <p className="truncate text-sm font-semibold text-white">PMS</p>
              <p className="truncate text-xs text-slate-400">
                Portfolio Management System
              </p>
            </div>
          )}
        </div>

        {/* Nav links */}
        <nav className="flex-1 overflow-y-auto px-2 py-4">
          <ul className="space-y-1">
            {navItems.map((item) => (
              <li key={item.path}>
                <NavLink
                  to={item.path}
                  end={item.path === "/"}
                  onClick={() => setMobileOpen(false)}
                  className={({ isActive }) =>
                    `flex items-center gap-3 rounded-md px-3 py-2.5 text-sm font-medium transition-colors ${
                      isActive
                        ? "bg-blue-600 text-white"
                        : "text-slate-300 hover:bg-slate-700 hover:text-white"
                    }`
                  }
                >
                  <span className="shrink-0">{item.icon}</span>
                  {!collapsed && (
                    <>
                      <span className="flex-1 truncate">{item.label}</span>
                      {item.badge && (
                        <span className="rounded-full bg-slate-600 px-2 py-0.5 text-[10px] font-medium text-slate-300">
                          {item.badge}
                        </span>
                      )}
                    </>
                  )}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>

        {/* Collapse toggle (desktop only) */}
        <div className="hidden border-t border-slate-700 p-2 lg:block">
          <button
            onClick={onToggle}
            className="flex w-full items-center justify-center rounded-md p-2 text-slate-400 transition-colors hover:bg-slate-700 hover:text-white"
            aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
          >
            {collapsed ? <ChevronRight size={18} /> : <ChevronLeft size={18} />}
          </button>
        </div>
      </aside>
    </>
  );
}

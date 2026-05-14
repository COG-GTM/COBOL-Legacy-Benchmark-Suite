import { NavLink } from "react-router-dom";
import {
  LayoutDashboard,
  Briefcase,
  ArrowLeftRight,
  TrendingUp,
} from "lucide-react";

const NAV = [
  { to: "/", icon: LayoutDashboard, label: "Dashboard" },
  { to: "/portfolios", icon: Briefcase, label: "Portfolios" },
  { to: "/transactions", icon: ArrowLeftRight, label: "Transactions" },
];

export default function Sidebar() {
  return (
    <aside className="w-64 bg-[#0F172A] text-white flex flex-col shrink-0 min-h-screen">
      <div className="p-6 border-b border-white/10">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-[#22D3EE] flex items-center justify-center">
            <TrendingUp size={20} className="text-[#0F172A]" />
          </div>
          <div>
            <h1 className="text-base font-semibold leading-tight">Portfolio</h1>
            <p className="text-xs text-[#94A3B8]">Investment Manager</p>
          </div>
        </div>
      </div>
      <nav className="flex-1 p-4 space-y-1">
        {NAV.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            end={to === "/"}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                isActive
                  ? "bg-[#22D3EE]/15 text-[#22D3EE]"
                  : "text-[#CBD5E1] hover:bg-white/5 hover:text-white"
              }`
            }
          >
            <Icon size={18} />
            {label}
          </NavLink>
        ))}
      </nav>
      <div className="p-4 border-t border-white/10">
        <p className="text-xs text-[#94A3B8] text-center">
          Modernized from COBOL
        </p>
        <p className="text-[10px] text-[#64748B] text-center mt-1">
          Legacy Benchmark Suite v1.0
        </p>
      </div>
    </aside>
  );
}

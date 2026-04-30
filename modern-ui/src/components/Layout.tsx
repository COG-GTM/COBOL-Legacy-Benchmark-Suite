import { NavLink, Outlet } from 'react-router-dom';
import {
  LayoutDashboard,
  Briefcase,
  ArrowLeftRight,
  PieChart,
  FileText,
  Cog,
  ClipboardList,
  Menu,
  X,
} from 'lucide-react';
import { useState } from 'react';

const NAV_ITEMS = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/portfolios', label: 'Portfolios', icon: Briefcase },
  { to: '/transactions', label: 'Transactions', icon: ArrowLeftRight },
  { to: '/positions', label: 'Positions', icon: PieChart },
  { to: '/reports', label: 'Reports', icon: FileText },
  { to: '/batch-jobs', label: 'Batch Jobs', icon: Cog },
  { to: '/audit-log', label: 'Audit Log', icon: ClipboardList },
];

export default function Layout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="min-h-screen bg-slate-50 flex">
      {sidebarOpen && (
        <div
          className="fixed inset-0 bg-black/30 z-40 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      <aside
        className={`fixed inset-y-0 left-0 z-50 w-64 bg-slate-900 text-white transform transition-transform lg:translate-x-0 lg:static lg:z-auto ${
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="flex items-center gap-3 px-6 py-5 border-b border-slate-700">
          <div className="w-8 h-8 rounded-lg bg-blue-500 flex items-center justify-center font-bold text-sm">
            IP
          </div>
          <div>
            <div className="font-semibold text-sm">IPMS</div>
            <div className="text-[10px] text-slate-400 uppercase tracking-wider">
              Portfolio Manager
            </div>
          </div>
          <button
            className="ml-auto lg:hidden text-slate-400 hover:text-white"
            onClick={() => setSidebarOpen(false)}
          >
            <X size={20} />
          </button>
        </div>

        <nav className="mt-4 px-3 space-y-1">
          {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              onClick={() => setSidebarOpen(false)}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-blue-600 text-white'
                    : 'text-slate-300 hover:bg-slate-800 hover:text-white'
                }`
              }
            >
              <Icon size={18} />
              {label}
            </NavLink>
          ))}
        </nav>

        <div className="absolute bottom-0 left-0 right-0 p-4 border-t border-slate-700">
          <div className="text-[10px] text-slate-500 uppercase tracking-wider">
            Modernized from COBOL
          </div>
          <div className="text-xs text-slate-400 mt-1">
            COBOL Legacy Benchmark Suite
          </div>
        </div>
      </aside>

      <div className="flex-1 flex flex-col min-w-0">
        <header className="bg-white border-b border-slate-200 px-4 py-3 flex items-center gap-4 lg:px-6">
          <button
            className="lg:hidden text-slate-600 hover:text-slate-900"
            onClick={() => setSidebarOpen(true)}
          >
            <Menu size={24} />
          </button>
          <h1 className="text-lg font-semibold text-slate-800">
            Investment Portfolio Management System
          </h1>
          <div className="ml-auto flex items-center gap-3">
            <span className="text-xs bg-emerald-100 text-emerald-700 px-2.5 py-1 rounded-full font-medium">
              System Online
            </span>
            <span className="text-xs text-slate-500">
              {new Date().toLocaleDateString('en-US', {
                weekday: 'short',
                year: 'numeric',
                month: 'short',
                day: 'numeric',
              })}
            </span>
          </div>
        </header>

        <main className="flex-1 p-4 lg:p-6 overflow-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

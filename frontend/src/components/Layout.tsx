import { NavLink, Outlet } from 'react-router-dom';
import { BarChart3, Briefcase, ArrowLeftRight, FileText, Activity } from 'lucide-react';

const NAV = [
  { to: '/', icon: BarChart3, label: 'Dashboard' },
  { to: '/portfolios', icon: Briefcase, label: 'Portfolios' },
  { to: '/transactions', icon: ArrowLeftRight, label: 'Transactions' },
  { to: '/reports', icon: FileText, label: 'Reports' },
  { to: '/admin', icon: Activity, label: 'Admin' },
];

export default function Layout() {
  return (
    <div className="flex h-screen overflow-hidden">
      {/* Sidebar */}
      <aside className="w-64 bg-slate-800 border-r border-slate-700 flex flex-col">
        <div className="p-5 border-b border-slate-700">
          <h1 className="text-lg font-bold text-blue-400 flex items-center gap-2">
            <BarChart3 size={22} />
            Portfolio Manager
          </h1>
          <p className="text-xs text-slate-400 mt-1">Modernized from COBOL/CICS</p>
        </div>
        <nav className="flex-1 p-3 space-y-1">
          {NAV.map(({ to, icon: Icon, label }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/'}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-blue-600/20 text-blue-400'
                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-700/50'
                }`
              }
            >
              <Icon size={18} />
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="p-4 border-t border-slate-700 text-xs text-slate-500">
          v2.0 — CLBS Migration
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 overflow-y-auto bg-slate-900">
        <div className="p-8 max-w-7xl mx-auto">
          <Outlet />
        </div>
      </main>
    </div>
  );
}

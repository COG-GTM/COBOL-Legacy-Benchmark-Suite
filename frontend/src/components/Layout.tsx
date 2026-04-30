import { NavLink, Outlet } from 'react-router-dom';
import {
  LayoutDashboard,
  Search,
  History,
  MonitorCog,
  TrendingUp,
  Menu,
  X,
} from 'lucide-react';
import { useState } from 'react';
import LiveTicker from './LiveTicker';

const navItems = [
  { to: '/', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/portfolio', icon: Search, label: 'Portfolio Inquiry' },
  { to: '/transactions', icon: History, label: 'Transaction History' },
  { to: '/batch', icon: MonitorCog, label: 'Batch Monitor' },
];

export default function Layout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="min-h-screen flex flex-col">
      <LiveTicker />

      <div className="flex flex-1">
        {/* Mobile menu button */}
        <button
          onClick={() => setSidebarOpen(!sidebarOpen)}
          className="lg:hidden fixed top-12 left-3 z-50 p-2 rounded-lg bg-surface-dark text-white shadow-lg"
          aria-label="Toggle sidebar"
        >
          {sidebarOpen ? <X size={20} /> : <Menu size={20} />}
        </button>

        {/* Sidebar */}
        <aside
          className={`
            fixed lg:sticky top-0 left-0 z-40 h-screen w-64
            bg-surface-dark text-text-dark flex flex-col
            transition-transform duration-300 ease-in-out
            ${sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
          `}
        >
          <div className="p-6 border-b border-border-dark">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-lg bg-accent-1 flex items-center justify-center">
                <TrendingUp size={20} className="text-surface-dark" />
              </div>
              <div>
                <h1 className="font-bold text-sm tracking-wide">IPMS</h1>
                <p className="text-[11px] text-text-muted tracking-wider uppercase">
                  Portfolio Manager
                </p>
              </div>
            </div>
          </div>

          <nav className="flex-1 p-4 space-y-1">
            {navItems.map(item => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/'}
                onClick={() => setSidebarOpen(false)}
                className={({ isActive }) =>
                  `flex items-center gap-3 px-4 py-2.5 rounded-lg text-sm font-medium transition-all duration-200 ${
                    isActive
                      ? 'bg-accent-1/15 text-accent-1'
                      : 'text-text-muted hover:text-text-dark hover:bg-white/5'
                  }`
                }
              >
                <item.icon size={18} />
                {item.label}
              </NavLink>
            ))}
          </nav>

          <div className="p-4 border-t border-border-dark">
            <div className="px-4 py-3 rounded-lg bg-surface-dark-secondary">
              <p className="text-[11px] text-text-muted uppercase tracking-wider mb-1">System</p>
              <div className="flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-gain animate-pulse-glow" />
                <span className="text-xs text-gain">All Systems Online</span>
              </div>
            </div>
          </div>
        </aside>

        {/* Overlay for mobile */}
        {sidebarOpen && (
          <div
            className="fixed inset-0 bg-black/50 z-30 lg:hidden"
            onClick={() => setSidebarOpen(false)}
          />
        )}

        {/* Main content */}
        <main className="flex-1 min-w-0 lg:pt-0 pt-8">
          <div className="p-6 lg:p-8 max-w-[1400px] mx-auto">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}

import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Briefcase,
  Clock,
  BarChart3,
  Server,
  Activity,
  X,
} from 'lucide-react';

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

const navItems = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/portfolio-inquiry', label: 'Portfolio Inquiry', icon: Briefcase },
  { to: '/transaction-history', label: 'Transaction History', icon: Clock },
  { to: '/reports', label: 'Reports', icon: BarChart3, badge: 'Coming Soon' },
  { to: '/batch-jobs', label: 'Batch Jobs', icon: Server, badge: 'Coming Soon' },
  { to: '/system-monitor', label: 'System Monitor', icon: Activity, badge: 'Coming Soon' },
];

export default function Sidebar({ isOpen, onClose }: SidebarProps) {
  return (
    <>
      {/* Mobile overlay */}
      {isOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/50 md:hidden"
          onClick={onClose}
        />
      )}

      {/* Sidebar */}
      <aside
        className={`fixed top-0 left-0 z-50 h-full w-64 bg-sidebar-background text-sidebar-foreground flex flex-col transition-transform duration-300 ease-in-out md:translate-x-0 md:static md:z-auto ${
          isOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {/* Logo / Title */}
        <div className="flex items-center justify-between px-4 py-5 border-b border-sidebar-border">
          <div>
            <h1 className="text-lg font-bold text-white tracking-wide">PMS</h1>
            <p className="text-xs text-slate-400">Portfolio Management System</p>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded hover:bg-sidebar-accent md:hidden"
            aria-label="Close sidebar"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Navigation */}
        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              onClick={onClose}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 rounded-md text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-sidebar-primary text-sidebar-primary-foreground'
                    : 'text-slate-300 hover:bg-sidebar-accent hover:text-sidebar-accent-foreground'
                }`
              }
            >
              <item.icon className="h-5 w-5 shrink-0" />
              <span className="flex-1">{item.label}</span>
              {item.badge && (
                <span className="text-[10px] font-medium bg-slate-600 text-slate-300 px-1.5 py-0.5 rounded-full">
                  {item.badge}
                </span>
              )}
            </NavLink>
          ))}
        </nav>

        {/* Footer */}
        <div className="px-4 py-3 border-t border-sidebar-border">
          <p className="text-xs text-slate-500">COBOL Legacy Modernization</p>
          <p className="text-xs text-slate-500">Phase 1 — Dashboard</p>
        </div>
      </aside>
    </>
  );
}

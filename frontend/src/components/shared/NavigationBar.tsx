import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Search,
  History,
  Briefcase,
  ArrowRightLeft,
  FileBarChart,
  Activity,
  LogOut,
  Menu,
  X,
} from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/context/useAuth';
import { cn } from '@/utils/cn';

const navItems = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/portfolio-inquiry', label: 'Position Inquiry', icon: Search },
  { to: '/transaction-history', label: 'Transaction History', icon: History },
  { to: '/portfolio-management', label: 'Portfolio Management', icon: Briefcase },
  { to: '/transaction-processing', label: 'Transaction Processing', icon: ArrowRightLeft },
  { to: '/reports', label: 'Reports', icon: FileBarChart },
  { to: '/batch-status', label: 'Batch Status', icon: Activity },
];

export function NavigationBar() {
  const { user, logout } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <>
      <nav className="bg-primary-dark text-white">
        <div className="flex items-center justify-between px-4 py-3">
          <div className="flex items-center gap-3">
            <Briefcase className="h-6 w-6" />
            <span className="text-lg font-semibold hidden sm:inline">Portfolio Management System</span>
            <span className="text-lg font-semibold sm:hidden">PMS</span>
          </div>
          <div className="flex items-center gap-4">
            {user && (
              <span className="text-sm text-blue-200 hidden md:inline">
                {user.username} ({user.role})
              </span>
            )}
            <button
              className="md:hidden p-1 hover:bg-primary rounded"
              onClick={() => setMobileOpen(!mobileOpen)}
              aria-label="Toggle menu"
            >
              {mobileOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
            </button>
          </div>
        </div>
        <div className={cn('md:flex md:px-4 md:pb-1 border-t border-primary', mobileOpen ? 'block' : 'hidden')}>
          <div className="flex flex-col md:flex-row md:gap-1 overflow-x-auto">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                onClick={() => setMobileOpen(false)}
                className={({ isActive }) =>
                  cn(
                    'flex items-center gap-2 px-3 py-2 text-sm rounded-t-md transition-colors whitespace-nowrap',
                    isActive
                      ? 'bg-white text-primary-dark font-medium'
                      : 'text-blue-200 hover:bg-primary hover:text-white'
                  )
                }
              >
                <item.icon className="h-4 w-4" />
                {item.label}
              </NavLink>
            ))}
            <button
              onClick={logout}
              className="flex items-center gap-2 px-3 py-2 text-sm text-blue-200 hover:bg-primary hover:text-white rounded-t-md transition-colors md:ml-auto"
            >
              <LogOut className="h-4 w-4" />
              Logout
            </button>
          </div>
        </div>
      </nav>
    </>
  );
}

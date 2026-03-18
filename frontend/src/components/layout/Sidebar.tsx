import { NavLink, useLocation } from 'react-router-dom';
import { LayoutDashboard, Briefcase, History, FileBarChart, X, ChevronDown } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import { useState } from 'react';

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

interface NavItem {
  to: string;
  label: string;
  icon: typeof LayoutDashboard;
  children?: { to: string; label: string }[];
}

const navItems: NavItem[] = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/portfolio', label: 'Portfolio Inquiry', icon: Briefcase },
  { to: '/transactions', label: 'Transaction History', icon: History },
  {
    to: '/reports',
    label: 'Reports',
    icon: FileBarChart,
    children: [
      { to: '/reports/positions', label: 'Position Reports' },
      { to: '/reports/audit', label: 'Audit Reports' },
      { to: '/reports/statistics', label: 'Statistics Reports' },
    ],
  },
];

export function Sidebar({ isOpen, onClose }: SidebarProps) {
  const location = useLocation();
  const [reportsOpen, setReportsOpen] = useState(location.pathname.startsWith('/reports'));

  return (
    <>
      {isOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/50 md:hidden"
          onClick={onClose}
          aria-hidden="true"
        />
      )}
      <aside
        className={cn(
          'fixed left-0 top-16 z-50 h-[calc(100vh-4rem)] w-64 border-r border-[#334155] bg-[#0F172A] transition-transform duration-200 md:sticky md:translate-x-0',
          isOpen ? 'translate-x-0' : '-translate-x-full'
        )}
      >
        <div className="flex items-center justify-between p-4 md:hidden">
          <span className="text-sm font-semibold text-[#94A3B8]">Navigation</span>
          <Button variant="ghost" size="icon" onClick={onClose} aria-label="Close navigation menu">
            <X className="h-4 w-4" />
          </Button>
        </div>
        <nav aria-label="Main navigation" className="space-y-1 p-3">
          <div className="mb-3 px-3 text-xs font-semibold uppercase tracking-wider text-[#94A3B8]">
            Main Menu
          </div>
          {navItems.map((item) => {
            if (item.children) {
              const isActive = location.pathname.startsWith(item.to);
              return (
                <div key={item.to}>
                  <button
                    onClick={() => setReportsOpen((prev) => !prev)}
                    className={cn(
                      'flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
                      isActive
                        ? 'bg-[#22D3EE]/10 text-[#22D3EE]'
                        : 'text-[#CBD5E1] hover:bg-[#1E293B] hover:text-white'
                    )}
                  >
                    <item.icon className="h-5 w-5 shrink-0" />
                    {item.label}
                    <ChevronDown
                      className={cn(
                        'ml-auto h-4 w-4 transition-transform',
                        reportsOpen && 'rotate-180'
                      )}
                    />
                  </button>
                  {reportsOpen && (
                    <div className="ml-8 mt-1 space-y-0.5">
                      {item.children.map((child) => (
                        <NavLink
                          key={child.to}
                          to={child.to}
                          onClick={onClose}
                          className={({ isActive: childActive }) =>
                            cn(
                              'block rounded-md px-3 py-2 text-xs font-medium transition-colors',
                              childActive
                                ? 'text-[#22D3EE]'
                                : 'text-[#94A3B8] hover:text-white'
                            )
                          }
                        >
                          {child.label}
                        </NavLink>
                      ))}
                    </div>
                  )}
                </div>
              );
            }
            return (
              <NavLink
                key={item.to}
                to={item.to}
                onClick={onClose}
                className={({ isActive: navActive }) =>
                  cn(
                    'flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
                    navActive
                      ? 'bg-[#22D3EE]/10 text-[#22D3EE]'
                      : 'text-[#CBD5E1] hover:bg-[#1E293B] hover:text-white'
                  )
                }
                end={item.to === '/'}
              >
                <item.icon className="h-5 w-5 shrink-0" />
                {item.label}
              </NavLink>
            );
          })}
        </nav>
      </aside>
    </>
  );
}

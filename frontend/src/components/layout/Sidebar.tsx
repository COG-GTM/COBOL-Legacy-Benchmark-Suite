import { NavLink } from 'react-router-dom';
import { LayoutDashboard, Briefcase, History, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

const navItems = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/portfolio', label: 'Portfolio Inquiry', icon: Briefcase },
  { to: '/transactions', label: 'Transaction History', icon: History },
];

export function Sidebar({ isOpen, onClose }: SidebarProps) {
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
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              onClick={onClose}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-[#22D3EE]/10 text-[#22D3EE]'
                    : 'text-[#CBD5E1] hover:bg-[#1E293B] hover:text-white'
                )
              }
              end={item.to === '/'}
            >
              <item.icon className="h-5 w-5 shrink-0" />
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>
    </>
  );
}

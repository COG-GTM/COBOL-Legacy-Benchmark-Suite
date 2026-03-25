import { useAuth } from '@/context/AuthContext';
import { LogOut, Menu, Building2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useNavigate } from 'react-router-dom';

interface HeaderProps {
  onMenuToggle?: () => void;
}

export function Header({ onMenuToggle }: HeaderProps) {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <header className="sticky top-0 z-40 border-b border-[#334155] bg-[#0F172A]">
      <div className="flex h-16 items-center justify-between px-4 md:px-6">
        <div className="flex items-center gap-3">
          {isAuthenticated && (
            <Button
              variant="ghost"
              size="icon"
              className="md:hidden"
              onClick={onMenuToggle}
              aria-label="Toggle navigation menu"
            >
              <Menu className="h-5 w-5" />
            </Button>
          )}
          <div className="flex items-center gap-2">
            <Building2 className="h-6 w-6 text-[#22D3EE]" />
            <h1 className="text-lg font-semibold text-white">
              CLBS <span className="hidden text-[#94A3B8] sm:inline">Portfolio Management</span>
            </h1>
          </div>
        </div>
        {isAuthenticated && user && (
          <div className="flex items-center gap-4">
            <span className="hidden text-sm text-[#CBD5E1] md:inline">
              {user.displayName}
            </span>
            <Button variant="ghost" size="sm" onClick={handleLogout} aria-label="Sign out">
              <LogOut className="mr-2 h-4 w-4" />
              Sign Out
            </Button>
          </div>
        )}
      </div>
    </header>
  );
}

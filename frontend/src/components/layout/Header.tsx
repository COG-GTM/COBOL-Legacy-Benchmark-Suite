import { useLocation } from 'react-router-dom';
import { Menu, Bell, LogOut, User, ChevronRight } from 'lucide-react';

interface HeaderProps {
  onMenuToggle: () => void;
}

const routeLabels: Record<string, string> = {
  '/': 'Dashboard',
  '/portfolio-inquiry': 'Portfolio Inquiry',
  '/transaction-history': 'Transaction History',
  '/reports': 'Reports',
  '/batch-jobs': 'Batch Jobs',
  '/system-monitor': 'System Monitor',
};

export default function Header({ onMenuToggle }: HeaderProps) {
  const location = useLocation();
  const currentLabel = routeLabels[location.pathname] ?? 'Page';

  return (
    <header className="sticky top-0 z-30 flex items-center justify-between h-16 px-4 md:px-6 bg-white border-b border-gray-200">
      {/* Left: hamburger + breadcrumb */}
      <div className="flex items-center gap-3">
        <button
          onClick={onMenuToggle}
          className="p-2 rounded-md hover:bg-gray-100 md:hidden"
          aria-label="Toggle menu"
        >
          <Menu className="h-5 w-5 text-gray-600" />
        </button>

        <nav className="flex items-center text-sm text-gray-500">
          <span>Home</span>
          <ChevronRight className="h-4 w-4 mx-1" />
          <span className="font-medium text-gray-900">{currentLabel}</span>
        </nav>
      </div>

      {/* Right: user controls */}
      <div className="flex items-center gap-2">
        <button
          className="relative p-2 rounded-md hover:bg-gray-100"
          aria-label="Notifications"
        >
          <Bell className="h-5 w-5 text-gray-500" />
          <span className="absolute top-1.5 right-1.5 h-2 w-2 bg-red-500 rounded-full" />
        </button>

        <div className="flex items-center gap-2 ml-2 pl-3 border-l border-gray-200">
          <div className="h-8 w-8 rounded-full bg-blue-600 flex items-center justify-center">
            <User className="h-4 w-4 text-white" />
          </div>
          <span className="hidden sm:block text-sm font-medium text-gray-700">USER001</span>
        </div>

        <button
          className="p-2 rounded-md hover:bg-gray-100 text-gray-500"
          aria-label="Logout"
          title="Exit"
        >
          <LogOut className="h-5 w-5" />
        </button>
      </div>
    </header>
  );
}

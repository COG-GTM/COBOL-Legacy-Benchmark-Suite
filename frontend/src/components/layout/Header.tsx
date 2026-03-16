import { useLocation } from "react-router-dom";
import { Bell, LogOut, User, ChevronRight } from "lucide-react";

const routeNames: Record<string, string> = {
  "/": "Dashboard",
  "/portfolio-inquiry": "Portfolio Inquiry",
  "/transaction-history": "Transaction History",
  "/reports": "Reports",
  "/batch-jobs": "Batch Jobs",
  "/system-monitor": "System Monitor",
};

export default function Header() {
  const location = useLocation();
  const currentPage = routeNames[location.pathname] || "Page Not Found";

  return (
    <header className="flex h-16 items-center justify-between border-b border-gray-200 bg-white px-6 pl-16 lg:pl-6">
      {/* Breadcrumb */}
      <nav className="flex items-center gap-2 text-sm">
        <span className="text-gray-400">Home</span>
        <ChevronRight size={14} className="text-gray-400" />
        <span className="font-medium text-gray-700">{currentPage}</span>
      </nav>

      {/* User area */}
      <div className="flex items-center gap-4">
        {/* Notifications */}
        <button
          className="relative rounded-md p-2 text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600"
          aria-label="Notifications"
        >
          <Bell size={20} />
          <span className="absolute right-1.5 top-1.5 h-2 w-2 rounded-full bg-red-500" />
        </button>

        {/* User info */}
        <div className="flex items-center gap-3 border-l border-gray-200 pl-4">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-100 text-blue-600">
            <User size={16} />
          </div>
          <span className="hidden text-sm font-medium text-gray-700 sm:block">
            USER001
          </span>
        </div>

        {/* Logout */}
        <button
          className="flex items-center gap-2 rounded-md px-3 py-2 text-sm text-gray-500 transition-colors hover:bg-gray-100 hover:text-gray-700"
          aria-label="Logout"
        >
          <LogOut size={16} />
          <span className="hidden sm:block">Logout</span>
        </button>
      </div>
    </header>
  );
}

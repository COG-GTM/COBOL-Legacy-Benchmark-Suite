import { ReactNode } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { 
  LayoutDashboard, 
  Briefcase, 
  ArrowRightLeft, 
  History, 
  FileText, 
  Shield, 
  LogOut,
  Menu
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useState } from 'react';

interface LayoutProps {
  children: ReactNode;
  onLogout: () => void;
  token: string | null;
}

const navItems = [
  { path: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { path: '/portfolios', label: 'Portfolios', icon: Briefcase },
  { path: '/transactions', label: 'Transactions', icon: ArrowRightLeft },
  { path: '/history', label: 'History', icon: History },
  { path: '/reports', label: 'Reports', icon: FileText },
  { path: '/audit', label: 'Audit Log', icon: Shield },
];

export default function Layout({ children, onLogout }: LayoutProps) {
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(true);

  return (
    <div className="min-h-screen bg-gray-100">
      <nav className="bg-blue-900 text-white shadow-lg fixed w-full z-10">
        <div className="px-4 py-3 flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <Button
              variant="ghost"
              size="icon"
              onClick={() => setSidebarOpen(!sidebarOpen)}
              className="text-white hover:bg-blue-800"
            >
              <Menu className="h-6 w-6" />
            </Button>
            <div className="flex items-center space-x-2">
              <Briefcase className="h-8 w-8" />
              <span className="text-xl font-bold">Portfolio Management System</span>
            </div>
          </div>
          <div className="flex items-center space-x-4">
            <span className="text-sm text-blue-200">COBOL to Java Migration</span>
            <Button
              variant="ghost"
              onClick={onLogout}
              className="text-white hover:bg-blue-800"
            >
              <LogOut className="h-5 w-5 mr-2" />
              Logout
            </Button>
          </div>
        </div>
      </nav>

      <div className="flex pt-14">
        <aside
          className={`${
            sidebarOpen ? 'w-64' : 'w-16'
          } bg-white shadow-lg fixed h-full transition-all duration-300 overflow-hidden`}
        >
          <nav className="mt-4">
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = location.pathname === item.path || 
                              (item.path !== '/dashboard' && location.pathname.startsWith(item.path));
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  className={`flex items-center px-4 py-3 text-gray-700 hover:bg-blue-50 hover:text-blue-900 transition-colors ${
                    isActive ? 'bg-blue-100 text-blue-900 border-r-4 border-blue-900' : ''
                  }`}
                >
                  <Icon className="h-5 w-5 min-w-5" />
                  {sidebarOpen && <span className="ml-3">{item.label}</span>}
                </Link>
              );
            })}
          </nav>
        </aside>

        <main
          className={`flex-1 p-6 transition-all duration-300 ${
            sidebarOpen ? 'ml-64' : 'ml-16'
          }`}
        >
          {children}
        </main>
      </div>
    </div>
  );
}

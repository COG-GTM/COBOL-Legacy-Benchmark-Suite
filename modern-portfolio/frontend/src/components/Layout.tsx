import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const navItems = [
  { to: '/', label: 'Dashboard', icon: '\u2302' },
  { to: '/inquiry', label: 'Portfolio Inquiry', icon: '\uD83D\uDD0D' },
  { to: '/transactions', label: 'Transaction History', icon: '\uD83D\uDCCA' },
  { to: '/manage', label: 'Portfolio Management', icon: '\u2699' },
  { to: '/reports', label: 'Reports', icon: '\uD83D\uDCC8' },
  { to: '/admin', label: 'Admin', icon: '\uD83D\uDD27' },
];

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-slate-800 text-white shadow-lg">
        <div className="max-w-7xl mx-auto px-4">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center space-x-2">
              <span className="text-xl font-bold">Portfolio Management System</span>
            </div>
            <div className="flex items-center space-x-4">
              <span className="text-sm text-gray-300">{user?.username || 'User'} ({user?.role || 'READ'})</span>
              <button
                onClick={handleLogout}
                className="text-sm bg-slate-700 hover:bg-slate-600 px-3 py-1 rounded transition-colors"
              >
                Logout
              </button>
            </div>
          </div>
        </div>
      </nav>

      <div className="flex">
        <aside className="w-56 bg-white shadow-md min-h-[calc(100vh-4rem)]">
          <nav className="py-4">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/'}
                className={({ isActive }) =>
                  `flex items-center px-6 py-3 text-sm transition-colors ${
                    isActive
                      ? 'bg-blue-50 text-blue-700 border-r-4 border-blue-700 font-medium'
                      : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                  }`
                }
              >
                <span className="mr-3">{item.icon}</span>
                {item.label}
              </NavLink>
            ))}
          </nav>
        </aside>

        <main className="flex-1 p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

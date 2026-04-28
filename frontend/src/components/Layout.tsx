import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

const navItems = [
  { to: '/', label: 'Dashboard', icon: '>' },
  { to: '/positions', label: 'Position Inquiry', icon: '>' },
  { to: '/history', label: 'Transaction History', icon: '>' },
  { to: '/portfolios', label: 'Portfolio Management', icon: '>' },
  { to: '/transactions/new', label: 'Transaction Entry', icon: '>' },
  { to: '/reports', label: 'Reports', icon: '>' },
];

export function Layout() {
  const { userId, role, logout } = useAuth();

  return (
    <div className="min-h-screen bg-gray-50 flex">
      {/* Sidebar */}
      <aside className="w-64 bg-gray-900 text-white flex flex-col">
        <div className="p-4 border-b border-gray-700">
          <h1 className="text-lg font-bold">Portfolio Management</h1>
          <p className="text-xs text-gray-400 mt-1">COBOL Legacy System</p>
        </div>
        <nav className="flex-1 py-4">
          {navItems.map(({ to, label }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/'}
              className={({ isActive }) =>
                `block px-4 py-2 text-sm transition-colors ${
                  isActive
                    ? 'bg-blue-600 text-white font-medium'
                    : 'text-gray-300 hover:bg-gray-800 hover:text-white'
                }`
              }
            >
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="p-4 border-t border-gray-700">
          <p className="text-xs text-gray-400">User: {userId}</p>
          <p className="text-xs text-gray-400">Role: {role === 'portfolio-manager' ? 'Manager' : 'Read-Only'}</p>
          <button
            onClick={logout}
            className="mt-2 w-full text-sm bg-gray-700 hover:bg-gray-600 text-white py-1.5 px-3 rounded transition-colors"
          >
            Exit (Logout)
          </button>
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 overflow-auto">
        <Outlet />
      </main>
    </div>
  );
}

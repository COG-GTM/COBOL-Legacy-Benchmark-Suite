import { Outlet, NavLink } from 'react-router-dom';

const navItems = [
  { to: '/', label: 'Dashboard', icon: '📊' },
  { to: '/portfolios', label: 'Portfolios', icon: '💼' },
  { to: '/inquiry', label: 'Inquiry', icon: '🔍' },
  { to: '/history', label: 'History', icon: '📜' },
  { to: '/batch', label: 'Batch Operations', icon: '⚙️' },
  { to: '/reports', label: 'Reports', icon: '📈' },
  { to: '/monitor', label: 'System Monitor', icon: '🖥️' },
];

export default function Layout() {
  return (
    <div className="flex h-screen bg-gray-100">
      <aside className="w-64 bg-gray-900 text-white flex flex-col">
        <div className="p-4 border-b border-gray-700">
          <h1 className="text-xl font-bold">CLBS Portfolio</h1>
          <p className="text-xs text-gray-400 mt-1">Investment Management</p>
        </div>
        <nav className="flex-1 p-4 space-y-1">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition-colors ${
                  isActive
                    ? 'bg-blue-600 text-white'
                    : 'text-gray-300 hover:bg-gray-800 hover:text-white'
                }`
              }
            >
              <span>{item.icon}</span>
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="p-4 border-t border-gray-700">
          <div className="text-xs text-gray-400">Logged in as admin</div>
        </div>
      </aside>
      <main className="flex-1 overflow-auto">
        <div className="p-6">
          <Outlet />
        </div>
      </main>
    </div>
  );
}

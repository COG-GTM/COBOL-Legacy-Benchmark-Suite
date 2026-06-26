import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { SessionTimeoutModal } from './SessionTimeoutModal';

/**
 * Authenticated application shell: header with identity + logout, primary nav,
 * and the routed content area. Navigation entries are gated by role so
 * read-only users do not see admin-only destinations.
 */
export function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="brand">CLBS Portfolio Management</div>
        <nav className="primary-nav">
          <NavLink to="/dashboard">Dashboard</NavLink>
          {user?.role === 'ADMIN' && <NavLink to="/admin">Administration</NavLink>}
        </nav>
        <div className="user-box">
          <span className="user-name">{user?.displayName}</span>
          <span className={`role-badge role-${user?.role.toLowerCase()}`}>
            {user?.role === 'ADMIN' ? 'Administrator' : 'Read-only'}
          </span>
          <button type="button" className="btn btn-secondary" onClick={handleLogout}>
            Log out
          </button>
        </div>
      </header>
      <main className="app-content">
        <Outlet />
      </main>
      <SessionTimeoutModal />
    </div>
  );
}

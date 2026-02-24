import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

function Header() {
  const { username, role, logout } = useAuth();
  const location = useLocation();

  return (
    <header className="header">
      <div className="header-inner">
        <h1>Portfolio Management System</h1>
        <nav className="header-nav">
          <Link to="/" className={location.pathname === '/' ? 'active' : ''}>
            Dashboard
          </Link>
          <span style={{ color: 'rgba(255,255,255,0.6)', fontSize: '0.8rem' }}>
            {username} ({role})
          </span>
          <button className="btn-logout" onClick={logout}>
            Logout
          </button>
        </nav>
      </div>
    </header>
  );
}

export default Header;

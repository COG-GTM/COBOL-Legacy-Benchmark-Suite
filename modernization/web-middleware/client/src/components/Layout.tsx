import { ReactNode } from 'react';
import { Link, useLocation } from 'react-router-dom';

const links = [
  { to: '/', label: 'Menu' },
  { to: '/position', label: 'Portfolio Position' },
  { to: '/history', label: 'Transaction History' },
];

export default function Layout({ children }: { children: ReactNode }) {
  const { pathname } = useLocation();
  return (
    <div className="app-shell">
      <header className="app-header">
        <span className="brand">OCBC</span>
        <span className="app-title">Portfolio Management System</span>
        <nav>
          {links.map((l) => (
            <Link key={l.to} to={l.to} className={pathname === l.to ? 'nav-link active' : 'nav-link'}>
              {l.label}
            </Link>
          ))}
        </nav>
      </header>
      <main className="app-main">{children}</main>
      <footer className="app-footer">CICS mapset INQSET &middot; PF3=Exit PF7=Previous PF8=Next</footer>
    </div>
  );
}

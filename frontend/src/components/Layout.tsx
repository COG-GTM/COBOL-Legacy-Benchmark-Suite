import { NavLink, Outlet } from 'react-router-dom';

const NAV_ITEMS = [
  { to: '/portfolios', label: 'Portfolios', enabled: true },
  { to: '/positions', label: 'Positions', enabled: true },
  { to: '/transactions', label: 'Transactions', enabled: true },
  { to: '/history', label: 'History', enabled: false },
  { to: '/reports', label: 'Reports', enabled: false },
];

/** App shell: header + primary navigation, with routed content in the outlet. */
export function Layout() {
  return (
    <div className="app">
      <header className="app__header">
        <div className="app__brand">
          <span className="app__logo" aria-hidden="true">
            CLBS
          </span>
          <div>
            <div className="app__title">Portfolio Management</div>
            <div className="app__subtitle">COBOL Legacy Benchmark Suite</div>
          </div>
        </div>
        <nav className="app__nav" aria-label="Primary">
          {NAV_ITEMS.map((item) =>
            item.enabled ? (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  isActive ? 'app__nav-link is-active' : 'app__nav-link'
                }
              >
                {item.label}
              </NavLink>
            ) : (
              <span
                key={item.to}
                className="app__nav-link is-disabled"
                title="Coming soon"
                aria-disabled="true"
              >
                {item.label}
              </span>
            ),
          )}
        </nav>
      </header>
      <main className="app__main">
        <Outlet />
      </main>
    </div>
  );
}

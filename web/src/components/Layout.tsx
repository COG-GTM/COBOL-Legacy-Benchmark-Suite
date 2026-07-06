import { NavLink, Outlet } from 'react-router-dom';

/**
 * Shared application shell with the "Portfolio Management System" header,
 * mirroring the BMS MENMAP title line rendered on every 3270 screen.
 */
export function Layout() {
  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="app-header__title">
          <span className="app-header__mark">CLBS</span>
          <span>Portfolio Management System</span>
        </div>
        <nav className="app-nav">
          <NavLink to="/" end className="app-nav__link">
            Menu
          </NavLink>
          <NavLink to="/positions" className="app-nav__link">
            Position Inquiry
          </NavLink>
          <NavLink to="/transactions" className="app-nav__link">
            Transaction History
          </NavLink>
        </nav>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
      <footer className="app-footer">
        Modernized front end for the COBOL/CICS INQPORT &amp; INQHIST inquiry
        transactions. Data shown is served by an in-browser mock API.
      </footer>
    </div>
  );
}

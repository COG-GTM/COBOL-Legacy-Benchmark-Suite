import { Link, Outlet, useLocation } from "react-router-dom";

// App shell: mirrors the BMS "Portfolio Management System" header (MENMAP) and
// provides navigation to the two inquiry screens (POSMAP / HISMAP).
export default function App() {
  const { pathname } = useLocation();

  return (
    <div className="app">
      <header className="app-header">
        <h1>Portfolio Management System</h1>
        <nav className="app-nav">
          <Link className={pathname === "/" ? "active" : ""} to="/">
            Menu
          </Link>
          <Link
            className={pathname.startsWith("/position") ? "active" : ""}
            to="/position"
          >
            Position Inquiry
          </Link>
          <Link
            className={pathname.startsWith("/history") ? "active" : ""}
            to="/history"
          >
            Transaction History
          </Link>
        </nav>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
      <footer className="app-footer">
        Modern REST/React bridge over the COBOL/CICS INQPORT &amp; INQHIST
        inquiries.
      </footer>
    </div>
  );
}

import { useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { NAV_ITEMS } from '../nav/navigation';
import { useKeyboardShortcuts } from '../hooks/useKeyboardShortcuts';
import { Breadcrumbs } from './Breadcrumbs';

/**
 * Application shell: a consistent header and footer wrap a collapsible primary
 * sidebar and the routed page content. Replaces the legacy MENMAP menu and
 * PF-key navigation from `src/maps/INQSET.bms` with a responsive,
 * desktop-first web layout that remains usable on tablet viewports.
 */
export function AppLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  useKeyboardShortcuts();

  const closeSidebar = () => setSidebarOpen(false);

  return (
    <div className={`app ${sidebarOpen ? 'app--sidebar-open' : ''}`}>
      <header className="app__header">
        <button
          type="button"
          className="app__nav-toggle"
          aria-label="Toggle navigation"
          aria-expanded={sidebarOpen}
          onClick={() => setSidebarOpen((open) => !open)}
        >
          <span aria-hidden="true">☰</span>
        </button>
        <div className="app__brand">
          <span className="app__logo" aria-hidden="true">
            CLBS
          </span>
          <div>
            <div className="app__title">Portfolio Management</div>
            <div className="app__subtitle">COBOL Legacy Benchmark Suite</div>
          </div>
        </div>
        <div className="app__header-spacer" />
        <div className="app__user" aria-label="Signed in user">
          <span className="app__user-avatar" aria-hidden="true">
            AH
          </span>
          <span className="app__user-name">A. Hammett</span>
        </div>
      </header>

      <div className="app__body">
        <aside className="app__sidebar">
          <nav className="app__nav" aria-label="Primary">
            {NAV_ITEMS.map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                end={item.path === '/'}
                className={({ isActive }) =>
                  isActive ? 'app__nav-link is-active' : 'app__nav-link'
                }
                onClick={closeSidebar}
                title={`${item.description} (g ${item.shortcut})`}
              >
                <span className="app__nav-label">{item.label}</span>
                <kbd className="app__nav-kbd">g {item.shortcut}</kbd>
              </NavLink>
            ))}
          </nav>
        </aside>

        <button
          type="button"
          className="app__scrim"
          aria-hidden="true"
          tabIndex={-1}
          onClick={closeSidebar}
        />

        <main className="app__main">
          <Breadcrumbs />
          <div className="app__content">
            <Outlet />
          </div>
        </main>
      </div>

      <footer className="app__footer">
        <span>COBOL Legacy Benchmark Suite — Portfolio Management System</span>
        <span className="app__footer-meta">
          Modernized web UI · Tip: press <kbd>g</kbd> then a section key
        </span>
      </footer>
    </div>
  );
}

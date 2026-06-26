import { Outlet } from 'react-router-dom'
import { useSession } from '../context/sessionContext'
import './AppShell.css'

/**
 * Application shell / layout. Mirrors the persistent framing of the legacy
 * 3270 screens defined in `src/maps/INQSET.bms`: a fixed title banner at the
 * top and a status line at the bottom (the BMS `ERRMSG`/`POSMSG` rows).
 */
export default function AppShell() {
  const { commarea, terminated } = useSession()

  return (
    <div className="app-shell">
      <header className="app-shell__header">
        <h1 className="app-shell__title">Portfolio Management System</h1>
        <span className="app-shell__subtitle">Online Inquiry Subsystem</span>
      </header>

      <main className="app-shell__main">
        <Outlet />
      </main>

      <footer className="app-shell__status" role="status">
        <span className="app-shell__status-item">
          FUNCTION: <strong>{commarea.func}</strong>
        </span>
        <span className="app-shell__status-item">
          RESP: <strong>{commarea.responseCode}</strong>
        </span>
        <span className="app-shell__status-item">
          SESSION: <strong>{terminated ? 'TERMINATED' : 'ACTIVE'}</strong>
        </span>
      </footer>
    </div>
  )
}

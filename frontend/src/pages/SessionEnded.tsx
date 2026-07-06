import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Layout } from '../components/Layout';
import { Routes } from '../routes/functionCodes';

/**
 * Session ended screen, route `/exit`.
 *
 * Mirrors the legacy `EXIT` function code, which executes
 * `SET SESSION-TERMINATED TO TRUE` in `INQONLN.cbl`, ending the CICS pseudo-
 * conversation. Here we conceptually clear any client-held session state and
 * present a terminal "session ended" screen. No backend calls are made.
 */
export function SessionEnded() {
  useEffect(() => {
    // Conceptually mirror clearing the COMMAREA / session state on terminate.
    sessionStorage.clear();
  }, []);

  return (
    <Layout title="Session Ended">
      <p className="session-ended-note" role="status">
        Your session has ended. Thank you for using the Portfolio Management
        System.
      </p>
      <Link className="back-link" to={Routes.MENU}>
        Start a new session
      </Link>
    </Layout>
  );
}

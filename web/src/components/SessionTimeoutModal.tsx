import { useAuth } from '../hooks/useAuth';
import { sessionConfig } from '../config/session';

/**
 * Warns the user that their session is about to expire from inactivity and
 * offers to keep it alive. Mirrors the legacy idle-terminal timeout behaviour.
 */
export function SessionTimeoutModal() {
  const { showTimeoutWarning, keepSessionAlive, logout } = useAuth();

  if (!showTimeoutWarning) return null;

  const warningSeconds = Math.round(sessionConfig.warningMs / 1000);

  return (
    <div className="modal-overlay" role="dialog" aria-modal="true"
      aria-labelledby="timeout-title">
      <div className="modal">
        <h2 id="timeout-title">Session about to expire</h2>
        <p>
          Your session will end in about {warningSeconds} seconds due to
          inactivity. Do you want to stay signed in?
        </p>
        <div className="modal-actions">
          <button type="button" className="btn btn-primary" onClick={keepSessionAlive}>
            Stay signed in
          </button>
          <button type="button" className="btn btn-secondary" onClick={logout}>
            Log out now
          </button>
        </div>
      </div>
    </div>
  );
}

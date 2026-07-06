import { Link } from 'react-router-dom';

/**
 * Not-found view — the web equivalent of the BMS ERRMAP "System Error" screen,
 * shown for unknown routes.
 */
export function NotFoundView() {
  return (
    <section className="card error-view">
      <h1 className="view-title">System Error</h1>
      <p>
        <strong>Error Code:</strong> 00404
      </p>
      <p>
        <strong>Details:</strong> The requested screen does not exist.
      </p>
      <Link to="/" className="btn">
        Return to Menu
      </Link>
    </section>
  );
}

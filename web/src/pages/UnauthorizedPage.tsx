import { Link } from 'react-router-dom';

/** Shown when an authenticated user lacks the role for a resource. */
export function UnauthorizedPage() {
  return (
    <section className="page">
      <h1>Access denied</h1>
      <p>You do not have permission to view this page.</p>
      <Link to="/dashboard">Return to dashboard</Link>
    </section>
  );
}

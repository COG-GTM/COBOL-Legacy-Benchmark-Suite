import { useAuth } from '../hooks/useAuth';
import { sessionConfig } from '../config/session';

/**
 * Landing page for any authenticated user. Placeholder content for the broader
 * frontend epic (MBA-1424); this story (MBA-1425) only establishes auth and
 * session management.
 */
export function DashboardPage() {
  const { user } = useAuth();
  const timeoutMinutes = Math.round(sessionConfig.timeoutMs / 60000);

  return (
    <section className="page">
      <h1>Dashboard</h1>
      <p>
        Welcome, <strong>{user?.displayName}</strong>. You are signed in as a{' '}
        {user?.role === 'ADMIN' ? 'administrator' : 'read-only user'}.
      </p>
      <ul className="info-list">
        <li>User ID: {user?.userId}</li>
        <li>Role: {user?.role}</li>
        <li>Inactivity timeout: {timeoutMinutes} minute(s)</li>
      </ul>
      <p className="muted">
        Portfolio, transaction, and reporting screens are delivered in
        subsequent stories of the frontend modernization epic.
      </p>
    </section>
  );
}

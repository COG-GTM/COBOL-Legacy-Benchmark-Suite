import { useState } from 'react';
import type { FormEvent } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

interface LocationState {
  from?: string;
}

/**
 * Login screen. Modern replacement for the SECMGR-gated CICS sign-on. Collects
 * a user id / password, delegates to the auth context, and surfaces validation
 * failures and session-timeout notices.
 */
export function LoginPage() {
  const { login, lastLogoutReason } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [userId, setUserId] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const result = await login(userId, password);
      if (result.ok) {
        const from = (location.state as LocationState | null)?.from;
        navigate(from && from !== '/login' ? from : '/dashboard', {
          replace: true,
        });
      } else if (result.reason === 'EMPTY_INPUT') {
        setError('Enter both a user ID and password.');
      } else {
        setError('Invalid user ID or password.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="login-page">
      <form className="login-card" onSubmit={handleSubmit} noValidate>
        <h1>Portfolio Management</h1>
        <p className="login-subtitle">Sign in to continue</p>

        {lastLogoutReason === 'timeout' && (
          <div className="alert alert-warning" role="status">
            Your session expired due to inactivity. Please sign in again.
          </div>
        )}

        {error && (
          <div className="alert alert-error" role="alert">
            {error}
          </div>
        )}

        <label htmlFor="userId">User ID</label>
        <input
          id="userId"
          name="userId"
          type="text"
          autoComplete="username"
          maxLength={8}
          value={userId}
          onChange={(e) => setUserId(e.target.value)}
          disabled={submitting}
        />

        <label htmlFor="password">Password</label>
        <input
          id="password"
          name="password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          disabled={submitting}
        />

        <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
          {submitting ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
    </div>
  );
}

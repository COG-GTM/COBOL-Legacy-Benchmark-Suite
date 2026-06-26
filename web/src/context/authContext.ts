import { createContext } from 'react';
import type { AuthResult, AuthUser } from '../types/auth';

/** Reason the previous session ended, surfaced on the login screen. */
export type LogoutReason = 'manual' | 'timeout';

export interface AuthContextValue {
  /** Currently authenticated user, or null when logged out. */
  user: AuthUser | null;
  /** Convenience flag derived from {@link user}. */
  isAuthenticated: boolean;
  /** Attempt to authenticate; on success the session is established. */
  login: (userId: string, password: string) => Promise<AuthResult>;
  /** Clear all session state. */
  logout: () => void;
  /** Reason the last session ended (cleared after the user logs in again). */
  lastLogoutReason: LogoutReason | null;
  /** True while within the pre-timeout warning window. */
  showTimeoutWarning: boolean;
  /** Dismiss the timeout warning and keep the session alive. */
  keepSessionAlive: () => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

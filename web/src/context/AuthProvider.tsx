import { useCallback, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import type { AuthResult, AuthUser } from '../types/auth';
import { authService } from '../services/authService';
import { recordAuditEvent } from '../services/auditService';
import { sessionConfig } from '../config/session';
import { useIdleTimer } from '../hooks/useIdleTimer';
import {
  AuthContext,
  type AuthContextValue,
  type LogoutReason,
} from './authContext';

const STORAGE_KEY = 'clbs.auth.user';
const SECMGR_PROGRAM = 'SECMGR';

function loadStoredUser(): AuthUser | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as AuthUser) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(loadStoredUser);
  const [lastLogoutReason, setLastLogoutReason] = useState<LogoutReason | null>(
    null,
  );
  const [showTimeoutWarning, setShowTimeoutWarning] = useState(false);

  const persist = useCallback((next: AuthUser | null) => {
    setUser(next);
    try {
      if (next) sessionStorage.setItem(STORAGE_KEY, JSON.stringify(next));
      else sessionStorage.removeItem(STORAGE_KEY);
    } catch {
      // Storage may be unavailable (e.g. private mode); session stays in memory.
    }
  }, []);

  const endSession = useCallback(
    (reason: LogoutReason) => {
      setShowTimeoutWarning(false);
      if (user) {
        // Mirrors SECMGR audit write for a logout event (AUD-LOGOUT).
        recordAuditEvent({
          userId: user.userId,
          action: 'LOGOUT',
          status: reason === 'timeout' ? 'WARN' : 'SUCC',
          message:
            reason === 'timeout'
              ? 'Session expired due to inactivity'
              : 'User logged out',
          program: SECMGR_PROGRAM,
        });
      }
      persist(null);
      setLastLogoutReason(reason);
    },
    [user, persist],
  );

  const login = useCallback(
    async (userId: string, password: string): Promise<AuthResult> => {
      const result = await authService.authenticate(userId, password);
      if (result.ok) {
        setLastLogoutReason(null);
        setShowTimeoutWarning(false);
        persist(result.user);
        // Mirrors SECMGR successful validation audit write (AUD-LOGIN / SUCC).
        recordAuditEvent({
          userId: result.user.userId,
          action: 'LOGIN',
          status: 'SUCC',
          message: 'User authenticated',
          program: SECMGR_PROGRAM,
        });
      } else if (result.reason === 'INVALID_CREDENTIALS') {
        // Mirrors SECMGR validation failure audit write (AUD-FAILURE).
        recordAuditEvent({
          userId: userId.trim() || 'ANON',
          action: 'LOGIN',
          status: 'FAIL',
          message: 'Failed login attempt: invalid credentials',
          program: SECMGR_PROGRAM,
        });
      }
      return result;
    },
    [persist],
  );

  const logout = useCallback(() => endSession('manual'), [endSession]);

  const keepSessionAlive = useCallback(() => setShowTimeoutWarning(false), []);

  useIdleTimer({
    timeoutMs: sessionConfig.timeoutMs,
    warningMs: sessionConfig.warningMs,
    enabled: user !== null,
    onWarning: () => setShowTimeoutWarning(true),
    onIdle: () => endSession('timeout'),
    onActivity: () => setShowTimeoutWarning(false),
  });

  // Keep session in sync across tabs: a logout in one tab clears the others.
  useEffect(() => {
    const onStorage = (event: StorageEvent) => {
      if (event.key === STORAGE_KEY && event.newValue === null) {
        setUser(null);
      }
    };
    window.addEventListener('storage', onStorage);
    return () => window.removeEventListener('storage', onStorage);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: user !== null,
      login,
      logout,
      lastLogoutReason,
      showTimeoutWarning,
      keepSessionAlive,
    }),
    [user, login, logout, lastLogoutReason, showTimeoutWarning, keepSessionAlive],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

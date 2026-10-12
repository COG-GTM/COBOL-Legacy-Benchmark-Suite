import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { api as defaultApi, apiConfig, type PortfolioApi } from '../api/client';
import { ApiRequestError } from '../api/errors';
import type { ApiError, ErrorSeverity, SessionInfo } from '../api/types';
import { INQUIRY_PERMISSION } from '../api/types';

export type SessionStatus = 'loading' | 'ready' | 'unauthenticated' | 'denied' | 'signedOut' | 'error';

export interface FlashMessage {
  text: string;
  severity: ErrorSeverity;
  traceId?: string;
}

interface SessionContextValue {
  api: PortfolioApi;
  status: SessionStatus;
  session: SessionInfo | null;
  sessionError: ApiError | null;
  canInquire: boolean;
  /** Replaces INQCOM-ACCOUNT-NO: the last valid account searched, carried between S2 and S3 (FR-008). */
  selectedAccountNo: string | null;
  setSelectedAccountNo: (accountNo: string | null) => void;
  /** Message handed to the menu's message area (legacy ERRMSG) after a RETURN action (FR-006, FR-029). */
  flash: FlashMessage | null;
  setFlash: (flash: FlashMessage | null) => void;
  markUnauthenticated: () => void;
  endSession: () => void;
}

const SessionContext = createContext<SessionContextValue | null>(null);

export function hasInquiryPermission(session: SessionInfo | null): boolean {
  return !!session?.permissions.some(
    (p) => p.resource === INQUIRY_PERMISSION.resource && p.accessType === INQUIRY_PERMISSION.accessType,
  );
}

export function SessionProvider({ children, api = defaultApi }: { children: ReactNode; api?: PortfolioApi }) {
  const [status, setStatus] = useState<SessionStatus>('loading');
  const [session, setSession] = useState<SessionInfo | null>(null);
  const [sessionError, setSessionError] = useState<ApiError | null>(null);
  const [selectedAccountNo, setSelectedAccountNo] = useState<string | null>(null);
  const [flash, setFlash] = useState<FlashMessage | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    api
      .getSession(controller.signal)
      .then((info) => {
        setSession(info);
        setStatus('ready');
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) return;
        if (err instanceof ApiRequestError && err.status === 401) {
          setStatus('unauthenticated');
          return;
        }
        if (err instanceof ApiRequestError && err.status === 403) {
          setStatus('denied');
          return;
        }
        if (err instanceof ApiRequestError) setSessionError(err.body);
        setStatus('error');
      });
    return () => controller.abort();
  }, [api]);

  const clearClientState = useCallback(() => {
    setSession(null);
    setSelectedAccountNo(null);
    setFlash(null);
    setSessionError(null);
  }, []);

  const markUnauthenticated = useCallback(() => {
    clearClientState();
    setStatus('unauthenticated');
  }, [clearClientState]);

  const endSession = useCallback(() => {
    clearClientState();
    setStatus('signedOut');
    if (apiConfig.logoutUrl) window.location.assign(apiConfig.logoutUrl);
  }, [clearClientState]);

  const value = useMemo<SessionContextValue>(
    () => ({
      api,
      status,
      session,
      sessionError,
      canInquire: hasInquiryPermission(session),
      selectedAccountNo,
      setSelectedAccountNo,
      flash,
      setFlash,
      markUnauthenticated,
      endSession,
    }),
    [api, status, session, sessionError, selectedAccountNo, flash, markUnauthenticated, endSession],
  );

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession(): SessionContextValue {
  const ctx = useContext(SessionContext);
  if (!ctx) throw new Error('useSession must be used inside <SessionProvider>');
  return ctx;
}

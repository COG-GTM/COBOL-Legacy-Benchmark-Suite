import { createContext, useContext } from 'react';
import type { InqCom, InqFunction } from '../types';

/**
 * Context shape for the inquiry session. Kept in its own module (no components)
 * so React Fast Refresh stays happy and the hook can be imported freely.
 */
export interface SessionContextValue {
  comm: InqCom;
  userId: string | null;
  isAuthenticated: boolean;
  /** Set the active function code (MENU/INQP/INQH/EXIT). */
  setFunction: (fn: InqFunction) => void;
  /** Update the account number carried in the COMMAREA. */
  setAccountNo: (accountNo: string) => void;
  /** Record an error message + response code (mirrors INQCOM-ERROR-MSG). */
  setError: (errorMsg: string, responseCode?: number) => void;
  /** Clear any error message / response code. */
  clearError: () => void;
  /** SECMGR sign-on. */
  signOn: (userId: string) => void;
  /** SECMGR sign-off; resets the COMMAREA. */
  signOff: () => void;
}

export const SessionContext = createContext<SessionContextValue | null>(null);

export function useSession(): SessionContextValue {
  const ctx = useContext(SessionContext);
  if (!ctx) {
    throw new Error('useSession must be used within a SessionProvider');
  }
  return ctx;
}

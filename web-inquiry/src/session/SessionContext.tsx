import {
  useCallback,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import type { InqCom, InqFunction } from '../types';
import {
  SessionContext,
  type SessionContextValue,
} from './sessionContextValue';

/**
 * Provides client-side session state for the inquiry subsystem.
 *
 * `comm` is the analog of the INQCOM COMMAREA carried between CICS programs:
 * current function code, the account number in context, the last response code,
 * and the current error message. `userId` is the SECMGR-style signed-on user.
 */
const EMPTY_COMM: InqCom = {
  function: 'MENU',
  accountNo: '',
  responseCode: 0,
  errorMsg: '',
};

export function SessionProvider({ children }: { children: ReactNode }) {
  const [comm, setComm] = useState<InqCom>(EMPTY_COMM);
  const [userId, setUserId] = useState<string | null>(null);

  const setFunction = useCallback((fn: InqFunction) => {
    setComm((prev) => ({ ...prev, function: fn }));
  }, []);

  const setAccountNo = useCallback((accountNo: string) => {
    setComm((prev) => ({ ...prev, accountNo }));
  }, []);

  const setError = useCallback((errorMsg: string, responseCode = 0) => {
    setComm((prev) => ({ ...prev, errorMsg, responseCode }));
  }, []);

  const clearError = useCallback(() => {
    setComm((prev) => ({ ...prev, errorMsg: '', responseCode: 0 }));
  }, []);

  const signOn = useCallback((id: string) => {
    setUserId(id);
    setComm({ ...EMPTY_COMM });
  }, []);

  const signOff = useCallback(() => {
    setUserId(null);
    setComm({ ...EMPTY_COMM });
  }, []);

  const value = useMemo<SessionContextValue>(
    () => ({
      comm,
      userId,
      isAuthenticated: userId !== null,
      setFunction,
      setAccountNo,
      setError,
      clearError,
      signOn,
      signOff,
    }),
    [comm, userId, setFunction, setAccountNo, setError, clearError, signOn, signOff],
  );

  return <SessionContext value={value}>{children}</SessionContext>;
}

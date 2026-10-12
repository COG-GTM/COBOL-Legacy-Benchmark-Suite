import { useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiRequestError, normaliseApiError } from '../api/errors';
import type { ApiError } from '../api/types';
import { useSession } from './SessionContext';

/**
 * Central reaction to API errors (section 5 / 7.3), replacing ERRHNDL's action decision:
 * 401 -> sign-in, 403 -> Access denied page, ABEND -> S4 error screen, RETURN -> menu with message.
 * Returns the error when it should be shown inline on the current screen (action CONTINUE), else null.
 */
export function useApiErrorRouting() {
  const navigate = useNavigate();
  const { markUnauthenticated, setFlash } = useSession();

  return useCallback(
    (err: unknown): ApiError | null => {
      const apiError = err instanceof ApiRequestError ? err.body : normaliseApiError(0, null, null);
      const status = err instanceof ApiRequestError ? err.status : 0;

      if (status === 401) {
        markUnauthenticated();
        navigate('/signin', { replace: true });
        return null;
      }
      if (status === 403) {
        navigate('/access-denied', { replace: true });
        return null;
      }
      if (apiError.action === 'ABEND') {
        navigate('/error', { state: { error: apiError } });
        return null;
      }
      if (apiError.action === 'RETURN') {
        setFlash({ text: apiError.message, severity: apiError.severity, traceId: status >= 500 ? apiError.traceId : undefined });
        navigate('/');
        return null;
      }
      return apiError;
    },
    [navigate, markUnauthenticated, setFlash],
  );
}

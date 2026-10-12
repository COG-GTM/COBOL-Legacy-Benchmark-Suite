import { useCallback, useEffect, useState } from 'react';
import type { ApiError } from '../api/types';

export type QueryState<T> =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'success'; data: T }
  | { status: 'error'; error: ApiError; httpStatus: number };

/**
 * Runs `fetcher` whenever `key` changes (null = do nothing). `onError` decides whether an error is
 * routed elsewhere (returns null) or shown inline. `retry()` repeats the same request exactly once.
 */
export function useApiQuery<T>(
  key: string | null,
  fetcher: (signal: AbortSignal) => Promise<T>,
  onError: (err: unknown) => ApiError | null,
) {
  const [state, setState] = useState<QueryState<T>>({ status: 'idle' });
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    if (key === null) {
      setState({ status: 'idle' });
      return;
    }
    const controller = new AbortController();
    setState({ status: 'loading' });
    fetcher(controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) setState({ status: 'success', data });
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) return;
        const inline = onError(err);
        const httpStatus = (err as { status?: number })?.status ?? 0;
        if (inline) setState({ status: 'error', error: inline, httpStatus });
        else setState({ status: 'idle' });
      });
    return () => controller.abort();
    // fetcher/onError are intentionally excluded: `key` fully identifies the request.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key, attempt]);

  const retry = useCallback(() => setAttempt((n) => n + 1), []);
  return { state, retry };
}

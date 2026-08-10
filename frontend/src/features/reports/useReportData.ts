import { useEffect, useRef, useState } from 'react';

export interface ReportDataState<T> {
  data: T | null;
  loading: boolean;
  error: string | null;
}

/**
 * Runs a report query and tracks its loading/error state. `load` must be a
 * stable callback (wrap it in `useCallback` keyed on the filters); a request id
 * guards against out-of-order responses when filters change quickly.
 */
export function useReportData<T>(load: () => Promise<T>): ReportDataState<T> {
  const [state, setState] = useState<ReportDataState<T>>({
    data: null,
    loading: true,
    error: null,
  });
  const requestId = useRef(0);

  useEffect(() => {
    const id = ++requestId.current;
    setState((current) => ({ ...current, loading: true, error: null }));
    load()
      .then((data) => {
        if (id !== requestId.current) return;
        setState({ data, loading: false, error: null });
      })
      .catch(() => {
        if (id !== requestId.current) return;
        setState({
          data: null,
          loading: false,
          error: 'Unable to run the report. Please try again.',
        });
      });
  }, [load]);

  return state;
}

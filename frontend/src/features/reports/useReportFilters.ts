import { useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import type { AuditAction, AuditStatus, AuditType } from '../../types/audit';
import type { AuditQuery } from '../../types/report';

/** Query-string keys backing the report filters. */
const KEYS = {
  fromDate: 'from',
  toDate: 'to',
  portfolioId: 'portfolio',
  userId: 'user',
  type: 'type',
  action: 'action',
  status: 'status',
} as const;

export type ReportFilterKey = keyof typeof KEYS;

/**
 * Report filters held in the URL query string, so a filtered report can be
 * bookmarked or shared and the selection survives switching report tabs.
 */
export function useReportFilters() {
  const [searchParams, setSearchParams] = useSearchParams();

  const filters = useMemo<AuditQuery>(
    () => ({
      fromDate: searchParams.get(KEYS.fromDate) ?? '',
      toDate: searchParams.get(KEYS.toDate) ?? '',
      portfolioId: searchParams.get(KEYS.portfolioId) ?? '',
      userId: searchParams.get(KEYS.userId) ?? '',
      type: (searchParams.get(KEYS.type) ?? '') as AuditType | '',
      action: (searchParams.get(KEYS.action) ?? '') as AuditAction | '',
      status: (searchParams.get(KEYS.status) ?? '') as AuditStatus | '',
    }),
    [searchParams],
  );

  const setFilter = useCallback(
    (key: ReportFilterKey, value: string) => {
      setSearchParams(
        (current) => {
          const next = new URLSearchParams(current);
          if (value) {
            next.set(KEYS[key], value);
          } else {
            next.delete(KEYS[key]);
          }
          return next;
        },
        { replace: true },
      );
    },
    [setSearchParams],
  );

  const resetFilters = useCallback(() => {
    setSearchParams(new URLSearchParams(), { replace: true });
  }, [setSearchParams]);

  return {
    filters,
    setFilter,
    resetFilters,
    /** Current query string, for links that should carry the filters along. */
    search: searchParams.toString(),
  };
}

import {
  AUDIT_ACTION_LABELS,
  AUDIT_ACTIONS,
  AUDIT_STATUS_LABELS,
  AUDIT_STATUSES,
  AUDIT_TYPE_LABELS,
  AUDIT_TYPES,
} from '../../types/audit';
import type { ReportFilterOptions } from '../../services/reportService';
import { toIsoDate } from '../../utils/date';
import { useReportFilters } from './useReportFilters';

/** Which optional filters a report view offers. */
export interface ReportFilterFields {
  /** Portfolio selector (PORT-ID). */
  portfolio?: boolean;
  /** User selector (AUD-USER-ID / POS-LAST-MAINT-USER). */
  user?: boolean;
  /** AUD-TYPE / AUD-ACTION / AUD-STATUS selectors. */
  audit?: boolean;
}

/** `<input type="date">` works in ISO form; the filters are stored as YYYYMMDD. */
function fromIsoDate(value: string): string {
  return value.replace(/-/g, '');
}

/**
 * Shared filter bar for the report views. Values live in the URL query string
 * (see {@link useReportFilters}) and apply as soon as they change, so the
 * report reruns without a separate submit step.
 */
export function ReportFilterBar({
  fields = {},
  options,
}: {
  fields?: ReportFilterFields;
  options: ReportFilterOptions;
}) {
  const { filters, setFilter, resetFilters } = useReportFilters();

  return (
    <div className="filters" role="group" aria-label="Report filters">
      <div className="field">
        <label htmlFor="report-from">From date</label>
        <input
          id="report-from"
          type="date"
          value={toIsoDate(filters.fromDate ?? '')}
          onChange={(e) => setFilter('fromDate', fromIsoDate(e.target.value))}
        />
      </div>
      <div className="field">
        <label htmlFor="report-to">To date</label>
        <input
          id="report-to"
          type="date"
          value={toIsoDate(filters.toDate ?? '')}
          onChange={(e) => setFilter('toDate', fromIsoDate(e.target.value))}
        />
      </div>

      {fields.portfolio && (
        <div className="field">
          <label htmlFor="report-portfolio">Portfolio</label>
          <select
            id="report-portfolio"
            value={filters.portfolioId ?? ''}
            onChange={(e) => setFilter('portfolioId', e.target.value)}
          >
            <option value="">All</option>
            {options.portfolioIds.map((id) => (
              <option key={id} value={id}>
                {id}
              </option>
            ))}
          </select>
        </div>
      )}

      {fields.user && (
        <div className="field">
          <label htmlFor="report-user">User</label>
          <select
            id="report-user"
            value={filters.userId ?? ''}
            onChange={(e) => setFilter('userId', e.target.value)}
          >
            <option value="">All</option>
            {options.userIds.map((id) => (
              <option key={id} value={id}>
                {id}
              </option>
            ))}
          </select>
        </div>
      )}

      {fields.audit && (
        <>
          <div className="field">
            <label htmlFor="report-type">Event type</label>
            <select
              id="report-type"
              value={filters.type ?? ''}
              onChange={(e) => setFilter('type', e.target.value)}
            >
              <option value="">All</option>
              {AUDIT_TYPES.map((type) => (
                <option key={type} value={type}>
                  {AUDIT_TYPE_LABELS[type]}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label htmlFor="report-action">Action</label>
            <select
              id="report-action"
              value={filters.action ?? ''}
              onChange={(e) => setFilter('action', e.target.value)}
            >
              <option value="">All</option>
              {AUDIT_ACTIONS.map((action) => (
                <option key={action} value={action}>
                  {AUDIT_ACTION_LABELS[action]}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label htmlFor="report-status">Status</label>
            <select
              id="report-status"
              value={filters.status ?? ''}
              onChange={(e) => setFilter('status', e.target.value)}
            >
              <option value="">All</option>
              {AUDIT_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {AUDIT_STATUS_LABELS[status]}
                </option>
              ))}
            </select>
          </div>
        </>
      )}

      <div className="filters__actions">
        <button type="button" className="btn btn--ghost" onClick={resetFilters}>
          Reset
        </button>
      </div>
    </div>
  );
}

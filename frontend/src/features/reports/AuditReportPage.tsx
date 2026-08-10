import { useCallback, useMemo, useState } from 'react';
import { Pagination } from '../../components/Pagination';
import { useReportService } from '../../services/servicesContext';
import {
  AUDIT_ACTION_LABELS,
  AUDIT_STATUS_LABELS,
  AUDIT_TYPE_LABELS,
  type AuditStatus,
} from '../../types/audit';
import { formatCobolTimestamp } from '../../utils/date';
import { formatPercent } from '../../utils/decimal';
import type { ReportDocument } from '../../utils/reportDocument';
import { summarizeAuditEvents } from './aggregation';
import { ReportExportBar } from './ReportExportBar';
import { ReportFilterBar } from './ReportFilterBar';
import { periodLabel, rateTone } from './reportFormat';
import { useReportsOutletContext } from './reportsOutlet';
import { useReportData } from './useReportData';
import { useReportFilters } from './useReportFilters';

const PAGE_SIZE = 15;

/** RPTAUD00 — the AUDITLOG event trail with the report's summary counters. */
export function AuditReportPage() {
  const service = useReportService();
  const { options } = useReportsOutletContext();
  const { filters } = useReportFilters();
  const { fromDate, toDate, portfolioId, userId, type, action, status } =
    filters;
  const [page, setPage] = useState(1);

  const load = useCallback(
    () =>
      service.listAuditEvents({
        fromDate,
        toDate,
        portfolioId,
        userId,
        type,
        action,
        status,
      }),
    [service, fromDate, toDate, portfolioId, userId, type, action, status],
  );
  const { data, loading, error } = useReportData(load);

  const events = useMemo(() => data ?? [], [data]);
  const summary = useMemo(() => summarizeAuditEvents(events), [events]);

  const pageCount = Math.max(1, Math.ceil(events.length / PAGE_SIZE));
  const currentPage = Math.min(page, pageCount);
  const pageEvents = events.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  );

  const buildReport = (): ReportDocument => ({
    title: 'Audit Report',
    meta: [
      { label: 'Program', value: 'RPTAUD00' },
      { label: 'Period', value: periodLabel(fromDate, toDate) },
      { label: 'Portfolio', value: portfolioId || 'All' },
      { label: 'User', value: userId || 'All' },
      {
        label: 'Events',
        value: `${summary.total} (${summary.success} success, ${summary.warning} warning, ${summary.failure} failure)`,
      },
    ],
    columns: [
      { label: 'Timestamp' },
      { label: 'User' },
      { label: 'Program' },
      { label: 'Type' },
      { label: 'Action' },
      { label: 'Status' },
      { label: 'Portfolio' },
      { label: 'Account' },
      { label: 'Message' },
    ],
    rows: events.map((event) => [
      formatCobolTimestamp(event.timestamp),
      event.userId,
      event.program,
      AUDIT_TYPE_LABELS[event.type],
      AUDIT_ACTION_LABELS[event.action],
      AUDIT_STATUS_LABELS[event.status],
      event.portfolioId,
      event.accountNo,
      event.message,
    ]),
  });

  return (
    <>
      <div className="report-toolbar">
        <ReportFilterBar
          fields={{ portfolio: true, user: true, audit: true }}
          options={options}
        />
        <ReportExportBar buildReport={buildReport} disabled={!events.length} />
      </div>

      {error && (
        <div className="alert alert--error" role="alert">
          {error}
        </div>
      )}

      {events.length > 0 && (
        <div className="valuation" data-testid="audit-summary">
          <StatCard label="Events" value={String(summary.total)} />
          <StatCard label="Success" value={String(summary.success)} />
          <StatCard label="Warnings" value={String(summary.warning)} />
          <StatCard
            label="Failures"
            value={`${summary.failure} (${formatPercent(summary.failureRatePct)})`}
            tone={rateTone(summary.failureRatePct)}
          />
        </div>
      )}

      <div className="card card--scroll">
        {loading ? (
          <p className="state-msg">Running audit report…</p>
        ) : events.length === 0 ? (
          <p className="state-msg" data-testid="empty-state">
            No audit events match the selected filters.
          </p>
        ) : (
          <table className="table table--compact" aria-label="Audit report">
            <thead>
              <tr>
                <th>Timestamp</th>
                <th>User</th>
                <th>Program</th>
                <th>Type</th>
                <th>Action</th>
                <th>Status</th>
                <th>Portfolio</th>
                <th>Message</th>
              </tr>
            </thead>
            <tbody>
              {pageEvents.map((event) => (
                <tr key={`${event.timestamp}-${event.program}-${event.userId}`}>
                  <td>{formatCobolTimestamp(event.timestamp)}</td>
                  <td>{event.userId}</td>
                  <td>{event.program}</td>
                  <td>{AUDIT_TYPE_LABELS[event.type]}</td>
                  <td>{AUDIT_ACTION_LABELS[event.action]}</td>
                  <td>
                    <AuditStatusBadge status={event.status} />
                  </td>
                  <td>{event.portfolioId || '—'}</td>
                  <td>{event.message}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {events.length > 0 && (
        <>
          <Pagination
            page={currentPage}
            pageCount={pageCount}
            onPageChange={setPage}
          />
          <p className="result-count">
            {events.length} event{events.length === 1 ? '' : 's'}
          </p>
        </>
      )}
    </>
  );
}

/** AUD-STATUS badge; mirrors the position status badge styling. */
function AuditStatusBadge({ status }: { status: AuditStatus }) {
  const modifier =
    status === 'SUCC' ? 'active' : status === 'WARN' ? 'suspended' : 'failed';
  return (
    <span className={`badge badge--${modifier}`}>
      <span className="badge__dot" aria-hidden="true" />
      {AUDIT_STATUS_LABELS[status]}
    </span>
  );
}

function StatCard({
  label,
  value,
  tone = '',
}: {
  label: string;
  value: string;
  tone?: string;
}) {
  return (
    <div className="card valuation-card">
      <span className="valuation-card__label">{label}</span>
      <span className={`valuation-card__value ${tone}`}>{value}</span>
    </div>
  );
}

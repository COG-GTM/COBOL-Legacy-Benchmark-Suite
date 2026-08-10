import { useCallback } from 'react';
import { useReportService } from '../../services/servicesContext';
import { toIsoDate } from '../../utils/date';
import { formatPercent } from '../../utils/decimal';
import type { ReportDocument } from '../../utils/reportDocument';
import { ReportExportBar } from './ReportExportBar';
import { ReportFilterBar } from './ReportFilterBar';
import { periodLabel, rateTone } from './reportFormat';
import { useReportsOutletContext } from './reportsOutlet';
import { useReportData } from './useReportData';
import { useReportFilters } from './useReportFilters';

const NUMBER_FORMAT = new Intl.NumberFormat('en-US');

/** Formats a count for display; volumes are plain integers, never money. */
function formatCount(value: number): string {
  return NUMBER_FORMAT.format(value);
}

/** Seconds with 2 decimals, as accumulated by the RPTSTA00 timing counters. */
function formatSeconds(value: string): string {
  return `${value}s`;
}

/**
 * RPTSTA00 — DB2 and batch processing volumes, timings and error rates for the
 * selected period, with the per-day trend section.
 */
export function SystemStatisticsPage() {
  const service = useReportService();
  const { options } = useReportsOutletContext();
  const { filters } = useReportFilters();
  const { fromDate, toDate } = filters;

  const load = useCallback(
    () => service.getSystemStatistics({ fromDate, toDate }),
    [service, fromDate, toDate],
  );
  const { data, loading, error } = useReportData(load);

  const buildReport = (): ReportDocument => ({
    title: 'System Statistics',
    meta: [
      { label: 'Program', value: 'RPTSTA00' },
      { label: 'Period', value: periodLabel(fromDate, toDate) },
      {
        label: 'DB2',
        value: data
          ? `${formatCount(data.db2.calls)} calls, avg ${data.db2.avgResponseMs ?? '—'} ms`
          : '',
      },
      {
        label: 'Batch',
        value: data
          ? `${data.batch.jobs} steps, ${data.batch.failed} failed`
          : '',
      },
    ],
    columns: [
      { label: 'Date' },
      { label: 'DB2 Calls', align: 'right' },
      { label: 'Batch Steps', align: 'right' },
      { label: 'Failed Steps', align: 'right' },
      { label: 'Records', align: 'right' },
      { label: 'Error Rate', align: 'right' },
    ],
    rows: (data?.daily ?? []).map((day) => [
      toIsoDate(day.date),
      formatCount(day.db2Calls),
      formatCount(day.batchJobs),
      formatCount(day.batchFailed),
      formatCount(day.recordsProcessed),
      formatPercent(day.errorRatePct),
    ]),
    totalsRow: data
      ? [
          'TOTALS',
          formatCount(data.db2.calls),
          formatCount(data.batch.jobs),
          formatCount(data.batch.failed),
          formatCount(data.batch.recordsProcessed),
          formatPercent(data.batch.errorRatePct),
        ]
      : undefined,
  });

  return (
    <>
      <div className="report-toolbar">
        <ReportFilterBar options={options} />
        <ReportExportBar
          buildReport={buildReport}
          disabled={!data?.daily.length}
        />
      </div>

      {error && (
        <div className="alert alert--error" role="alert">
          {error}
        </div>
      )}

      {loading && <p className="state-msg">Running system statistics…</p>}

      {!loading && data && data.daily.length === 0 && (
        <div className="card">
          <p className="state-msg" data-testid="empty-state">
            No processing activity recorded for the selected period.
          </p>
        </div>
      )}

      {!loading && data && data.daily.length > 0 && (
        <>
          <div className="valuation" data-testid="statistics-summary">
            <StatCard
              label="DB2 Calls"
              value={formatCount(data.db2.calls)}
              hint={`${formatSeconds(data.db2.elapsedSeconds)} elapsed`}
            />
            <StatCard
              label="Avg DB2 Response"
              value={
                data.db2.avgResponseMs === null
                  ? '—'
                  : `${data.db2.avgResponseMs} ms`
              }
              hint={`${formatSeconds(data.db2.cpuSeconds)} CPU / ${formatSeconds(data.db2.waitSeconds)} wait`}
            />
            <StatCard
              label="Records Processed"
              value={formatCount(data.batch.recordsProcessed)}
              hint={`${data.batch.jobs} batch steps`}
            />
            <StatCard
              label="Error Rate"
              value={formatPercent(data.batch.errorRatePct)}
              hint={`${data.batch.failed} failed, ${data.batch.restarts} restarts`}
              tone={rateTone(data.batch.errorRatePct)}
            />
          </div>

          <div className="card card--scroll">
            <table
              className="table table--compact"
              aria-label="Daily processing volumes"
            >
              <thead>
                <tr>
                  <th>Date</th>
                  <th className="num">DB2 Calls</th>
                  <th className="num">Batch Steps</th>
                  <th className="num">Failed Steps</th>
                  <th className="num">Records</th>
                  <th className="num">Error Rate</th>
                  <th>Volume</th>
                </tr>
              </thead>
              <tbody>
                {data.daily.map((day) => (
                  <tr key={day.date}>
                    <td>{toIsoDate(day.date)}</td>
                    <td className="num">{formatCount(day.db2Calls)}</td>
                    <td className="num">{formatCount(day.batchJobs)}</td>
                    <td className="num">{formatCount(day.batchFailed)}</td>
                    <td className="num">{formatCount(day.recordsProcessed)}</td>
                    <td className={`num ${rateTone(day.errorRatePct)}`}>
                      {formatPercent(day.errorRatePct)}
                    </td>
                    <td>
                      <VolumeBar
                        value={day.db2Calls}
                        max={Math.max(...data.daily.map((d) => d.db2Calls))}
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr>
                  <th>Totals</th>
                  <th className="num">{formatCount(data.db2.calls)}</th>
                  <th className="num">{formatCount(data.batch.jobs)}</th>
                  <th className="num">{formatCount(data.batch.failed)}</th>
                  <th className="num">
                    {formatCount(data.batch.recordsProcessed)}
                  </th>
                  <th className={`num ${rateTone(data.batch.errorRatePct)}`}>
                    {formatPercent(data.batch.errorRatePct)}
                  </th>
                  <th />
                </tr>
              </tfoot>
            </table>
          </div>

          <p className="result-count">
            Batch success rate {formatPercent(data.batch.successRatePct)} across{' '}
            {formatSeconds(data.batch.elapsedSeconds)} of batch run time
          </p>
        </>
      )}
    </>
  );
}

/** Horizontal bar showing a day's volume relative to the busiest day. */
function VolumeBar({ value, max }: { value: number; max: number }) {
  const width = max > 0 ? Math.round((value / max) * 100) : 0;
  return (
    <span className="volume-bar" aria-hidden="true">
      <span className="volume-bar__fill" style={{ width: `${width}%` }} />
    </span>
  );
}

function StatCard({
  label,
  value,
  hint,
  tone = '',
}: {
  label: string;
  value: string;
  hint?: string;
  tone?: string;
}) {
  return (
    <div className="card valuation-card">
      <span className="valuation-card__label">{label}</span>
      <span className={`valuation-card__value ${tone}`}>{value}</span>
      {hint && <span className="valuation-card__hint">{hint}</span>}
    </div>
  );
}

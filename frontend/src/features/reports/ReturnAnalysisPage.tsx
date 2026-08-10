import { useCallback } from 'react';
import { useReportService } from '../../services/servicesContext';
import type { ReturnAnalysisRow } from '../../types/report';
import { toIsoDate } from '../../utils/date';
import { formatPercent } from '../../utils/decimal';
import type { ReportDocument } from '../../utils/reportDocument';
import { ReportExportBar } from './ReportExportBar';
import { ReportFilterBar } from './ReportFilterBar';
import { periodLabel, rateTone } from './reportFormat';
import { useReportsOutletContext } from './reportsOutlet';
import { useReportData } from './useReportData';
import { useReportFilters } from './useReportFilters';

/**
 * RTNANA00 — return codes grouped by program, each compared with the
 * immediately preceding period of the same length.
 */
export function ReturnAnalysisPage() {
  const service = useReportService();
  const { options } = useReportsOutletContext();
  const { filters } = useReportFilters();
  const { fromDate, toDate } = filters;

  const load = useCallback(
    () => service.getReturnAnalysis({ fromDate, toDate }),
    [service, fromDate, toDate],
  );
  const { data, loading, error } = useReportData(load);

  const rowCells = (row: ReturnAnalysisRow) => [
    row.program,
    String(row.total),
    String(row.success),
    String(row.warning),
    String(row.error),
    String(row.severe),
    formatPercent(row.failureRatePct),
    formatPercent(row.priorFailureRatePct),
    formatPercent(row.failureRateDeltaPct, { signed: true }),
    formatPercent(row.volumeChangePct, { signed: true }),
  ];

  const buildReport = (): ReportDocument => ({
    title: 'Return Code Analysis',
    meta: [
      { label: 'Program', value: 'RTNANA00' },
      { label: 'Period', value: periodLabel(fromDate, toDate) },
      {
        label: 'Prior period',
        value: data
          ? periodLabel(data.priorPeriod.fromDate, data.priorPeriod.toDate)
          : '',
      },
    ],
    columns: [
      { label: 'Program' },
      { label: 'Total', align: 'right' },
      { label: 'Success', align: 'right' },
      { label: 'Warning', align: 'right' },
      { label: 'Error', align: 'right' },
      { label: 'Severe', align: 'right' },
      { label: 'Failure %', align: 'right' },
      { label: 'Prior %', align: 'right' },
      { label: 'Delta', align: 'right' },
      { label: 'Volume', align: 'right' },
    ],
    rows: (data?.rows ?? []).map(rowCells),
    totalsRow: data ? rowCells(data.totals) : undefined,
  });

  return (
    <>
      <div className="report-toolbar">
        <ReportFilterBar options={options} />
        <ReportExportBar
          buildReport={buildReport}
          disabled={!data?.rows.length}
        />
      </div>

      {error && (
        <div className="alert alert--error" role="alert">
          {error}
        </div>
      )}

      {data && data.rows.length > 0 && (
        <p className="report-period" data-testid="comparison-period">
          {toIsoDate(data.period.fromDate)} – {toIsoDate(data.period.toDate)}{' '}
          compared with {toIsoDate(data.priorPeriod.fromDate)} –{' '}
          {toIsoDate(data.priorPeriod.toDate)}
        </p>
      )}

      <div className="card card--scroll">
        {loading ? (
          <p className="state-msg">Running return code analysis…</p>
        ) : !data || data.rows.length === 0 ? (
          <p className="state-msg" data-testid="empty-state">
            No return codes logged for the selected period.
          </p>
        ) : (
          <table
            className="table table--compact"
            aria-label="Return code analysis"
          >
            <thead>
              <tr>
                <th>Program</th>
                <th className="num">Total</th>
                <th className="num">Success</th>
                <th className="num">Warning</th>
                <th className="num">Error</th>
                <th className="num">Severe</th>
                <th className="num">Failure %</th>
                <th className="num">Prior %</th>
                <th className="num">Delta</th>
                <th className="num">Volume</th>
              </tr>
            </thead>
            <tbody>
              {data.rows.map((row) => (
                <tr key={row.program}>
                  <td>{row.program}</td>
                  <td className="num">{row.total}</td>
                  <td className="num">{row.success}</td>
                  <td className="num">{row.warning}</td>
                  <td className="num">{row.error}</td>
                  <td className="num">{row.severe}</td>
                  <td className={`num ${rateTone(row.failureRatePct)}`}>
                    {formatPercent(row.failureRatePct)}
                  </td>
                  <td className="num">
                    {formatPercent(row.priorFailureRatePct)}
                  </td>
                  <td className={`num ${rateTone(row.failureRateDeltaPct)}`}>
                    {formatPercent(row.failureRateDeltaPct, { signed: true })}
                  </td>
                  <td className="num">
                    {formatPercent(row.volumeChangePct, { signed: true })}
                  </td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr>
                <th>Totals</th>
                <th className="num">{data.totals.total}</th>
                <th className="num">{data.totals.success}</th>
                <th className="num">{data.totals.warning}</th>
                <th className="num">{data.totals.error}</th>
                <th className="num">{data.totals.severe}</th>
                <th className={`num ${rateTone(data.totals.failureRatePct)}`}>
                  {formatPercent(data.totals.failureRatePct)}
                </th>
                <th className="num">
                  {formatPercent(data.totals.priorFailureRatePct)}
                </th>
                <th
                  className={`num ${rateTone(data.totals.failureRateDeltaPct)}`}
                >
                  {formatPercent(data.totals.failureRateDeltaPct, {
                    signed: true,
                  })}
                </th>
                <th className="num">
                  {formatPercent(data.totals.volumeChangePct, { signed: true })}
                </th>
              </tr>
            </tfoot>
          </table>
        )}
      </div>
    </>
  );
}

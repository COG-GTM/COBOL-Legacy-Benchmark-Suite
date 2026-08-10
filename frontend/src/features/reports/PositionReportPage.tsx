import { useCallback } from 'react';
import { useReportService } from '../../services/servicesContext';
import type { PositionReport } from '../../types/report';
import { toIsoDate } from '../../utils/date';
import {
  formatCurrency,
  formatPercent,
  formatQuantity,
} from '../../utils/decimal';
import type { ReportDocument } from '../../utils/reportDocument';
import { ReportExportBar } from './ReportExportBar';
import { ReportFilterBar } from './ReportFilterBar';
import { useReportsOutletContext } from './reportsOutlet';
import { useReportData } from './useReportData';
import { useReportFilters } from './useReportFilters';
import { periodLabel, valueTone } from './reportFormat';

/** RPTPOS00 — position holdings rolled up per portfolio, with valuations. */
export function PositionReportPage() {
  const service = useReportService();
  const { options } = useReportsOutletContext();
  const { filters } = useReportFilters();
  const { fromDate, toDate, portfolioId, userId } = filters;

  const load = useCallback(
    () => service.getPositionReport({ fromDate, toDate, portfolioId, userId }),
    [service, fromDate, toDate, portfolioId, userId],
  );
  const { data, loading, error } = useReportData(load);

  const buildReport = (): ReportDocument => ({
    title: 'Position Report',
    meta: [
      { label: 'Program', value: 'RPTPOS00' },
      { label: 'Period', value: periodLabel(fromDate, toDate) },
      { label: 'Portfolio', value: portfolioId || 'All' },
      { label: 'User', value: userId || 'All' },
    ],
    columns: [
      { label: 'Portfolio' },
      { label: 'Client' },
      { label: 'Account' },
      { label: 'Holdings', align: 'right' },
      { label: 'Units', align: 'right' },
      { label: 'Cost Basis', align: 'right' },
      { label: 'Market Value', align: 'right' },
      { label: 'Gain / Loss', align: 'right' },
      { label: 'Change %', align: 'right' },
    ],
    rows: (data?.rows ?? []).map((row) => [
      row.portfolioId,
      row.clientName,
      row.accountNo,
      String(row.holdings),
      formatQuantity(row.totalQuantity),
      formatCurrency(row.totalCostBasis),
      formatCurrency(row.totalMarketValue),
      formatCurrency(row.gainLoss),
      formatPercent(row.gainLossPct, { signed: true }),
    ]),
    totalsRow: data
      ? [
          'TOTALS',
          '',
          '',
          String(data.totals.holdings),
          '',
          formatCurrency(data.totals.totalCostBasis),
          formatCurrency(data.totals.totalMarketValue),
          formatCurrency(data.totals.gainLoss),
          formatPercent(data.totals.gainLossPct, { signed: true }),
        ]
      : undefined,
  });

  return (
    <>
      <div className="report-toolbar">
        <ReportFilterBar
          fields={{ portfolio: true, user: true }}
          options={options}
        />
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

      {data && data.rows.length > 0 && <ValuationSummary report={data} />}

      <div className="card card--scroll">
        {loading ? (
          <p className="state-msg">Running position report…</p>
        ) : !data || data.rows.length === 0 ? (
          <p className="state-msg" data-testid="empty-state">
            No positions match the selected filters.
          </p>
        ) : (
          <table className="table table--compact" aria-label="Position report">
            <thead>
              <tr>
                <th>Portfolio</th>
                <th>Client</th>
                <th>Account</th>
                <th className="num">Holdings</th>
                <th className="num">Units</th>
                <th className="num">Cost Basis</th>
                <th className="num">Market Value</th>
                <th className="num">Gain / Loss</th>
                <th className="num">Change %</th>
              </tr>
            </thead>
            <tbody>
              {data.rows.map((row) => (
                <tr key={row.portfolioId}>
                  <td>{row.portfolioId}</td>
                  <td>{row.clientName}</td>
                  <td>{row.accountNo}</td>
                  <td className="num">{row.holdings}</td>
                  <td className="num">{formatQuantity(row.totalQuantity)}</td>
                  <td className="num">{formatCurrency(row.totalCostBasis)}</td>
                  <td className="num">
                    {formatCurrency(row.totalMarketValue)}
                  </td>
                  <td className={`num ${valueTone(row.gainLoss)}`}>
                    {formatCurrency(row.gainLoss)}
                  </td>
                  <td className={`num ${valueTone(row.gainLoss)}`}>
                    {formatPercent(row.gainLossPct, { signed: true })}
                  </td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr>
                <th colSpan={3}>Totals</th>
                <th className="num">{data.totals.holdings}</th>
                <th />
                <th className="num">
                  {formatCurrency(data.totals.totalCostBasis)}
                </th>
                <th className="num">
                  {formatCurrency(data.totals.totalMarketValue)}
                </th>
                <th className={`num ${valueTone(data.totals.gainLoss)}`}>
                  {formatCurrency(data.totals.gainLoss)}
                </th>
                <th className={`num ${valueTone(data.totals.gainLoss)}`}>
                  {formatPercent(data.totals.gainLossPct, { signed: true })}
                </th>
              </tr>
            </tfoot>
          </table>
        )}
      </div>

      {data && data.rows.length > 0 && (
        <p className="result-count">
          {data.rows.length} portfolio{data.rows.length === 1 ? '' : 's'} as of{' '}
          {toIsoDate(toDate ?? '') || 'latest snapshot'}
        </p>
      )}
    </>
  );
}

function ValuationSummary({ report }: { report: PositionReport }) {
  return (
    <div className="valuation" data-testid="valuation-summary">
      <SummaryCard
        label="Total Market Value"
        value={formatCurrency(report.totals.totalMarketValue)}
      />
      <SummaryCard
        label="Total Cost Basis"
        value={formatCurrency(report.totals.totalCostBasis)}
      />
      <SummaryCard
        label="Gain / Loss"
        value={formatCurrency(report.totals.gainLoss)}
        tone={valueTone(report.totals.gainLoss)}
      />
      <SummaryCard
        label="Change %"
        value={formatPercent(report.totals.gainLossPct, { signed: true })}
        tone={valueTone(report.totals.gainLoss)}
      />
    </div>
  );
}

function SummaryCard({
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

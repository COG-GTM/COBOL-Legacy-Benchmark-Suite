import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchPositionReport, fetchAuditReport } from '../services/api';
import { formatCurrency, formatDateTime } from '../utils/format';
import GainLoss from '../components/GainLoss';
import Loading from '../components/Loading';
import ErrorMessage from '../components/ErrorMessage';

type Tab = 'position' | 'audit';

export default function Reports() {
  const [tab, setTab] = useState<Tab>('position');

  return (
    <div className="space-y-6 animate-fade-in">
      <div>
        <h1 className="text-3xl font-bold">Reports</h1>
        <p className="text-slate-400 mt-1">Position and audit reports — replaces RPTPOS00, RPTAUD00, RPTSTA00</p>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 bg-slate-800 rounded-lg p-1 w-fit">
        {(['position', 'audit'] as Tab[]).map(t => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
              tab === t ? 'bg-blue-600 text-white' : 'text-slate-400 hover:text-white'
            }`}
          >
            {t === 'position' ? 'Position Report' : 'Audit Report'}
          </button>
        ))}
      </div>

      {tab === 'position' ? <PositionReport /> : <AuditReport />}
    </div>
  );
}

function PositionReport() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['positionReport'],
    queryFn: fetchPositionReport,
  });

  if (isLoading) return <Loading text="Generating position report..." />;
  if (error) return <ErrorMessage message={(error as Error).message} />;
  if (!data) return null;

  return (
    <div className="space-y-6">
      {/* Summary */}
      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold">Daily Position Report</h2>
          <span className="text-sm text-slate-400">{formatDateTime(data.report_date)}</span>
        </div>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div>
            <p className="text-xs text-slate-400">Portfolios</p>
            <p className="text-xl font-bold">{data.total_portfolios}</p>
          </div>
          <div>
            <p className="text-xs text-slate-400">Positions</p>
            <p className="text-xl font-bold">{data.total_positions}</p>
          </div>
          <div>
            <p className="text-xs text-slate-400">Total Market Value</p>
            <p className="text-xl font-bold">{formatCurrency(data.total_market_value)}</p>
          </div>
          <div>
            <p className="text-xs text-slate-400">Total Gain/Loss</p>
            <GainLoss value={data.total_gain_loss} size="lg" />
          </div>
        </div>
      </div>

      {/* Position Details */}
      <div className="card">
        <h3 className="font-semibold mb-4">Position Details</h3>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-700 text-left text-slate-400">
                <th className="pb-3 pr-4">Portfolio</th>
                <th className="pb-3 pr-4">Symbol</th>
                <th className="pb-3 pr-4">Name</th>
                <th className="pb-3 pr-4 text-right">Qty</th>
                <th className="pb-3 pr-4 text-right">Cost Basis</th>
                <th className="pb-3 pr-4 text-right">Market Value</th>
                <th className="pb-3 text-right">Gain/Loss</th>
              </tr>
            </thead>
            <tbody>
              {data.items.map((item, i) => (
                <tr key={i} className="border-b border-slate-700/50 hover:bg-slate-700/20">
                  <td className="py-3 pr-4 font-mono text-blue-400">{item.portfolio_id}</td>
                  <td className="py-3 pr-4 font-mono font-bold">{item.symbol}</td>
                  <td className="py-3 pr-4">{item.name}</td>
                  <td className="py-3 pr-4 text-right">{item.quantity.toLocaleString()}</td>
                  <td className="py-3 pr-4 text-right">{formatCurrency(item.cost_basis)}</td>
                  <td className="py-3 pr-4 text-right font-medium">{formatCurrency(item.market_value)}</td>
                  <td className="py-3 text-right">
                    <GainLoss value={item.gain_loss} percent={item.gain_loss_percent} size="sm" showIcon={false} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function AuditReport() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['auditReport'],
    queryFn: fetchAuditReport,
  });

  if (isLoading) return <Loading text="Generating audit report..." />;
  if (error) return <ErrorMessage message={(error as Error).message} />;
  if (!data) return null;

  return (
    <div className="space-y-6">
      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold">Audit Report</h2>
          <span className="text-sm text-slate-400">{formatDateTime(data.report_date)}</span>
        </div>
        <div className="grid grid-cols-3 gap-4">
          <div>
            <p className="text-xs text-slate-400">Total Entries</p>
            <p className="text-xl font-bold">{data.total_entries}</p>
          </div>
          <div>
            <p className="text-xs text-slate-400">Errors</p>
            <p className="text-xl font-bold text-red-400">{data.error_count}</p>
          </div>
          <div>
            <p className="text-xs text-slate-400">Warnings</p>
            <p className="text-xl font-bold text-yellow-400">{data.warning_count}</p>
          </div>
        </div>
      </div>

      {data.entries.length === 0 ? (
        <div className="card text-center py-10 text-slate-500">No audit entries recorded yet</div>
      ) : (
        <div className="card">
          <h3 className="font-semibold mb-4">Audit Trail</h3>
          <div className="space-y-2">
            {data.entries.map((entry, i) => (
              <div key={i} className="flex items-start gap-3 p-3 rounded-lg bg-slate-700/30">
                <span className={`badge mt-0.5 ${entry.severity === 'ERROR' ? 'badge-danger' : 'badge-warning'}`}>
                  {entry.severity}
                </span>
                <div className="flex-1 min-w-0">
                  <p className="text-sm">{entry.description}</p>
                  <p className="text-xs text-slate-500 mt-1">
                    {entry.program_id} · {entry.error_code} · {formatDateTime(entry.timestamp)}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

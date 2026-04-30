import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchPortfolios, fetchPortfolio } from '../services/api';
import { formatCurrency, formatDate } from '../utils/format';
import { RISK_LABELS } from '../types';
import type { PortfolioDetail } from '../types';
import StatusBadge from '../components/StatusBadge';
import GainLoss from '../components/GainLoss';
import Loading from '../components/Loading';
import ErrorMessage from '../components/ErrorMessage';
import { Search, X } from 'lucide-react';

export default function Portfolios() {
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');

  const { data, isLoading, error } = useQuery({
    queryKey: ['portfolios'],
    queryFn: () => fetchPortfolios(),
  });

  const detail = useQuery({
    queryKey: ['portfolio', selectedId],
    queryFn: () => fetchPortfolio(selectedId!),
    enabled: !!selectedId,
  });

  if (isLoading) return <Loading text="Loading portfolios..." />;
  if (error) return <ErrorMessage message={(error as Error).message} />;

  const portfolios = (data?.portfolios ?? []).filter(p =>
    !searchTerm ||
    p.portfolio_id.toLowerCase().includes(searchTerm.toLowerCase()) ||
    p.client_name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    p.account_number.includes(searchTerm)
  );

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Portfolio Inquiry</h1>
          <p className="text-slate-400 mt-1">View portfolio positions and performance — replaces CICS POSMAP screen</p>
        </div>
      </div>

      {/* Search */}
      <div className="relative">
        <Search size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
        <input
          className="input pl-10"
          placeholder="Search by portfolio ID, client name, or account number..."
          value={searchTerm}
          onChange={e => setSearchTerm(e.target.value)}
        />
        {searchTerm && (
          <button onClick={() => setSearchTerm('')} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-200">
            <X size={18} />
          </button>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Portfolio List */}
        <div className="lg:col-span-1 space-y-3">
          <p className="text-sm text-slate-400">{portfolios.length} portfolios</p>
          {portfolios.map(p => (
            <button
              key={p.portfolio_id}
              onClick={() => setSelectedId(p.portfolio_id)}
              className={`w-full text-left card transition-all ${
                selectedId === p.portfolio_id ? 'border-blue-500 bg-blue-500/5' : 'hover:border-slate-600'
              }`}
            >
              <div className="flex items-center justify-between mb-2">
                <span className="font-mono text-sm text-blue-400">{p.portfolio_id}</span>
                <StatusBadge status={p.status} />
              </div>
              <p className="font-semibold">{p.client_name}</p>
              <p className="text-sm text-slate-400">{p.portfolio_name}</p>
              <div className="flex items-center justify-between mt-3">
                <span className="text-lg font-bold">{formatCurrency(p.total_value)}</span>
                <span className="text-xs text-slate-500">{RISK_LABELS[p.risk_level]} Risk</span>
              </div>
            </button>
          ))}
        </div>

        {/* Portfolio Detail */}
        <div className="lg:col-span-2">
          {selectedId ? (
            detail.isLoading ? (
              <Loading text="Loading positions..." />
            ) : detail.error ? (
              <ErrorMessage message={(detail.error as Error).message} />
            ) : detail.data ? (
              <PortfolioDetailView portfolio={detail.data} />
            ) : null
          ) : (
            <div className="card flex items-center justify-center py-20">
              <p className="text-slate-500">Select a portfolio to view positions</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function PortfolioDetailView({ portfolio }: { portfolio: PortfolioDetail }) {
  return (
    <div className="space-y-6 animate-fade-in">
      {/* Summary */}
      <div className="card">
        <div className="flex items-start justify-between mb-4">
          <div>
            <h2 className="text-xl font-bold">{portfolio.portfolio_name || portfolio.portfolio_id}</h2>
            <p className="text-sm text-slate-400">Account: {portfolio.account_number} · {portfolio.client_name}</p>
          </div>
          <StatusBadge status={portfolio.status} />
        </div>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div>
            <p className="text-xs text-slate-400">Market Value</p>
            <p className="text-lg font-bold">{formatCurrency(portfolio.total_value)}</p>
          </div>
          <div>
            <p className="text-xs text-slate-400">Total Gain/Loss</p>
            <GainLoss value={portfolio.total_gain_loss} percent={portfolio.total_gain_loss_percent} />
          </div>
          <div>
            <p className="text-xs text-slate-400">Positions</p>
            <p className="text-lg font-bold">{portfolio.position_count}</p>
          </div>
          <div>
            <p className="text-xs text-slate-400">Opened</p>
            <p className="text-lg font-bold">{formatDate(portfolio.open_date)}</p>
          </div>
        </div>
      </div>

      {/* Positions Table */}
      <div className="card">
        <h3 className="text-lg font-semibold mb-4">Holdings</h3>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-700 text-left text-slate-400">
                <th className="pb-3 pr-4">Symbol</th>
                <th className="pb-3 pr-4">Name</th>
                <th className="pb-3 pr-4 text-right">Quantity</th>
                <th className="pb-3 pr-4 text-right">Price</th>
                <th className="pb-3 pr-4 text-right">Cost Basis</th>
                <th className="pb-3 pr-4 text-right">Market Value</th>
                <th className="pb-3 text-right">Gain/Loss</th>
              </tr>
            </thead>
            <tbody>
              {portfolio.positions.map(pos => (
                <tr key={pos.investment_id} className="border-b border-slate-700/50 hover:bg-slate-700/20">
                  <td className="py-3 pr-4 font-mono font-bold text-blue-400">{pos.symbol}</td>
                  <td className="py-3 pr-4">{pos.name}</td>
                  <td className="py-3 pr-4 text-right">{pos.quantity.toLocaleString()}</td>
                  <td className="py-3 pr-4 text-right">{formatCurrency(pos.current_price)}</td>
                  <td className="py-3 pr-4 text-right">{formatCurrency(pos.cost_basis)}</td>
                  <td className="py-3 pr-4 text-right font-medium">{formatCurrency(pos.market_value)}</td>
                  <td className="py-3 text-right">
                    <GainLoss value={pos.gain_loss} percent={pos.gain_loss_percent} size="sm" showIcon={false} />
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

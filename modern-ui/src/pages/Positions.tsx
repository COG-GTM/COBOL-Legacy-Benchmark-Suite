import { useState } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import Card from '../components/Card';
import StatusBadge from '../components/StatusBadge';
import { positions, portfolios } from '../data/mockData';
import { POSITION_STATUS_LABELS } from '../types';

function formatCurrency(v: number): string {
  return v.toLocaleString('en-US', { style: 'currency', currency: 'USD' });
}

export default function Positions() {
  const [selectedPortfolio, setSelectedPortfolio] = useState<string>('all');

  const activePortfolios = portfolios.filter(p => p.status === 'A');
  const filtered = selectedPortfolio === 'all'
    ? positions
    : positions.filter(p => p.portfolioId === selectedPortfolio);

  const totalCostBasis = filtered.reduce((s, p) => s + p.costBasis, 0);
  const totalMarketValue = filtered.reduce((s, p) => s + p.marketValue, 0);
  const totalPnL = totalMarketValue - totalCostBasis;
  const totalPnLPct = totalCostBasis > 0 ? (totalPnL / totalCostBasis) * 100 : 0;

  const chartData = activePortfolios.map(port => {
    const portPositions = positions.filter(p => p.portfolioId === port.id);
    const costBasis = portPositions.reduce((s, p) => s + p.costBasis, 0);
    const marketValue = portPositions.reduce((s, p) => s + p.marketValue, 0);
    return {
      name: port.clientName.split(' ').slice(0, 2).join(' '),
      costBasis: Math.round(costBasis / 1000),
      marketValue: Math.round(marketValue / 1000),
    };
  }).filter(d => d.costBasis > 0);

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold text-slate-900">Positions</h2>
        <p className="text-sm text-slate-500 mt-0.5">
          Portfolio holdings & market values — modernized from POSUPDT / INQPORT
        </p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
        <div className="bg-white rounded-xl border border-slate-200 p-4">
          <div className="text-xs text-slate-500">Total Positions</div>
          <div className="text-2xl font-bold text-slate-900 mt-1">{filtered.length}</div>
        </div>
        <div className="bg-white rounded-xl border border-slate-200 p-4">
          <div className="text-xs text-slate-500">Total Cost Basis</div>
          <div className="text-2xl font-bold text-slate-900 mt-1">{formatCurrency(totalCostBasis)}</div>
        </div>
        <div className="bg-white rounded-xl border border-slate-200 p-4">
          <div className="text-xs text-slate-500">Market Value</div>
          <div className="text-2xl font-bold text-slate-900 mt-1">{formatCurrency(totalMarketValue)}</div>
        </div>
        <div className="bg-white rounded-xl border border-slate-200 p-4">
          <div className="text-xs text-slate-500">Unrealized P&L</div>
          <div className={`text-2xl font-bold mt-1 ${totalPnL >= 0 ? 'text-emerald-600' : 'text-red-600'}`}>
            {totalPnL >= 0 ? '+' : ''}{formatCurrency(totalPnL)}
            <span className="text-sm ml-1">({totalPnLPct.toFixed(1)}%)</span>
          </div>
        </div>
      </div>

      <div className="flex gap-2 flex-wrap">
        <button
          onClick={() => setSelectedPortfolio('all')}
          className={`px-3 py-1.5 text-xs font-medium rounded-lg transition-colors ${
            selectedPortfolio === 'all' ? 'bg-blue-600 text-white' : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
          }`}
        >
          All Portfolios
        </button>
        {activePortfolios.map(p => (
          <button
            key={p.id}
            onClick={() => setSelectedPortfolio(p.id)}
            className={`px-3 py-1.5 text-xs font-medium rounded-lg transition-colors ${
              selectedPortfolio === p.id ? 'bg-blue-600 text-white' : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
            }`}
          >
            {p.clientName.split(' ').slice(0, 2).join(' ')}
          </button>
        ))}
      </div>

      <Card title="Cost Basis vs Market Value by Portfolio">
        <div className="h-64">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis dataKey="name" tick={{ fontSize: 11 }} stroke="#94a3b8" />
              <YAxis tick={{ fontSize: 11 }} stroke="#94a3b8" tickFormatter={(v: number) => `$${v}K`} />
              <Tooltip formatter={(v) => `$${Number(v).toLocaleString()}K`} />
              <Legend />
              <Bar dataKey="costBasis" name="Cost Basis" fill="#94a3b8" radius={[2, 2, 0, 0]} />
              <Bar dataKey="marketValue" name="Market Value" fill="#3b82f6" radius={[2, 2, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </Card>

      <Card title="Position Details">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100">
                <th className="text-left py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">Portfolio</th>
                <th className="text-left py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">Investment</th>
                <th className="text-right py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">Quantity</th>
                <th className="text-right py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">Cost Basis</th>
                <th className="text-right py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">Market Value</th>
                <th className="text-right py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">P&L</th>
                <th className="text-right py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">P&L %</th>
                <th className="text-left py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">Status</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((pos, i) => {
                const pnl = pos.marketValue - pos.costBasis;
                const pnlPct = pos.costBasis > 0 ? (pnl / pos.costBasis) * 100 : 0;
                const port = portfolios.find(p => p.id === pos.portfolioId);
                return (
                  <tr key={i} className="border-b border-slate-50 hover:bg-slate-50">
                    <td className="py-2.5 px-3 text-slate-600 text-xs">{port?.clientName ?? pos.portfolioId}</td>
                    <td className="py-2.5 px-3">
                      <div className="font-medium text-slate-800">{pos.investmentName}</div>
                      <div className="text-xs text-slate-400">{pos.investmentId}</div>
                    </td>
                    <td className="py-2.5 px-3 text-right">{pos.quantity.toLocaleString()}</td>
                    <td className="py-2.5 px-3 text-right text-slate-600">{formatCurrency(pos.costBasis)}</td>
                    <td className="py-2.5 px-3 text-right font-medium">{formatCurrency(pos.marketValue)}</td>
                    <td className={`py-2.5 px-3 text-right font-medium ${pnl >= 0 ? 'text-emerald-600' : 'text-red-600'}`}>
                      {pnl >= 0 ? '+' : ''}{formatCurrency(pnl)}
                    </td>
                    <td className={`py-2.5 px-3 text-right font-medium ${pnl >= 0 ? 'text-emerald-600' : 'text-red-600'}`}>
                      {pnl >= 0 ? '+' : ''}{pnlPct.toFixed(2)}%
                    </td>
                    <td className="py-2.5 px-3">
                      <StatusBadge status={pos.status} label={POSITION_STATUS_LABELS[pos.status]} />
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}

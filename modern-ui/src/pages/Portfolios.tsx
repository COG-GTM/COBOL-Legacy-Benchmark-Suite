import { useState } from 'react';
import { Eye, Plus, Pencil, Trash2, X } from 'lucide-react';
import Card from '../components/Card';
import StatusBadge from '../components/StatusBadge';
import { portfolios as initialPortfolios, positions } from '../data/mockData';
import {
  CLIENT_TYPE_LABELS,
  PORTFOLIO_STATUS_LABELS,
  POSITION_STATUS_LABELS,
} from '../types';
import type { Portfolio } from '../types';

function formatCurrency(v: number): string {
  return v.toLocaleString('en-US', { style: 'currency', currency: 'USD' });
}

export default function Portfolios() {
  const [data, setData] = useState<Portfolio[]>(initialPortfolios);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [filter, setFilter] = useState<'all' | 'A' | 'C' | 'S'>('all');

  const filtered = filter === 'all' ? data : data.filter(p => p.status === filter);
  const selected = data.find(p => p.id === selectedId);
  const selectedPositions = positions.filter(p => p.portfolioId === selectedId);

  function handleDelete(id: string) {
    setData(prev => prev.filter(p => p.id !== id));
    if (selectedId === id) setSelectedId(null);
  }

  function handleCreate(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    const newPort: Portfolio = {
      id: `PORT${String(data.length + 1).padStart(4, '0')}`,
      accountNo: `ACCT${String(100000 + data.length + 1)}`,
      clientName: fd.get('clientName') as string,
      clientType: (fd.get('clientType') as Portfolio['clientType']) ?? 'I',
      createDate: new Date().toISOString().slice(0, 10),
      lastMaintDate: new Date().toISOString().slice(0, 10),
      status: 'A',
      totalValue: 0,
      cashBalance: parseFloat(fd.get('cashBalance') as string) || 0,
      lastUser: 'WEBUSER',
      lastTransDate: new Date().toISOString().slice(0, 10),
    };
    setData(prev => [...prev, newPort]);
    setShowCreate(false);
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Portfolios</h2>
          <p className="text-sm text-slate-500 mt-0.5">
            Manage client portfolios — modernized from PORTADD / PORTREAD / PORTUPDT / PORTDEL
          </p>
        </div>
        <button
          onClick={() => setShowCreate(true)}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors"
        >
          <Plus size={16} /> New Portfolio
        </button>
      </div>

      <div className="flex gap-2">
        {(['all', 'A', 'S', 'C'] as const).map(f => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={`px-3 py-1.5 text-xs font-medium rounded-lg transition-colors ${
              filter === f
                ? 'bg-blue-600 text-white'
                : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
            }`}
          >
            {f === 'all' ? 'All' : PORTFOLIO_STATUS_LABELS[f]}
          </button>
        ))}
      </div>

      <Card>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100">
                <th className="text-left py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">ID</th>
                <th className="text-left py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">Client</th>
                <th className="text-left py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">Type</th>
                <th className="text-left py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">Status</th>
                <th className="text-right py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">Total Value</th>
                <th className="text-right py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">Cash</th>
                <th className="text-left py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">Created</th>
                <th className="text-right py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map(p => (
                <tr
                  key={p.id}
                  className={`border-b border-slate-50 hover:bg-blue-50/50 cursor-pointer transition-colors ${
                    selectedId === p.id ? 'bg-blue-50' : ''
                  }`}
                  onClick={() => setSelectedId(p.id === selectedId ? null : p.id)}
                >
                  <td className="py-3 px-3 font-mono text-xs text-slate-500">{p.id}</td>
                  <td className="py-3 px-3 font-medium text-slate-800">{p.clientName}</td>
                  <td className="py-3 px-3 text-slate-600">{CLIENT_TYPE_LABELS[p.clientType]}</td>
                  <td className="py-3 px-3">
                    <StatusBadge status={p.status} label={PORTFOLIO_STATUS_LABELS[p.status]} />
                  </td>
                  <td className="py-3 px-3 text-right font-medium text-slate-800">
                    {formatCurrency(p.totalValue)}
                  </td>
                  <td className="py-3 px-3 text-right text-slate-600">
                    {formatCurrency(p.cashBalance)}
                  </td>
                  <td className="py-3 px-3 text-slate-500">{p.createDate}</td>
                  <td className="py-3 px-3 text-right">
                    <div className="flex items-center justify-end gap-1">
                      <button
                        onClick={e => { e.stopPropagation(); setSelectedId(p.id); }}
                        className="p-1.5 text-slate-400 hover:text-blue-600 rounded"
                        title="View"
                      >
                        <Eye size={14} />
                      </button>
                      <button className="p-1.5 text-slate-400 hover:text-amber-600 rounded" title="Edit">
                        <Pencil size={14} />
                      </button>
                      <button
                        onClick={e => { e.stopPropagation(); handleDelete(p.id); }}
                        className="p-1.5 text-slate-400 hover:text-red-600 rounded"
                        title="Delete"
                      >
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      {selected && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <Card title={`${selected.clientName}`} className="lg:col-span-1">
            <dl className="space-y-3 text-sm">
              {[
                ['Portfolio ID', selected.id],
                ['Account No', selected.accountNo],
                ['Client Type', CLIENT_TYPE_LABELS[selected.clientType]],
                ['Status', PORTFOLIO_STATUS_LABELS[selected.status]],
                ['Created', selected.createDate],
                ['Last Maintenance', selected.lastMaintDate],
                ['Last User', selected.lastUser],
                ['Total Value', formatCurrency(selected.totalValue)],
                ['Cash Balance', formatCurrency(selected.cashBalance)],
              ].map(([label, value]) => (
                <div key={label} className="flex justify-between">
                  <dt className="text-slate-500">{label}</dt>
                  <dd className="font-medium text-slate-800">{value}</dd>
                </div>
              ))}
            </dl>
          </Card>

          <Card title="Holdings" className="lg:col-span-2">
            {selectedPositions.length === 0 ? (
              <p className="text-sm text-slate-400">No positions found for this portfolio.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-100">
                      <th className="text-left py-2 px-3 text-xs font-medium text-slate-500 uppercase">Investment</th>
                      <th className="text-right py-2 px-3 text-xs font-medium text-slate-500 uppercase">Qty</th>
                      <th className="text-right py-2 px-3 text-xs font-medium text-slate-500 uppercase">Cost Basis</th>
                      <th className="text-right py-2 px-3 text-xs font-medium text-slate-500 uppercase">Market Value</th>
                      <th className="text-right py-2 px-3 text-xs font-medium text-slate-500 uppercase">P&L</th>
                      <th className="text-left py-2 px-3 text-xs font-medium text-slate-500 uppercase">Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {selectedPositions.map((pos, i) => {
                      const pnl = pos.marketValue - pos.costBasis;
                      const pnlPct = pos.costBasis > 0 ? (pnl / pos.costBasis) * 100 : 0;
                      return (
                        <tr key={i} className="border-b border-slate-50">
                          <td className="py-2.5 px-3">
                            <div className="font-medium text-slate-800">{pos.investmentName}</div>
                            <div className="text-xs text-slate-400">{pos.investmentId}</div>
                          </td>
                          <td className="py-2.5 px-3 text-right">{pos.quantity.toLocaleString()}</td>
                          <td className="py-2.5 px-3 text-right">{formatCurrency(pos.costBasis)}</td>
                          <td className="py-2.5 px-3 text-right font-medium">{formatCurrency(pos.marketValue)}</td>
                          <td className={`py-2.5 px-3 text-right font-medium ${pnl >= 0 ? 'text-emerald-600' : 'text-red-600'}`}>
                            {pnl >= 0 ? '+' : ''}{formatCurrency(pnl)} ({pnlPct.toFixed(1)}%)
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
            )}
          </Card>
        </div>
      )}

      {showCreate && (
        <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
              <h3 className="font-semibold text-slate-800">Create Portfolio</h3>
              <button onClick={() => setShowCreate(false)} className="text-slate-400 hover:text-slate-600">
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleCreate} className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Client Name</label>
                <input name="clientName" required className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Client Type</label>
                <select name="clientType" className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500">
                  <option value="I">Individual</option>
                  <option value="C">Corporate</option>
                  <option value="T">Trust</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Initial Cash Balance</label>
                <input name="cashBalance" type="number" step="0.01" defaultValue="0" className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500" />
              </div>
              <div className="flex justify-end gap-3 pt-2">
                <button type="button" onClick={() => setShowCreate(false)} className="px-4 py-2 text-sm text-slate-600 hover:text-slate-800">
                  Cancel
                </button>
                <button type="submit" className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700">
                  Create
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

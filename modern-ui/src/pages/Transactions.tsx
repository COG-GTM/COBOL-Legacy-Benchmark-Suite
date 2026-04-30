import { useState } from 'react';
import { Plus, X, Filter } from 'lucide-react';
import Card from '../components/Card';
import StatusBadge from '../components/StatusBadge';
import { transactions as initialTransactions, portfolios } from '../data/mockData';
import { TXN_TYPE_LABELS, TXN_STATUS_LABELS } from '../types';
import type { Transaction } from '../types';

function formatCurrency(v: number): string {
  return v.toLocaleString('en-US', { style: 'currency', currency: 'USD' });
}

export default function Transactions() {
  const [data, setData] = useState<Transaction[]>(initialTransactions);
  const [typeFilter, setTypeFilter] = useState<string>('all');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [showCreate, setShowCreate] = useState(false);

  const filtered = data
    .filter(t => typeFilter === 'all' || t.type === typeFilter)
    .filter(t => statusFilter === 'all' || t.status === statusFilter);

  const totalVolume = filtered.reduce((s, t) => s + t.amount, 0);

  function handleCreate(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    const qty = parseFloat(fd.get('quantity') as string) || 0;
    const price = parseFloat(fd.get('price') as string) || 0;
    const newTxn: Transaction = {
      id: `TXN-${String(data.length + 1).padStart(6, '0')}`,
      date: new Date().toISOString().slice(0, 10),
      time: new Date().toTimeString().slice(0, 8).replace(/:/g, ''),
      portfolioId: fd.get('portfolioId') as string,
      sequenceNo: String(data.length + 1).padStart(6, '0'),
      investmentId: fd.get('investmentId') as string,
      type: fd.get('type') as Transaction['type'],
      quantity: qty,
      price,
      amount: qty * price,
      currency: 'USD',
      status: 'P',
      processDate: '',
      processUser: '',
    };
    setData(prev => [newTxn, ...prev]);
    setShowCreate(false);
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Transactions</h2>
          <p className="text-sm text-slate-500 mt-0.5">
            Transaction processing — modernized from TRNVAL00
          </p>
        </div>
        <button
          onClick={() => setShowCreate(true)}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors"
        >
          <Plus size={16} /> New Transaction
        </button>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <div className="flex items-center gap-2 text-sm text-slate-500">
          <Filter size={14} />
          <span>Type:</span>
        </div>
        {['all', 'BU', 'SL', 'TR', 'FE'].map(t => (
          <button
            key={t}
            onClick={() => setTypeFilter(t)}
            className={`px-3 py-1.5 text-xs font-medium rounded-lg transition-colors ${
              typeFilter === t ? 'bg-blue-600 text-white' : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
            }`}
          >
            {t === 'all' ? 'All' : TXN_TYPE_LABELS[t as Transaction['type']]}
          </button>
        ))}
        <div className="w-px h-5 bg-slate-200" />
        <div className="flex items-center gap-2 text-sm text-slate-500">
          <span>Status:</span>
        </div>
        {['all', 'D', 'P', 'F', 'R'].map(s => (
          <button
            key={s}
            onClick={() => setStatusFilter(s)}
            className={`px-3 py-1.5 text-xs font-medium rounded-lg transition-colors ${
              statusFilter === s ? 'bg-blue-600 text-white' : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
            }`}
          >
            {s === 'all' ? 'All' : TXN_STATUS_LABELS[s as Transaction['status']]}
          </button>
        ))}
      </div>

      <div className="flex gap-4 text-sm">
        <div className="bg-white border border-slate-200 rounded-lg px-4 py-2.5">
          <span className="text-slate-500">Showing:</span>{' '}
          <span className="font-semibold text-slate-800">{filtered.length}</span> transactions
        </div>
        <div className="bg-white border border-slate-200 rounded-lg px-4 py-2.5">
          <span className="text-slate-500">Total Volume:</span>{' '}
          <span className="font-semibold text-slate-800">{formatCurrency(totalVolume)}</span>
        </div>
      </div>

      <Card>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100">
                <th className="text-left py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">ID</th>
                <th className="text-left py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">Date / Time</th>
                <th className="text-left py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">Portfolio</th>
                <th className="text-left py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">Type</th>
                <th className="text-left py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">Investment</th>
                <th className="text-right py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">Qty</th>
                <th className="text-right py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">Price</th>
                <th className="text-right py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">Amount</th>
                <th className="text-left py-2.5 px-3 text-xs font-medium text-slate-500 uppercase">Status</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map(txn => {
                const port = portfolios.find(p => p.id === txn.portfolioId);
                return (
                  <tr key={txn.id} className="border-b border-slate-50 hover:bg-slate-50">
                    <td className="py-2.5 px-3 font-mono text-xs text-slate-400">{txn.id}</td>
                    <td className="py-2.5 px-3 text-slate-600">
                      <div>{txn.date}</div>
                      <div className="text-xs text-slate-400">{txn.time.replace(/(\d{2})(\d{2})(\d{2})/, '$1:$2:$3')}</div>
                    </td>
                    <td className="py-2.5 px-3 text-slate-800">{port?.clientName ?? txn.portfolioId}</td>
                    <td className="py-2.5 px-3">
                      <span className={`text-xs font-bold px-2 py-0.5 rounded ${
                        txn.type === 'BU' ? 'bg-emerald-50 text-emerald-700' :
                        txn.type === 'SL' ? 'bg-red-50 text-red-700' :
                        txn.type === 'TR' ? 'bg-blue-50 text-blue-700' :
                        'bg-slate-50 text-slate-600'
                      }`}>
                        {TXN_TYPE_LABELS[txn.type]}
                      </span>
                    </td>
                    <td className="py-2.5 px-3 font-mono text-xs text-slate-600">{txn.investmentId}</td>
                    <td className="py-2.5 px-3 text-right text-slate-600">{txn.quantity.toLocaleString()}</td>
                    <td className="py-2.5 px-3 text-right text-slate-600">${txn.price.toFixed(2)}</td>
                    <td className="py-2.5 px-3 text-right font-medium text-slate-800">{formatCurrency(txn.amount)}</td>
                    <td className="py-2.5 px-3">
                      <StatusBadge status={txn.status} label={TXN_STATUS_LABELS[txn.status]} />
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </Card>

      {showCreate && (
        <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
              <h3 className="font-semibold text-slate-800">New Transaction</h3>
              <button onClick={() => setShowCreate(false)} className="text-slate-400 hover:text-slate-600">
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleCreate} className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Portfolio</label>
                <select name="portfolioId" required className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm">
                  {portfolios.filter(p => p.status === 'A').map(p => (
                    <option key={p.id} value={p.id}>{p.clientName} ({p.id})</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Type</label>
                <select name="type" className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm">
                  <option value="BU">Buy</option>
                  <option value="SL">Sell</option>
                  <option value="TR">Transfer</option>
                  <option value="FE">Fee</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Investment ID</label>
                <input name="investmentId" required placeholder="INV-XXXXXX" className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm" />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">Quantity</label>
                  <input name="quantity" type="number" step="0.0001" required className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">Price</label>
                  <input name="price" type="number" step="0.01" required className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm" />
                </div>
              </div>
              <div className="flex justify-end gap-3 pt-2">
                <button type="button" onClick={() => setShowCreate(false)} className="px-4 py-2 text-sm text-slate-600 hover:text-slate-800">Cancel</button>
                <button type="submit" className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700">Submit</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

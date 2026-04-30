import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { fetchTransactions, submitTransaction } from '../services/api';
import { formatCurrency, formatDate } from '../utils/format';
import { TXN_TYPE_LABELS } from '../types';
import StatusBadge from '../components/StatusBadge';
import Loading from '../components/Loading';
import ErrorMessage from '../components/ErrorMessage';
import { Plus, Filter } from 'lucide-react';

export default function Transactions() {
  const [showForm, setShowForm] = useState(false);
  const [filterType, setFilterType] = useState('');
  const [filterPortfolio, setFilterPortfolio] = useState('');

  const { data, isLoading, error } = useQuery({
    queryKey: ['transactions', filterType, filterPortfolio],
    queryFn: () => fetchTransactions(filterPortfolio || undefined, filterType || undefined),
  });

  if (isLoading) return <Loading text="Loading transactions..." />;
  if (error) return <ErrorMessage message={(error as Error).message} />;

  const transactions = data?.transactions ?? [];

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Transaction History</h1>
          <p className="text-slate-400 mt-1">Browse and submit transactions — replaces CICS HISMAP screen</p>
        </div>
        <button onClick={() => setShowForm(!showForm)} className="btn-primary flex items-center gap-2">
          <Plus size={18} />
          New Transaction
        </button>
      </div>

      {showForm && <TransactionForm onClose={() => setShowForm(false)} />}

      {/* Filters */}
      <div className="flex items-center gap-4">
        <Filter size={16} className="text-slate-400" />
        <input
          className="input max-w-xs"
          placeholder="Filter by portfolio ID..."
          value={filterPortfolio}
          onChange={e => setFilterPortfolio(e.target.value)}
        />
        <select
          className="input max-w-xs"
          value={filterType}
          onChange={e => setFilterType(e.target.value)}
        >
          <option value="">All Types</option>
          <option value="BU">Buy</option>
          <option value="SL">Sell</option>
          <option value="TR">Transfer</option>
          <option value="FE">Fee</option>
        </select>
      </div>

      {/* Table */}
      <div className="card">
        <p className="text-sm text-slate-400 mb-4">{data?.total ?? 0} transactions</p>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-700 text-left text-slate-400">
                <th className="pb-3 pr-4">Date</th>
                <th className="pb-3 pr-4">Portfolio</th>
                <th className="pb-3 pr-4">Investment</th>
                <th className="pb-3 pr-4">Type</th>
                <th className="pb-3 pr-4 text-right">Qty</th>
                <th className="pb-3 pr-4 text-right">Price</th>
                <th className="pb-3 pr-4 text-right">Amount</th>
                <th className="pb-3">Status</th>
              </tr>
            </thead>
            <tbody>
              {transactions.map(txn => (
                <tr key={txn.transaction_id} className="border-b border-slate-700/50 hover:bg-slate-700/20">
                  <td className="py-3 pr-4">{formatDate(txn.transaction_date)}</td>
                  <td className="py-3 pr-4 font-mono text-blue-400">{txn.portfolio_id}</td>
                  <td className="py-3 pr-4 font-mono">{txn.investment_id}</td>
                  <td className="py-3 pr-4">
                    <span className={`badge ${txn.transaction_type === 'BU' ? 'badge-success' : txn.transaction_type === 'SL' ? 'badge-danger' : 'badge-info'}`}>
                      {TXN_TYPE_LABELS[txn.transaction_type] ?? txn.transaction_type}
                    </span>
                  </td>
                  <td className="py-3 pr-4 text-right">{txn.quantity.toLocaleString()}</td>
                  <td className="py-3 pr-4 text-right">{formatCurrency(txn.price)}</td>
                  <td className="py-3 pr-4 text-right font-medium">{formatCurrency(txn.amount)}</td>
                  <td className="py-3"><StatusBadge status={txn.status} /></td>
                </tr>
              ))}
              {transactions.length === 0 && (
                <tr>
                  <td colSpan={8} className="py-10 text-center text-slate-500">No transactions found</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function TransactionForm({ onClose }: { onClose: () => void }) {
  const queryClient = useQueryClient();
  const [formError, setFormError] = useState('');
  const [form, setForm] = useState({
    portfolio_id: '',
    investment_id: '',
    transaction_type: 'BU',
    quantity: '',
    price: '',
  });

  const mutation = useMutation({
    mutationFn: submitTransaction,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
      queryClient.invalidateQueries({ queryKey: ['statistics'] });
      queryClient.invalidateQueries({ queryKey: ['portfolios'] });
      onClose();
    },
    onError: (err: Error) => setFormError(err.message),
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setFormError('');
    mutation.mutate({
      portfolio_id: form.portfolio_id,
      investment_id: form.investment_id,
      transaction_type: form.transaction_type,
      quantity: parseFloat(form.quantity),
      price: parseFloat(form.price),
    });
  };

  return (
    <div className="card border-blue-500/50">
      <h3 className="text-lg font-semibold mb-4">Submit New Transaction</h3>
      {formError && <ErrorMessage message={formError} />}
      <form onSubmit={handleSubmit} className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
        <div>
          <label className="block text-xs text-slate-400 mb-1">Portfolio ID (8 chars)</label>
          <input className="input" maxLength={8} required value={form.portfolio_id} onChange={e => setForm({ ...form, portfolio_id: e.target.value })} placeholder="PORT0001" />
        </div>
        <div>
          <label className="block text-xs text-slate-400 mb-1">Investment ID</label>
          <input className="input" maxLength={10} required value={form.investment_id} onChange={e => setForm({ ...form, investment_id: e.target.value })} placeholder="AAPL" />
        </div>
        <div>
          <label className="block text-xs text-slate-400 mb-1">Type</label>
          <select className="input" value={form.transaction_type} onChange={e => setForm({ ...form, transaction_type: e.target.value })}>
            <option value="BU">Buy</option>
            <option value="SL">Sell</option>
            <option value="TR">Transfer</option>
            <option value="FE">Fee</option>
          </select>
        </div>
        <div>
          <label className="block text-xs text-slate-400 mb-1">Quantity</label>
          <input className="input" type="number" step="0.0001" min="0.0001" required value={form.quantity} onChange={e => setForm({ ...form, quantity: e.target.value })} />
        </div>
        <div>
          <label className="block text-xs text-slate-400 mb-1">Price</label>
          <input className="input" type="number" step="0.0001" min="0.0001" required value={form.price} onChange={e => setForm({ ...form, price: e.target.value })} />
        </div>
        <div className="flex items-end gap-2">
          <button type="submit" className="btn-primary flex-1" disabled={mutation.isPending}>
            {mutation.isPending ? 'Submitting...' : 'Submit'}
          </button>
          <button type="button" onClick={onClose} className="btn-secondary">Cancel</button>
        </div>
      </form>
    </div>
  );
}

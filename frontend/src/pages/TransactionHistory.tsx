import { useState, useMemo } from 'react';
import { Filter, ArrowUpDown, ArrowUpRight, ArrowDownRight, ArrowRightLeft, Receipt } from 'lucide-react';
import { transactions, portfolios } from '../data/mockData';
import StatusBadge, { getTransactionStatusVariant } from '../components/StatusBadge';
import { formatCurrency, formatDate, formatTime, cn } from '../lib/format';
import type { TransactionTypeFilter, StatusFilter } from '../types';

const typeIcons = {
  BUY: ArrowUpRight,
  SELL: ArrowDownRight,
  TRANSFER: ArrowRightLeft,
  FEE: Receipt,
};

const typeColors = {
  BUY: 'text-gain',
  SELL: 'text-loss',
  TRANSFER: 'text-info',
  FEE: 'text-warning',
};

type SortField = 'date' | 'symbol' | 'amount' | 'type';
type SortDir = 'asc' | 'desc';

export default function TransactionHistory() {
  const [typeFilter, setTypeFilter] = useState<TransactionTypeFilter>('ALL');
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
  const [portfolioFilter, setPortfolioFilter] = useState<string>('ALL');
  const [sortField, setSortField] = useState<SortField>('date');
  const [sortDir, setSortDir] = useState<SortDir>('desc');

  const toggleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDir(d => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortField(field);
      setSortDir('desc');
    }
  };

  const filtered = useMemo(() => {
    let result = [...transactions];

    if (typeFilter !== 'ALL') {
      result = result.filter(t => t.type === typeFilter);
    }
    if (statusFilter !== 'ALL') {
      result = result.filter(t => t.status === statusFilter);
    }
    if (portfolioFilter !== 'ALL') {
      result = result.filter(t => t.portfolioId === portfolioFilter);
    }

    result.sort((a, b) => {
      let cmp = 0;
      switch (sortField) {
        case 'date':
          cmp = `${a.date}${a.time}`.localeCompare(`${b.date}${b.time}`);
          break;
        case 'symbol':
          cmp = a.symbol.localeCompare(b.symbol);
          break;
        case 'amount':
          cmp = a.amount - b.amount;
          break;
        case 'type':
          cmp = a.type.localeCompare(b.type);
          break;
      }
      return sortDir === 'asc' ? cmp : -cmp;
    });

    return result;
  }, [typeFilter, statusFilter, portfolioFilter, sortField, sortDir]);

  const totalVolume = filtered.reduce((s, t) => s + t.amount, 0);
  const totalFees = filtered.reduce((s, t) => s + t.fees, 0);

  const SortButton = ({ field, children }: { field: SortField; children: React.ReactNode }) => (
    <button
      onClick={() => toggleSort(field)}
      className="flex items-center gap-1 hover:text-text-primary transition-colors"
    >
      {children}
      <ArrowUpDown
        size={12}
        className={cn(sortField === field ? 'text-accent-1' : 'text-text-muted')}
      />
    </button>
  );

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-text-primary">Transaction History</h2>
        <p className="text-text-secondary text-sm mt-1">
          Browse and filter all portfolio transactions
        </p>
      </div>

      {/* Summary */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-surface rounded-xl border border-border p-4">
          <p className="text-sm text-text-muted">Matching Transactions</p>
          <p className="text-xl font-bold mt-1 text-text-primary">{filtered.length}</p>
        </div>
        <div className="bg-surface rounded-xl border border-border p-4">
          <p className="text-sm text-text-muted">Total Volume</p>
          <p className="text-xl font-bold mt-1 tabular-nums text-text-primary">{formatCurrency(totalVolume)}</p>
        </div>
        <div className="bg-surface rounded-xl border border-border p-4">
          <p className="text-sm text-text-muted">Total Fees</p>
          <p className="text-xl font-bold mt-1 tabular-nums text-text-primary">{formatCurrency(totalFees)}</p>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-surface rounded-xl border border-border p-4">
        <div className="flex items-center gap-2 mb-3">
          <Filter size={16} className="text-text-muted" />
          <span className="text-sm font-medium text-text-secondary">Filters</span>
        </div>
        <div className="flex flex-wrap gap-3">
          <select
            value={portfolioFilter}
            onChange={e => setPortfolioFilter(e.target.value)}
            className="px-3 py-2 rounded-lg border border-border bg-surface-secondary text-sm text-text-primary
                       focus:outline-none focus:ring-2 focus:ring-accent-1"
          >
            <option value="ALL">All Portfolios</option>
            {portfolios.map(p => (
              <option key={p.portfolioId} value={p.portfolioId}>
                {p.clientName} ({p.portfolioId})
              </option>
            ))}
          </select>

          <select
            value={typeFilter}
            onChange={e => setTypeFilter(e.target.value as TransactionTypeFilter)}
            className="px-3 py-2 rounded-lg border border-border bg-surface-secondary text-sm text-text-primary
                       focus:outline-none focus:ring-2 focus:ring-accent-1"
          >
            <option value="ALL">All Types</option>
            <option value="BUY">Buy</option>
            <option value="SELL">Sell</option>
            <option value="TRANSFER">Transfer</option>
            <option value="FEE">Fee</option>
          </select>

          <select
            value={statusFilter}
            onChange={e => setStatusFilter(e.target.value as StatusFilter)}
            className="px-3 py-2 rounded-lg border border-border bg-surface-secondary text-sm text-text-primary
                       focus:outline-none focus:ring-2 focus:ring-accent-1"
          >
            <option value="ALL">All Statuses</option>
            <option value="Done">Done</option>
            <option value="Pending">Pending</option>
            <option value="Failed">Failed</option>
            <option value="Reversed">Reversed</option>
          </select>
        </div>
      </div>

      {/* Table */}
      <div className="bg-surface rounded-xl border border-border overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-surface border-b border-border">
                <th className="text-left py-3 px-4 text-text-heading font-medium text-xs uppercase tracking-wider">
                  <SortButton field="date">Date / Time</SortButton>
                </th>
                <th className="text-left py-3 px-4 text-text-heading font-medium text-xs uppercase tracking-wider">
                  <SortButton field="type">Type</SortButton>
                </th>
                <th className="text-left py-3 px-4 text-text-heading font-medium text-xs uppercase tracking-wider">
                  <SortButton field="symbol">Asset</SortButton>
                </th>
                <th className="text-left py-3 px-4 text-text-heading font-medium text-xs uppercase tracking-wider">Portfolio</th>
                <th className="text-right py-3 px-4 text-text-heading font-medium text-xs uppercase tracking-wider">Qty</th>
                <th className="text-right py-3 px-4 text-text-heading font-medium text-xs uppercase tracking-wider">Price</th>
                <th className="text-right py-3 px-4 text-text-heading font-medium text-xs uppercase tracking-wider">
                  <SortButton field="amount">Amount</SortButton>
                </th>
                <th className="text-center py-3 px-4 text-text-heading font-medium text-xs uppercase tracking-wider">Status</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((tx, i) => {
                const Icon = typeIcons[tx.type];
                const portfolio = portfolios.find(p => p.portfolioId === tx.portfolioId);
                return (
                  <tr
                    key={tx.transactionId}
                    className={cn(
                      'border-b border-border/50 hover:bg-surface-alt/50 transition-colors animate-fade-in-up',
                      i % 2 === 1 && 'bg-surface-alt/30'
                    )}
                    style={{ animationDelay: `${i * 30}ms` }}
                  >
                    <td className="py-3 px-4">
                      <p className="font-medium text-text-primary">{formatDate(tx.date)}</p>
                      <p className="text-xs text-text-muted">{formatTime(tx.time)}</p>
                    </td>
                    <td className="py-3 px-4">
                      <div className="flex items-center gap-2">
                        <Icon size={14} className={typeColors[tx.type]} />
                        <span className="font-medium text-text-secondary">{tx.type}</span>
                      </div>
                    </td>
                    <td className="py-3 px-4 font-semibold text-text-primary">{tx.symbol}</td>
                    <td className="py-3 px-4">
                      <p className="text-xs text-text-muted">{portfolio?.clientName ?? tx.portfolioId}</p>
                    </td>
                    <td className="py-3 px-4 text-right tabular-nums text-text-secondary">
                      {tx.quantity > 0 ? tx.quantity.toLocaleString() : '-'}
                    </td>
                    <td className="py-3 px-4 text-right tabular-nums text-text-secondary">
                      {tx.price > 0 ? formatCurrency(tx.price) : '-'}
                    </td>
                    <td className="py-3 px-4 text-right tabular-nums font-medium text-text-primary">
                      {formatCurrency(tx.amount)}
                      {tx.fees > 0 && (
                        <p className="text-xs text-text-muted">+{formatCurrency(tx.fees)} fee</p>
                      )}
                    </td>
                    <td className="py-3 px-4 text-center">
                      <StatusBadge label={tx.status} variant={getTransactionStatusVariant(tx.status)} />
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        {filtered.length === 0 && (
          <div className="py-12 text-center text-text-muted">
            No transactions match the selected filters.
          </div>
        )}
      </div>
    </div>
  );
}

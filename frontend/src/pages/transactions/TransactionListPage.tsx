import { useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import {
  Plus,
  Filter,
  ChevronUp,
  ChevronDown,
  ChevronsUpDown,
  ChevronLeft,
  ChevronRight,
  ArrowLeftRight,
} from 'lucide-react';
import { PageHeader } from '@/components/ui/PageHeader';
import { SearchInput } from '@/components/ui/SearchInput';
import { StatusBadge, getTransactionStatusVariant, getTransactionStatusLabel, getTransTypeLabel } from '@/components/ui/StatusBadge';
import { EmptyState } from '@/components/ui/EmptyState';
import { transactions } from '@/data/mockData';
import type { Transaction } from '@/data/types';

type TransTypeFilter = 'ALL' | 'BY' | 'SL' | 'FE';
type StatusFilter = 'ALL' | 'P' | 'C' | 'E';
type SortKey = 'transId' | 'accountNo' | 'fundId' | 'transType' | 'transDate' | 'shareQty' | 'price' | 'amount' | 'status';
type SortDir = 'asc' | 'desc';

const PAGE_SIZE = 10;

const transTypeVariant: Record<string, 'info' | 'error' | 'neutral'> = {
  BY: 'info',
  SL: 'error',
  FE: 'neutral',
};

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
}

function formatQuantity(value: number): string {
  return new Intl.NumberFormat('en-US', { minimumFractionDigits: 3, maximumFractionDigits: 3 }).format(value);
}

export function TransactionListPage() {
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState<TransTypeFilter>('ALL');
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [sortKey, setSortKey] = useState<SortKey>('transDate');
  const [sortDir, setSortDir] = useState<SortDir>('desc');
  const [page, setPage] = useState(0);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [showFilters, setShowFilters] = useState(false);

  const filtered = useMemo(() => {
    let data = [...transactions];

    if (search) {
      const q = search.toLowerCase();
      data = data.filter(
        (t) =>
          t.transId.toLowerCase().includes(q) ||
          t.accountNo.toLowerCase().includes(q) ||
          t.fundId.toLowerCase().includes(q),
      );
    }

    if (typeFilter !== 'ALL') {
      data = data.filter((t) => t.transType === typeFilter);
    }

    if (statusFilter !== 'ALL') {
      data = data.filter((t) => t.status === statusFilter);
    }

    if (dateFrom) {
      data = data.filter((t) => t.transDate >= dateFrom);
    }

    if (dateTo) {
      data = data.filter((t) => t.transDate <= dateTo);
    }

    return data;
  }, [search, typeFilter, statusFilter, dateFrom, dateTo]);

  const sorted = useMemo(() => {
    return [...filtered].sort((a, b) => {
      const aVal = a[sortKey];
      const bVal = b[sortKey];
      if (typeof aVal === 'number' && typeof bVal === 'number') {
        return sortDir === 'asc' ? aVal - bVal : bVal - aVal;
      }
      const aStr = String(aVal);
      const bStr = String(bVal);
      return sortDir === 'asc' ? aStr.localeCompare(bStr) : bStr.localeCompare(aStr);
    });
  }, [filtered, sortKey, sortDir]);

  const totalPages = Math.max(1, Math.ceil(sorted.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages - 1);
  const paged = sorted.slice(currentPage * PAGE_SIZE, (currentPage + 1) * PAGE_SIZE);

  const handleSort = (key: SortKey) => {
    if (sortKey === key) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortKey(key);
      setSortDir('asc');
    }
  };

  const handleFilterReset = () => {
    setTypeFilter('ALL');
    setStatusFilter('ALL');
    setDateFrom('');
    setDateTo('');
    setSearch('');
    setPage(0);
  };

  const sortIcon = (col: SortKey) => {
    if (sortKey !== col) return <ChevronsUpDown className="w-3.5 h-3.5 text-slate-400" />;
    return sortDir === 'asc'
      ? <ChevronUp className="w-3.5 h-3.5 text-blue-600" />
      : <ChevronDown className="w-3.5 h-3.5 text-blue-600" />;
  };

  const renderSortHeader = (col: SortKey, label: string, className = '') => (
    <th
      key={col}
      className={`px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider cursor-pointer select-none hover:bg-slate-100 ${className}`}
      onClick={() => handleSort(col)}
    >
      <div className="flex items-center gap-1">
        {label}
        {sortIcon(col)}
      </div>
    </th>
  );

  return (
    <div>
      <PageHeader
        title="Transaction History"
        description="View and filter all transactions"
        actions={
          <Link
            to="/transactions/new"
            className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors"
          >
            <Plus className="w-4 h-4" />
            New Transaction
          </Link>
        }
      />

      <div className="bg-white rounded-lg border border-slate-200 shadow-sm">
        <div className="p-4 border-b border-slate-200 space-y-4">
          <div className="flex flex-col sm:flex-row gap-3">
            <SearchInput
              value={search}
              onChange={(v) => { setSearch(v); setPage(0); }}
              placeholder="Search by Transaction ID, Account No, or Fund ID..."
              className="flex-1"
            />
            <button
              onClick={() => setShowFilters((f) => !f)}
              className={`inline-flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-lg border transition-colors ${
                showFilters
                  ? 'bg-blue-50 text-blue-700 border-blue-200'
                  : 'bg-white text-slate-700 border-slate-300 hover:bg-slate-50'
              }`}
            >
              <Filter className="w-4 h-4" />
              Filters
            </button>
          </div>

          {showFilters && (
            <div className="flex flex-wrap gap-4 items-end pt-2">
              <div>
                <label className="block text-xs font-medium text-slate-600 mb-1">Transaction Type</label>
                <select
                  value={typeFilter}
                  onChange={(e) => { setTypeFilter(e.target.value as TransTypeFilter); setPage(0); }}
                  className="block w-36 px-3 py-2 text-sm border border-slate-300 rounded-lg bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="ALL">All Types</option>
                  <option value="BY">Buy</option>
                  <option value="SL">Sell</option>
                  <option value="FE">Fee</option>
                </select>
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-600 mb-1">Status</label>
                <select
                  value={statusFilter}
                  onChange={(e) => { setStatusFilter(e.target.value as StatusFilter); setPage(0); }}
                  className="block w-36 px-3 py-2 text-sm border border-slate-300 rounded-lg bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="ALL">All Statuses</option>
                  <option value="P">Pending</option>
                  <option value="C">Complete</option>
                  <option value="E">Error</option>
                </select>
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-600 mb-1">Date From</label>
                <input
                  type="date"
                  value={dateFrom}
                  onChange={(e) => { setDateFrom(e.target.value); setPage(0); }}
                  className="block w-40 px-3 py-2 text-sm border border-slate-300 rounded-lg bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-600 mb-1">Date To</label>
                <input
                  type="date"
                  value={dateTo}
                  onChange={(e) => { setDateTo(e.target.value); setPage(0); }}
                  className="block w-40 px-3 py-2 text-sm border border-slate-300 rounded-lg bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <button
                onClick={handleFilterReset}
                className="px-3 py-2 text-sm text-slate-600 hover:text-slate-900 transition-colors"
              >
                Reset
              </button>
            </div>
          )}
        </div>

        {sorted.length === 0 ? (
          <EmptyState
            title="No transactions found"
            message="Try adjusting your search or filter criteria."
            icon={<ArrowLeftRight className="w-12 h-12" />}
          />
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200">
                <thead className="bg-slate-50">
                  <tr>
                    {renderSortHeader('transId', 'Trans ID')}
                    {renderSortHeader('accountNo', 'Account No')}
                    {renderSortHeader('fundId', 'Fund ID')}
                    {renderSortHeader('transType', 'Type')}
                    {renderSortHeader('transDate', 'Date')}
                    {renderSortHeader('shareQty', 'Quantity', 'text-right')}
                    {renderSortHeader('price', 'Price', 'text-right')}
                    {renderSortHeader('amount', 'Amount', 'text-right')}
                    {renderSortHeader('status', 'Status')}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200">
                  {paged.map((txn) => (
                    <TransactionRow
                      key={txn.transId}
                      txn={txn}
                      expanded={expandedId === txn.transId}
                      onToggle={() => setExpandedId(expandedId === txn.transId ? null : txn.transId)}
                    />
                  ))}
                </tbody>
              </table>
            </div>

            <div className="flex items-center justify-between px-4 py-3 border-t border-slate-200">
              <p className="text-sm text-slate-600">
                Showing {currentPage * PAGE_SIZE + 1}–{Math.min((currentPage + 1) * PAGE_SIZE, sorted.length)} of {sorted.length} transactions
              </p>
              <div className="flex items-center gap-2">
                <button
                  disabled={currentPage === 0}
                  onClick={() => setPage((p) => p - 1)}
                  className="p-1.5 rounded-lg border border-slate-300 text-slate-600 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>
                <span className="text-sm text-slate-700 font-medium">
                  Page {currentPage + 1} of {totalPages}
                </span>
                <button
                  disabled={currentPage >= totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                  className="p-1.5 rounded-lg border border-slate-300 text-slate-600 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                >
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

function TransactionRow({
  txn,
  expanded,
  onToggle,
}: {
  txn: Transaction;
  expanded: boolean;
  onToggle: () => void;
}) {
  return (
    <>
      <tr
        className="hover:bg-slate-50 transition-colors cursor-pointer"
        onClick={onToggle}
      >
        <td className="px-4 py-3 text-sm font-mono text-slate-900">{txn.transId}</td>
        <td className="px-4 py-3 text-sm text-slate-600">{txn.accountNo}</td>
        <td className="px-4 py-3 text-sm font-mono text-slate-600">{txn.fundId}</td>
        <td className="px-4 py-3">
          <StatusBadge
            label={getTransTypeLabel(txn.transType)}
            variant={transTypeVariant[txn.transType] ?? 'neutral'}
          />
        </td>
        <td className="px-4 py-3 text-sm text-slate-600">{txn.transDate}</td>
        <td className="px-4 py-3 text-sm text-slate-700 text-right font-medium tabular-nums">
          {txn.shareQty > 0 ? formatQuantity(txn.shareQty) : '—'}
        </td>
        <td className="px-4 py-3 text-sm text-slate-700 text-right tabular-nums">
          {txn.price > 0 ? formatCurrency(txn.price) : '—'}
        </td>
        <td className="px-4 py-3 text-sm text-slate-900 text-right font-medium tabular-nums">
          {formatCurrency(txn.amount)}
        </td>
        <td className="px-4 py-3">
          <StatusBadge
            label={getTransactionStatusLabel(txn.status)}
            variant={getTransactionStatusVariant(txn.status)}
          />
        </td>
      </tr>
      {expanded && (
        <tr className="bg-slate-50">
          <td colSpan={9} className="px-4 py-4">
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-sm">
              <div>
                <p className="text-slate-500 text-xs font-medium uppercase">Transaction ID</p>
                <p className="font-mono text-slate-900 mt-0.5">{txn.transId}</p>
              </div>
              <div>
                <p className="text-slate-500 text-xs font-medium uppercase">Before Balance</p>
                <p className="font-medium text-slate-900 mt-0.5 tabular-nums">
                  {formatQuantity(txn.beforeBalance)}
                </p>
              </div>
              <div>
                <p className="text-slate-500 text-xs font-medium uppercase">After Balance</p>
                <p className="font-medium text-slate-900 mt-0.5 tabular-nums">
                  {formatQuantity(txn.afterBalance)}
                </p>
              </div>
              <div>
                <p className="text-slate-500 text-xs font-medium uppercase">Timestamp</p>
                <p className="text-slate-900 mt-0.5">{txn.transDate} 18:00:00</p>
              </div>
            </div>
          </td>
        </tr>
      )}
    </>
  );
}

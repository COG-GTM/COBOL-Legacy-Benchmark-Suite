import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { ChevronLeft, ChevronRight, Plus } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { EmptyState } from '@/components/ui/EmptyState';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';
import { PageHeader } from '@/components/ui/PageHeader';
import {
  StatusBadge,
  getTransactionStatusLabel,
  getTransactionStatusVariant,
  getTransTypeLabel,
} from '@/components/ui/StatusBadge';
import { portfolios } from '@/data/mockData';
import type { Transaction } from '@/data/types';
import {
  accountNoForPortfolio,
  portfolioNameForAccount,
  useTransactionStore,
} from './transactionStore';

const PAGE_SIZE = 10;

const TYPE_OPTIONS = [
  { code: 'BU', label: 'Buy' },
  { code: 'SL', label: 'Sell' },
  { code: 'TR', label: 'Transfer' },
  { code: 'FE', label: 'Fee' },
] as const;

const STATUS_OPTIONS = [
  { code: 'P', label: 'Pending' },
  { code: 'D', label: 'Done' },
  { code: 'F', label: 'Failed' },
  { code: 'R', label: 'Reversed' },
] as const;

const currencyFormat = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
});

const quantityFormat = new Intl.NumberFormat('en-US', {
  minimumFractionDigits: 3,
  maximumFractionDigits: 3,
});

const selectClass =
  'px-3 py-2 text-sm border border-slate-300 rounded-lg bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500';

export function TransactionListPage() {
  const { transactions } = useTransactionStore();
  const [loading, setLoading] = useState(true);
  const [portfolioFilter, setPortfolioFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [page, setPage] = useState(1);

  useEffect(() => {
    const timer = setTimeout(() => setLoading(false), 400);
    return () => clearTimeout(timer);
  }, []);

  const filtered = useMemo(() => {
    const accountNo = portfolioFilter ? accountNoForPortfolio(portfolioFilter) : '';
    return [...transactions]
      .filter((t) => !accountNo || t.accountNo === accountNo)
      .filter((t) => !typeFilter || t.transType === typeFilter)
      .filter((t) => !statusFilter || t.status === statusFilter)
      .filter((t) => !dateFrom || t.transDate >= dateFrom)
      .filter((t) => !dateTo || t.transDate <= dateTo)
      .sort((a, b) => b.transDate.localeCompare(a.transDate) || b.transId.localeCompare(a.transId));
  }, [transactions, portfolioFilter, typeFilter, statusFilter, dateFrom, dateTo]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages);
  const pageRows = filtered.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  const resetFilters = () => {
    setPortfolioFilter('');
    setTypeFilter('');
    setStatusFilter('');
    setDateFrom('');
    setDateTo('');
    setPage(1);
  };

  const hasFilters = Boolean(portfolioFilter || typeFilter || statusFilter || dateFrom || dateTo);

  const columns: Column<Transaction>[] = [
    { key: 'transId', header: 'Trans ID', sortable: true },
    { key: 'transDate', header: 'Date', sortable: true },
    {
      key: 'accountNo',
      header: 'Portfolio',
      render: (t) => (
        <div>
          <div className="font-medium text-slate-900">{portfolioNameForAccount(t.accountNo)}</div>
          <div className="text-xs text-slate-500">{t.accountNo}</div>
        </div>
      ),
    },
    { key: 'fundId', header: 'Fund', sortable: true },
    {
      key: 'transType',
      header: 'Type',
      render: (t) => (
        <span title={t.transType}>
          {getTransTypeLabel(t.transType)}
          <span className="ml-1 text-xs text-slate-400">({t.transType})</span>
        </span>
      ),
    },
    {
      key: 'shareQty',
      header: 'Quantity',
      sortable: true,
      className: 'text-right',
      render: (t) => quantityFormat.format(t.shareQty),
    },
    {
      key: 'price',
      header: 'Price',
      sortable: true,
      className: 'text-right',
      render: (t) => currencyFormat.format(t.price),
    },
    {
      key: 'amount',
      header: 'Amount',
      sortable: true,
      className: 'text-right',
      render: (t) => currencyFormat.format(t.amount),
    },
    {
      key: 'status',
      header: 'Status',
      render: (t) => (
        <StatusBadge
          label={`${getTransactionStatusLabel(t.status)} (${t.status})`}
          variant={getTransactionStatusVariant(t.status)}
        />
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title="Transaction History"
        description="View and filter all portfolio transactions"
        actions={
          <Link
            to="/transactions/new"
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
          >
            <Plus className="w-4 h-4" />
            New Transaction
          </Link>
        }
      />

      <Card className="mb-6">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
          <div>
            <label htmlFor="filter-portfolio" className="block text-xs font-medium text-slate-500 mb-1">
              Portfolio
            </label>
            <select
              id="filter-portfolio"
              value={portfolioFilter}
              onChange={(e) => {
                setPortfolioFilter(e.target.value);
                setPage(1);
              }}
              className={`w-full ${selectClass}`}
            >
              <option value="">All portfolios</option>
              {portfolios.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.id} — {p.name}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="filter-from" className="block text-xs font-medium text-slate-500 mb-1">
              From date
            </label>
            <input
              id="filter-from"
              type="date"
              value={dateFrom}
              onChange={(e) => {
                setDateFrom(e.target.value);
                setPage(1);
              }}
              className={`w-full ${selectClass}`}
            />
          </div>
          <div>
            <label htmlFor="filter-to" className="block text-xs font-medium text-slate-500 mb-1">
              To date
            </label>
            <input
              id="filter-to"
              type="date"
              value={dateTo}
              onChange={(e) => {
                setDateTo(e.target.value);
                setPage(1);
              }}
              className={`w-full ${selectClass}`}
            />
          </div>
          <div>
            <label htmlFor="filter-type" className="block text-xs font-medium text-slate-500 mb-1">
              Type
            </label>
            <select
              id="filter-type"
              value={typeFilter}
              onChange={(e) => {
                setTypeFilter(e.target.value);
                setPage(1);
              }}
              className={`w-full ${selectClass}`}
            >
              <option value="">All types</option>
              {TYPE_OPTIONS.map((t) => (
                <option key={t.code} value={t.code}>
                  {t.label} ({t.code})
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="filter-status" className="block text-xs font-medium text-slate-500 mb-1">
              Status
            </label>
            <select
              id="filter-status"
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(1);
              }}
              className={`w-full ${selectClass}`}
            >
              <option value="">All statuses</option>
              {STATUS_OPTIONS.map((s) => (
                <option key={s.code} value={s.code}>
                  {s.label} ({s.code})
                </option>
              ))}
            </select>
          </div>
        </div>
      </Card>

      <Card>
        {loading ? (
          <LoadingSpinner message="Loading transactions..." />
        ) : filtered.length === 0 ? (
          <EmptyState
            title="No transactions found"
            message={
              hasFilters
                ? 'No transactions match the current filters.'
                : 'There are no transactions to display.'
            }
            action={
              hasFilters ? (
                <button
                  onClick={resetFilters}
                  className="px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
                >
                  Clear filters
                </button>
              ) : undefined
            }
          />
        ) : (
          <>
            <div className="-m-6 mb-0">
              <DataTable columns={columns} data={pageRows} keyExtractor={(t) => t.transId} />
            </div>
            <div className="flex items-center justify-between pt-4 mt-6 border-t border-slate-200">
              <p className="text-sm text-slate-500">
                Showing {(currentPage - 1) * PAGE_SIZE + 1}–
                {Math.min(currentPage * PAGE_SIZE, filtered.length)} of {filtered.length}{' '}
                transactions
              </p>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setPage((p) => Math.max(1, p - 1))}
                  disabled={currentPage <= 1}
                  className="inline-flex items-center gap-1 px-3 py-1.5 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  <ChevronLeft className="w-4 h-4" />
                  Previous
                </button>
                <span className="text-sm text-slate-600">
                  Page {currentPage} of {totalPages}
                </span>
                <button
                  onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                  disabled={currentPage >= totalPages}
                  className="inline-flex items-center gap-1 px-3 py-1.5 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  Next
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          </>
        )}
      </Card>
    </div>
  );
}

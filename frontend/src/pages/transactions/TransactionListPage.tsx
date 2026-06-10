import { useState, useMemo } from 'react';
import { ChevronLeft, ChevronRight, ArrowLeftRight } from 'lucide-react';
import { PageHeader } from '@/components/ui/PageHeader';
import { SearchInput } from '@/components/ui/SearchInput';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { EmptyState } from '@/components/ui/EmptyState';
import {
  StatusBadge,
  getTransactionStatusVariant,
  getTransactionStatusLabel,
  getTransTypeLabel,
} from '@/components/ui/StatusBadge';
import { transactions } from '@/data/mockData';
import type { Transaction } from '@/data/types';
import type { Column } from '@/components/ui/DataTable';

type TransactionRow = Transaction & Record<string, unknown>;

const PAGE_SIZE = 10;

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
}

function getTransTypeVariant(type: string): 'info' | 'error' | 'warning' {
  switch (type) {
    case 'BY': return 'info';
    case 'SL': return 'error';
    case 'FE': return 'warning';
    default: return 'info';
  }
}

const columns: Column<TransactionRow>[] = [
  {
    key: 'transDate',
    header: 'Date',
    sortable: true,
  },
  {
    key: 'accountNo',
    header: 'Account',
    sortable: true,
    render: (row) => <span className="font-mono">{row.accountNo}</span>,
  },
  {
    key: 'transType',
    header: 'Type',
    sortable: true,
    render: (row) => (
      <StatusBadge label={getTransTypeLabel(row.transType)} variant={getTransTypeVariant(row.transType)} />
    ),
  },
  {
    key: 'fundId',
    header: 'Fund ID',
    sortable: true,
    render: (row) => <span className="font-mono">{row.fundId}</span>,
  },
  {
    key: 'shareQty',
    header: 'Units',
    sortable: true,
    className: 'text-right',
    render: (row) => (
      <span className="text-right block">
        {row.shareQty.toLocaleString('en-US', { minimumFractionDigits: 3 })}
      </span>
    ),
  },
  {
    key: 'price',
    header: 'Price',
    sortable: true,
    className: 'text-right',
    render: (row) => <span className="text-right block">{formatCurrency(row.price)}</span>,
  },
  {
    key: 'amount',
    header: 'Amount',
    sortable: true,
    className: 'text-right',
    render: (row) => <span className="text-right block font-medium">{formatCurrency(row.amount)}</span>,
  },
  {
    key: 'status',
    header: 'Status',
    sortable: true,
    render: (row) => (
      <StatusBadge
        label={getTransactionStatusLabel(row.status)}
        variant={getTransactionStatusVariant(row.status)}
      />
    ),
  },
];

export function TransactionListPage() {
  const [accountSearch, setAccountSearch] = useState('');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [page, setPage] = useState(0);

  const filtered = useMemo(() => {
    let result = transactions;

    if (accountSearch.trim()) {
      const q = accountSearch.trim().toLowerCase();
      result = result.filter((t) => t.accountNo.toLowerCase().includes(q));
    }

    if (dateFrom) {
      result = result.filter((t) => t.transDate >= dateFrom);
    }
    if (dateTo) {
      result = result.filter((t) => t.transDate <= dateTo);
    }

    return [...result].sort((a, b) => b.transDate.localeCompare(a.transDate));
  }, [accountSearch, dateFrom, dateTo]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const pageData = filtered.slice(safePage * PAGE_SIZE, (safePage + 1) * PAGE_SIZE);

  const handlePageChange = (newPage: number) => {
    setPage(Math.max(0, Math.min(newPage, totalPages - 1)));
  };

  const resetFilters = () => {
    setAccountSearch('');
    setDateFrom('');
    setDateTo('');
    setPage(0);
  };

  const hasFilters = accountSearch || dateFrom || dateTo;

  return (
    <div>
      <PageHeader
        title="Transaction History"
        description="View and filter all transactions across accounts"
      />

      <Card className="mb-6">
        <div className="flex flex-col sm:flex-row gap-4">
          <SearchInput
            value={accountSearch}
            onChange={(v) => { setAccountSearch(v); setPage(0); }}
            placeholder="Search by account number..."
            className="sm:w-64"
          />
          <div className="flex items-center gap-2">
            <label className="text-sm text-slate-500 whitespace-nowrap">From</label>
            <input
              type="date"
              value={dateFrom}
              onChange={(e) => { setDateFrom(e.target.value); setPage(0); }}
              className="px-3 py-2 text-sm border border-slate-300 rounded-lg bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
          </div>
          <div className="flex items-center gap-2">
            <label className="text-sm text-slate-500 whitespace-nowrap">To</label>
            <input
              type="date"
              value={dateTo}
              onChange={(e) => { setDateTo(e.target.value); setPage(0); }}
              className="px-3 py-2 text-sm border border-slate-300 rounded-lg bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
          </div>
          {hasFilters && (
            <button
              onClick={resetFilters}
              className="text-sm text-blue-600 hover:text-blue-800 whitespace-nowrap"
            >
              Clear filters
            </button>
          )}
        </div>
      </Card>

      <Card>
        <div className="flex items-center justify-between mb-4">
          <p className="text-sm text-slate-500">
            {filtered.length === 0
              ? 'No transactions found'
              : `Showing ${safePage * PAGE_SIZE + 1}–${Math.min((safePage + 1) * PAGE_SIZE, filtered.length)} of ${filtered.length} transactions`}
          </p>
        </div>

        {filtered.length === 0 ? (
          <EmptyState
            title="No transactions found"
            message={hasFilters ? 'Try adjusting your search criteria or date range.' : 'There are no transactions to display.'}
            icon={<ArrowLeftRight className="w-12 h-12" />}
            action={
              hasFilters ? (
                <button
                  onClick={resetFilters}
                  className="text-sm font-medium text-blue-600 hover:text-blue-800"
                >
                  Clear all filters
                </button>
              ) : undefined
            }
          />
        ) : (
          <>
            <div className="-m-6 mt-0">
              <DataTable<TransactionRow>
                columns={columns}
                data={pageData as TransactionRow[]}
                keyExtractor={(row) => row.transId}
                emptyMessage="No transactions found"
              />
            </div>

            {totalPages > 1 && (
              <div className="flex items-center justify-between pt-4 mt-4 border-t border-slate-200 -mx-6 px-6">
                <button
                  onClick={() => handlePageChange(safePage - 1)}
                  disabled={safePage === 0}
                  className="inline-flex items-center gap-1 px-3 py-2 text-sm font-medium rounded-lg border border-slate-300 bg-white text-slate-700 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <ChevronLeft className="w-4 h-4" />
                  Previous
                </button>
                <span className="text-sm text-slate-600">
                  Page {safePage + 1} of {totalPages}
                </span>
                <button
                  onClick={() => handlePageChange(safePage + 1)}
                  disabled={safePage >= totalPages - 1}
                  className="inline-flex items-center gap-1 px-3 py-2 text-sm font-medium rounded-lg border border-slate-300 bg-white text-slate-700 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Next
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            )}
          </>
        )}
      </Card>
    </div>
  );
}

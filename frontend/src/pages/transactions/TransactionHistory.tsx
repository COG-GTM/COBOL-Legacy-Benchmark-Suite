import { useState, useMemo, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Search, ChevronLeft, ChevronRight, Download } from 'lucide-react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import {
  StatusBadge,
  getTransactionStatusVariant,
  getTransactionStatusLabel,
  getTransTypeLabel,
} from '@/components/ui/StatusBadge';
import { EmptyState } from '@/components/ui/EmptyState';
import { transactions } from '@/data/mockData';
import type { Transaction } from '@/data/types';

const ROWS_PER_PAGE = 10;

type TransTypeFilter = 'ALL' | 'BY' | 'SL' | 'FE';
type StatusFilter = 'ALL' | 'P' | 'C' | 'E';

function formatShares(value: number): string {
  return value.toLocaleString('en-US', { minimumFractionDigits: 3, maximumFractionDigits: 3 });
}

function formatPrice(value: number): string {
  return value.toLocaleString('en-US', { minimumFractionDigits: 4, maximumFractionDigits: 4 });
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
}

function formatSignedCurrency(value: number, transType: Transaction['transType']): string {
  const sign = transType === 'SL' ? '+' : transType === 'BY' ? '-' : '-';
  const formatted = formatCurrency(Math.abs(value));
  return transType === 'FE' ? `-${formatted}` : `${sign}${formatted}`;
}

function formatSignedUnits(value: number, transType: Transaction['transType']): string {
  if (transType === 'FE') return formatShares(0);
  const sign = transType === 'BY' ? '+' : '-';
  return `${sign}${formatShares(value)}`;
}

function getTransTypeBadgeVariant(type: Transaction['transType']): 'success' | 'error' | 'info' {
  switch (type) {
    case 'BY':
      return 'success';
    case 'SL':
      return 'error';
    case 'FE':
      return 'info';
  }
}

function isValidAccountNumber(value: string): boolean {
  return /^\d{9}$/.test(value);
}

export function TransactionHistory() {
  const [searchParams] = useSearchParams();
  const initialAccount = searchParams.get('account') ?? '';

  const [searchInput, setSearchInput] = useState(initialAccount);
  const [activeSearch, setActiveSearch] = useState(initialAccount);
  const [validationError, setValidationError] = useState('');
  const [hasSearched, setHasSearched] = useState(initialAccount.length > 0);

  const [typeFilter, setTypeFilter] = useState<TransTypeFilter>('ALL');
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  const [currentPage, setCurrentPage] = useState(1);
  const [toastVisible, setToastVisible] = useState(false);

  const handleSearch = useCallback(() => {
    const trimmed = searchInput.trim();
    if (trimmed.length === 0) {
      setValidationError('Please enter an account number');
      return;
    }
    if (!isValidAccountNumber(trimmed)) {
      setValidationError('Account number must be exactly 9 digits');
      return;
    }
    setValidationError('');
    setActiveSearch(trimmed);
    setCurrentPage(1);
    setHasSearched(true);
  }, [searchInput]);

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLInputElement>) => {
      if (e.key === 'Enter') {
        handleSearch();
      }
    },
    [handleSearch],
  );

  const handleExport = useCallback(() => {
    setToastVisible(true);
    setTimeout(() => setToastVisible(false), 3000);
  }, []);

  const filteredTransactions = useMemo(() => {
    if (!activeSearch) return [];
    let result = transactions
      .filter((t) => t.accountNo === activeSearch)
      .sort((a, b) => b.transDate.localeCompare(a.transDate));

    if (typeFilter !== 'ALL') {
      result = result.filter((t) => t.transType === typeFilter);
    }
    if (statusFilter !== 'ALL') {
      result = result.filter((t) => t.status === statusFilter);
    }
    if (startDate) {
      result = result.filter((t) => t.transDate >= startDate);
    }
    if (endDate) {
      result = result.filter((t) => t.transDate <= endDate);
    }
    return result;
  }, [activeSearch, typeFilter, statusFilter, startDate, endDate]);

  const totalPages = Math.max(1, Math.ceil(filteredTransactions.length / ROWS_PER_PAGE));
  const paginatedTransactions = useMemo(
    () =>
      filteredTransactions.slice(
        (currentPage - 1) * ROWS_PER_PAGE,
        currentPage * ROWS_PER_PAGE,
      ),
    [filteredTransactions, currentPage],
  );

  const summary = useMemo(() => {
    const total = filteredTransactions.length;
    const buys = filteredTransactions.filter((t) => t.transType === 'BY').length;
    const sells = filteredTransactions.filter((t) => t.transType === 'SL').length;
    const netAmount = filteredTransactions.reduce((sum, t) => {
      if (t.transType === 'BY') return sum - t.amount;
      if (t.transType === 'SL') return sum + t.amount;
      return sum - t.amount;
    }, 0);
    return { total, buys, sells, netAmount };
  }, [filteredTransactions]);

  const resetFilters = useCallback(() => {
    setTypeFilter('ALL');
    setStatusFilter('ALL');
    setStartDate('');
    setEndDate('');
    setCurrentPage(1);
  }, []);

  return (
    <div>
      <PageHeader
        title="Transaction History"
        description="Search and filter transactions by account — modernized from CICS HISMAP screen"
        actions={
          <button
            onClick={handleExport}
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 transition-colors"
          >
            <Download className="w-4 h-4" />
            Export
          </button>
        }
      />

      {/* Search Bar */}
      <Card className="mb-6">
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="flex-1">
            <label htmlFor="txn-account-search" className="sr-only">
              Account Number
            </label>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
              <input
                id="txn-account-search"
                type="text"
                value={searchInput}
                onChange={(e) => {
                  setSearchInput(e.target.value);
                  if (validationError) setValidationError('');
                }}
                onKeyDown={handleKeyDown}
                placeholder="Enter Account Number (9-digit numeric)"
                maxLength={9}
                className="w-full pl-10 pr-4 py-2 text-sm border border-slate-300 rounded-lg bg-white text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                aria-label="Account Number"
                aria-describedby={validationError ? 'txn-search-error' : undefined}
              />
            </div>
            {validationError && (
              <p id="txn-search-error" className="mt-1 text-xs text-red-600">
                {validationError}
              </p>
            )}
          </div>
          <button
            onClick={handleSearch}
            className="px-5 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 transition-colors"
          >
            Search
          </button>
        </div>
      </Card>

      {/* Filters — only show after a search */}
      {hasSearched && (
        <Card className="mb-6">
          <div className="flex flex-col md:flex-row gap-4 items-end">
            <div className="flex-1 min-w-0">
              <label
                htmlFor="type-filter"
                className="block text-xs font-semibold text-slate-600 uppercase tracking-wider mb-1"
              >
                Transaction Type
              </label>
              <select
                id="type-filter"
                value={typeFilter}
                onChange={(e) => {
                  setTypeFilter(e.target.value as TransTypeFilter);
                  setCurrentPage(1);
                }}
                className="w-full py-2 px-3 text-sm border border-slate-300 rounded-lg bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              >
                <option value="ALL">All Types</option>
                <option value="BY">Buy</option>
                <option value="SL">Sell</option>
                <option value="FE">Fee</option>
              </select>
            </div>
            <div className="flex-1 min-w-0">
              <label
                htmlFor="status-filter"
                className="block text-xs font-semibold text-slate-600 uppercase tracking-wider mb-1"
              >
                Status
              </label>
              <select
                id="status-filter"
                value={statusFilter}
                onChange={(e) => {
                  setStatusFilter(e.target.value as StatusFilter);
                  setCurrentPage(1);
                }}
                className="w-full py-2 px-3 text-sm border border-slate-300 rounded-lg bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              >
                <option value="ALL">All Statuses</option>
                <option value="P">Pending</option>
                <option value="C">Complete</option>
                <option value="E">Error</option>
              </select>
            </div>
            <div className="flex-1 min-w-0">
              <label
                htmlFor="start-date"
                className="block text-xs font-semibold text-slate-600 uppercase tracking-wider mb-1"
              >
                Start Date
              </label>
              <input
                id="start-date"
                type="date"
                value={startDate}
                onChange={(e) => {
                  setStartDate(e.target.value);
                  setCurrentPage(1);
                }}
                className="w-full py-2 px-3 text-sm border border-slate-300 rounded-lg bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
            </div>
            <div className="flex-1 min-w-0">
              <label
                htmlFor="end-date"
                className="block text-xs font-semibold text-slate-600 uppercase tracking-wider mb-1"
              >
                End Date
              </label>
              <input
                id="end-date"
                type="date"
                value={endDate}
                onChange={(e) => {
                  setEndDate(e.target.value);
                  setCurrentPage(1);
                }}
                className="w-full py-2 px-3 text-sm border border-slate-300 rounded-lg bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
            </div>
            <button
              onClick={resetFilters}
              className="px-4 py-2 text-sm font-medium text-slate-600 bg-slate-100 rounded-lg hover:bg-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors whitespace-nowrap"
            >
              Reset Filters
            </button>
          </div>
        </Card>
      )}

      {hasSearched && filteredTransactions.length === 0 && (
        <EmptyState
          title="No transactions found"
          message={`No transactions match your search criteria for account ${activeSearch}.`}
        />
      )}

      {filteredTransactions.length > 0 && (
        <>
          <Card className="mb-6">
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Date
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Transaction ID
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Fund ID
                    </th>
                    <th className="px-4 py-3 text-center text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Type
                    </th>
                    <th className="px-4 py-3 text-right text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Units
                    </th>
                    <th className="px-4 py-3 text-right text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Price
                    </th>
                    <th className="px-4 py-3 text-right text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Amount
                    </th>
                    <th className="px-4 py-3 text-center text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="px-4 py-3 text-right text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Before Bal
                    </th>
                    <th className="px-4 py-3 text-right text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      After Bal
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-slate-200">
                  {paginatedTransactions.map((txn) => (
                    <tr
                      key={txn.transId}
                      className="hover:bg-slate-50 transition-colors"
                    >
                      <td className="px-4 py-3 text-sm text-slate-700 whitespace-nowrap">
                        {txn.transDate}
                      </td>
                      <td className="px-4 py-3 text-sm font-medium text-slate-900 whitespace-nowrap">
                        {txn.transId}
                      </td>
                      <td className="px-4 py-3 text-sm text-slate-700 whitespace-nowrap">
                        {txn.fundId}
                      </td>
                      <td className="px-4 py-3 text-sm whitespace-nowrap text-center">
                        <StatusBadge
                          label={getTransTypeLabel(txn.transType)}
                          variant={getTransTypeBadgeVariant(txn.transType)}
                        />
                      </td>
                      <td className="px-4 py-3 text-sm text-slate-700 whitespace-nowrap text-right font-mono">
                        {formatSignedUnits(txn.shareQty, txn.transType)}
                      </td>
                      <td className="px-4 py-3 text-sm text-slate-700 whitespace-nowrap text-right font-mono">
                        {formatPrice(txn.price)}
                      </td>
                      <td className="px-4 py-3 text-sm whitespace-nowrap text-right font-mono">
                        <span
                          className={
                            txn.transType === 'SL'
                              ? 'text-emerald-600'
                              : 'text-red-600'
                          }
                        >
                          {formatSignedCurrency(txn.amount, txn.transType)}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-sm whitespace-nowrap text-center">
                        <StatusBadge
                          label={getTransactionStatusLabel(txn.status)}
                          variant={getTransactionStatusVariant(txn.status)}
                        />
                      </td>
                      <td className="px-4 py-3 text-sm text-slate-700 whitespace-nowrap text-right font-mono">
                        {formatShares(txn.beforeBalance)}
                      </td>
                      <td className="px-4 py-3 text-sm text-slate-700 whitespace-nowrap text-right font-mono">
                        {formatShares(txn.afterBalance)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            <div className="flex items-center justify-between px-4 py-3 border-t border-slate-200">
              <p className="text-sm text-slate-600">
                Showing {(currentPage - 1) * ROWS_PER_PAGE + 1}–
                {Math.min(currentPage * ROWS_PER_PAGE, filteredTransactions.length)} of{' '}
                {filteredTransactions.length} transactions
              </p>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                  disabled={currentPage === 1}
                  className="inline-flex items-center gap-1 px-3 py-1.5 text-sm font-medium rounded-lg border border-slate-300 bg-white text-slate-700 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                  aria-label="Previous page"
                >
                  <ChevronLeft className="w-4 h-4" />
                  Previous
                </button>
                <span className="text-sm text-slate-600">
                  Page {currentPage} of {totalPages}
                </span>
                <button
                  onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                  disabled={currentPage === totalPages}
                  className="inline-flex items-center gap-1 px-3 py-1.5 text-sm font-medium rounded-lg border border-slate-300 bg-white text-slate-700 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                  aria-label="Next page"
                >
                  Next
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          </Card>

          {/* Summary Bar */}
          <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
            <div className="bg-white rounded-lg border border-slate-200 shadow-sm p-4">
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                Total Transactions
              </p>
              <p className="mt-1 text-2xl font-bold text-slate-900">{summary.total}</p>
            </div>
            <div className="bg-white rounded-lg border border-slate-200 shadow-sm p-4">
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                Total Buys
              </p>
              <p className="mt-1 text-2xl font-bold text-emerald-600">{summary.buys}</p>
            </div>
            <div className="bg-white rounded-lg border border-slate-200 shadow-sm p-4">
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                Total Sells
              </p>
              <p className="mt-1 text-2xl font-bold text-red-600">{summary.sells}</p>
            </div>
            <div className="bg-white rounded-lg border border-slate-200 shadow-sm p-4">
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                Net Amount
              </p>
              <p
                className={`mt-1 text-2xl font-bold ${
                  summary.netAmount >= 0 ? 'text-emerald-600' : 'text-red-600'
                }`}
              >
                {formatCurrency(summary.netAmount)}
              </p>
            </div>
          </div>
        </>
      )}

      {/* Toast notification */}
      {toastVisible && (
        <div className="fixed bottom-6 right-6 z-50 bg-slate-800 text-white px-5 py-3 rounded-lg shadow-lg text-sm font-medium animate-fade-in">
          Export feature coming soon
        </div>
      )}
    </div>
  );
}

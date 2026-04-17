import { useState, useMemo, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Search, ChevronLeft, ChevronRight } from 'lucide-react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { StatusBadge, getPositionStatusVariant, getPositionStatusLabel } from '@/components/ui/StatusBadge';
import { EmptyState } from '@/components/ui/EmptyState';
import { positions } from '@/data/mockData';
import type { Position } from '@/data/types';

const ROWS_PER_PAGE = 10;

function formatShares(value: number): string {
  return value.toLocaleString('en-US', { minimumFractionDigits: 3, maximumFractionDigits: 3 });
}

function formatAvgCost(value: number): string {
  return value.toLocaleString('en-US', { minimumFractionDigits: 4, maximumFractionDigits: 4 });
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
}

function isValidAccountNumber(value: string): boolean {
  return /^\d{9}$/.test(value);
}

export function PositionSearch() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const initialAccount = searchParams.get('account') ?? '';

  const [searchInput, setSearchInput] = useState(initialAccount);
  const [activeSearch, setActiveSearch] = useState(initialAccount);
  const [validationError, setValidationError] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [hasSearched, setHasSearched] = useState(initialAccount.length > 0);

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

  const filteredPositions = useMemo(() => {
    if (!activeSearch) return [];
    return positions.filter((p) => p.accountNo === activeSearch);
  }, [activeSearch]);

  const totalPages = Math.max(1, Math.ceil(filteredPositions.length / ROWS_PER_PAGE));
  const paginatedPositions = useMemo(
    () => filteredPositions.slice((currentPage - 1) * ROWS_PER_PAGE, currentPage * ROWS_PER_PAGE),
    [filteredPositions, currentPage],
  );

  const summary = useMemo(() => {
    const totalPositions = filteredPositions.length;
    const totalShares = filteredPositions.reduce((sum, p) => sum + p.shareBalance, 0);
    const totalMarketValue = filteredPositions.reduce(
      (sum, p) => sum + p.shareBalance * p.avgCost,
      0,
    );
    return { totalPositions, totalShares, totalMarketValue };
  }, [filteredPositions]);

  const handleRowClick = (position: Position) => {
    navigate(`/transactions?account=${position.accountNo}`);
  };

  return (
    <div>
      <PageHeader
        title="Position Inquiry"
        description="Search positions by account number — modernized from CICS POSMAP screen"
      />

      <Card className="mb-6">
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="flex-1">
            <label htmlFor="account-search" className="sr-only">
              Account Number
            </label>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
              <input
                id="account-search"
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
                aria-describedby={validationError ? 'search-error' : undefined}
              />
            </div>
            {validationError && (
              <p id="search-error" className="mt-1 text-xs text-red-600">
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

      {hasSearched && filteredPositions.length === 0 && (
        <EmptyState
          title="Position not found"
          message={`No positions found for account number ${activeSearch}. Please verify the account number and try again.`}
        />
      )}

      {filteredPositions.length > 0 && (
        <>
          <Card className="mb-6">
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Fund ID
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      CUSIP
                    </th>
                    <th className="px-4 py-3 text-right text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Share Balance
                    </th>
                    <th className="px-4 py-3 text-right text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Avg Cost
                    </th>
                    <th className="px-4 py-3 text-right text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Cost Basis
                    </th>
                    <th className="px-4 py-3 text-right text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Market Value
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Last Transaction
                    </th>
                    <th className="px-4 py-3 text-center text-xs font-semibold text-slate-600 uppercase tracking-wider">
                      Status
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-slate-200">
                  {paginatedPositions.map((pos) => {
                    const marketValue = pos.shareBalance * pos.avgCost;
                    return (
                      <tr
                        key={`${pos.accountNo}-${pos.fundId}`}
                        onClick={() => handleRowClick(pos)}
                        className="hover:bg-slate-50 cursor-pointer transition-colors"
                        role="button"
                        tabIndex={0}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter' || e.key === ' ') {
                            e.preventDefault();
                            handleRowClick(pos);
                          }
                        }}
                        aria-label={`View transactions for ${pos.fundId}`}
                      >
                        <td className="px-4 py-3 text-sm font-medium text-slate-900 whitespace-nowrap">
                          {pos.fundId}
                        </td>
                        <td className="px-4 py-3 text-sm text-slate-700 whitespace-nowrap">
                          {pos.cusip}
                        </td>
                        <td className="px-4 py-3 text-sm text-slate-700 whitespace-nowrap text-right font-mono">
                          {formatShares(pos.shareBalance)}
                        </td>
                        <td className="px-4 py-3 text-sm text-slate-700 whitespace-nowrap text-right font-mono">
                          {formatAvgCost(pos.avgCost)}
                        </td>
                        <td className="px-4 py-3 text-sm text-slate-700 whitespace-nowrap text-right font-mono">
                          {formatCurrency(pos.costBasis)}
                        </td>
                        <td className="px-4 py-3 text-sm text-slate-700 whitespace-nowrap text-right font-mono">
                          {formatCurrency(marketValue)}
                        </td>
                        <td className="px-4 py-3 text-sm text-slate-700 whitespace-nowrap">
                          {pos.lastDate}
                        </td>
                        <td className="px-4 py-3 text-sm whitespace-nowrap text-center">
                          <StatusBadge
                            label={getPositionStatusLabel(pos.status)}
                            variant={getPositionStatusVariant(pos.status)}
                          />
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            <div className="flex items-center justify-between px-4 py-3 border-t border-slate-200">
              <p className="text-sm text-slate-600">
                Showing {(currentPage - 1) * ROWS_PER_PAGE + 1}–
                {Math.min(currentPage * ROWS_PER_PAGE, filteredPositions.length)} of{' '}
                {filteredPositions.length} positions
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

          {/* Summary Row */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="bg-white rounded-lg border border-slate-200 shadow-sm p-4">
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                Total Positions
              </p>
              <p className="mt-1 text-2xl font-bold text-slate-900">{summary.totalPositions}</p>
            </div>
            <div className="bg-white rounded-lg border border-slate-200 shadow-sm p-4">
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                Total Shares
              </p>
              <p className="mt-1 text-2xl font-bold text-slate-900">
                {formatShares(summary.totalShares)}
              </p>
            </div>
            <div className="bg-white rounded-lg border border-slate-200 shadow-sm p-4">
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                Total Market Value
              </p>
              <p className="mt-1 text-2xl font-bold text-slate-900">
                {formatCurrency(summary.totalMarketValue)}
              </p>
            </div>
          </div>
        </>
      )}
    </div>
  );
}

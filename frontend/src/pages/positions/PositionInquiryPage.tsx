import { useState, useMemo } from 'react';
import { Search } from 'lucide-react';
import { PageHeader } from '@/components/ui/PageHeader';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { EmptyState } from '@/components/ui/EmptyState';
import { StatusBadge, getPositionStatusVariant, getPositionStatusLabel, getTransTypeLabel } from '@/components/ui/StatusBadge';
import { positions } from '@/data/mockData';
import type { Position } from '@/data/types';

const PAGE_SIZE = 10;

const columns: Column<Position>[] = [
  { key: 'fundId', header: 'Fund ID' },
  { key: 'cusip', header: 'CUSIP' },
  {
    key: 'shareBalance',
    header: 'Share Balance',
    className: 'text-right',
    render: (row) => row.shareBalance.toLocaleString('en-US', { minimumFractionDigits: 3 }),
  },
  {
    key: 'avgCost',
    header: 'Avg Cost',
    className: 'text-right',
    render: (row) =>
      new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(row.avgCost),
  },
  {
    key: 'costBasis',
    header: 'Cost Basis',
    className: 'text-right',
    render: (row) =>
      new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(row.costBasis),
  },
  { key: 'lastDate', header: 'Last Activity' },
  {
    key: 'lastTrans',
    header: 'Last Trans',
    render: (row) => getTransTypeLabel(row.lastTrans),
  },
  {
    key: 'status',
    header: 'Status',
    render: (row) => (
      <StatusBadge
        label={getPositionStatusLabel(row.status)}
        variant={getPositionStatusVariant(row.status)}
      />
    ),
  },
];

export function PositionInquiryPage() {
  const [accountInput, setAccountInput] = useState('');
  const [searchedAccount, setSearchedAccount] = useState('');
  const [page, setPage] = useState(0);

  const filteredPositions = useMemo(() => {
    if (!searchedAccount) return [];
    return positions.filter((p) => p.accountNo === searchedAccount);
  }, [searchedAccount]);

  const totalPages = Math.ceil(filteredPositions.length / PAGE_SIZE);
  const pagedPositions = useMemo(
    () => filteredPositions.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE),
    [filteredPositions, page],
  );

  const handleSearch = () => {
    const trimmed = accountInput.trim();
    setSearchedAccount(trimmed);
    setPage(0);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleSearch();
  };

  const statusMessage = useMemo(() => {
    if (!searchedAccount) return null;
    if (filteredPositions.length === 0) return { type: 'error' as const, text: `Account not found: ${searchedAccount}` };
    return { type: 'info' as const, text: `Showing ${filteredPositions.length} position(s) for account ${searchedAccount}` };
  }, [searchedAccount, filteredPositions.length]);

  return (
    <div>
      <PageHeader
        title="Position Inquiry"
        description="Look up portfolio positions by account number (maps to POSMAP/INQONLN)"
      />

      <div className="bg-white rounded-lg border border-slate-200 shadow-sm p-6 mb-6">
        <label className="block text-sm font-medium text-slate-700 mb-2">
          Account / Portfolio ID
        </label>
        <div className="flex gap-3">
          <div className="relative flex-1 max-w-sm">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
            <input
              type="text"
              value={accountInput}
              onChange={(e) => setAccountInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Enter account number (e.g. 100000001)"
              className="w-full pl-10 pr-4 py-2 text-sm border border-slate-300 rounded-lg bg-white text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
          </div>
          <button
            onClick={handleSearch}
            className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 transition-colors"
          >
            Search
          </button>
        </div>

        {statusMessage && (
          <div
            className={`mt-4 px-4 py-2.5 rounded-lg text-sm font-medium ${
              statusMessage.type === 'error'
                ? 'bg-red-50 text-red-700 border border-red-200'
                : 'bg-blue-50 text-blue-700 border border-blue-200'
            }`}
          >
            {statusMessage.text}
          </div>
        )}
      </div>

      {!searchedAccount ? (
        <EmptyState
          icon={<Search className="w-12 h-12" />}
          title="No account selected"
          message="Enter an account number above to view positions."
        />
      ) : filteredPositions.length === 0 ? (
        <EmptyState
          title="No positions found"
          message={`No positions exist for account ${searchedAccount}.`}
        />
      ) : (
        <div className="bg-white rounded-lg border border-slate-200 shadow-sm">
          <DataTable
            columns={columns as unknown as Column<Record<string, unknown>>[]}
            data={pagedPositions as unknown as Record<string, unknown>[]}
            keyExtractor={(row) => `${(row as unknown as Position).fundId}-${(row as unknown as Position).cusip}`}
          />

          {totalPages > 1 && (
            <div className="flex items-center justify-between px-4 py-3 border-t border-slate-200">
              <p className="text-sm text-slate-600">
                Page {page + 1} of {totalPages}
              </p>
              <div className="flex gap-2">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="px-3 py-1.5 text-sm font-medium rounded-lg border border-slate-300 text-slate-700 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  Previous
                </button>
                <button
                  onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                  className="px-3 py-1.5 text-sm font-medium rounded-lg border border-slate-300 text-slate-700 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

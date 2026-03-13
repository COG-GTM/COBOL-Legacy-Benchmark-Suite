import { useState, useCallback } from 'react';
import { AccountSearchForm } from '@/components/shared/AccountSearchForm';
import { Pagination } from '@/components/shared/Pagination';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { useToast } from '@/context/ToastContext';
import { getTransactionHistory } from '@/services/portfolioService';
import type { PaginatedResult, Transaction } from '@/types/portfolio';
import { TRANSACTION_TYPE_LABELS, TRANSACTION_STATUS_LABELS } from '@/types/portfolio';

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
}

function formatDate(dateStr: string): string {
  if (dateStr.length !== 8) return dateStr;
  return `${dateStr.slice(0, 4)}-${dateStr.slice(4, 6)}-${dateStr.slice(6, 8)}`;
}

function formatTime(timeStr: string): string {
  if (timeStr.length !== 6) return timeStr;
  return `${timeStr.slice(0, 2)}:${timeStr.slice(2, 4)}:${timeStr.slice(4, 6)}`;
}

function getTypeBadgeVariant(
  type: string
): 'success' | 'destructive' | 'secondary' | 'warning' | 'default' {
  switch (type) {
    case 'BU':
      return 'success';
    case 'SL':
      return 'destructive';
    case 'TR':
      return 'secondary';
    case 'FE':
      return 'warning';
    default:
      return 'default';
  }
}

function getStatusBadgeVariant(
  status: string
): 'success' | 'destructive' | 'warning' | 'default' {
  switch (status) {
    case 'D':
      return 'success';
    case 'F':
      return 'destructive';
    case 'P':
      return 'warning';
    case 'R':
      return 'warning';
    default:
      return 'default';
  }
}

export function TransactionsPage() {
  const { addToast } = useToast();
  const [isLoading, setIsLoading] = useState(false);
  const [result, setResult] = useState<PaginatedResult<Transaction> | null>(null);
  const [searchedAccount, setSearchedAccount] = useState('');
  const [hasSearched, setHasSearched] = useState(false);

  const handleSearch = useCallback(
    async (accountNumber: string) => {
      setIsLoading(true);
      setHasSearched(true);
      setSearchedAccount(accountNumber);
      try {
        const res = await getTransactionHistory(accountNumber, 1, 10);
        setResult(res);
        if (!res) {
          addToast(`No transaction history found for account "${accountNumber}"`, 'error');
        } else {
          addToast(`Found ${res.totalItems} transactions`, 'success');
        }
      } catch {
        addToast('Error searching for transactions', 'error');
        setResult(null);
      } finally {
        setIsLoading(false);
      }
    },
    [addToast]
  );

  const handlePageChange = useCallback(
    async (page: number) => {
      if (!searchedAccount) return;
      setIsLoading(true);
      try {
        const res = await getTransactionHistory(searchedAccount, page, 10);
        setResult(res);
      } catch {
        addToast('Error loading transactions', 'error');
      } finally {
        setIsLoading(false);
      }
    },
    [searchedAccount, addToast]
  );

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-white">Transaction History</h2>
        <p className="mt-1 text-[#94A3B8]">
          Search for an account to view transaction history including buys, sells, transfers, and
          fees.
        </p>
      </div>

      <Card>
        <CardContent className="pt-6">
          <AccountSearchForm onSearch={handleSearch} isLoading={isLoading} />
        </CardContent>
      </Card>

      {isLoading && !result && (
        <div className="space-y-4">
          <Skeleton className="h-64 w-full" />
        </div>
      )}

      {hasSearched && !isLoading && !result && (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <p className="text-lg font-medium text-[#F87171]">No transaction history found</p>
            <p className="mt-1 text-sm text-[#94A3B8]">
              No transactions found for account &quot;{searchedAccount}&quot;
            </p>
          </CardContent>
        </Card>
      )}

      {result && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">
              Transaction History ({result.totalItems} records)
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="overflow-x-auto">
              <table className="w-full text-sm" aria-label="Transaction history">
                <thead>
                  <tr className="border-b border-[#334155]">
                    <th className="px-3 py-3 text-left font-medium text-[#E2E8F0]">Date</th>
                    <th className="px-3 py-3 text-left font-medium text-[#E2E8F0]">Time</th>
                    <th className="px-3 py-3 text-left font-medium text-[#E2E8F0]">Type</th>
                    <th className="px-3 py-3 text-left font-medium text-[#E2E8F0]">Investment</th>
                    <th className="px-3 py-3 text-right font-medium text-[#E2E8F0]">Quantity</th>
                    <th className="px-3 py-3 text-right font-medium text-[#E2E8F0]">Price</th>
                    <th className="px-3 py-3 text-right font-medium text-[#E2E8F0]">Amount</th>
                    <th className="px-3 py-3 text-center font-medium text-[#E2E8F0]">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {result.data.map((txn, index) => (
                    <tr
                      key={`${txn.date}-${txn.sequenceNo}-${index}`}
                      className={`border-b border-[#334155]/50 ${
                        index % 2 === 0 ? 'bg-[#1E293B]' : 'bg-[#243449]'
                      }`}
                    >
                      <td className="px-3 py-3 text-[#CBD5E1]">{formatDate(txn.date)}</td>
                      <td className="px-3 py-3 text-[#CBD5E1]">{formatTime(txn.time)}</td>
                      <td className="px-3 py-3">
                        <Badge variant={getTypeBadgeVariant(txn.type)}>
                          {TRANSACTION_TYPE_LABELS[txn.type]}
                        </Badge>
                      </td>
                      <td className="px-3 py-3">
                        <div>
                          <span className="font-medium text-white">{txn.investmentId}</span>
                          <span className="ml-2 text-[#94A3B8]">{txn.investmentName}</span>
                        </div>
                      </td>
                      <td className="px-3 py-3 text-right text-[#CBD5E1]">
                        {txn.quantity > 0 ? txn.quantity.toLocaleString() : '-'}
                      </td>
                      <td className="px-3 py-3 text-right text-[#CBD5E1]">
                        {txn.price > 0 ? formatCurrency(txn.price) : '-'}
                      </td>
                      <td
                        className={`px-3 py-3 text-right font-medium ${
                          txn.type === 'SL' ? 'text-[#4ADE80]' : 'text-[#CBD5E1]'
                        }`}
                      >
                        {formatCurrency(txn.amount)}
                      </td>
                      <td className="px-3 py-3 text-center">
                        <Badge variant={getStatusBadgeVariant(txn.status)}>
                          {TRANSACTION_STATUS_LABELS[txn.status]}
                        </Badge>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {result.totalPages > 1 && (
              <div className="mt-4">
                <Pagination
                  currentPage={result.currentPage}
                  totalPages={result.totalPages}
                  onPageChange={handlePageChange}
                  totalItems={result.totalItems}
                  pageSize={result.pageSize}
                />
              </div>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
}

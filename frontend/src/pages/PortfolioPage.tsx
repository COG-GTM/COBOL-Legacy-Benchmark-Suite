import { useState, useCallback } from 'react';
import { AccountSearchForm } from '@/components/shared/AccountSearchForm';
import { Pagination } from '@/components/shared/Pagination';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { useToast } from '@/context/ToastContext';
import {
  getPortfolioPositions,
  getPortfolioSummary,
} from '@/services/portfolioService';
import type { PortfolioSummary, PaginatedResult, Position } from '@/types/portfolio';
import {
  CLIENT_TYPE_LABELS,
  PORTFOLIO_STATUS_LABELS,
  POSITION_STATUS_LABELS,
} from '@/types/portfolio';
import { TrendingUp, TrendingDown, DollarSign, Wallet } from 'lucide-react';

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
}

function formatDate(dateStr: string): string {
  if (dateStr.length !== 8) return dateStr;
  return `${dateStr.slice(0, 4)}-${dateStr.slice(4, 6)}-${dateStr.slice(6, 8)}`;
}

function getStatusBadgeVariant(status: string): 'success' | 'destructive' | 'warning' | 'default' {
  switch (status) {
    case 'A':
      return 'success';
    case 'C':
      return 'destructive';
    case 'S':
    case 'P':
      return 'warning';
    default:
      return 'default';
  }
}

export function PortfolioPage() {
  const { addToast } = useToast();
  const [isLoading, setIsLoading] = useState(false);
  const [summary, setSummary] = useState<PortfolioSummary | null>(null);
  const [positionsResult, setPositionsResult] = useState<PaginatedResult<Position> | null>(null);
  const [searchedAccount, setSearchedAccount] = useState('');
  const [hasSearched, setHasSearched] = useState(false);

  const handleSearch = useCallback(
    async (accountNumber: string) => {
      setIsLoading(true);
      setHasSearched(true);
      setSearchedAccount(accountNumber);
      try {
        const [summaryResult, posResult] = await Promise.all([
          getPortfolioSummary(accountNumber),
          getPortfolioPositions(accountNumber, 1, 5),
        ]);
        setSummary(summaryResult);
        setPositionsResult(posResult);
        if (!summaryResult) {
          addToast(`No portfolio found for account "${accountNumber}"`, 'error');
        } else {
          addToast(
            `Portfolio found for ${summaryResult.portfolio.clientName}`,
            'success'
          );
        }
      } catch {
        addToast('Error searching for portfolio', 'error');
        setSummary(null);
        setPositionsResult(null);
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
        const posResult = await getPortfolioPositions(searchedAccount, page, 5);
        setPositionsResult(posResult);
      } catch {
        addToast('Error loading positions', 'error');
      } finally {
        setIsLoading(false);
      }
    },
    [searchedAccount, addToast]
  );

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-white">Portfolio Position Inquiry</h2>
        <p className="mt-1 text-[#94A3B8]">
          Search for a portfolio by account number to view positions and market values.
        </p>
      </div>

      <Card>
        <CardContent className="pt-6">
          <AccountSearchForm onSearch={handleSearch} isLoading={isLoading} />
        </CardContent>
      </Card>

      {isLoading && !summary && (
        <div className="space-y-4">
          <Skeleton className="h-32 w-full" />
          <Skeleton className="h-64 w-full" />
        </div>
      )}

      {hasSearched && !isLoading && !summary && (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <p className="text-lg font-medium text-[#F87171]">Portfolio not found</p>
            <p className="mt-1 text-sm text-[#94A3B8]">
              No portfolio matches the account number &quot;{searchedAccount}&quot;
            </p>
          </CardContent>
        </Card>
      )}

      {summary && (
        <>
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="text-lg">
                  {summary.portfolio.clientName}
                </CardTitle>
                <Badge
                  variant={getStatusBadgeVariant(summary.portfolio.status)}
                  aria-label={`Status: ${PORTFOLIO_STATUS_LABELS[summary.portfolio.status]}`}
                >
                  {PORTFOLIO_STATUS_LABELS[summary.portfolio.status]}
                </Badge>
              </div>
            </CardHeader>
            <CardContent>
              <div className="mb-4 grid gap-3 text-sm sm:grid-cols-2 lg:grid-cols-4">
                <div>
                  <span className="text-[#94A3B8]">Account: </span>
                  <span className="text-white">{summary.portfolio.accountNumber}</span>
                </div>
                <div>
                  <span className="text-[#94A3B8]">Type: </span>
                  <span className="text-white">
                    {CLIENT_TYPE_LABELS[summary.portfolio.clientType]}
                  </span>
                </div>
                <div>
                  <span className="text-[#94A3B8]">Created: </span>
                  <span className="text-white">
                    {formatDate(summary.portfolio.createDate)}
                  </span>
                </div>
                <div>
                  <span className="text-[#94A3B8]">Last Updated: </span>
                  <span className="text-white">
                    {formatDate(summary.portfolio.lastMaintenance)}
                  </span>
                </div>
              </div>

              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                <div className="flex items-center gap-3 rounded-lg bg-[#0F172A] p-4">
                  <DollarSign className="h-8 w-8 text-[#22D3EE]" />
                  <div>
                    <p className="text-xs text-[#94A3B8]">Total Market Value</p>
                    <p className="text-lg font-bold text-white">
                      {formatCurrency(summary.totalMarketValue)}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-3 rounded-lg bg-[#0F172A] p-4">
                  <Wallet className="h-8 w-8 text-[#60A5FA]" />
                  <div>
                    <p className="text-xs text-[#94A3B8]">Total Cost Basis</p>
                    <p className="text-lg font-bold text-white">
                      {formatCurrency(summary.totalCostBasis)}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-3 rounded-lg bg-[#0F172A] p-4">
                  {summary.totalGainLoss >= 0 ? (
                    <TrendingUp className="h-8 w-8 text-[#4ADE80]" />
                  ) : (
                    <TrendingDown className="h-8 w-8 text-[#F87171]" />
                  )}
                  <div>
                    <p className="text-xs text-[#94A3B8]">Gain/Loss</p>
                    <p
                      className={`text-lg font-bold ${summary.totalGainLoss >= 0 ? 'text-[#4ADE80]' : 'text-[#F87171]'}`}
                    >
                      {formatCurrency(summary.totalGainLoss)}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-3 rounded-lg bg-[#0F172A] p-4">
                  <Wallet className="h-8 w-8 text-[#A78BFA]" />
                  <div>
                    <p className="text-xs text-[#94A3B8]">Cash Balance</p>
                    <p className="text-lg font-bold text-white">
                      {formatCurrency(summary.portfolio.cashBalance)}
                    </p>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          {positionsResult && (
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">Positions</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="overflow-x-auto">
                  <table className="w-full text-sm" aria-label="Portfolio positions">
                    <thead>
                      <tr className="border-b border-[#334155]">
                        <th className="px-3 py-3 text-left font-medium text-[#E2E8F0]">
                          Investment
                        </th>
                        <th className="px-3 py-3 text-right font-medium text-[#E2E8F0]">
                          Quantity
                        </th>
                        <th className="px-3 py-3 text-right font-medium text-[#E2E8F0]">
                          Cost Basis
                        </th>
                        <th className="px-3 py-3 text-right font-medium text-[#E2E8F0]">
                          Market Value
                        </th>
                        <th className="px-3 py-3 text-right font-medium text-[#E2E8F0]">
                          Gain/Loss
                        </th>
                        <th className="px-3 py-3 text-center font-medium text-[#E2E8F0]">
                          Status
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {positionsResult.data.map((position, index) => {
                        const gainLoss = position.marketValue - position.costBasis;
                        const gainLossPercent =
                          position.costBasis > 0
                            ? (gainLoss / position.costBasis) * 100
                            : 0;
                        return (
                          <tr
                            key={`${position.investmentId}-${index}`}
                            className={`border-b border-[#334155]/50 ${
                              index % 2 === 0 ? 'bg-[#1E293B]' : 'bg-[#243449]'
                            }`}
                          >
                            <td className="px-3 py-3">
                              <div>
                                <span className="font-medium text-white">
                                  {position.investmentId}
                                </span>
                                <span className="ml-2 text-[#94A3B8]">
                                  {position.investmentName}
                                </span>
                              </div>
                            </td>
                            <td className="px-3 py-3 text-right text-[#CBD5E1]">
                              {position.quantity.toLocaleString()}
                            </td>
                            <td className="px-3 py-3 text-right text-[#CBD5E1]">
                              {formatCurrency(position.costBasis)}
                            </td>
                            <td className="px-3 py-3 text-right text-[#CBD5E1]">
                              {formatCurrency(position.marketValue)}
                            </td>
                            <td
                              className={`px-3 py-3 text-right font-medium ${
                                gainLoss >= 0 ? 'text-[#4ADE80]' : 'text-[#F87171]'
                              }`}
                            >
                              {formatCurrency(gainLoss)} ({gainLossPercent.toFixed(1)}%)
                            </td>
                            <td className="px-3 py-3 text-center">
                              <Badge
                                variant={getStatusBadgeVariant(position.status)}
                                aria-label={`Status: ${POSITION_STATUS_LABELS[position.status]}`}
                              >
                                {POSITION_STATUS_LABELS[position.status]}
                              </Badge>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
                {positionsResult.totalPages > 1 && (
                  <div className="mt-4">
                    <Pagination
                      currentPage={positionsResult.currentPage}
                      totalPages={positionsResult.totalPages}
                      onPageChange={handlePageChange}
                      totalItems={positionsResult.totalItems}
                      pageSize={positionsResult.pageSize}
                    />
                  </div>
                )}
              </CardContent>
            </Card>
          )}
        </>
      )}
    </div>
  );
}

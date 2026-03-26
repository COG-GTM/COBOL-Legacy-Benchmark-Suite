import { useState, useMemo } from 'react';
import { ReportSummaryCard } from '@/components/common/ReportSummaryCard';
import { DataTable } from '@/components/common/DataTable';
import type { ColumnDef } from '@/components/common/DataTable';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { positionReportSummary, positionReportEntries } from '@/mock/reportsData';
import type { PositionReportEntry } from '@/types/reports';

const formatCurrency = (val: number) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(val);

const formatPercent = (val: number) =>
  `${val >= 0 ? '+' : ''}${val.toFixed(2)}%`;

const formatGainLoss = (val: number) =>
  `${val >= 0 ? '+' : ''}${formatCurrency(val)}`;

const statusVariant = (status: string) => {
  if (status === 'Active') return 'success' as const;
  if (status === 'Closed') return 'outline' as const;
  return 'warning' as const;
};

export function PositionReportTab() {
  const [portfolioFilter, setPortfolioFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('All');
  const [gainLossFilter, setGainLossFilter] = useState('All');

  const uniquePortfolioIds = useMemo(
    () => [...new Set(positionReportEntries.map((e) => e.portfolioId))],
    []
  );

  const filteredData = useMemo(() => {
    return positionReportEntries.filter((entry) => {
      if (portfolioFilter && !entry.portfolioId.toLowerCase().includes(portfolioFilter.toLowerCase())) {
        return false;
      }
      if (statusFilter !== 'All' && entry.status !== statusFilter) return false;
      if (gainLossFilter === 'Gains Only' && entry.unrealizedGainLoss <= 0) return false;
      if (gainLossFilter === 'Losses Only' && entry.unrealizedGainLoss >= 0) return false;
      return true;
    });
  }, [portfolioFilter, statusFilter, gainLossFilter]);

  const columns: ColumnDef<PositionReportEntry>[] = [
    { key: 'portfolioId', header: 'Portfolio ID', sortable: true },
    { key: 'portfolioName', header: 'Portfolio Name', sortable: true },
    { key: 'investmentId', header: 'Investment ID', sortable: true },
    { key: 'investmentName', header: 'Investment Name', sortable: true },
    {
      key: 'quantity',
      header: 'Quantity',
      sortable: true,
      render: (row) => row.quantity.toLocaleString('en-US', { minimumFractionDigits: 0 }),
    },
    {
      key: 'costBasis',
      header: 'Cost Basis',
      sortable: true,
      render: (row) => formatCurrency(row.costBasis),
    },
    {
      key: 'marketValue',
      header: 'Market Value',
      sortable: true,
      render: (row) => formatCurrency(row.marketValue),
    },
    {
      key: 'unrealizedGainLoss',
      header: 'Gain/Loss ($)',
      sortable: true,
      render: (row) => (
        <span className={row.unrealizedGainLoss >= 0 ? 'text-[#4ADE80]' : 'text-[#F87171]'}>
          {formatGainLoss(row.unrealizedGainLoss)}
        </span>
      ),
    },
    {
      key: 'gainLossPercent',
      header: 'Gain/Loss (%)',
      sortable: true,
      render: (row) => (
        <span className={row.gainLossPercent >= 0 ? 'text-[#4ADE80]' : 'text-[#F87171]'}>
          {formatPercent(row.gainLossPercent)}
        </span>
      ),
    },
    {
      key: 'status',
      header: 'Status',
      sortable: true,
      render: (row) => <Badge variant={statusVariant(row.status)}>{row.status}</Badge>,
    },
  ];

  const groupSummary = (_groupKey: string, rows: PositionReportEntry[]) => {
    const totalCost = rows.reduce((s, r) => s + r.costBasis, 0);
    const totalMarket = rows.reduce((s, r) => s + r.marketValue, 0);
    const totalGL = totalMarket - totalCost;
    const glPercent = totalCost > 0 ? (totalGL / totalCost) * 100 : 0;
    return {
      portfolioId: '',
      portfolioName: `Subtotal (${rows.length} positions)`,
      investmentId: '',
      investmentName: '',
      quantity: '',
      costBasis: formatCurrency(totalCost),
      marketValue: formatCurrency(totalMarket),
      unrealizedGainLoss: (
        <span className={totalGL >= 0 ? 'text-[#4ADE80]' : 'text-[#F87171]'}>
          {formatGainLoss(totalGL)}
        </span>
      ),
      gainLossPercent: (
        <span className={glPercent >= 0 ? 'text-[#4ADE80]' : 'text-[#F87171]'}>
          {formatPercent(glPercent)}
        </span>
      ),
      status: '',
    };
  };

  const grandTotalCost = filteredData.reduce((s, r) => s + r.costBasis, 0);
  const grandTotalMarket = filteredData.reduce((s, r) => s + r.marketValue, 0);
  const grandTotalGL = grandTotalMarket - grandTotalCost;
  const grandGLPercent = grandTotalCost > 0 ? (grandTotalGL / grandTotalCost) * 100 : 0;

  const totalRow: Record<string, React.ReactNode> = {
    portfolioId: '',
    portfolioName: 'Grand Total',
    investmentId: '',
    investmentName: '',
    quantity: '',
    costBasis: formatCurrency(grandTotalCost),
    marketValue: formatCurrency(grandTotalMarket),
    unrealizedGainLoss: (
      <span className={grandTotalGL >= 0 ? 'text-[#4ADE80]' : 'text-[#F87171]'}>
        {formatGainLoss(grandTotalGL)}
      </span>
    ),
    gainLossPercent: (
      <span className={grandGLPercent >= 0 ? 'text-[#4ADE80]' : 'text-[#F87171]'}>
        {formatPercent(grandGLPercent)}
      </span>
    ),
    status: '',
  };

  const clearFilters = () => {
    setPortfolioFilter('');
    setStatusFilter('All');
    setGainLossFilter('All');
  };

  const hasFilters = portfolioFilter || statusFilter !== 'All' || gainLossFilter !== 'All';
  const summary = positionReportSummary;

  return (
    <div className="space-y-6">
      {/* Summary Cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <ReportSummaryCard title="Total Portfolios" value={summary.totalPortfolios} color="blue" />
        <ReportSummaryCard title="Total Positions" value={summary.totalPositions} color="blue" />
        <ReportSummaryCard
          title="Total Market Value"
          value={formatCurrency(summary.totalMarketValue)}
          color="blue"
        />
        <ReportSummaryCard
          title="Overall Gain/Loss"
          value={`${formatGainLoss(summary.totalUnrealizedGainLoss)} / ${formatPercent(summary.overallGainLossPercent)}`}
          color={summary.totalUnrealizedGainLoss >= 0 ? 'green' : 'red'}
        />
      </div>

      {/* Filters Bar */}
      <div className="flex flex-wrap items-end gap-3 rounded-lg bg-[#0F172A] p-4">
        <div className="flex flex-col gap-1">
          <label htmlFor="portfolio-filter" className="sr-only">
            Portfolio ID
          </label>
          <Input
            id="portfolio-filter"
            placeholder="Filter by Portfolio ID..."
            value={portfolioFilter}
            onChange={(e) => setPortfolioFilter(e.target.value)}
            className="w-48"
            list="portfolio-ids"
          />
          <datalist id="portfolio-ids">
            {uniquePortfolioIds.map((id) => (
              <option key={id} value={id} />
            ))}
          </datalist>
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor="status-filter" className="sr-only">
            Status
          </label>
          <select
            id="status-filter"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="h-10 rounded-md border border-[#334155] bg-[#0F172A] px-3 py-2 text-sm text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#22D3EE]"
          >
            <option>All</option>
            <option>Active</option>
            <option>Closed</option>
            <option>Pending</option>
          </select>
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor="gainloss-filter" className="sr-only">
            Gain/Loss
          </label>
          <select
            id="gainloss-filter"
            value={gainLossFilter}
            onChange={(e) => setGainLossFilter(e.target.value)}
            className="h-10 rounded-md border border-[#334155] bg-[#0F172A] px-3 py-2 text-sm text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#22D3EE]"
          >
            <option>All</option>
            <option>Gains Only</option>
            <option>Losses Only</option>
          </select>
        </div>
        {hasFilters && (
          <Button variant="ghost" size="sm" onClick={clearFilters}>
            Clear Filters
          </Button>
        )}
      </div>

      {/* Position Table */}
      <DataTable<PositionReportEntry>
        columns={columns}
        data={filteredData}
        pageSize={10}
        groupBy="portfolioId"
        groupSummary={groupSummary}
        totalRow={totalRow}
        getRowKey={(row, i) => `${row.portfolioId}-${row.investmentId}-${i}`}
      />
    </div>
  );
}

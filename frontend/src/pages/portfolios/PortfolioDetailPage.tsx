import { useMemo } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, Pencil } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge, getPortfolioStatusVariant, getPortfolioStatusLabel, getPositionStatusVariant, getPositionStatusLabel } from '@/components/ui/StatusBadge';
import { PageHeader } from '@/components/ui/PageHeader';
import { EmptyState } from '@/components/ui/EmptyState';
import { portfolios, positions } from '@/data/mockData';
import type { Position } from '@/data/types';

type PositionWithMarketValue = Position & { marketValue: number };
type PositionRecord = PositionWithMarketValue & Record<string, unknown>;

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value);
}

function formatShares(value: number): string {
  return new Intl.NumberFormat('en-US', { minimumFractionDigits: 3, maximumFractionDigits: 3 }).format(value);
}

const clientTypeLabels: Record<string, string> = {
  I: 'Individual',
  C: 'Corporate',
  T: 'Trust',
};

export function PortfolioDetailPage() {
  const { id } = useParams<{ id: string }>();

  const portfolio = useMemo(() => portfolios.find((p) => p.id === id), [id]);

  const portfolioPositions = useMemo(() => {
    if (!portfolio) return [];
    return positions
      .filter((p) => p.accountNo === portfolio.accountNo)
      .map((p) => ({ ...p, marketValue: p.shareBalance * p.avgCost }));
  }, [portfolio]);

  if (!portfolio) {
    return (
      <div>
        <PageHeader title="Portfolio Not Found" />
        <EmptyState
          title="Portfolio not found"
          message={`No portfolio with ID "${id ?? ''}" was found.`}
          action={
            <Link
              to="/portfolios"
              className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
            >
              <ArrowLeft className="w-4 h-4" />
              Back to Portfolios
            </Link>
          }
        />
      </div>
    );
  }

  const positionColumns: Column<PositionRecord>[] = [
    { key: 'fundId', header: 'Fund ID', sortable: true },
    { key: 'cusip', header: 'CUSIP', sortable: true },
    {
      key: 'shareBalance',
      header: 'Share Balance',
      sortable: true,
      className: 'text-right',
      render: (row) => <span>{formatShares(row.shareBalance)}</span>,
    },
    {
      key: 'avgCost',
      header: 'Avg Cost',
      sortable: true,
      className: 'text-right',
      render: (row) => <span>{formatCurrency(row.avgCost)}</span>,
    },
    {
      key: 'costBasis',
      header: 'Cost Basis',
      sortable: true,
      className: 'text-right',
      render: (row) => <span>{formatCurrency(row.costBasis)}</span>,
    },
    {
      key: 'marketValue',
      header: 'Market Value',
      sortable: true,
      className: 'text-right',
      render: (row) => (
        <span className="font-medium">{formatCurrency(row.marketValue)}</span>
      ),
    },
    { key: 'lastDate', header: 'Last Date', sortable: true },
    {
      key: 'status',
      header: 'Status',
      sortable: true,
      render: (row) => (
        <StatusBadge
          label={getPositionStatusLabel(row.status)}
          variant={getPositionStatusVariant(row.status)}
        />
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title={portfolio.name}
        description={`Portfolio ${portfolio.id}`}
        actions={
          <div className="flex items-center gap-3">
            <Link
              to="/portfolios"
              className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
            >
              <ArrowLeft className="w-4 h-4" />
              Back to Portfolios
            </Link>
            <Link
              to={`/portfolios/${portfolio.id}/edit`}
              className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
            >
              <Pencil className="w-4 h-4" />
              Edit
            </Link>
          </div>
        }
      />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
        <Card title="Portfolio Summary" className="lg:col-span-2">
          <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-4">
            <div>
              <dt className="text-sm font-medium text-slate-500">Portfolio ID</dt>
              <dd className="mt-1 text-sm font-mono text-slate-900">{portfolio.id}</dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-slate-500">Account Number</dt>
              <dd className="mt-1 text-sm font-mono text-slate-900">{portfolio.accountNo}</dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-slate-500">Name</dt>
              <dd className="mt-1 text-sm text-slate-900">{portfolio.name}</dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-slate-500">Client Type</dt>
              <dd className="mt-1 text-sm text-slate-900">{clientTypeLabels[portfolio.clientType] ?? portfolio.clientType}</dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-slate-500">Status</dt>
              <dd className="mt-1">
                <StatusBadge
                  label={getPortfolioStatusLabel(portfolio.status)}
                  variant={getPortfolioStatusVariant(portfolio.status)}
                />
              </dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-slate-500">Create Date</dt>
              <dd className="mt-1 text-sm text-slate-900">{portfolio.createDate}</dd>
            </div>
          </dl>
        </Card>

        <Card title="Financials">
          <dl className="space-y-4">
            <div>
              <dt className="text-sm font-medium text-slate-500">Total Value</dt>
              <dd className="mt-1 text-xl font-bold text-slate-900">{formatCurrency(portfolio.totalValue)}</dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-slate-500">Cash Balance</dt>
              <dd className="mt-1 text-lg font-semibold text-slate-900">{formatCurrency(portfolio.cashBalance)}</dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-slate-500">Active Positions</dt>
              <dd className="mt-1 text-sm text-slate-900">{portfolioPositions.filter((p) => p.status === 'A').length}</dd>
            </div>
          </dl>
        </Card>
      </div>

      <Card title="Positions">
        <div className="-m-6 mt-0">
          <DataTable<PositionRecord>
            columns={positionColumns}
            data={portfolioPositions as PositionRecord[]}
            keyExtractor={(row) => `${row.accountNo}-${row.fundId}`}
            emptyMessage="No positions found for this portfolio"
          />
        </div>
      </Card>
    </div>
  );
}

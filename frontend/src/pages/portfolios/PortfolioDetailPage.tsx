import { useState } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { ArrowLeft, Pencil, Trash2, Briefcase } from 'lucide-react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';
import { EmptyState } from '@/components/ui/EmptyState';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { getPositionStatusLabel, getPositionStatusVariant } from '@/components/ui/StatusBadge';
import { usePortfolios } from './usePortfolios';
import {
  CLIENT_TYPE_LABELS,
  STATUS_LABELS,
  formatCurrency,
  formatQuantity,
  getHoldings,
  getStatusVariant,
  isDeletable,
} from './portfolioData';
import type { Holding } from './portfolioData';

const holdingColumns: Column<Holding>[] = [
  {
    key: 'fundId',
    header: 'Fund',
    sortable: true,
    render: (h) => <span className="font-medium text-slate-900">{h.fundId}</span>,
  },
  {
    key: 'cusip',
    header: 'CUSIP',
    render: (h) => <span className="font-mono">{h.cusip}</span>,
  },
  {
    key: 'quantity',
    header: 'Quantity',
    sortable: true,
    className: 'text-right tabular-nums',
    render: (h) => formatQuantity(h.quantity),
  },
  {
    key: 'costBasis',
    header: 'Cost Basis',
    sortable: true,
    className: 'text-right tabular-nums',
    render: (h) => formatCurrency(h.costBasis),
  },
  {
    key: 'marketValue',
    header: 'Market Value',
    sortable: true,
    className: 'text-right tabular-nums',
    render: (h) => formatCurrency(h.marketValue),
  },
  {
    key: 'gainLoss',
    header: 'Gain/Loss',
    sortable: true,
    className: 'text-right tabular-nums',
    render: (h) => (
      <span className={h.gainLoss >= 0 ? 'text-emerald-600' : 'text-red-600'}>
        {h.gainLoss >= 0 ? '+' : ''}
        {formatCurrency(h.gainLoss)}
      </span>
    ),
  },
  {
    key: 'status',
    header: 'Status',
    render: (h) => (
      <StatusBadge label={getPositionStatusLabel(h.status)} variant={getPositionStatusVariant(h.status)} />
    ),
  },
];

function DetailField({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div>
      <dt className="text-sm font-medium text-slate-500">{label}</dt>
      <dd className="mt-1 text-sm text-slate-900">{value}</dd>
    </div>
  );
}

export function PortfolioDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { loading, getPortfolio, deletePortfolio } = usePortfolios();
  const [confirmOpen, setConfirmOpen] = useState(false);

  if (loading) {
    return <LoadingSpinner message="Loading portfolio..." />;
  }

  const portfolio = id ? getPortfolio(id) : undefined;

  if (!portfolio) {
    return (
      <EmptyState
        title="Portfolio not found"
        message={`No portfolio exists with ID "${id ?? ''}".`}
        icon={<Briefcase className="w-12 h-12" />}
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
    );
  }

  const holdings = getHoldings(portfolio.accountNo);
  const deletable = isDeletable(portfolio);

  const handleDelete = () => {
    deletePortfolio(portfolio.id);
    navigate('/portfolios');
  };

  return (
    <div>
      <PageHeader
        title={portfolio.id}
        description={portfolio.clientName}
        actions={
          <>
            <button
              onClick={() => navigate('/portfolios')}
              className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
            >
              <ArrowLeft className="w-4 h-4" />
              Back
            </button>
            <button
              onClick={() => navigate(`/portfolios/${portfolio.id}/edit`)}
              className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
            >
              <Pencil className="w-4 h-4" />
              Edit
            </button>
            <button
              onClick={() => setConfirmOpen(true)}
              disabled={!deletable}
              title={deletable ? 'Delete portfolio' : 'Active portfolios cannot be deleted. Close or suspend the portfolio first.'}
              className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-red-600 rounded-lg hover:bg-red-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-red-600"
            >
              <Trash2 className="w-4 h-4" />
              Delete
            </button>
          </>
        }
      />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
        <Card title="Account Information" className="lg:col-span-2">
          <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-4">
            <DetailField label="Portfolio ID" value={<span className="font-mono">{portfolio.id}</span>} />
            <DetailField label="Account Number" value={<span className="font-mono">{portfolio.accountNo}</span>} />
            <DetailField label="Client Name" value={portfolio.clientName} />
            <DetailField label="Client Type" value={CLIENT_TYPE_LABELS[portfolio.clientType]} />
            <DetailField
              label="Status"
              value={<StatusBadge label={STATUS_LABELS[portfolio.status]} variant={getStatusVariant(portfolio.status)} />}
            />
            <DetailField label="Created" value={portfolio.createDate} />
            <DetailField label="Last Maintained" value={portfolio.lastMaint} />
          </dl>
        </Card>

        <Card title="Financial Summary">
          <dl className="space-y-4">
            <DetailField
              label="Total Value"
              value={<span className="text-2xl font-semibold tabular-nums">{formatCurrency(portfolio.totalValue)}</span>}
            />
            <DetailField
              label="Cash Balance"
              value={<span className="text-lg font-medium tabular-nums">{formatCurrency(portfolio.cashBalance)}</span>}
            />
          </dl>
        </Card>
      </div>

      <Card title="Holdings">
        {holdings.length === 0 ? (
          <EmptyState
            title="No holdings"
            message="This portfolio has no positions on record."
            icon={<Briefcase className="w-12 h-12" />}
          />
        ) : (
          <DataTable
            columns={holdingColumns}
            data={holdings}
            keyExtractor={(h) => `${h.fundId}-${h.cusip}`}
          />
        )}
      </Card>

      <ConfirmDialog
        open={confirmOpen}
        title="Delete Portfolio"
        message={`Are you sure you want to delete portfolio ${portfolio.id} (${portfolio.clientName})? This action cannot be undone.`}
        confirmLabel="Delete"
        variant="danger"
        onConfirm={handleDelete}
        onCancel={() => setConfirmOpen(false)}
      />
    </div>
  );
}

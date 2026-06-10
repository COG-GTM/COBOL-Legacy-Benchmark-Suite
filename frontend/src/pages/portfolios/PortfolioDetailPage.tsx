import { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { Pencil, Trash2 } from 'lucide-react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { StatusBadge, getPortfolioStatusVariant, getPortfolioStatusLabel } from '@/components/ui/StatusBadge';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { EmptyState } from '@/components/ui/EmptyState';
import { usePortfolios } from '@/context/PortfolioContext';

export function PortfolioDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { getPortfolio, deletePortfolio } = usePortfolios();
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);

  const portfolio = id ? getPortfolio(id) : undefined;

  if (!portfolio) {
    return (
      <div>
        <PageHeader title="Portfolio Not Found" />
        <EmptyState
          title="Portfolio not found"
          message={`No portfolio exists with ID "${id ?? ''}".`}
          action={
            <Link
              to="/portfolios"
              className="inline-flex items-center px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
            >
              Back to Portfolios
            </Link>
          }
        />
      </div>
    );
  }

  const handleDelete = () => {
    deletePortfolio(portfolio.id);
    navigate('/portfolios');
  };

  return (
    <div>
      <PageHeader
        title={portfolio.name}
        description={`Portfolio ${portfolio.id}`}
        actions={
          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate(`/portfolios/${portfolio.id}/edit`)}
              className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
            >
              <Pencil className="w-4 h-4" />
              Edit
            </button>
            <button
              onClick={() => setShowDeleteDialog(true)}
              className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-red-600 rounded-lg hover:bg-red-700 transition-colors"
            >
              <Trash2 className="w-4 h-4" />
              Delete
            </button>
          </div>
        }
      />

      <Card>
        <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-8 gap-y-6">
          <div>
            <dt className="text-sm font-medium text-slate-500">Portfolio ID</dt>
            <dd className="mt-1 text-sm text-slate-900 font-mono">{portfolio.id}</dd>
          </div>
          <div>
            <dt className="text-sm font-medium text-slate-500">Name</dt>
            <dd className="mt-1 text-sm text-slate-900">{portfolio.name}</dd>
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
            <dt className="text-sm font-medium text-slate-500">Total Value</dt>
            <dd className="mt-1 text-sm text-slate-900 font-mono">
              {portfolio.totalValue.toLocaleString('en-US', { style: 'currency', currency: 'USD' })}
            </dd>
          </div>
          <div>
            <dt className="text-sm font-medium text-slate-500">Create Date</dt>
            <dd className="mt-1 text-sm text-slate-900">{portfolio.createDate}</dd>
          </div>
        </dl>
      </Card>

      <ConfirmDialog
        open={showDeleteDialog}
        title="Delete Portfolio"
        message={`Are you sure you want to delete "${portfolio.name}"? This action cannot be undone.`}
        confirmLabel="Delete"
        cancelLabel="Cancel"
        variant="danger"
        onConfirm={handleDelete}
        onCancel={() => setShowDeleteDialog(false)}
      />
    </div>
  );
}

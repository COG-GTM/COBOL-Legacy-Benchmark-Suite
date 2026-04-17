import { useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Pencil, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { StatusBadge, getPortfolioStatusVariant, getPortfolioStatusLabel, getPositionStatusVariant, getPositionStatusLabel, getTransactionStatusVariant, getTransactionStatusLabel, getTransTypeLabel } from '@/components/ui/StatusBadge';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { Toast } from '@/components/ui/Toast';
import { usePortfolioContext, getAccountForPortfolio } from '@/context/PortfolioContext';
import type { Position, Transaction } from '@/data/types';

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
}

export function PortfolioDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const {
    getPortfolio,
    deletePortfolio,
    getPositionsForAccount,
    getTransactionsForAccount,
    notification,
    showNotification,
    clearNotification,
  } = usePortfolioContext();
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);

  const portfolio = id ? getPortfolio(id) : undefined;
  const accountNo = id ? getAccountForPortfolio(id) : undefined;

  const positions = useMemo(
    () => (accountNo ? getPositionsForAccount(accountNo) : []),
    [accountNo, getPositionsForAccount],
  );

  const transactions = useMemo(
    () => (accountNo ? getTransactionsForAccount(accountNo) : []),
    [accountNo, getTransactionsForAccount],
  );

  if (!portfolio) {
    return (
      <div className="flex flex-col items-center justify-center py-16 text-center">
        <h2 className="text-xl font-semibold text-slate-900 mb-2">Portfolio Not Found</h2>
        <p className="text-sm text-slate-500 mb-4">
          The portfolio with ID &quot;{id}&quot; could not be found.
        </p>
        <button
          onClick={() => navigate('/portfolios')}
          className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to Portfolios
        </button>
      </div>
    );
  }

  const handleDelete = () => {
    deletePortfolio(portfolio.id);
    showNotification(`Portfolio "${portfolio.name}" deleted successfully`, 'success');
    navigate('/portfolios');
  };

  const positionColumns: Column<Position & Record<string, unknown>>[] = [
    { key: 'fundId', header: 'Fund ID', sortable: true },
    { key: 'cusip', header: 'CUSIP', sortable: true },
    {
      key: 'shareBalance',
      header: 'Shares',
      sortable: true,
      className: 'text-right',
      render: (row) => <span className="font-mono">{(row.shareBalance as number).toLocaleString('en-US', { minimumFractionDigits: 3 })}</span>,
    },
    {
      key: 'avgCost',
      header: 'Avg Cost',
      sortable: true,
      className: 'text-right',
      render: (row) => <span className="font-mono">{formatCurrency(row.avgCost as number)}</span>,
    },
    {
      key: 'costBasis',
      header: 'Cost Basis',
      sortable: true,
      className: 'text-right',
      render: (row) => <span className="font-mono">{formatCurrency(row.costBasis as number)}</span>,
    },
    { key: 'lastDate', header: 'Last Date', sortable: true },
    {
      key: 'status',
      header: 'Status',
      sortable: true,
      render: (row) => (
        <StatusBadge
          label={getPositionStatusLabel(row.status as string)}
          variant={getPositionStatusVariant(row.status as string)}
        />
      ),
    },
  ];

  const transactionColumns: Column<Transaction & Record<string, unknown>>[] = [
    { key: 'transId', header: 'Trans ID', sortable: true },
    {
      key: 'transType',
      header: 'Type',
      sortable: true,
      render: (row) => <span>{getTransTypeLabel(row.transType as string)}</span>,
    },
    { key: 'fundId', header: 'Fund ID', sortable: true },
    { key: 'transDate', header: 'Date', sortable: true },
    {
      key: 'shareQty',
      header: 'Shares',
      sortable: true,
      className: 'text-right',
      render: (row) => <span className="font-mono">{(row.shareQty as number).toLocaleString('en-US', { minimumFractionDigits: 3 })}</span>,
    },
    {
      key: 'amount',
      header: 'Amount',
      sortable: true,
      className: 'text-right',
      render: (row) => <span className="font-mono">{formatCurrency(row.amount as number)}</span>,
    },
    {
      key: 'status',
      header: 'Status',
      sortable: true,
      render: (row) => (
        <StatusBadge
          label={getTransactionStatusLabel(row.status as string)}
          variant={getTransactionStatusVariant(row.status as string)}
        />
      ),
    },
  ];

  const positionData = positions.map((p) => ({ ...p } as Position & Record<string, unknown>));
  const transactionData = transactions.map((t) => ({ ...t } as Transaction & Record<string, unknown>));

  return (
    <div>
      {notification && (
        <Toast message={notification.message} type={notification.type} onClose={clearNotification} />
      )}

      <PageHeader
        title={portfolio.name}
        description={`Portfolio ${portfolio.id}`}
        actions={
          <div className="flex items-center gap-2">
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
              onClick={() => setShowDeleteDialog(true)}
              className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-red-600 rounded-lg hover:bg-red-700 transition-colors"
            >
              <Trash2 className="w-4 h-4" />
              Delete
            </button>
          </div>
        }
      />

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <Card>
          <p className="text-sm text-slate-500">Portfolio ID</p>
          <p className="text-lg font-semibold text-slate-900 font-mono">{portfolio.id}</p>
        </Card>
        <Card>
          <p className="text-sm text-slate-500">Status</p>
          <div className="mt-1">
            <StatusBadge
              label={getPortfolioStatusLabel(portfolio.status)}
              variant={getPortfolioStatusVariant(portfolio.status)}
            />
          </div>
        </Card>
        <Card>
          <p className="text-sm text-slate-500">Total Value</p>
          <p className="text-lg font-semibold text-slate-900 font-mono">{formatCurrency(portfolio.totalValue)}</p>
        </Card>
        <Card>
          <p className="text-sm text-slate-500">Created Date</p>
          <p className="text-lg font-semibold text-slate-900">{portfolio.createDate}</p>
        </Card>
      </div>

      <div className="space-y-6">
        <Card title={`Positions (${positions.length})`}>
          <DataTable
            columns={positionColumns}
            data={positionData}
            keyExtractor={(row) => `${row.accountNo}-${row.fundId}`}
            emptyMessage="No positions found for this portfolio"
          />
        </Card>

        <Card title={`Recent Transactions (${transactions.length})`}>
          <DataTable
            columns={transactionColumns}
            data={transactionData}
            keyExtractor={(row) => row.transId as string}
            emptyMessage="No transactions found for this portfolio"
          />
        </Card>
      </div>

      <ConfirmDialog
        open={showDeleteDialog}
        title="Delete Portfolio"
        message={`Are you sure you want to delete "${portfolio.name}"? This action cannot be undone.`}
        confirmLabel="Delete"
        variant="danger"
        onConfirm={handleDelete}
        onCancel={() => setShowDeleteDialog(false)}
      />
    </div>
  );
}

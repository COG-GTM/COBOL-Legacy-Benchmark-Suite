import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Trash2, Briefcase } from 'lucide-react';
import { PageHeader } from '@/components/ui/PageHeader';
import { SearchInput } from '@/components/ui/SearchInput';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge, getPortfolioStatusVariant, getPortfolioStatusLabel } from '@/components/ui/StatusBadge';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { Card } from '@/components/ui/Card';
import { Toast } from '@/components/ui/Toast';
import { usePortfolioContext } from '@/context/PortfolioContext';
import type { Portfolio } from '@/data/types';

type StatusFilter = 'ALL' | 'A' | 'I' | 'C';

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
}

export function PortfolioListPage() {
  const navigate = useNavigate();
  const { portfolios, deletePortfolio, notification, showNotification, clearNotification } = usePortfolioContext();
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
  const [deleteTarget, setDeleteTarget] = useState<Portfolio | null>(null);

  const filtered = useMemo(() => {
    let result = portfolios;
    if (statusFilter !== 'ALL') {
      result = result.filter((p) => p.status === statusFilter);
    }
    if (search.trim()) {
      const q = search.trim().toLowerCase();
      result = result.filter(
        (p) => p.id.toLowerCase().includes(q) || p.name.toLowerCase().includes(q),
      );
    }
    return result;
  }, [portfolios, search, statusFilter]);

  const totalValue = useMemo(
    () => filtered.reduce((sum, p) => sum + p.totalValue, 0),
    [filtered],
  );

  const handleDelete = () => {
    if (deleteTarget) {
      deletePortfolio(deleteTarget.id);
      showNotification(`Portfolio "${deleteTarget.name}" deleted successfully`, 'success');
      setDeleteTarget(null);
    }
  };

  const columns: Column<Portfolio & Record<string, unknown>>[] = [
    { key: 'id', header: 'ID', sortable: true },
    {
      key: 'name',
      header: 'Name',
      sortable: true,
      render: (row) => (
        <button
          onClick={() => navigate(`/portfolios/${row.id}`)}
          className="text-blue-600 hover:text-blue-800 hover:underline font-medium text-left"
        >
          {row.name}
        </button>
      ),
    },
    {
      key: 'status',
      header: 'Status',
      sortable: true,
      render: (row) => (
        <StatusBadge
          label={getPortfolioStatusLabel(row.status)}
          variant={getPortfolioStatusVariant(row.status)}
        />
      ),
    },
    {
      key: 'totalValue',
      header: 'Total Value',
      sortable: true,
      className: 'text-right',
      render: (row) => (
        <span className="font-mono">{formatCurrency(row.totalValue)}</span>
      ),
    },
    {
      key: 'createDate',
      header: 'Created Date',
      sortable: true,
    },
    {
      key: '_actions',
      header: '',
      render: (row) => (
        <button
          onClick={(e) => {
            e.stopPropagation();
            setDeleteTarget(row as Portfolio);
          }}
          className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-md transition-colors"
          title="Delete portfolio"
        >
          <Trash2 className="w-4 h-4" />
        </button>
      ),
    },
  ];

  const tableData = filtered.map((p) => ({ ...p } as Portfolio & Record<string, unknown>));

  return (
    <div>
      {notification && (
        <Toast message={notification.message} type={notification.type} onClose={clearNotification} />
      )}

      <PageHeader
        title="Portfolios"
        description="Manage your investment portfolios"
        actions={
          <button
            onClick={() => navigate('/portfolios/new')}
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
          >
            <Plus className="w-4 h-4" />
            Create Portfolio
          </button>
        }
      />

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-6">
        <Card className="!p-0">
          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-10 h-10 rounded-lg bg-blue-50">
              <Briefcase className="w-5 h-5 text-blue-600" />
            </div>
            <div>
              <p className="text-sm text-slate-500">Total Portfolios</p>
              <p className="text-xl font-bold text-slate-900">{filtered.length}</p>
            </div>
          </div>
        </Card>
        <Card className="!p-0">
          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-10 h-10 rounded-lg bg-emerald-50">
              <span className="text-lg font-bold text-emerald-600">$</span>
            </div>
            <div>
              <p className="text-sm text-slate-500">Total Value</p>
              <p className="text-xl font-bold text-slate-900">{formatCurrency(totalValue)}</p>
            </div>
          </div>
        </Card>
      </div>

      <Card>
        <div className="flex flex-col sm:flex-row gap-3 mb-4">
          <SearchInput
            value={search}
            onChange={setSearch}
            placeholder="Search by name or ID..."
            className="flex-1"
          />
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as StatusFilter)}
            className="px-3 py-2 text-sm border border-slate-300 rounded-lg bg-white text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          >
            <option value="ALL">All Statuses</option>
            <option value="A">Active</option>
            <option value="I">Inactive</option>
            <option value="C">Closed</option>
          </select>
        </div>

        <DataTable
          columns={columns}
          data={tableData}
          keyExtractor={(row) => row.id as string}
          emptyMessage="No portfolios found matching your criteria"
        />
      </Card>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Delete Portfolio"
        message={`Are you sure you want to delete "${deleteTarget?.name ?? ''}"? This action cannot be undone.`}
        confirmLabel="Delete"
        variant="danger"
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
}

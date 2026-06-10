import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Briefcase } from 'lucide-react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { SearchInput } from '@/components/ui/SearchInput';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';
import { EmptyState } from '@/components/ui/EmptyState';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { usePortfolios } from './usePortfolios';
import {
  CLIENT_TYPE_LABELS,
  STATUS_LABELS,
  formatCurrency,
  getStatusVariant,
} from './portfolioData';
import type { PortfolioMaster } from './portfolioData';

type StatusFilter = 'ALL' | 'A' | 'C' | 'S';

const statusFilters: { value: StatusFilter; label: string }[] = [
  { value: 'ALL', label: 'All' },
  { value: 'A', label: 'Active' },
  { value: 'C', label: 'Closed' },
  { value: 'S', label: 'Suspended' },
];

const columns: Column<PortfolioMaster>[] = [
  {
    key: 'id',
    header: 'Portfolio ID',
    sortable: true,
    render: (p) => <span className="font-medium text-blue-600">{p.id}</span>,
  },
  {
    key: 'accountNo',
    header: 'Account No',
    sortable: true,
    render: (p) => <span className="font-mono">{p.accountNo}</span>,
  },
  { key: 'clientName', header: 'Client Name', sortable: true },
  {
    key: 'clientType',
    header: 'Client Type',
    render: (p) => CLIENT_TYPE_LABELS[p.clientType],
  },
  {
    key: 'status',
    header: 'Status',
    render: (p) => <StatusBadge label={STATUS_LABELS[p.status]} variant={getStatusVariant(p.status)} />,
  },
  {
    key: 'totalValue',
    header: 'Total Value',
    sortable: true,
    className: 'text-right tabular-nums',
    render: (p) => formatCurrency(p.totalValue),
  },
  {
    key: 'cashBalance',
    header: 'Cash Balance',
    sortable: true,
    className: 'text-right tabular-nums',
    render: (p) => formatCurrency(p.cashBalance),
  },
];

export function PortfolioListPage() {
  const navigate = useNavigate();
  const { portfolios, loading } = usePortfolios();
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');

  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase();
    return portfolios.filter((p) => {
      if (statusFilter !== 'ALL' && p.status !== statusFilter) return false;
      if (!query) return true;
      return (
        p.id.toLowerCase().includes(query) ||
        p.accountNo.includes(query) ||
        p.clientName.toLowerCase().includes(query)
      );
    });
  }, [portfolios, search, statusFilter]);

  return (
    <div>
      <PageHeader
        title="Portfolios"
        description="Manage your investment portfolios"
        actions={
          <button
            onClick={() => navigate('/portfolios/new')}
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
          >
            <Plus className="w-4 h-4" />
            New Portfolio
          </button>
        }
      />

      <Card>
        <div className="flex flex-col sm:flex-row sm:items-center gap-4 mb-4">
          <SearchInput
            value={search}
            onChange={setSearch}
            placeholder="Search by portfolio ID, account number, or client name..."
            className="flex-1"
          />
          <div className="flex items-center gap-1 bg-slate-100 rounded-lg p-1">
            {statusFilters.map((f) => (
              <button
                key={f.value}
                onClick={() => setStatusFilter(f.value)}
                className={`px-3 py-1.5 text-sm font-medium rounded-md transition-colors ${
                  statusFilter === f.value
                    ? 'bg-white text-slate-900 shadow-sm'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                {f.label}
              </button>
            ))}
          </div>
        </div>

        {loading ? (
          <LoadingSpinner message="Loading portfolios..." />
        ) : filtered.length === 0 ? (
          <EmptyState
            title="No portfolios found"
            message={
              search || statusFilter !== 'ALL'
                ? 'No portfolios match your search criteria. Try adjusting your filters.'
                : 'There are no portfolios in the system yet.'
            }
            icon={<Briefcase className="w-12 h-12" />}
          />
        ) : (
          <DataTable
            columns={columns}
            data={filtered}
            keyExtractor={(p) => p.id}
            onRowClick={(p) => navigate(`/portfolios/${p.id}`)}
          />
        )}
      </Card>
    </div>
  );
}

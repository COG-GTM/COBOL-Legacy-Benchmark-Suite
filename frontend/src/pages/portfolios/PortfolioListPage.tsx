import { useState, useMemo } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Plus } from 'lucide-react';
import { PageHeader } from '@/components/ui/PageHeader';
import { SearchInput } from '@/components/ui/SearchInput';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge, getPortfolioStatusVariant, getPortfolioStatusLabel } from '@/components/ui/StatusBadge';
import { usePortfolios } from '@/context/PortfolioContext';
import type { Portfolio } from '@/data/types';

type PortfolioRow = Portfolio & Record<string, unknown>;
type StatusFilter = 'all' | 'A' | 'I' | 'C';

export function PortfolioListPage() {
  const { portfolios } = usePortfolios();
  const navigate = useNavigate();
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('all');

  const filteredPortfolios = useMemo(() => {
    return portfolios.filter((p) => {
      const matchesSearch =
        p.name.toLowerCase().includes(search.toLowerCase()) ||
        p.id.toLowerCase().includes(search.toLowerCase());
      const matchesStatus = statusFilter === 'all' || p.status === statusFilter;
      return matchesSearch && matchesStatus;
    });
  }, [portfolios, search, statusFilter]);

  const columns: Column<PortfolioRow>[] = [
    {
      key: 'id',
      header: 'ID',
      sortable: true,
      render: (row) => (
        <Link to={`/portfolios/${row.id}`} className="text-blue-600 hover:text-blue-800 font-medium">
          {row.id}
        </Link>
      ),
    },
    { key: 'name', header: 'Name', sortable: true },
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
        <span className="font-mono">
          {row.totalValue.toLocaleString('en-US', { style: 'currency', currency: 'USD' })}
        </span>
      ),
    },
    { key: 'createDate', header: 'Create Date', sortable: true },
  ];

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

      <div className="flex flex-col sm:flex-row gap-3 mb-4">
        <SearchInput
          value={search}
          onChange={setSearch}
          placeholder="Search by name or ID..."
          className="sm:w-72"
        />
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as StatusFilter)}
          className="px-3 py-2 text-sm border border-slate-300 rounded-lg bg-white text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="all">All Statuses</option>
          <option value="A">Active</option>
          <option value="I">Inactive</option>
          <option value="C">Closed</option>
        </select>
      </div>

      <div className="bg-white rounded-lg border border-slate-200 shadow-sm overflow-hidden">
        <DataTable<PortfolioRow>
          columns={columns}
          data={filteredPortfolios as PortfolioRow[]}
          keyExtractor={(row) => row.id}
          emptyMessage="No portfolios match your search criteria"
        />
      </div>
    </div>
  );
}

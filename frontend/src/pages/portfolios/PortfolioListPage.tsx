import { useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { Plus } from 'lucide-react';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge, getPortfolioStatusVariant, getPortfolioStatusLabel } from '@/components/ui/StatusBadge';
import { SearchInput } from '@/components/ui/SearchInput';
import { PageHeader } from '@/components/ui/PageHeader';
import { portfolios } from '@/data/mockData';
import type { Portfolio } from '@/data/types';

type PortfolioRecord = Portfolio & Record<string, unknown>;

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(value);
}

type StatusFilter = 'all' | 'A' | 'I' | 'C';

export function PortfolioListPage() {
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('all');

  const filteredData = useMemo(() => {
    return portfolios.filter((p) => {
      const matchesSearch =
        search === '' ||
        p.name.toLowerCase().includes(search.toLowerCase()) ||
        p.id.toLowerCase().includes(search.toLowerCase());
      const matchesStatus = statusFilter === 'all' || p.status === statusFilter;
      return matchesSearch && matchesStatus;
    });
  }, [search, statusFilter]);

  const columns: Column<PortfolioRecord>[] = [
    {
      key: 'id',
      header: 'Portfolio ID',
      sortable: true,
      render: (row) => (
        <Link to={`/portfolios/${row.id}`} className="font-mono text-blue-600 hover:text-blue-800 hover:underline">
          {row.id}
        </Link>
      ),
    },
    {
      key: 'name',
      header: 'Name',
      sortable: true,
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
        <span className="font-medium">{formatCurrency(row.totalValue)}</span>
      ),
    },
    {
      key: 'createDate',
      header: 'Create Date',
      sortable: true,
    },
  ];

  return (
    <div>
      <PageHeader
        title="Portfolios"
        description="Manage your investment portfolios"
        actions={
          <Link
            to="/portfolios/new"
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
          >
            <Plus className="w-4 h-4" />
            New Portfolio
          </Link>
        }
      />

      <div className="bg-white rounded-lg border border-slate-200 shadow-sm">
        <div className="p-4 border-b border-slate-200 flex flex-col sm:flex-row gap-3">
          <SearchInput
            value={search}
            onChange={setSearch}
            placeholder="Search by name or ID..."
            className="flex-1"
          />
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as StatusFilter)}
            className="px-3 py-2 text-sm border border-slate-300 rounded-lg bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          >
            <option value="all">All Statuses</option>
            <option value="A">Active</option>
            <option value="I">Inactive</option>
            <option value="C">Closed</option>
          </select>
        </div>

        <DataTable<PortfolioRecord>
          columns={columns}
          data={filteredData as PortfolioRecord[]}
          keyExtractor={(row) => row.id}
          emptyMessage="No portfolios match your search criteria"
        />
      </div>
    </div>
  );
}

// Portfolio Management (replaces PORTMSTR CRUD operations)
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { api } from '../lib/api';
import DataTable from '../components/DataTable';

interface Portfolio {
  id: string;
  portfolioId: string;
  accountNo: string;
  clientName: string;
  clientType: string;
  status: string;
  totalValue: number;
  cashBalance: number;
  updatedAt: string;
}

interface PaginatedResult {
  data: Portfolio[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

interface PortfolioForm {
  portfolioId: string;
  accountNo: string;
  clientName: string;
  clientType: string;
  status: string;
  currencyCode: string;
  riskLevel: string;
}

export default function PortfolioManagement() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [deleteConfirm, setDeleteConfirm] = useState<string | null>(null);
  const [error, setError] = useState('');

  const { register, handleSubmit, reset, setValue } = useForm<PortfolioForm>();

  const { data, isLoading } = useQuery({
    queryKey: ['portfolios', page, search],
    queryFn: () => {
      const params: Record<string, string> = { page: String(page), pageSize: '20' };
      if (search) params.search = search;
      return api.getPortfolios(params) as Promise<{ data: PaginatedResult }>;
    },
  });

  const createMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => api.createPortfolio(body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['portfolios'] });
      setShowForm(false);
      reset();
      setError('');
    },
    onError: (err: Error) => setError(err.message),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, body }: { id: string; body: Record<string, unknown> }) => api.updatePortfolio(id, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['portfolios'] });
      setShowForm(false);
      setEditingId(null);
      reset();
      setError('');
    },
    onError: (err: Error) => setError(err.message),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => api.deletePortfolio(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['portfolios'] });
      setDeleteConfirm(null);
    },
    onError: (err: Error) => setError(err.message),
  });

  const onSubmit = (formData: PortfolioForm) => {
    const body = formData as unknown as Record<string, unknown>;
    if (editingId) {
      updateMutation.mutate({ id: editingId, body });
    } else {
      createMutation.mutate(body);
    }
  };

  const startEdit = (portfolio: Portfolio) => {
    setEditingId(portfolio.id);
    setValue('portfolioId', portfolio.portfolioId);
    setValue('clientName', portfolio.clientName);
    setValue('clientType', portfolio.clientType);
    setValue('status', portfolio.status);
    setShowForm(true);
  };

  const statusColor = (s: string) => {
    if (s === 'ACTIVE') return 'bg-green-100 text-green-800';
    if (s === 'CLOSED') return 'bg-red-100 text-red-800';
    if (s === 'SUSPENDED') return 'bg-yellow-100 text-yellow-800';
    return 'bg-gray-100 text-gray-800';
  };

  const columns = [
    { key: 'portfolioId', header: 'Portfolio ID' },
    { key: 'clientName', header: 'Client Name' },
    { key: 'clientType', header: 'Type' },
    {
      key: 'status',
      header: 'Status',
      render: (row: Record<string, unknown>) => (
        <span className={`px-2 py-1 rounded-full text-xs font-medium ${statusColor(String(row.status))}`}>
          {String(row.status)}
        </span>
      ),
    },
    {
      key: 'totalValue',
      header: 'Total Value',
      render: (row: Record<string, unknown>) => `$${Number(row.totalValue).toLocaleString()}`,
    },
    {
      key: 'updatedAt',
      header: 'Last Updated',
      render: (row: Record<string, unknown>) => new Date(String(row.updatedAt)).toLocaleDateString(),
    },
    {
      key: 'actions',
      header: 'Actions',
      render: (row: Record<string, unknown>) => (
        <div className="flex space-x-2">
          <button
            onClick={() => startEdit(row as unknown as Portfolio)}
            className="text-blue-600 hover:text-blue-800 text-sm font-medium"
          >
            Edit
          </button>
          <button
            onClick={() => setDeleteConfirm(String(row.id))}
            className="text-red-600 hover:text-red-800 text-sm font-medium"
          >
            Delete
          </button>
        </div>
      ),
    },
  ];

  const result = data?.data;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Portfolio Management</h1>
          <p className="text-gray-500 mt-1">Create, update, and manage portfolios</p>
        </div>
        <button
          onClick={() => { setShowForm(true); setEditingId(null); reset(); setError(''); }}
          className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
        >
          + New Portfolio
        </button>
      </div>

      {/* Search */}
      <div className="bg-white rounded-lg shadow-sm border p-4">
        <div className="flex gap-4">
          <input
            type="text"
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(1); }}
            placeholder="Search by portfolio ID, name, or account..."
            className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
          />
        </div>
      </div>

      {/* Create/Edit form */}
      {showForm && (
        <div className="bg-white rounded-lg shadow-sm border p-5">
          <h2 className="text-lg font-semibold mb-4">{editingId ? 'Edit Portfolio' : 'Create New Portfolio'}</h2>
          {error && <div className="bg-red-50 text-red-700 px-4 py-2 rounded mb-4 text-sm border border-red-200">{error}</div>}
          <form onSubmit={handleSubmit(onSubmit)} className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Portfolio ID *</label>
              <input
                {...register('portfolioId', { required: true })}
                disabled={!!editingId}
                placeholder="PORT12345"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none disabled:bg-gray-100"
              />
              <p className="text-xs text-gray-400 mt-1">Format: PORT + 5 digits</p>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Client Name *</label>
              <input
                {...register('clientName', { required: true })}
                placeholder="Client name"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Client Type</label>
              <select
                {...register('clientType')}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
              >
                <option value="INDIVIDUAL">Individual</option>
                <option value="CORPORATE">Corporate</option>
                <option value="TRUST">Trust</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Status</label>
              <select
                {...register('status')}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
              >
                <option value="ACTIVE">Active</option>
                <option value="SUSPENDED">Suspended</option>
                <option value="CLOSED">Closed</option>
              </select>
            </div>
            <div className="md:col-span-2 flex justify-end space-x-3">
              <button type="button" onClick={() => { setShowForm(false); setEditingId(null); setError(''); }} className="px-4 py-2 border rounded-lg hover:bg-gray-50">
                Cancel
              </button>
              <button type="submit" className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 transition-colors">
                {editingId ? 'Update' : 'Create'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Delete confirmation */}
      {deleteConfirm && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-center justify-between">
          <p className="text-red-700">Are you sure you want to delete this portfolio?</p>
          <div className="flex space-x-3">
            <button onClick={() => setDeleteConfirm(null)} className="px-4 py-1 border rounded hover:bg-white">Cancel</button>
            <button onClick={() => deleteMutation.mutate(deleteConfirm)} className="bg-red-600 text-white px-4 py-1 rounded hover:bg-red-700">
              Delete
            </button>
          </div>
        </div>
      )}

      {/* Portfolio list */}
      <div className="bg-white rounded-lg shadow-sm border">
        <DataTable
          columns={columns}
          data={(result?.data || []) as unknown as Record<string, unknown>[]}
          page={result?.page}
          totalPages={result?.totalPages}
          onPageChange={setPage}
          loading={isLoading}
        />
      </div>
    </div>
  );
}

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Search, Plus, Filter } from 'lucide-react';
import { listPortfolios } from '../lib/api';
import StatusBadge from '../components/StatusBadge';

export default function PortfolioList() {
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['portfolios', page, search, statusFilter],
    queryFn: () =>
      listPortfolios({
        page,
        pageSize: 10,
        ...(search ? { search } : {}),
        ...(statusFilter ? { status: statusFilter } : {}),
      }),
  });

  const portfolios = data?.data ?? [];
  const pagination = data?.pagination;

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">Portfolios</h1>
        <Link
          to="/portfolios/new"
          className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
        >
          <Plus size={16} /> New Portfolio
        </Link>
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-3 mb-4">
        <div className="relative flex-1">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <input
            type="text"
            placeholder="Search portfolios..."
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(1); }}
            className="w-full pl-10 pr-4 py-2 border rounded-lg focus:ring-2 focus:ring-indigo-500"
          />
        </div>
        <div className="relative">
          <Filter size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <select
            value={statusFilter}
            onChange={(e) => { setStatusFilter(e.target.value); setPage(1); }}
            className="pl-10 pr-8 py-2 border rounded-lg focus:ring-2 focus:ring-indigo-500 appearance-none bg-white"
          >
            <option value="">All Status</option>
            <option value="A">Active</option>
            <option value="C">Closed</option>
            <option value="S">Suspended</option>
          </select>
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl shadow-sm border overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">ID</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Name</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Client</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Status</th>
                <th className="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase">Total Value</th>
                <th className="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase">Cash</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Risk</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {isLoading ? (
                <tr><td colSpan={7} className="px-4 py-8 text-center text-gray-500">Loading...</td></tr>
              ) : portfolios.length === 0 ? (
                <tr><td colSpan={7} className="px-4 py-8 text-center text-gray-500">No portfolios found</td></tr>
              ) : portfolios.map((p) => (
                <tr key={p.portfolioId} className="hover:bg-gray-50 transition-colors">
                  <td className="px-4 py-3">
                    <Link to={`/portfolios/${p.portfolioId}`} className="text-indigo-600 font-medium hover:underline">
                      {p.portfolioId}
                    </Link>
                  </td>
                  <td className="px-4 py-3 font-medium">{p.portfolioName}</td>
                  <td className="px-4 py-3 text-gray-500">{p.clientId}</td>
                  <td className="px-4 py-3"><StatusBadge status={p.status} /></td>
                  <td className="px-4 py-3 text-right font-medium">
                    {Number(p.totalValue).toLocaleString('en-US', { style: 'currency', currency: p.currencyCode })}
                  </td>
                  <td className="px-4 py-3 text-right text-gray-500">
                    {Number(p.cashBalance).toLocaleString('en-US', { style: 'currency', currency: p.currencyCode })}
                  </td>
                  <td className="px-4 py-3">
                    <span className="inline-block w-6 h-6 rounded-full bg-gradient-to-r from-green-400 to-red-400 text-center text-white text-xs leading-6 font-bold">
                      {p.riskLevel}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Pagination — maps PF7/PF8 from CICS */}
        {pagination && pagination.totalPages > 1 && (
          <div className="flex items-center justify-between px-4 py-3 border-t bg-gray-50">
            <span className="text-sm text-gray-500">
              Page {pagination.page} of {pagination.totalPages} ({pagination.totalCount} total)
            </span>
            <div className="flex gap-2">
              <button
                disabled={page <= 1}
                onClick={() => setPage(page - 1)}
                className="px-3 py-1 text-sm border rounded hover:bg-white disabled:opacity-50"
              >
                Previous (PF7)
              </button>
              <button
                disabled={page >= pagination.totalPages}
                onClick={() => setPage(page + 1)}
                className="px-3 py-1 text-sm border rounded hover:bg-white disabled:opacity-50"
              >
                Next (PF8)
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

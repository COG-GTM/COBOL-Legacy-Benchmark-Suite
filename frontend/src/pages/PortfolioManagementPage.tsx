import { useState } from 'react';
import { Link } from 'react-router-dom';
import { mockPortfolios } from '../mocks/mockData';
import { PORTFOLIO_STATUS_LABELS, CLIENT_TYPE_LABELS, type Portfolio } from '../types';
import { formatCurrency } from '../utils/validation';
import { DeleteConfirmation } from '../components/DeleteConfirmation';
import { useToast } from '../hooks/useToast';

export function PortfolioManagementPage() {
  const [portfolios, setPortfolios] = useState<Portfolio[]>(mockPortfolios);
  const [search, setSearch] = useState('');
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);
  const { addToast } = useToast();

  const filtered = portfolios.filter(
    p =>
      p.portfolioId.toLowerCase().includes(search.toLowerCase()) ||
      p.clientName.toLowerCase().includes(search.toLowerCase()) ||
      p.accountNumber.includes(search)
  );

  const handleDelete = (reasonCode: string) => {
    setPortfolios(prev => prev.filter(p => p.portfolioId !== deleteTarget));
    addToast(`Portfolio ${deleteTarget} deleted (reason: ${reasonCode})`, 'success');
    setDeleteTarget(null);
  };

  return (
    <div className="p-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Portfolio Management</h1>
        <Link
          to="/portfolios/new"
          className="bg-blue-600 text-white px-4 py-2 rounded-md text-sm hover:bg-blue-700 transition-colors"
        >
          Create Portfolio
        </Link>
      </div>

      <div className="mb-4">
        <input
          type="text"
          value={search}
          onChange={e => setSearch(e.target.value)}
          placeholder="Search by Portfolio ID, Client Name, or Account..."
          className="border border-gray-300 rounded-md px-3 py-2 text-sm w-80 focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b-2 border-gray-300">
              <th className="text-left py-2 px-3 font-semibold">Portfolio ID</th>
              <th className="text-left py-2 px-3 font-semibold">Account</th>
              <th className="text-left py-2 px-3 font-semibold">Client Name</th>
              <th className="text-left py-2 px-3 font-semibold">Type</th>
              <th className="text-left py-2 px-3 font-semibold">Status</th>
              <th className="text-right py-2 px-3 font-semibold">Total Value</th>
              <th className="text-right py-2 px-3 font-semibold">Actions</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map(p => (
              <tr key={p.portfolioId} className="border-b border-gray-100 hover:bg-gray-50">
                <td className="py-2 px-3 font-mono">{p.portfolioId}</td>
                <td className="py-2 px-3 font-mono">{p.accountNumber}</td>
                <td className="py-2 px-3">{p.clientName}</td>
                <td className="py-2 px-3">{CLIENT_TYPE_LABELS[p.clientType]}</td>
                <td className="py-2 px-3">
                  <span className={`inline-block px-2 py-0.5 rounded text-xs font-medium ${
                    p.status === 'A' ? 'bg-green-100 text-green-800' :
                    p.status === 'C' ? 'bg-gray-100 text-gray-800' :
                    'bg-yellow-100 text-yellow-800'
                  }`}>
                    {PORTFOLIO_STATUS_LABELS[p.status]}
                  </span>
                </td>
                <td className="py-2 px-3 text-right font-mono">{formatCurrency(p.totalValue)}</td>
                <td className="py-2 px-3 text-right">
                  <div className="flex gap-2 justify-end">
                    <Link to={`/portfolios/${p.portfolioId}`} className="text-blue-600 hover:text-blue-800 text-xs">View</Link>
                    <Link to={`/portfolios/${p.portfolioId}/edit`} className="text-green-600 hover:text-green-800 text-xs">Edit</Link>
                    <button
                      onClick={() => setDeleteTarget(p.portfolioId)}
                      className="text-red-600 hover:text-red-800 text-xs"
                    >
                      Delete
                    </button>
                  </div>
                </td>
              </tr>
            ))}
            {filtered.length === 0 && (
              <tr><td colSpan={7} className="text-center py-8 text-gray-400">No portfolios found</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {deleteTarget && (
        <DeleteConfirmation
          portfolioId={deleteTarget}
          onConfirm={handleDelete}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  );
}

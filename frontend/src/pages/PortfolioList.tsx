import { useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { Plus, Eye, Pencil, Trash2, Search } from 'lucide-react';
import { usePortfolio } from '../context/PortfolioContext';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import ConfirmDialog from '../components/ConfirmDialog';
import Pagination from '../components/Pagination';

const PAGE_SIZE = 10;

const statusLabels: Record<string, string> = { A: 'Active', I: 'Inactive', C: 'Closed' };
const statusColors: Record<string, string> = {
  A: 'bg-green-100 text-green-700',
  I: 'bg-yellow-100 text-yellow-700',
  C: 'bg-red-100 text-red-700',
};

export default function PortfolioList() {
  const { portfolios, deletePortfolio } = usePortfolio();
  const { user } = useAuth();
  const { addToast } = useToast();
  const [search, setSearch] = useState('');
  const [sortField, setSortField] = useState<'id' | 'clientName' | 'totalValue' | 'status'>('id');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('asc');
  const [page, setPage] = useState(1);
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);

  const filtered = useMemo(() => {
    let list = portfolios;
    if (search) {
      const q = search.toLowerCase();
      list = list.filter(
        (p) =>
          p.id.toLowerCase().includes(q) ||
          p.accountNo.includes(q) ||
          p.clientName.toLowerCase().includes(q),
      );
    }
    list = [...list].sort((a, b) => {
      const va = a[sortField];
      const vb = b[sortField];
      const cmp = typeof va === 'number' ? va - (vb as number) : String(va).localeCompare(String(vb));
      return sortDir === 'asc' ? cmp : -cmp;
    });
    return list;
  }, [portfolios, search, sortField, sortDir]);

  const totalPages = Math.ceil(filtered.length / PAGE_SIZE);
  const paged = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  const toggleSort = (field: typeof sortField) => {
    if (sortField === field) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortField(field);
      setSortDir('asc');
    }
    setPage(1);
  };

  const handleDelete = () => {
    if (deleteTarget) {
      deletePortfolio(deleteTarget);
      addToast(`Portfolio ${deleteTarget} deleted successfully.`, 'success');
      setDeleteTarget(null);
    }
  };

  const isReadWrite = user?.role === 'read-write';

  return (
    <div>
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <h1 className="text-2xl font-bold text-gray-800">Portfolios</h1>
        <div className="flex items-center gap-3">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="text"
              placeholder="Search by ID, account, or name..."
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(1);
              }}
              className="pl-9 pr-4 py-2 border border-gray-300 rounded-md text-sm w-64 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          {isReadWrite && (
            <Link
              to="/portfolios/new"
              className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm rounded-md hover:bg-blue-700"
            >
              <Plus className="w-4 h-4" /> New Portfolio
            </Link>
          )}
        </div>
      </div>

      <div className="bg-white rounded-lg shadow-sm border overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="bg-gray-50">
            <tr>
              {[
                { field: 'id' as const, label: 'Portfolio ID' },
                { field: 'clientName' as const, label: 'Client Name' },
              ].map(({ field, label }) => (
                <th
                  key={field}
                  onClick={() => toggleSort(field)}
                  className="text-left px-4 py-3 text-gray-600 font-medium cursor-pointer hover:text-gray-800"
                >
                  {label} {sortField === field ? (sortDir === 'asc' ? '↑' : '↓') : ''}
                </th>
              ))}
              <th className="text-left px-4 py-3 text-gray-600 font-medium">Account No</th>
              <th
                onClick={() => toggleSort('status')}
                className="text-left px-4 py-3 text-gray-600 font-medium cursor-pointer hover:text-gray-800"
              >
                Status {sortField === 'status' ? (sortDir === 'asc' ? '↑' : '↓') : ''}
              </th>
              <th
                onClick={() => toggleSort('totalValue')}
                className="text-right px-4 py-3 text-gray-600 font-medium cursor-pointer hover:text-gray-800"
              >
                Total Value {sortField === 'totalValue' ? (sortDir === 'asc' ? '↑' : '↓') : ''}
              </th>
              <th className="text-right px-4 py-3 text-gray-600 font-medium">Actions</th>
            </tr>
          </thead>
          <tbody>
            {paged.map((p) => (
              <tr key={p.id} className="border-t hover:bg-gray-50">
                <td className="px-4 py-3 font-mono text-gray-700">{p.id}</td>
                <td className="px-4 py-3 text-gray-700">{p.clientName}</td>
                <td className="px-4 py-3 font-mono text-gray-500">{p.accountNo}</td>
                <td className="px-4 py-3">
                  <span
                    className={`px-2 py-0.5 rounded-full text-xs font-medium ${statusColors[p.status]}`}
                  >
                    {statusLabels[p.status]}
                  </span>
                </td>
                <td className="px-4 py-3 text-right text-gray-700">
                  ${p.totalValue.toLocaleString('en-US', { minimumFractionDigits: 2 })}
                </td>
                <td className="px-4 py-3 text-right">
                  <div className="flex items-center justify-end gap-2">
                    <Link
                      to={`/portfolios/${p.id}`}
                      className="p-1.5 text-gray-500 hover:text-blue-600 hover:bg-blue-50 rounded"
                      title="View"
                    >
                      <Eye className="w-4 h-4" />
                    </Link>
                    {isReadWrite && (
                      <>
                        <Link
                          to={`/portfolios/${p.id}/edit`}
                          className="p-1.5 text-gray-500 hover:text-yellow-600 hover:bg-yellow-50 rounded"
                          title="Edit"
                        >
                          <Pencil className="w-4 h-4" />
                        </Link>
                        <button
                          onClick={() => setDeleteTarget(p.id)}
                          className="p-1.5 text-gray-500 hover:text-red-600 hover:bg-red-50 rounded"
                          title="Delete"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </>
                    )}
                  </div>
                </td>
              </tr>
            ))}
            {paged.length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-gray-500">
                  No portfolios found.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} />

      {deleteTarget && (
        <ConfirmDialog
          title="Delete Portfolio"
          message={`Are you sure you want to delete portfolio ${deleteTarget}? This action cannot be undone.`}
          confirmLabel="Delete"
          variant="danger"
          onConfirm={handleDelete}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  );
}

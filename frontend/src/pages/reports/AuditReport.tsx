import { useState, useMemo } from 'react';
import { ArrowLeft } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import reportData from '../../mocks/reports/auditReport.json';
import type { AuditEntry } from '../../types';
import Pagination from '../../components/Pagination';

const PAGE_SIZE = 10;

const statusColors: Record<string, string> = {
  SUCC: 'bg-green-100 text-green-700',
  FAIL: 'bg-red-100 text-red-700',
  WARN: 'bg-yellow-100 text-yellow-700',
};

const typeColors: Record<string, string> = {
  TRAN: 'bg-blue-100 text-blue-700',
  USER: 'bg-purple-100 text-purple-700',
  SYST: 'bg-gray-100 text-gray-700',
};

export default function AuditReport() {
  const navigate = useNavigate();
  const data = reportData as AuditEntry[];

  const [typeFilter, setTypeFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(1);

  const filtered = useMemo(() => {
    let list = data;
    if (typeFilter) list = list.filter((r) => r.type === typeFilter);
    if (statusFilter) list = list.filter((r) => r.status === statusFilter);
    return list;
  }, [data, typeFilter, statusFilter]);

  const totalPages = Math.ceil(filtered.length / PAGE_SIZE);
  const paged = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  return (
    <div>
      <div className="flex items-center gap-3 mb-6">
        <button onClick={() => navigate('/reports')} className="p-2 hover:bg-gray-100 rounded-md">
          <ArrowLeft className="w-5 h-5 text-gray-600" />
        </button>
        <h1 className="text-2xl font-bold text-gray-800">Audit Report</h1>
      </div>

      <div className="bg-white rounded-lg shadow-sm border p-4 mb-6 flex flex-wrap gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Type</label>
          <select
            value={typeFilter}
            onChange={(e) => { setTypeFilter(e.target.value); setPage(1); }}
            className="px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">All Types</option>
            <option value="TRAN">Transaction</option>
            <option value="USER">User Action</option>
            <option value="SYST">System Event</option>
          </select>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Status</label>
          <select
            value={statusFilter}
            onChange={(e) => { setStatusFilter(e.target.value); setPage(1); }}
            className="px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">All Statuses</option>
            <option value="SUCC">Success</option>
            <option value="FAIL">Failure</option>
            <option value="WARN">Warning</option>
          </select>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow-sm border overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="bg-gray-50">
            <tr>
              <th className="text-left px-4 py-2 text-gray-600">Timestamp</th>
              <th className="text-left px-4 py-2 text-gray-600">User</th>
              <th className="text-left px-4 py-2 text-gray-600">Program</th>
              <th className="text-left px-4 py-2 text-gray-600">Type</th>
              <th className="text-left px-4 py-2 text-gray-600">Action</th>
              <th className="text-left px-4 py-2 text-gray-600">Status</th>
              <th className="text-left px-4 py-2 text-gray-600">Message</th>
            </tr>
          </thead>
          <tbody>
            {paged.map((r, i) => (
              <tr key={i} className="border-t">
                <td className="px-4 py-2 text-gray-700 whitespace-nowrap">
                  {new Date(r.timestamp).toLocaleString()}
                </td>
                <td className="px-4 py-2 font-mono text-gray-700">{r.userId}</td>
                <td className="px-4 py-2 font-mono text-gray-700">{r.program}</td>
                <td className="px-4 py-2">
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${typeColors[r.type]}`}>
                    {r.type}
                  </span>
                </td>
                <td className="px-4 py-2 text-gray-700">{r.action}</td>
                <td className="px-4 py-2">
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${statusColors[r.status]}`}>
                    {r.status}
                  </span>
                </td>
                <td className="px-4 py-2 text-gray-600 max-w-xs truncate">{r.message}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} />
    </div>
  );
}

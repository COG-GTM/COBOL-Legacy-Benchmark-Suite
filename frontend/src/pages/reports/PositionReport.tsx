import { useState, useMemo } from 'react';
import { ArrowLeft } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import reportData from '../../mocks/reports/positionReport.json';
import type { PositionReportEntry } from '../../types';
import Pagination from '../../components/Pagination';

const PAGE_SIZE = 10;

export default function PositionReport() {
  const navigate = useNavigate();
  const data = reportData as PositionReportEntry[];

  const [dateFilter, setDateFilter] = useState('');
  const [portfolioFilter, setPortfolioFilter] = useState('');
  const [page, setPage] = useState(1);

  const filtered = useMemo(() => {
    let list = data;
    if (dateFilter) {
      list = list.filter((r) => r.date === dateFilter);
    }
    if (portfolioFilter) {
      list = list.filter((r) =>
        r.portfolioId.toLowerCase().includes(portfolioFilter.toLowerCase()),
      );
    }
    return list;
  }, [data, dateFilter, portfolioFilter]);

  const totalPages = Math.ceil(filtered.length / PAGE_SIZE);
  const paged = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  const totalCost = filtered.reduce((s, r) => s + r.costBasis, 0);
  const totalMarket = filtered.reduce((s, r) => s + r.marketValue, 0);
  const totalGL = filtered.reduce((s, r) => s + r.gainLoss, 0);

  return (
    <div>
      <div className="flex items-center gap-3 mb-6">
        <button onClick={() => navigate('/reports')} className="p-2 hover:bg-gray-100 rounded-md">
          <ArrowLeft className="w-5 h-5 text-gray-600" />
        </button>
        <h1 className="text-2xl font-bold text-gray-800">Position Report</h1>
      </div>

      <div className="bg-white rounded-lg shadow-sm border p-4 mb-6 flex flex-wrap gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Date</label>
          <input
            type="date"
            value={dateFilter}
            onChange={(e) => { setDateFilter(e.target.value); setPage(1); }}
            className="px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Portfolio ID</label>
          <input
            type="text"
            value={portfolioFilter}
            onChange={(e) => { setPortfolioFilter(e.target.value); setPage(1); }}
            placeholder="Filter by ID"
            className="px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
      </div>

      <div className="bg-white rounded-lg shadow-sm border overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="bg-gray-50">
            <tr>
              <th className="text-left px-4 py-2 text-gray-600">Date</th>
              <th className="text-left px-4 py-2 text-gray-600">Portfolio</th>
              <th className="text-left px-4 py-2 text-gray-600">Fund</th>
              <th className="text-right px-4 py-2 text-gray-600">Quantity</th>
              <th className="text-right px-4 py-2 text-gray-600">Cost Basis</th>
              <th className="text-right px-4 py-2 text-gray-600">Market Value</th>
              <th className="text-right px-4 py-2 text-gray-600">Gain/Loss</th>
            </tr>
          </thead>
          <tbody>
            {paged.map((r, i) => (
              <tr key={i} className="border-t">
                <td className="px-4 py-2 text-gray-700">{r.date}</td>
                <td className="px-4 py-2 font-mono text-gray-700">{r.portfolioId}</td>
                <td className="px-4 py-2 text-gray-700">{r.fundName}</td>
                <td className="px-4 py-2 text-right text-gray-700">{r.quantity.toLocaleString()}</td>
                <td className="px-4 py-2 text-right text-gray-700">
                  ${r.costBasis.toLocaleString('en-US', { minimumFractionDigits: 2 })}
                </td>
                <td className="px-4 py-2 text-right text-gray-700">
                  ${r.marketValue.toLocaleString('en-US', { minimumFractionDigits: 2 })}
                </td>
                <td className={`px-4 py-2 text-right font-medium ${r.gainLoss >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                  ${r.gainLoss.toLocaleString('en-US', { minimumFractionDigits: 2 })}
                </td>
              </tr>
            ))}
          </tbody>
          <tfoot className="bg-gray-50 font-medium">
            <tr className="border-t-2">
              <td colSpan={4} className="px-4 py-2 text-gray-700">Totals</td>
              <td className="px-4 py-2 text-right text-gray-700">
                ${totalCost.toLocaleString('en-US', { minimumFractionDigits: 2 })}
              </td>
              <td className="px-4 py-2 text-right text-gray-700">
                ${totalMarket.toLocaleString('en-US', { minimumFractionDigits: 2 })}
              </td>
              <td className={`px-4 py-2 text-right ${totalGL >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                ${totalGL.toLocaleString('en-US', { minimumFractionDigits: 2 })}
              </td>
            </tr>
          </tfoot>
        </table>
      </div>

      <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} />
    </div>
  );
}

import { useState, useMemo } from 'react';
import { Search } from 'lucide-react';
import { usePortfolio } from '../context/PortfolioContext';
import Pagination from '../components/Pagination';

const PAGE_SIZE = 5;

export default function PositionInquiry() {
  const { positions, portfolios } = usePortfolio();
  const [accountNo, setAccountNo] = useState('');
  const [searched, setSearched] = useState(false);

  const portfolio = useMemo(
    () => portfolios.find((p) => p.accountNo === accountNo),
    [portfolios, accountNo],
  );

  const results = useMemo(() => {
    if (!portfolio) return [];
    return positions.filter((p) => p.portfolioId === portfolio.id);
  }, [positions, portfolio]);

  const [page, setPage] = useState(1);
  const totalPages = Math.ceil(results.length / PAGE_SIZE);
  const paged = results.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setSearched(true);
    setPage(1);
  };

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Portfolio Position Inquiry</h1>

      <div className="bg-white rounded-lg shadow-sm border p-6 mb-6">
        <form onSubmit={handleSearch} className="flex items-end gap-4">
          <div className="flex-1 max-w-xs">
            <label htmlFor="account" className="block text-sm font-medium text-gray-700 mb-1">
              Account Number
            </label>
            <input
              id="account"
              type="text"
              value={accountNo}
              onChange={(e) => {
                setAccountNo(e.target.value);
                setSearched(false);
              }}
              placeholder="Enter 10-digit account number"
              maxLength={10}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <button
            type="submit"
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm rounded-md hover:bg-blue-700"
          >
            <Search className="w-4 h-4" /> Lookup
          </button>
        </form>
      </div>

      {searched && !portfolio && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-6 text-center">
          <p className="text-yellow-800 font-medium">Position not found for account</p>
          <p className="text-yellow-600 text-sm mt-1">
            No portfolio exists with account number "{accountNo}"
          </p>
        </div>
      )}

      {searched && portfolio && results.length === 0 && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-6 text-center">
          <p className="text-yellow-800 font-medium">No positions found</p>
          <p className="text-yellow-600 text-sm mt-1">
            Portfolio {portfolio.id} ({portfolio.clientName}) has no open positions.
          </p>
        </div>
      )}

      {searched && results.length > 0 && (
        <div className="bg-white rounded-lg shadow-sm border">
          <div className="p-4 border-b">
            <p className="text-sm text-gray-600">
              Portfolio <span className="font-mono font-medium">{portfolio!.id}</span> —{' '}
              {portfolio!.clientName}
            </p>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50">
                <tr>
                  <th className="text-left px-4 py-2 text-gray-600">Fund ID</th>
                  <th className="text-left px-4 py-2 text-gray-600">Fund Name</th>
                  <th className="text-right px-4 py-2 text-gray-600">Units</th>
                  <th className="text-right px-4 py-2 text-gray-600">Cost Basis</th>
                  <th className="text-right px-4 py-2 text-gray-600">Market Value</th>
                </tr>
              </thead>
              <tbody>
                {paged.map((p, i) => (
                  <tr key={i} className="border-t">
                    <td className="px-4 py-2 font-mono text-gray-700">{p.investmentId}</td>
                    <td className="px-4 py-2 text-gray-700">{p.fundName}</td>
                    <td className="px-4 py-2 text-right text-gray-700">
                      {p.quantity.toLocaleString('en-US', { minimumFractionDigits: 4 })}
                    </td>
                    <td className="px-4 py-2 text-right text-gray-700">
                      ${p.costBasis.toLocaleString('en-US', { minimumFractionDigits: 2 })}
                    </td>
                    <td className="px-4 py-2 text-right text-gray-700">
                      ${p.marketValue.toLocaleString('en-US', { minimumFractionDigits: 2 })}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="px-4 pb-4">
            <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} />
          </div>
        </div>
      )}
    </div>
  );
}

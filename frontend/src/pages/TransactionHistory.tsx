import { useState, useMemo } from 'react';
import { Search } from 'lucide-react';
import { usePortfolio } from '../context/PortfolioContext';
import Pagination from '../components/Pagination';

const PAGE_SIZE = 10;

const typeLabels: Record<string, string> = {
  BU: 'Buy',
  SL: 'Sell',
  TR: 'Transfer',
  FE: 'Fee',
};
const typeColors: Record<string, string> = {
  BU: 'bg-green-100 text-green-700',
  SL: 'bg-red-100 text-red-700',
  TR: 'bg-blue-100 text-blue-700',
  FE: 'bg-gray-100 text-gray-700',
};

function formatCobolDate(d: string) {
  if (d.length !== 8) return d;
  return `${d.slice(0, 4)}-${d.slice(4, 6)}-${d.slice(6, 8)}`;
}

export default function TransactionHistory() {
  const { transactions, portfolios } = usePortfolio();
  const [accountNo, setAccountNo] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [searched, setSearched] = useState(false);
  const [page, setPage] = useState(1);

  const portfolio = useMemo(
    () => portfolios.find((p) => p.accountNo === accountNo),
    [portfolios, accountNo],
  );

  const results = useMemo(() => {
    if (!portfolio) return [];
    let list = transactions.filter((t) => t.portfolioId === portfolio.id);

    if (startDate) {
      const sd = startDate.replace(/-/g, '');
      list = list.filter((t) => t.date >= sd);
    }
    if (endDate) {
      const ed = endDate.replace(/-/g, '');
      list = list.filter((t) => t.date <= ed);
    }

    return list.sort((a, b) => b.date.localeCompare(a.date));
  }, [transactions, portfolio, startDate, endDate]);

  const totalPages = Math.ceil(results.length / PAGE_SIZE);
  const paged = results.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setSearched(true);
    setPage(1);
  };

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Transaction History Inquiry</h1>

      <div className="bg-white rounded-lg shadow-sm border p-6 mb-6">
        <form onSubmit={handleSearch} className="flex flex-wrap items-end gap-4">
          <div className="w-48">
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
              placeholder="10-digit account"
              maxLength={10}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div>
            <label htmlFor="startDate" className="block text-sm font-medium text-gray-700 mb-1">
              Start Date
            </label>
            <input
              id="startDate"
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div>
            <label htmlFor="endDate" className="block text-sm font-medium text-gray-700 mb-1">
              End Date
            </label>
            <input
              id="endDate"
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <button
            type="submit"
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm rounded-md hover:bg-blue-700"
          >
            <Search className="w-4 h-4" /> Search
          </button>
        </form>
      </div>

      {searched && !portfolio && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-6 text-center">
          <p className="text-yellow-800 font-medium">Account not found</p>
          <p className="text-yellow-600 text-sm mt-1">
            No portfolio exists with account number "{accountNo}"
          </p>
        </div>
      )}

      {searched && portfolio && results.length === 0 && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-6 text-center">
          <p className="text-yellow-800 font-medium">No transactions found</p>
          <p className="text-yellow-600 text-sm mt-1">
            No transaction history for the specified criteria.
          </p>
        </div>
      )}

      {searched && results.length > 0 && (
        <div className="bg-white rounded-lg shadow-sm border">
          <div className="p-4 border-b">
            <p className="text-sm text-gray-600">
              Showing {results.length} transaction(s) for{' '}
              <span className="font-mono font-medium">{portfolio!.id}</span> — {portfolio!.clientName}
            </p>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50">
                <tr>
                  <th className="text-left px-4 py-2 text-gray-600">Date</th>
                  <th className="text-left px-4 py-2 text-gray-600">Type</th>
                  <th className="text-right px-4 py-2 text-gray-600">Units</th>
                  <th className="text-right px-4 py-2 text-gray-600">Price</th>
                  <th className="text-right px-4 py-2 text-gray-600">Amount</th>
                </tr>
              </thead>
              <tbody>
                {paged.map((t, i) => (
                  <tr key={i} className="border-t">
                    <td className="px-4 py-2 text-gray-700">{formatCobolDate(t.date)}</td>
                    <td className="px-4 py-2">
                      <span
                        className={`px-2 py-0.5 rounded-full text-xs font-medium ${typeColors[t.type]}`}
                      >
                        {typeLabels[t.type]}
                      </span>
                    </td>
                    <td className="px-4 py-2 text-right text-gray-700">
                      {t.quantity.toLocaleString('en-US', { minimumFractionDigits: 4 })}
                    </td>
                    <td className="px-4 py-2 text-right text-gray-700">
                      ${t.price.toLocaleString('en-US', { minimumFractionDigits: 2 })}
                    </td>
                    <td className="px-4 py-2 text-right text-gray-700">
                      ${t.amount.toLocaleString('en-US', { minimumFractionDigits: 2 })}
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

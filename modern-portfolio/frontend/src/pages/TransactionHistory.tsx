// Transaction History (replaces HISMAP from INQSET.bms lines 53-85)
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';
import DataTable from '../components/DataTable';

interface Transaction {
  transactionId: string;
  transactionDate: string;
  type: string;
  quantity: number;
  price: number;
  amount: number;
  status: string;
  investmentId: string;
  portfolio?: { portfolioId: string; clientName: string };
}

interface PaginatedResult {
  data: Transaction[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export default function TransactionHistory() {
  const [searchId, setSearchId] = useState('');
  const [activeSearch, setActiveSearch] = useState('');
  const [page, setPage] = useState(1);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['transactions', activeSearch, page, startDate, endDate],
    queryFn: () => {
      const params: Record<string, string> = { page: String(page), pageSize: '10' };
      if (startDate) params.startDate = startDate;
      if (endDate) params.endDate = endDate;

      if (activeSearch) {
        return api.getPortfolioTransactions(activeSearch, params) as Promise<{ data: PaginatedResult }>;
      }
      return api.getTransactions(params) as Promise<{ data: PaginatedResult }>;
    },
  });

  const result = data?.data;

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setActiveSearch(searchId.trim());
    setPage(1);
  };

  const typeLabel = (t: string) => {
    const labels: Record<string, string> = { BUY: 'Buy', SELL: 'Sell', TRANSFER: 'Transfer', FEE: 'Fee' };
    return labels[t] || t;
  };

  const typeColor = (t: string) => {
    const colors: Record<string, string> = {
      BUY: 'bg-green-100 text-green-800',
      SELL: 'bg-red-100 text-red-800',
      TRANSFER: 'bg-blue-100 text-blue-800',
      FEE: 'bg-gray-100 text-gray-800',
    };
    return colors[t] || 'bg-gray-100 text-gray-800';
  };

  const statusColor = (s: string) => {
    const colors: Record<string, string> = {
      PENDING: 'bg-yellow-100 text-yellow-800',
      DONE: 'bg-green-100 text-green-800',
      FAILED: 'bg-red-100 text-red-800',
      REVERSED: 'bg-purple-100 text-purple-800',
    };
    return colors[s] || 'bg-gray-100 text-gray-800';
  };

  // Matching the 10 rows per page from BMS (ROW1-ROW10)
  const columns = [
    {
      key: 'transactionDate',
      header: 'Date',
      render: (row: Record<string, unknown>) => new Date(String(row.transactionDate)).toLocaleDateString(),
    },
    {
      key: 'type',
      header: 'Type',
      render: (row: Record<string, unknown>) => (
        <span className={`px-2 py-1 rounded-full text-xs font-medium ${typeColor(String(row.type))}`}>
          {typeLabel(String(row.type))}
        </span>
      ),
    },
    { key: 'investmentId', header: 'Investment' },
    {
      key: 'quantity',
      header: 'Units',
      render: (row: Record<string, unknown>) => Number(row.quantity).toLocaleString(undefined, { maximumFractionDigits: 4 }),
    },
    {
      key: 'price',
      header: 'Price',
      render: (row: Record<string, unknown>) => `$${Number(row.price).toLocaleString(undefined, { minimumFractionDigits: 2 })}`,
    },
    {
      key: 'amount',
      header: 'Amount',
      render: (row: Record<string, unknown>) => `$${Number(row.amount).toLocaleString(undefined, { minimumFractionDigits: 2 })}`,
    },
    {
      key: 'status',
      header: 'Status',
      render: (row: Record<string, unknown>) => (
        <span className={`px-2 py-1 rounded-full text-xs font-medium ${statusColor(String(row.status))}`}>
          {String(row.status)}
        </span>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-800">Transaction History</h1>
        <p className="text-gray-500 mt-1">Browse and search transaction records</p>
      </div>

      {/* Search form (replaces HISAIN input from HISMAP) */}
      <div className="bg-white rounded-lg shadow-sm border p-5">
        <form onSubmit={handleSearch} className="flex flex-wrap items-end gap-4">
          <div className="flex-1 min-w-[200px]">
            <label className="block text-sm font-medium text-gray-700 mb-1">Portfolio ID</label>
            <input
              type="text"
              value={searchId}
              onChange={(e) => setSearchId(e.target.value)}
              placeholder="e.g. PORT10001 (leave empty for all)"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Start Date</label>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">End Date</label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
            />
          </div>
          <button type="submit" className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 transition-colors">
            Search
          </button>
        </form>
      </div>

      {/* Transaction table (10 rows per page matching BMS ROW1-ROW10) */}
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

// Portfolio Inquiry (replaces POSMAP from INQSET.bms lines 23-49)
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';
import DataTable from '../components/DataTable';

interface Position {
  id: string;
  investmentId: string;
  positionDate: string;
  quantity: number;
  costBasis: number;
  marketValue: number;
  currency: string;
  status: string;
}

interface Portfolio {
  id: string;
  portfolioId: string;
  accountNo: string;
  clientName: string;
  clientType: string;
  status: string;
  totalValue: number;
  cashBalance: number;
  positions: Position[];
}

export default function PortfolioInquiry() {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const { data: portfolioData, isLoading: loadingPortfolio } = useQuery({
    queryKey: ['portfolio', selectedId],
    queryFn: () => api.getPortfolio(selectedId!) as Promise<{ data: Portfolio }>,
    enabled: !!selectedId,
  });

  const portfolio = portfolioData?.data;

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchTerm.trim()) {
      setSelectedId(searchTerm.trim());
    }
  };

  const statusColor = (s: string) => {
    if (s === 'ACTIVE' || s === 'A') return 'bg-green-100 text-green-800';
    if (s === 'CLOSED' || s === 'C') return 'bg-red-100 text-red-800';
    if (s === 'SUSPENDED' || s === 'S') return 'bg-yellow-100 text-yellow-800';
    return 'bg-gray-100 text-gray-800';
  };

  const columns = [
    { key: 'investmentId', header: 'Fund ID' },
    {
      key: 'quantity',
      header: 'Units',
      render: (row: Record<string, unknown>) => Number(row.quantity).toLocaleString(undefined, { maximumFractionDigits: 4 }),
    },
    {
      key: 'costBasis',
      header: 'Cost Basis',
      render: (row: Record<string, unknown>) => `$${Number(row.costBasis).toLocaleString(undefined, { minimumFractionDigits: 2 })}`,
    },
    {
      key: 'marketValue',
      header: 'Market Value',
      render: (row: Record<string, unknown>) => `$${Number(row.marketValue).toLocaleString(undefined, { minimumFractionDigits: 2 })}`,
    },
    {
      key: 'gainLoss',
      header: 'Gain/Loss',
      render: (row: Record<string, unknown>) => {
        const gl = Number(row.marketValue) - Number(row.costBasis);
        const color = gl >= 0 ? 'text-green-600' : 'text-red-600';
        return <span className={color}>${gl.toLocaleString(undefined, { minimumFractionDigits: 2 })}</span>;
      },
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
        <h1 className="text-2xl font-bold text-gray-800">Portfolio Position Inquiry</h1>
        <p className="text-gray-500 mt-1">Search by account number or portfolio ID</p>
      </div>

      {/* Search form (replaces ACCTIN input field from POSMAP) */}
      <div className="bg-white rounded-lg shadow-sm border p-5">
        <form onSubmit={handleSearch} className="flex items-end gap-4">
          <div className="flex-1">
            <label className="block text-sm font-medium text-gray-700 mb-1">Account / Portfolio ID</label>
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="e.g. PORT10001"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
            />
          </div>
          <button
            type="submit"
            className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 transition-colors"
          >
            Search
          </button>
        </form>
      </div>

      {/* Portfolio Details */}
      {loadingPortfolio && (
        <div className="flex justify-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600" />
        </div>
      )}

      {portfolio && (
        <div className="space-y-4">
          <div className="bg-white rounded-lg shadow-sm border p-5">
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div>
                <p className="text-sm text-gray-500">Portfolio ID</p>
                <p className="font-semibold">{portfolio.portfolioId}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Client Name</p>
                <p className="font-semibold">{portfolio.clientName}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Account No</p>
                <p className="font-semibold">{portfolio.accountNo}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Status</p>
                <span className={`px-2 py-1 rounded-full text-xs font-medium ${statusColor(portfolio.status)}`}>
                  {portfolio.status}
                </span>
              </div>
              <div>
                <p className="text-sm text-gray-500">Total Value</p>
                <p className="font-semibold text-lg">${Number(portfolio.totalValue).toLocaleString()}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Cash Balance</p>
                <p className="font-semibold">${Number(portfolio.cashBalance).toLocaleString()}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Client Type</p>
                <p className="font-semibold">{portfolio.clientType}</p>
              </div>
            </div>
          </div>

          {/* Positions table (replaces Fund ID, Fund Name, Units, Cost Basis, Market Value display from POSMAP) */}
          <div className="bg-white rounded-lg shadow-sm border">
            <div className="p-4 border-b">
              <h2 className="text-lg font-semibold text-gray-800">Positions</h2>
            </div>
            <DataTable
              columns={columns}
              data={(portfolio.positions || []) as unknown as Record<string, unknown>[]}
            />
          </div>
        </div>
      )}

      {selectedId && !loadingPortfolio && !portfolio && (
        <div className="bg-red-50 text-red-700 p-4 rounded-lg border border-red-200">
          Portfolio not found: {selectedId}
        </div>
      )}
    </div>
  );
}

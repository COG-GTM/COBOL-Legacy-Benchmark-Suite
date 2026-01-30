import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Calendar, Filter, Download } from 'lucide-react';
import { transactionApi, portfolioApi } from '../api';
import type { Transaction, Portfolio } from '../types';

function TransactionHistory() {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [portfolios, setPortfolios] = useState<Portfolio[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedPortfolio, setSelectedPortfolio] = useState<string>('');
  const [typeFilter, setTypeFilter] = useState<string>('ALL');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  useEffect(() => {
    loadData();
  }, []);

  useEffect(() => {
    if (selectedPortfolio) {
      loadTransactions(selectedPortfolio);
    } else {
      setTransactions([]);
    }
  }, [selectedPortfolio]);

  const loadData = async () => {
    try {
      setLoading(true);
      const portfolioData = await portfolioApi.getAll();
      setPortfolios(portfolioData);
      if (portfolioData.length > 0) {
        setSelectedPortfolio(portfolioData[0].portfolioId);
      }
    } catch (err) {
      setError('Failed to load data');
      console.error('Error loading data:', err);
    } finally {
      setLoading(false);
    }
  };

  const loadTransactions = async (portfolioId: string) => {
    try {
      const data = await transactionApi.getByPortfolio(portfolioId);
      setTransactions(data);
    } catch (err) {
      console.error('Error loading transactions:', err);
      setTransactions([]);
    }
  };

  const filteredTransactions = transactions.filter(txn => {
    if (typeFilter !== 'ALL' && txn.type !== typeFilter) return false;
    if (statusFilter !== 'ALL' && txn.status !== statusFilter) return false;
    return true;
  });

  const getTypeBadge = (type: string) => {
    const styles = {
      BUY: 'bg-[#4ADE80]/20 text-[#4ADE80]',
      SELL: 'bg-[#F87171]/20 text-[#F87171]',
      TRANSFER: 'bg-[#60A5FA]/20 text-[#60A5FA]',
      FEE: 'bg-[#FBBF24]/20 text-[#FBBF24]',
    };
    return styles[type as keyof typeof styles] || 'bg-[#94A3B8]/20 text-[#94A3B8]';
  };

  const getStatusBadge = (status: string) => {
    const styles = {
      DONE: 'bg-[#4ADE80]/20 text-[#4ADE80]',
      PENDING: 'bg-[#FBBF24]/20 text-[#FBBF24]',
      FAILED: 'bg-[#F87171]/20 text-[#F87171]',
      REVERSED: 'bg-[#818CF8]/20 text-[#818CF8]',
    };
    return styles[status as keyof typeof styles] || 'bg-[#94A3B8]/20 text-[#94A3B8]';
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-[#CBD5E1]">Loading transaction history...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-white">Transaction History</h1>
          <p className="mt-1 text-[#94A3B8]">View and filter transaction records</p>
        </div>
        <Link
          to="/transactions/new"
          className="flex items-center px-4 py-2 bg-[#22D3EE] text-[#0F172A] rounded-lg font-medium hover:bg-[#22D3EE]/90 transition-colors"
        >
          New Transaction
        </Link>
      </div>

      {error && (
        <div className="bg-[#F87171]/10 border border-[#F87171]/30 rounded-xl p-4">
          <span className="text-[#F87171]">{error}</span>
        </div>
      )}

      <div className="bg-[#1E293B] rounded-xl border border-[#334155]">
        <div className="p-4 border-b border-[#334155] flex flex-col md:flex-row gap-4">
          <div className="flex-1">
            <label className="block text-sm text-[#94A3B8] mb-1">Portfolio</label>
            <select
              value={selectedPortfolio}
              onChange={(e) => setSelectedPortfolio(e.target.value)}
              className="w-full px-4 py-2 bg-[#0F172A] border border-[#334155] rounded-lg text-white focus:outline-none focus:border-[#22D3EE]"
            >
              <option value="">Select a portfolio</option>
              {portfolios.map(p => (
                <option key={p.portfolioId} value={p.portfolioId}>
                  {p.portfolioId} - {p.clientName}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm text-[#94A3B8] mb-1">Type</label>
            <select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              className="px-4 py-2 bg-[#0F172A] border border-[#334155] rounded-lg text-white focus:outline-none focus:border-[#22D3EE]"
            >
              <option value="ALL">All Types</option>
              <option value="BUY">Buy</option>
              <option value="SELL">Sell</option>
              <option value="FEE">Fee</option>
              <option value="TRANSFER">Transfer</option>
            </select>
          </div>
          <div>
            <label className="block text-sm text-[#94A3B8] mb-1">Status</label>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="px-4 py-2 bg-[#0F172A] border border-[#334155] rounded-lg text-white focus:outline-none focus:border-[#22D3EE]"
            >
              <option value="ALL">All Status</option>
              <option value="DONE">Done</option>
              <option value="PENDING">Pending</option>
              <option value="FAILED">Failed</option>
              <option value="REVERSED">Reversed</option>
            </select>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="text-left text-[#94A3B8] text-sm bg-[#243449]">
                <th className="px-6 py-3 font-medium">Date</th>
                <th className="px-6 py-3 font-medium">Time</th>
                <th className="px-6 py-3 font-medium">Type</th>
                <th className="px-6 py-3 font-medium">Investment</th>
                <th className="px-6 py-3 font-medium">Status</th>
                <th className="px-6 py-3 font-medium text-right">Quantity</th>
                <th className="px-6 py-3 font-medium text-right">Price</th>
                <th className="px-6 py-3 font-medium text-right">Amount</th>
              </tr>
            </thead>
            <tbody>
              {filteredTransactions.map((txn, index) => (
                <tr 
                  key={txn.id} 
                  className={`border-b border-[#334155]/50 ${
                    index % 2 === 0 ? 'bg-[#1E293B]' : 'bg-[#243449]/30'
                  }`}
                >
                  <td className="px-6 py-4 text-[#CBD5E1]">{txn.transactionDate}</td>
                  <td className="px-6 py-4 text-[#94A3B8]">{txn.transactionTime}</td>
                  <td className="px-6 py-4">
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getTypeBadge(txn.type)}`}>
                      {txn.type}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-white">{txn.investmentId || '-'}</td>
                  <td className="px-6 py-4">
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusBadge(txn.status)}`}>
                      {txn.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right text-white">
                    {txn.quantity?.toLocaleString('en-US', { minimumFractionDigits: 4 })}
                  </td>
                  <td className="px-6 py-4 text-right text-[#CBD5E1]">
                    ${txn.price?.toLocaleString('en-US', { minimumFractionDigits: 2 }) || '-'}
                  </td>
                  <td className="px-6 py-4 text-right text-white font-medium">
                    ${txn.amount?.toLocaleString('en-US', { minimumFractionDigits: 2 }) || '0.00'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {!selectedPortfolio && (
          <div className="p-8 text-center text-[#94A3B8]">
            Please select a portfolio to view transactions.
          </div>
        )}

        {selectedPortfolio && filteredTransactions.length === 0 && (
          <div className="p-8 text-center text-[#94A3B8]">
            No transactions found matching your criteria.
          </div>
        )}

        <div className="p-4 border-t border-[#334155] flex items-center justify-between">
          <span className="text-[#94A3B8] text-sm">
            Showing {filteredTransactions.length} of {transactions.length} transactions
          </span>
          <div className="flex gap-2">
            <button className="flex items-center px-3 py-1.5 text-sm text-[#94A3B8] hover:text-white hover:bg-[#334155] rounded-lg transition-colors">
              <Calendar className="h-4 w-4 mr-1" />
              Date Range
            </button>
            <button className="flex items-center px-3 py-1.5 text-sm text-[#94A3B8] hover:text-white hover:bg-[#334155] rounded-lg transition-colors">
              <Download className="h-4 w-4 mr-1" />
              Export
            </button>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-[#1E293B] rounded-xl p-4 border border-[#334155]">
          <p className="text-[#94A3B8] text-sm">Total Transactions</p>
          <p className="text-2xl font-bold text-white mt-1">{transactions.length}</p>
        </div>
        <div className="bg-[#1E293B] rounded-xl p-4 border border-[#334155]">
          <p className="text-[#94A3B8] text-sm">Buy Orders</p>
          <p className="text-2xl font-bold text-[#4ADE80] mt-1">
            {transactions.filter(t => t.type === 'BUY').length}
          </p>
        </div>
        <div className="bg-[#1E293B] rounded-xl p-4 border border-[#334155]">
          <p className="text-[#94A3B8] text-sm">Sell Orders</p>
          <p className="text-2xl font-bold text-[#F87171] mt-1">
            {transactions.filter(t => t.type === 'SELL').length}
          </p>
        </div>
        <div className="bg-[#1E293B] rounded-xl p-4 border border-[#334155]">
          <p className="text-[#94A3B8] text-sm">Total Volume</p>
          <p className="text-2xl font-bold text-[#22D3EE] mt-1">
            ${transactions.reduce((sum, t) => sum + (t.amount || 0), 0).toLocaleString('en-US', { minimumFractionDigits: 2 })}
          </p>
        </div>
      </div>
    </div>
  );
}

export default TransactionHistory;

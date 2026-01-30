import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Search, Filter, Plus, ChevronRight } from 'lucide-react';
import { portfolioApi } from '../api';
import type { Portfolio } from '../types';

function PortfolioList() {
  const [portfolios, setPortfolios] = useState<Portfolio[]>([]);
  const [filteredPortfolios, setFilteredPortfolios] = useState<Portfolio[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  useEffect(() => {
    loadPortfolios();
  }, []);

  useEffect(() => {
    filterPortfolios();
  }, [portfolios, searchTerm, statusFilter]);

  const loadPortfolios = async () => {
    try {
      setLoading(true);
      const data = await portfolioApi.getAll();
      setPortfolios(data);
      setError(null);
    } catch (err) {
      setError('Failed to load portfolios');
      console.error('Error loading portfolios:', err);
    } finally {
      setLoading(false);
    }
  };

  const filterPortfolios = () => {
    let filtered = portfolios;

    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      filtered = filtered.filter(
        p => p.portfolioId.toLowerCase().includes(term) ||
             p.clientName?.toLowerCase().includes(term) ||
             p.accountNo.toLowerCase().includes(term)
      );
    }

    if (statusFilter !== 'ALL') {
      filtered = filtered.filter(p => p.status === statusFilter);
    }

    setFilteredPortfolios(filtered);
  };

  const getStatusBadge = (status: string) => {
    const styles = {
      ACTIVE: 'bg-[#4ADE80]/20 text-[#4ADE80]',
      SUSPENDED: 'bg-[#FBBF24]/20 text-[#FBBF24]',
      CLOSED: 'bg-[#F87171]/20 text-[#F87171]',
    };
    return styles[status as keyof typeof styles] || 'bg-[#94A3B8]/20 text-[#94A3B8]';
  };

  const getClientTypeBadge = (type: string) => {
    const styles = {
      INDIVIDUAL: 'bg-[#22D3EE]/20 text-[#22D3EE]',
      CORPORATE: 'bg-[#60A5FA]/20 text-[#60A5FA]',
      TRUST: 'bg-[#818CF8]/20 text-[#818CF8]',
    };
    return styles[type as keyof typeof styles] || 'bg-[#94A3B8]/20 text-[#94A3B8]';
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-[#CBD5E1]">Loading portfolios...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-white">Portfolios</h1>
          <p className="mt-1 text-[#94A3B8]">Manage and view all portfolio accounts</p>
        </div>
        <Link
          to="/admin"
          className="flex items-center px-4 py-2 bg-[#22D3EE] text-[#0F172A] rounded-lg font-medium hover:bg-[#22D3EE]/90 transition-colors"
        >
          <Plus className="h-4 w-4 mr-2" />
          New Portfolio
        </Link>
      </div>

      {error && (
        <div className="bg-[#F87171]/10 border border-[#F87171]/30 rounded-xl p-4">
          <span className="text-[#F87171]">{error}</span>
        </div>
      )}

      <div className="bg-[#1E293B] rounded-xl border border-[#334155]">
        <div className="p-4 border-b border-[#334155] flex flex-col md:flex-row gap-4">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-[#94A3B8]" />
            <input
              type="text"
              placeholder="Search by ID, name, or account..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-[#0F172A] border border-[#334155] rounded-lg text-white placeholder-[#94A3B8] focus:outline-none focus:border-[#22D3EE]"
            />
          </div>
          <div className="flex items-center gap-2">
            <Filter className="h-4 w-4 text-[#94A3B8]" />
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="px-4 py-2 bg-[#0F172A] border border-[#334155] rounded-lg text-white focus:outline-none focus:border-[#22D3EE]"
            >
              <option value="ALL">All Status</option>
              <option value="ACTIVE">Active</option>
              <option value="SUSPENDED">Suspended</option>
              <option value="CLOSED">Closed</option>
            </select>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="text-left text-[#94A3B8] text-sm bg-[#243449]">
                <th className="px-6 py-3 font-medium">Portfolio ID</th>
                <th className="px-6 py-3 font-medium">Account No</th>
                <th className="px-6 py-3 font-medium">Client Name</th>
                <th className="px-6 py-3 font-medium">Type</th>
                <th className="px-6 py-3 font-medium">Status</th>
                <th className="px-6 py-3 font-medium text-right">Total Value</th>
                <th className="px-6 py-3 font-medium text-right">Total Units</th>
                <th className="px-6 py-3 font-medium"></th>
              </tr>
            </thead>
            <tbody>
              {filteredPortfolios.map((portfolio, index) => (
                <tr 
                  key={portfolio.id} 
                  className={`border-b border-[#334155]/50 hover:bg-[#243449]/50 ${
                    index % 2 === 0 ? 'bg-[#1E293B]' : 'bg-[#243449]/30'
                  }`}
                >
                  <td className="px-6 py-4">
                    <Link to={`/portfolios/${portfolio.portfolioId}`} className="text-[#22D3EE] hover:underline font-medium">
                      {portfolio.portfolioId}
                    </Link>
                  </td>
                  <td className="px-6 py-4 text-[#CBD5E1]">{portfolio.accountNo}</td>
                  <td className="px-6 py-4 text-white">{portfolio.clientName}</td>
                  <td className="px-6 py-4">
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getClientTypeBadge(portfolio.clientType)}`}>
                      {portfolio.clientType}
                    </span>
                  </td>
                  <td className="px-6 py-4">
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusBadge(portfolio.status)}`}>
                      {portfolio.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right text-white">
                    ${portfolio.totalValue?.toLocaleString('en-US', { minimumFractionDigits: 2 }) || '0.00'}
                  </td>
                  <td className="px-6 py-4 text-right text-[#CBD5E1]">
                    {portfolio.totalUnits?.toLocaleString('en-US', { minimumFractionDigits: 4 }) || '0.0000'}
                  </td>
                  <td className="px-6 py-4">
                    <Link to={`/portfolios/${portfolio.portfolioId}`} className="text-[#94A3B8] hover:text-white">
                      <ChevronRight className="h-5 w-5" />
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {filteredPortfolios.length === 0 && (
          <div className="p-8 text-center text-[#94A3B8]">
            No portfolios found matching your criteria.
          </div>
        )}

        <div className="p-4 border-t border-[#334155] text-[#94A3B8] text-sm">
          Showing {filteredPortfolios.length} of {portfolios.length} portfolios
        </div>
      </div>
    </div>
  );
}

export default PortfolioList;

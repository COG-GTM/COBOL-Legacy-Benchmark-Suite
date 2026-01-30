import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Briefcase, TrendingUp, DollarSign, AlertCircle, CheckCircle, Clock } from 'lucide-react';
import { portfolioApi } from '../api';
import type { Portfolio } from '../types';

function Dashboard() {
  const [portfolios, setPortfolios] = useState<Portfolio[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadPortfolios();
  }, []);

  const loadPortfolios = async () => {
    try {
      setLoading(true);
      const data = await portfolioApi.getAll();
      setPortfolios(data);
      setError(null);
    } catch (err) {
      setError('Failed to load portfolios. Please ensure the backend is running.');
      console.error('Error loading portfolios:', err);
    } finally {
      setLoading(false);
    }
  };

  const activePortfolios = portfolios.filter(p => p.status === 'ACTIVE');
  const totalValue = portfolios.reduce((sum, p) => sum + (p.totalValue || 0), 0);
  const totalUnits = portfolios.reduce((sum, p) => sum + (p.totalUnits || 0), 0);

  const statusCounts = {
    ACTIVE: portfolios.filter(p => p.status === 'ACTIVE').length,
    SUSPENDED: portfolios.filter(p => p.status === 'SUSPENDED').length,
    CLOSED: portfolios.filter(p => p.status === 'CLOSED').length,
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-[#CBD5E1]">Loading dashboard...</div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-semibold text-white">Dashboard</h1>
        <p className="mt-1 text-[#94A3B8]">Portfolio Management System Overview</p>
      </div>

      {error && (
        <div className="bg-[#F87171]/10 border border-[#F87171]/30 rounded-xl p-4 flex items-center">
          <AlertCircle className="h-5 w-5 text-[#F87171] mr-3" />
          <span className="text-[#F87171]">{error}</span>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="bg-gradient-to-br from-[#22D3EE] to-[#818CF8] rounded-xl p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-white/80 text-sm">Total Portfolios</p>
              <p className="text-3xl font-bold text-white mt-1">{portfolios.length}</p>
            </div>
            <Briefcase className="h-12 w-12 text-white/30" />
          </div>
        </div>

        <div className="bg-[#1E293B] rounded-xl p-6 border border-[#334155]">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[#94A3B8] text-sm">Total Value</p>
              <p className="text-2xl font-bold text-white mt-1">
                ${totalValue.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
              </p>
            </div>
            <DollarSign className="h-10 w-10 text-[#22D3EE]" />
          </div>
        </div>

        <div className="bg-[#1E293B] rounded-xl p-6 border border-[#334155]">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[#94A3B8] text-sm">Total Units</p>
              <p className="text-2xl font-bold text-white mt-1">
                {totalUnits.toLocaleString('en-US', { minimumFractionDigits: 4, maximumFractionDigits: 4 })}
              </p>
            </div>
            <TrendingUp className="h-10 w-10 text-[#60A5FA]" />
          </div>
        </div>

        <div className="bg-[#1E293B] rounded-xl p-6 border border-[#334155]">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[#94A3B8] text-sm">Active Portfolios</p>
              <p className="text-2xl font-bold text-white mt-1">{activePortfolios.length}</p>
            </div>
            <CheckCircle className="h-10 w-10 text-[#4ADE80]" />
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="bg-[#1E293B] rounded-xl p-6 border border-[#334155]">
          <h3 className="text-lg font-medium text-white mb-4">Portfolio Status</h3>
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center">
                <div className="w-3 h-3 rounded-full bg-[#4ADE80] mr-3"></div>
                <span className="text-[#CBD5E1]">Active</span>
              </div>
              <span className="text-white font-medium">{statusCounts.ACTIVE}</span>
            </div>
            <div className="flex items-center justify-between">
              <div className="flex items-center">
                <div className="w-3 h-3 rounded-full bg-[#FBBF24] mr-3"></div>
                <span className="text-[#CBD5E1]">Suspended</span>
              </div>
              <span className="text-white font-medium">{statusCounts.SUSPENDED}</span>
            </div>
            <div className="flex items-center justify-between">
              <div className="flex items-center">
                <div className="w-3 h-3 rounded-full bg-[#F87171] mr-3"></div>
                <span className="text-[#CBD5E1]">Closed</span>
              </div>
              <span className="text-white font-medium">{statusCounts.CLOSED}</span>
            </div>
          </div>
        </div>

        <div className="lg:col-span-2 bg-[#1E293B] rounded-xl p-6 border border-[#334155]">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-medium text-white">Recent Portfolios</h3>
            <Link to="/portfolios" className="text-[#22D3EE] text-sm hover:underline">
              View All
            </Link>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="text-left text-[#94A3B8] text-sm border-b border-[#334155]">
                  <th className="pb-3 font-medium">Portfolio ID</th>
                  <th className="pb-3 font-medium">Client Name</th>
                  <th className="pb-3 font-medium">Status</th>
                  <th className="pb-3 font-medium text-right">Total Value</th>
                </tr>
              </thead>
              <tbody>
                {portfolios.slice(0, 5).map((portfolio) => (
                  <tr key={portfolio.id} className="border-b border-[#334155]/50">
                    <td className="py-3">
                      <Link to={`/portfolios/${portfolio.portfolioId}`} className="text-[#22D3EE] hover:underline">
                        {portfolio.portfolioId}
                      </Link>
                    </td>
                    <td className="py-3 text-[#CBD5E1]">{portfolio.clientName}</td>
                    <td className="py-3">
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        portfolio.status === 'ACTIVE' ? 'bg-[#4ADE80]/20 text-[#4ADE80]' :
                        portfolio.status === 'SUSPENDED' ? 'bg-[#FBBF24]/20 text-[#FBBF24]' :
                        'bg-[#F87171]/20 text-[#F87171]'
                      }`}>
                        {portfolio.status}
                      </span>
                    </td>
                    <td className="py-3 text-right text-white">
                      ${portfolio.totalValue?.toLocaleString('en-US', { minimumFractionDigits: 2 }) || '0.00'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div className="bg-[#1E293B] rounded-xl p-6 border border-[#334155]">
        <h3 className="text-lg font-medium text-white mb-4">Quick Actions</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <Link
            to="/transactions/new"
            className="flex items-center p-4 bg-[#243449] rounded-lg hover:bg-[#2D3F5A] transition-colors"
          >
            <TrendingUp className="h-8 w-8 text-[#22D3EE] mr-4" />
            <div>
              <p className="text-white font-medium">New Transaction</p>
              <p className="text-[#94A3B8] text-sm">Process buy/sell orders</p>
            </div>
          </Link>
          <Link
            to="/portfolios"
            className="flex items-center p-4 bg-[#243449] rounded-lg hover:bg-[#2D3F5A] transition-colors"
          >
            <Briefcase className="h-8 w-8 text-[#60A5FA] mr-4" />
            <div>
              <p className="text-white font-medium">View Portfolios</p>
              <p className="text-[#94A3B8] text-sm">Manage all portfolios</p>
            </div>
          </Link>
          <Link
            to="/audit"
            className="flex items-center p-4 bg-[#243449] rounded-lg hover:bg-[#2D3F5A] transition-colors"
          >
            <Clock className="h-8 w-8 text-[#818CF8] mr-4" />
            <div>
              <p className="text-white font-medium">Audit Trail</p>
              <p className="text-[#94A3B8] text-sm">View system activity</p>
            </div>
          </Link>
        </div>
      </div>
    </div>
  );
}

export default Dashboard;

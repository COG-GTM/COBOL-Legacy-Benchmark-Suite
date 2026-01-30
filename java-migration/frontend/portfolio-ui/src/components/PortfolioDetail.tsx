import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, TrendingUp, DollarSign, Calendar, User, RefreshCw } from 'lucide-react';
import { portfolioApi, transactionApi, auditApi } from '../api';
import type { Portfolio, Transaction, AuditLog } from '../types';

function PortfolioDetail() {
  const { portfolioId } = useParams<{ portfolioId: string }>();
  const [portfolio, setPortfolio] = useState<Portfolio | null>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'overview' | 'transactions' | 'audit'>('overview');

  useEffect(() => {
    if (portfolioId) {
      loadPortfolioData();
    }
  }, [portfolioId]);

  const loadPortfolioData = async () => {
    try {
      setLoading(true);
      const [portfolioData, transactionsData, auditData] = await Promise.all([
        portfolioApi.getById(portfolioId!),
        transactionApi.getByPortfolio(portfolioId!).catch(() => []),
        auditApi.getByPortfolio(portfolioId!).catch(() => []),
      ]);
      setPortfolio(portfolioData);
      setTransactions(transactionsData);
      setAuditLogs(auditData);
      setError(null);
    } catch (err) {
      setError('Failed to load portfolio details');
      console.error('Error loading portfolio:', err);
    } finally {
      setLoading(false);
    }
  };

  const getStatusBadge = (status: string) => {
    const styles = {
      ACTIVE: 'bg-[#4ADE80]/20 text-[#4ADE80]',
      SUSPENDED: 'bg-[#FBBF24]/20 text-[#FBBF24]',
      CLOSED: 'bg-[#F87171]/20 text-[#F87171]',
    };
    return styles[status as keyof typeof styles] || 'bg-[#94A3B8]/20 text-[#94A3B8]';
  };

  const getTransactionTypeBadge = (type: string) => {
    const styles = {
      BUY: 'bg-[#4ADE80]/20 text-[#4ADE80]',
      SELL: 'bg-[#F87171]/20 text-[#F87171]',
      TRANSFER: 'bg-[#60A5FA]/20 text-[#60A5FA]',
      FEE: 'bg-[#FBBF24]/20 text-[#FBBF24]',
    };
    return styles[type as keyof typeof styles] || 'bg-[#94A3B8]/20 text-[#94A3B8]';
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-[#CBD5E1]">Loading portfolio details...</div>
      </div>
    );
  }

  if (error || !portfolio) {
    return (
      <div className="space-y-4">
        <Link to="/portfolios" className="flex items-center text-[#22D3EE] hover:underline">
          <ArrowLeft className="h-4 w-4 mr-2" />
          Back to Portfolios
        </Link>
        <div className="bg-[#F87171]/10 border border-[#F87171]/30 rounded-xl p-4">
          <span className="text-[#F87171]">{error || 'Portfolio not found'}</span>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center">
          <Link to="/portfolios" className="text-[#94A3B8] hover:text-white mr-4">
            <ArrowLeft className="h-5 w-5" />
          </Link>
          <div>
            <h1 className="text-2xl font-semibold text-white">{portfolio.portfolioId}</h1>
            <p className="mt-1 text-[#94A3B8]">{portfolio.clientName}</p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${getStatusBadge(portfolio.status)}`}>
            {portfolio.status}
          </span>
          <button
            onClick={loadPortfolioData}
            className="p-2 text-[#94A3B8] hover:text-white hover:bg-[#334155] rounded-lg transition-colors"
          >
            <RefreshCw className="h-5 w-5" />
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-[#1E293B] rounded-xl p-5 border border-[#334155]">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[#94A3B8] text-sm">Total Value</p>
              <p className="text-xl font-bold text-white mt-1">
                ${portfolio.totalValue?.toLocaleString('en-US', { minimumFractionDigits: 2 }) || '0.00'}
              </p>
            </div>
            <DollarSign className="h-8 w-8 text-[#22D3EE]" />
          </div>
        </div>

        <div className="bg-[#1E293B] rounded-xl p-5 border border-[#334155]">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[#94A3B8] text-sm">Total Units</p>
              <p className="text-xl font-bold text-white mt-1">
                {portfolio.totalUnits?.toLocaleString('en-US', { minimumFractionDigits: 4 }) || '0.0000'}
              </p>
            </div>
            <TrendingUp className="h-8 w-8 text-[#60A5FA]" />
          </div>
        </div>

        <div className="bg-[#1E293B] rounded-xl p-5 border border-[#334155]">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[#94A3B8] text-sm">Total Cost</p>
              <p className="text-xl font-bold text-white mt-1">
                ${portfolio.totalCost?.toLocaleString('en-US', { minimumFractionDigits: 2 }) || '0.00'}
              </p>
            </div>
            <DollarSign className="h-8 w-8 text-[#818CF8]" />
          </div>
        </div>

        <div className="bg-[#1E293B] rounded-xl p-5 border border-[#334155]">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[#94A3B8] text-sm">Cash Balance</p>
              <p className="text-xl font-bold text-white mt-1">
                ${portfolio.cashBalance?.toLocaleString('en-US', { minimumFractionDigits: 2 }) || '0.00'}
              </p>
            </div>
            <DollarSign className="h-8 w-8 text-[#4ADE80]" />
          </div>
        </div>
      </div>

      <div className="bg-[#1E293B] rounded-xl border border-[#334155]">
        <div className="border-b border-[#334155]">
          <nav className="flex">
            {(['overview', 'transactions', 'audit'] as const).map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={`px-6 py-4 text-sm font-medium border-b-2 transition-colors ${
                  activeTab === tab
                    ? 'border-[#22D3EE] text-[#22D3EE]'
                    : 'border-transparent text-[#94A3B8] hover:text-white'
                }`}
              >
                {tab.charAt(0).toUpperCase() + tab.slice(1)}
              </button>
            ))}
          </nav>
        </div>

        <div className="p-6">
          {activeTab === 'overview' && (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-4">
                <h3 className="text-lg font-medium text-white">Account Information</h3>
                <div className="space-y-3">
                  <div className="flex justify-between py-2 border-b border-[#334155]/50">
                    <span className="text-[#94A3B8]">Account Number</span>
                    <span className="text-white">{portfolio.accountNo}</span>
                  </div>
                  <div className="flex justify-between py-2 border-b border-[#334155]/50">
                    <span className="text-[#94A3B8]">Client Type</span>
                    <span className="text-white">{portfolio.clientType}</span>
                  </div>
                  <div className="flex justify-between py-2 border-b border-[#334155]/50">
                    <span className="text-[#94A3B8]">Created Date</span>
                    <span className="text-white">{portfolio.createDate || 'N/A'}</span>
                  </div>
                  <div className="flex justify-between py-2 border-b border-[#334155]/50">
                    <span className="text-[#94A3B8]">Last Maintenance</span>
                    <span className="text-white">{portfolio.lastMaintDate || 'N/A'}</span>
                  </div>
                </div>
              </div>
              <div className="space-y-4">
                <h3 className="text-lg font-medium text-white">Activity</h3>
                <div className="space-y-3">
                  <div className="flex justify-between py-2 border-b border-[#334155]/50">
                    <span className="text-[#94A3B8]">Last User</span>
                    <span className="text-white">{portfolio.lastUser || 'N/A'}</span>
                  </div>
                  <div className="flex justify-between py-2 border-b border-[#334155]/50">
                    <span className="text-[#94A3B8]">Last Transaction</span>
                    <span className="text-white">{portfolio.lastTransDate || 'N/A'}</span>
                  </div>
                  <div className="flex justify-between py-2 border-b border-[#334155]/50">
                    <span className="text-[#94A3B8]">Total Transactions</span>
                    <span className="text-white">{transactions.length}</span>
                  </div>
                  <div className="flex justify-between py-2 border-b border-[#334155]/50">
                    <span className="text-[#94A3B8]">Audit Records</span>
                    <span className="text-white">{auditLogs.length}</span>
                  </div>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'transactions' && (
            <div>
              {transactions.length === 0 ? (
                <div className="text-center py-8 text-[#94A3B8]">
                  No transactions found for this portfolio.
                </div>
              ) : (
                <table className="w-full">
                  <thead>
                    <tr className="text-left text-[#94A3B8] text-sm">
                      <th className="pb-3 font-medium">Date</th>
                      <th className="pb-3 font-medium">Type</th>
                      <th className="pb-3 font-medium">Status</th>
                      <th className="pb-3 font-medium text-right">Quantity</th>
                      <th className="pb-3 font-medium text-right">Amount</th>
                    </tr>
                  </thead>
                  <tbody>
                    {transactions.map((txn) => (
                      <tr key={txn.id} className="border-t border-[#334155]/50">
                        <td className="py-3 text-[#CBD5E1]">{txn.transactionDate}</td>
                        <td className="py-3">
                          <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getTransactionTypeBadge(txn.type)}`}>
                            {txn.type}
                          </span>
                        </td>
                        <td className="py-3">
                          <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                            txn.status === 'DONE' ? 'bg-[#4ADE80]/20 text-[#4ADE80]' :
                            txn.status === 'FAILED' ? 'bg-[#F87171]/20 text-[#F87171]' :
                            'bg-[#FBBF24]/20 text-[#FBBF24]'
                          }`}>
                            {txn.status}
                          </span>
                        </td>
                        <td className="py-3 text-right text-white">{txn.quantity?.toLocaleString('en-US', { minimumFractionDigits: 4 })}</td>
                        <td className="py-3 text-right text-white">${txn.amount?.toLocaleString('en-US', { minimumFractionDigits: 2 }) || '0.00'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          )}

          {activeTab === 'audit' && (
            <div>
              {auditLogs.length === 0 ? (
                <div className="text-center py-8 text-[#94A3B8]">
                  No audit records found for this portfolio.
                </div>
              ) : (
                <table className="w-full">
                  <thead>
                    <tr className="text-left text-[#94A3B8] text-sm">
                      <th className="pb-3 font-medium">Timestamp</th>
                      <th className="pb-3 font-medium">Action</th>
                      <th className="pb-3 font-medium">Status</th>
                      <th className="pb-3 font-medium">User</th>
                      <th className="pb-3 font-medium">Message</th>
                    </tr>
                  </thead>
                  <tbody>
                    {auditLogs.map((log) => (
                      <tr key={log.id} className="border-t border-[#334155]/50">
                        <td className="py-3 text-[#CBD5E1] text-sm">{log.timestamp}</td>
                        <td className="py-3 text-white">{log.action}</td>
                        <td className="py-3">
                          <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                            log.status === 'SUCCESS' ? 'bg-[#4ADE80]/20 text-[#4ADE80]' :
                            log.status === 'FAILURE' ? 'bg-[#F87171]/20 text-[#F87171]' :
                            'bg-[#FBBF24]/20 text-[#FBBF24]'
                          }`}>
                            {log.status}
                          </span>
                        </td>
                        <td className="py-3 text-[#CBD5E1]">{log.userId}</td>
                        <td className="py-3 text-[#94A3B8] text-sm truncate max-w-xs">{log.message}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default PortfolioDetail;

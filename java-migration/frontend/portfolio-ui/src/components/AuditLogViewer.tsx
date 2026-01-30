import { useState, useEffect } from 'react';
import { FileText, Search, Filter, Calendar, RefreshCw } from 'lucide-react';
import { auditApi, portfolioApi } from '../api';
import type { AuditLog, Portfolio } from '../types';

function AuditLogViewer() {
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [portfolios, setPortfolios] = useState<Portfolio[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedPortfolio, setSelectedPortfolio] = useState<string>('');
  const [actionFilter, setActionFilter] = useState<string>('ALL');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  useEffect(() => {
    loadPortfolios();
  }, []);

  useEffect(() => {
    if (selectedPortfolio) {
      loadAuditLogs(selectedPortfolio);
    } else {
      setAuditLogs([]);
    }
  }, [selectedPortfolio]);

  const loadPortfolios = async () => {
    try {
      setLoading(true);
      const data = await portfolioApi.getAll();
      setPortfolios(data);
      if (data.length > 0) {
        setSelectedPortfolio(data[0].portfolioId);
      }
    } catch (err) {
      setError('Failed to load portfolios');
      console.error('Error loading portfolios:', err);
    } finally {
      setLoading(false);
    }
  };

  const loadAuditLogs = async (portfolioId: string) => {
    try {
      const data = await auditApi.getByPortfolio(portfolioId);
      setAuditLogs(data);
    } catch (err) {
      console.error('Error loading audit logs:', err);
      setAuditLogs([]);
    }
  };

  const filteredLogs = auditLogs.filter(log => {
    if (actionFilter !== 'ALL' && log.action !== actionFilter) return false;
    if (statusFilter !== 'ALL' && log.status !== statusFilter) return false;
    return true;
  });

  const getStatusBadge = (status: string) => {
    const styles = {
      SUCCESS: 'bg-[#4ADE80]/20 text-[#4ADE80]',
      FAILURE: 'bg-[#F87171]/20 text-[#F87171]',
      WARNING: 'bg-[#FBBF24]/20 text-[#FBBF24]',
    };
    return styles[status as keyof typeof styles] || 'bg-[#94A3B8]/20 text-[#94A3B8]';
  };

  const getActionBadge = (action: string) => {
    const styles = {
      CREATE: 'bg-[#4ADE80]/20 text-[#4ADE80]',
      UPDATE: 'bg-[#60A5FA]/20 text-[#60A5FA]',
      DELETE: 'bg-[#F87171]/20 text-[#F87171]',
      INQUIRE: 'bg-[#22D3EE]/20 text-[#22D3EE]',
      LOGIN: 'bg-[#818CF8]/20 text-[#818CF8]',
      LOGOUT: 'bg-[#A78BFA]/20 text-[#A78BFA]',
      STARTUP: 'bg-[#FBBF24]/20 text-[#FBBF24]',
      SHUTDOWN: 'bg-[#F87171]/20 text-[#F87171]',
    };
    return styles[action as keyof typeof styles] || 'bg-[#94A3B8]/20 text-[#94A3B8]';
  };

  const formatTimestamp = (timestamp: string) => {
    try {
      const date = new Date(timestamp);
      return date.toLocaleString('en-US', {
        year: 'numeric',
        month: 'short',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      });
    } catch {
      return timestamp;
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-[#CBD5E1]">Loading audit logs...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-white">Audit Log</h1>
          <p className="mt-1 text-[#94A3B8]">View system activity and audit trail</p>
        </div>
        <button
          onClick={() => selectedPortfolio && loadAuditLogs(selectedPortfolio)}
          className="flex items-center px-4 py-2 bg-[#334155] text-white rounded-lg font-medium hover:bg-[#475569] transition-colors"
        >
          <RefreshCw className="h-4 w-4 mr-2" />
          Refresh
        </button>
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
            <label className="block text-sm text-[#94A3B8] mb-1">Action</label>
            <select
              value={actionFilter}
              onChange={(e) => setActionFilter(e.target.value)}
              className="px-4 py-2 bg-[#0F172A] border border-[#334155] rounded-lg text-white focus:outline-none focus:border-[#22D3EE]"
            >
              <option value="ALL">All Actions</option>
              <option value="CREATE">Create</option>
              <option value="UPDATE">Update</option>
              <option value="DELETE">Delete</option>
              <option value="INQUIRE">Inquire</option>
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
              <option value="SUCCESS">Success</option>
              <option value="FAILURE">Failure</option>
              <option value="WARNING">Warning</option>
            </select>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="text-left text-[#94A3B8] text-sm bg-[#243449]">
                <th className="px-6 py-3 font-medium">Timestamp</th>
                <th className="px-6 py-3 font-medium">Action</th>
                <th className="px-6 py-3 font-medium">Status</th>
                <th className="px-6 py-3 font-medium">User</th>
                <th className="px-6 py-3 font-medium">Program</th>
                <th className="px-6 py-3 font-medium">Message</th>
              </tr>
            </thead>
            <tbody>
              {filteredLogs.map((log, index) => (
                <tr 
                  key={log.id} 
                  className={`border-b border-[#334155]/50 ${
                    index % 2 === 0 ? 'bg-[#1E293B]' : 'bg-[#243449]/30'
                  }`}
                >
                  <td className="px-6 py-4 text-[#CBD5E1] text-sm whitespace-nowrap">
                    {formatTimestamp(log.timestamp)}
                  </td>
                  <td className="px-6 py-4">
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getActionBadge(log.action)}`}>
                      {log.action}
                    </span>
                  </td>
                  <td className="px-6 py-4">
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusBadge(log.status)}`}>
                      {log.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-white">{log.userId}</td>
                  <td className="px-6 py-4 text-[#94A3B8] font-mono text-sm">{log.program}</td>
                  <td className="px-6 py-4 text-[#CBD5E1] text-sm max-w-xs truncate" title={log.message || ''}>
                    {log.message || '-'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {!selectedPortfolio && (
          <div className="p-8 text-center text-[#94A3B8]">
            Please select a portfolio to view audit logs.
          </div>
        )}

        {selectedPortfolio && filteredLogs.length === 0 && (
          <div className="p-8 text-center text-[#94A3B8]">
            No audit records found matching your criteria.
          </div>
        )}

        <div className="p-4 border-t border-[#334155] flex items-center justify-between">
          <span className="text-[#94A3B8] text-sm">
            Showing {filteredLogs.length} of {auditLogs.length} records
          </span>
          <button className="flex items-center px-3 py-1.5 text-sm text-[#94A3B8] hover:text-white hover:bg-[#334155] rounded-lg transition-colors">
            <Calendar className="h-4 w-4 mr-1" />
            Date Range
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-[#1E293B] rounded-xl p-4 border border-[#334155]">
          <p className="text-[#94A3B8] text-sm">Total Records</p>
          <p className="text-2xl font-bold text-white mt-1">{auditLogs.length}</p>
        </div>
        <div className="bg-[#1E293B] rounded-xl p-4 border border-[#334155]">
          <p className="text-[#94A3B8] text-sm">Successful</p>
          <p className="text-2xl font-bold text-[#4ADE80] mt-1">
            {auditLogs.filter(l => l.status === 'SUCCESS').length}
          </p>
        </div>
        <div className="bg-[#1E293B] rounded-xl p-4 border border-[#334155]">
          <p className="text-[#94A3B8] text-sm">Failures</p>
          <p className="text-2xl font-bold text-[#F87171] mt-1">
            {auditLogs.filter(l => l.status === 'FAILURE').length}
          </p>
        </div>
        <div className="bg-[#1E293B] rounded-xl p-4 border border-[#334155]">
          <p className="text-[#94A3B8] text-sm">Warnings</p>
          <p className="text-2xl font-bold text-[#FBBF24] mt-1">
            {auditLogs.filter(l => l.status === 'WARNING').length}
          </p>
        </div>
      </div>

      <div className="bg-[#1E293B] rounded-xl border border-[#334155] p-6">
        <h3 className="text-lg font-medium text-white mb-4">Audit Trail Information</h3>
        <p className="text-[#CBD5E1] text-sm mb-4">
          The audit log captures all system activities for compliance and troubleshooting purposes. 
          This mirrors the audit trail functionality from the original COBOL system.
        </p>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
          <div className="space-y-2">
            <h4 className="text-white font-medium">Tracked Actions</h4>
            <ul className="text-[#94A3B8] space-y-1">
              <li>Transaction processing (BUY, SELL, FEE)</li>
              <li>Portfolio updates (status, name, value)</li>
              <li>Position inquiries</li>
              <li>Administrative changes</li>
            </ul>
          </div>
          <div className="space-y-2">
            <h4 className="text-white font-medium">Captured Data</h4>
            <ul className="text-[#94A3B8] space-y-1">
              <li>Timestamp of operation</li>
              <li>User performing the action</li>
              <li>Before and after images (when applicable)</li>
              <li>Success/failure status and messages</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}

export default AuditLogViewer;

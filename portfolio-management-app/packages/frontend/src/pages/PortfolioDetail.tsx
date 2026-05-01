import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getPortfolio, getPositions, getTransactionHistory, deletePortfolio, getAuditReport } from '../lib/api';
import { useWebSocket } from '../hooks/useWebSocket';
import StatusBadge, { TransactionTypeBadge, GainLossDisplay } from '../components/StatusBadge';
import toast from 'react-hot-toast';
import type { AuditLog } from '../types';

export default function PortfolioDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { subscribePortfolio, unsubscribePortfolio } = useWebSocket();
  const [activeTab, setActiveTab] = useState<'positions' | 'transactions' | 'audit'>('positions');
  const [txnPage, setTxnPage] = useState(1);

  useEffect(() => {
    if (id) {
      subscribePortfolio(id);
      return () => unsubscribePortfolio(id);
    }
  }, [id, subscribePortfolio, unsubscribePortfolio]);

  const { data: portfolio, isLoading } = useQuery({
    queryKey: ['portfolio', id],
    queryFn: () => getPortfolio(id!),
    enabled: !!id,
  });

  const { data: positionsData } = useQuery({
    queryKey: ['positions', id],
    queryFn: () => getPositions(id!),
    enabled: !!id && activeTab === 'positions',
    refetchInterval: 30000,
  });

  const { data: txnData } = useQuery({
    queryKey: ['transactions', id, txnPage],
    queryFn: () => getTransactionHistory(id!, { page: txnPage, pageSize: 10 }),
    enabled: !!id && activeTab === 'transactions',
  });

  const { data: auditData } = useQuery({
    queryKey: ['audit', id],
    queryFn: () => getAuditReport({ portfolioId: id! }),
    enabled: !!id && activeTab === 'audit',
  });

  const deleteMutation = useMutation({
    mutationFn: () => deletePortfolio(id!, '03'),
    onSuccess: () => {
      toast.success('Portfolio closed');
      queryClient.invalidateQueries({ queryKey: ['portfolios'] });
      navigate('/portfolios');
    },
    onError: (err: { response?: { data?: { error?: { message?: string } } } }) => {
      toast.error(err.response?.data?.error?.message || 'Failed to close portfolio');
    },
  });

  if (isLoading) {
    return <div className="text-center py-12 text-gray-500">Loading portfolio...</div>;
  }

  const p = portfolio?.data;
  if (!p) {
    return <div className="text-center py-12 text-gray-500">Portfolio not found</div>;
  }

  const positions = positionsData?.data ?? [];
  const transactions = txnData?.data ?? [];
  const txnPagination = txnData?.pagination;
  const auditLogs: AuditLog[] = auditData?.data?.auditLogs ?? [];

  const totalMarketValue = positions.reduce((s, pos) => s + Number(pos.marketValue), 0);
  const totalCostBasis = positions.reduce((s, pos) => s + Number(pos.costBasis), 0);
  const totalGainLoss = totalMarketValue - totalCostBasis;

  return (
    <div>
      {/* Header */}
      <div className="flex items-start justify-between mb-6">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold">{p.portfolioName}</h1>
            <StatusBadge status={p.status} />
          </div>
          <p className="text-gray-500 mt-1">ID: {p.portfolioId} | Client: {p.clientId} | Branch: {p.branchId}</p>
        </div>
        {p.status === 'A' && (
          <button
            onClick={() => {
              if (confirm('Close this portfolio?')) deleteMutation.mutate();
            }}
            className="px-4 py-2 bg-red-50 text-red-600 rounded-lg hover:bg-red-100 text-sm"
          >
            Close Portfolio
          </button>
        )}
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
        <SummaryCard label="Total Value" value={fmt(Number(p.totalValue), p.currencyCode)} />
        <SummaryCard label="Cash Balance" value={fmt(Number(p.cashBalance), p.currencyCode)} />
        <SummaryCard
          label="Unrealized G/L"
          value={
            <GainLossDisplay value={totalGainLoss} />
          }
        />
        <SummaryCard label="Risk Level" value={`Level ${p.riskLevel}`} />
      </div>

      {/* Tabs — maps POSMAP / HISMAP from BMS */}
      <div className="border-b mb-4">
        <nav className="flex gap-4">
          {(['positions', 'transactions', 'audit'] as const).map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
                activeTab === tab
                  ? 'border-indigo-600 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              {tab.charAt(0).toUpperCase() + tab.slice(1)}
            </button>
          ))}
        </nav>
      </div>

      {/* Positions Tab — maps from POSMAP in BMS */}
      {activeTab === 'positions' && (
        <div className="bg-white rounded-xl shadow-sm border overflow-hidden">
          <table className="w-full">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Fund ID</th>
                <th className="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase">Units</th>
                <th className="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase">Cost Basis</th>
                <th className="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase">Market Value</th>
                <th className="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase">Gain/Loss</th>
                <th className="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase">Change %</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {positions.length === 0 ? (
                <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-500">No positions</td></tr>
              ) : positions.map((pos) => {
                const gl = Number(pos.marketValue) - Number(pos.costBasis);
                const pct = Number(pos.costBasis) > 0 ? (gl / Number(pos.costBasis)) * 100 : 0;
                return (
                  <tr key={pos.investmentId} className="hover:bg-gray-50">
                    <td className="px-4 py-3 font-medium">{pos.investmentId.trim()}</td>
                    <td className="px-4 py-3 text-right">{Number(pos.quantity).toLocaleString()}</td>
                    <td className="px-4 py-3 text-right">{fmt(Number(pos.costBasis))}</td>
                    <td className="px-4 py-3 text-right font-medium">{fmt(Number(pos.marketValue))}</td>
                    <td className="px-4 py-3 text-right"><GainLossDisplay value={gl} /></td>
                    <td className={`px-4 py-3 text-right font-medium ${pct >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                      {pct >= 0 ? '+' : ''}{pct.toFixed(2)}%
                    </td>
                  </tr>
                );
              })}
            </tbody>
            {positions.length > 0 && (
              <tfoot className="bg-gray-50 border-t font-medium">
                <tr>
                  <td className="px-4 py-3">Total</td>
                  <td className="px-4 py-3 text-right" />
                  <td className="px-4 py-3 text-right">{fmt(totalCostBasis)}</td>
                  <td className="px-4 py-3 text-right">{fmt(totalMarketValue)}</td>
                  <td className="px-4 py-3 text-right"><GainLossDisplay value={totalGainLoss} /></td>
                  <td className="px-4 py-3 text-right" />
                </tr>
              </tfoot>
            )}
          </table>
        </div>
      )}

      {/* Transactions Tab — maps from HISMAP in BMS */}
      {activeTab === 'transactions' && (
        <div className="bg-white rounded-xl shadow-sm border overflow-hidden">
          <table className="w-full">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Date</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Type</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Investment</th>
                <th className="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase">Units</th>
                <th className="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase">Price</th>
                <th className="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase">Amount</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {transactions.length === 0 ? (
                <tr><td colSpan={7} className="px-4 py-8 text-center text-gray-500">No transactions</td></tr>
              ) : transactions.map((t) => (
                <tr key={t.transactionId} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm">{new Date(t.transactionDate).toLocaleDateString()}</td>
                  <td className="px-4 py-3"><TransactionTypeBadge type={t.transactionType} /></td>
                  <td className="px-4 py-3">{t.investmentId.trim()}</td>
                  <td className="px-4 py-3 text-right">{Number(t.quantity).toLocaleString()}</td>
                  <td className="px-4 py-3 text-right">{fmt(Number(t.price))}</td>
                  <td className="px-4 py-3 text-right font-medium">{fmt(Number(t.amount))}</td>
                  <td className="px-4 py-3"><StatusBadge status={t.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
          {txnPagination && txnPagination.totalPages > 1 && (
            <div className="flex items-center justify-between px-4 py-3 border-t bg-gray-50">
              <span className="text-sm text-gray-500">Page {txnPagination.page} of {txnPagination.totalPages}</span>
              <div className="flex gap-2">
                <button disabled={txnPage <= 1} onClick={() => setTxnPage(txnPage - 1)} className="px-3 py-1 text-sm border rounded disabled:opacity-50">Previous</button>
                <button disabled={txnPage >= txnPagination.totalPages} onClick={() => setTxnPage(txnPage + 1)} className="px-3 py-1 text-sm border rounded disabled:opacity-50">Next</button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Audit Tab */}
      {activeTab === 'audit' && (
        <div className="bg-white rounded-xl shadow-sm border overflow-hidden">
          <table className="w-full">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Date</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Record Type</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Action</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">User</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Reason</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {auditLogs.length === 0 ? (
                <tr><td colSpan={5} className="px-4 py-8 text-center text-gray-500">No audit records</td></tr>
              ) : auditLogs.map((a) => (
                <tr key={a.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm">{new Date(a.processDate).toLocaleString()}</td>
                  <td className="px-4 py-3">{recordTypeLabel(a.recordType)}</td>
                  <td className="px-4 py-3">{actionLabel(a.actionCode)}</td>
                  <td className="px-4 py-3">{a.processUser}</td>
                  <td className="px-4 py-3 text-gray-500">{a.reasonCode || '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function SummaryCard({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="bg-white rounded-xl shadow-sm border p-4">
      <p className="text-sm text-gray-500 mb-1">{label}</p>
      <p className="text-xl font-bold">{value}</p>
    </div>
  );
}

function fmt(n: number, currency = 'USD') {
  return n.toLocaleString('en-US', { style: 'currency', currency });
}

function recordTypeLabel(rt: string) {
  const m: Record<string, string> = { PT: 'Portfolio', PS: 'Position', TR: 'Transaction' };
  return m[rt] || rt;
}

function actionLabel(ac: string) {
  const m: Record<string, string> = { A: 'Add', C: 'Change', D: 'Delete' };
  return m[ac] || ac;
}

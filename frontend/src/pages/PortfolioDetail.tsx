import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, Cell,
} from "recharts";
import { ArrowLeft, Briefcase, DollarSign, TrendingUp, Activity } from "lucide-react";
import { api } from "../api/client";
import type { PortfolioDetail as PortfolioDetailType } from "../types";
import { TXN_TYPE_LABELS, CLIENT_TYPE_LABELS, RISK_LABELS } from "../types";
import StatCard from "../components/StatCard";
import StatusBadge from "../components/StatusBadge";
import GainLoss from "../components/GainLoss";

const fmt = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD", minimumFractionDigits: 2 });

export default function PortfolioDetail() {
  const { id } = useParams<{ id: string }>();
  const [portfolio, setPortfolio] = useState<PortfolioDetailType | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!id) return;
    api.getPortfolio(id)
      .then(setPortfolio)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center bg-[#0F172A]">
        <div className="animate-spin w-8 h-8 border-2 border-[#22D3EE] border-t-transparent rounded-full" />
      </div>
    );
  }

  if (error || !portfolio) {
    return (
      <div className="flex-1 bg-[#0F172A] p-6">
        <Link to="/portfolios" className="text-[#22D3EE] flex items-center gap-1 text-sm mb-4 hover:underline">
          <ArrowLeft size={14} /> Back to portfolios
        </Link>
        <div className="text-[#F87171]">{error || "Portfolio not found"}</div>
      </div>
    );
  }

  const chartData = portfolio.positions.map((p) => ({
    name: p.investment_name,
    market_value: p.market_value,
    cost_basis: p.cost_basis,
    gain_loss: p.gain_loss,
  }));

  return (
    <div className="flex-1 bg-[#0F172A] p-6 overflow-auto">
      {/* Header */}
      <Link to="/portfolios" className="text-[#22D3EE] flex items-center gap-1 text-sm mb-4 hover:underline">
        <ArrowLeft size={14} /> Back to portfolios
      </Link>

      <div className="flex items-start justify-between mb-6">
        <div>
          <h1 className="text-2xl font-semibold text-white">{portfolio.client_name}</h1>
          <p className="text-sm text-[#94A3B8] mt-1">
            {portfolio.portfolio_id} &middot; Acct: {portfolio.account_no} &middot;{" "}
            {CLIENT_TYPE_LABELS[portfolio.client_type]} &middot; Risk: {RISK_LABELS[portfolio.risk_level]}
          </p>
        </div>
        <StatusBadge code={portfolio.status} />
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
        <StatCard
          label="Market Value"
          value={fmt.format(portfolio.total_market_value)}
          icon={<DollarSign size={16} />}
        />
        <StatCard
          label="Cost Basis"
          value={fmt.format(portfolio.total_cost_basis)}
          icon={<Briefcase size={16} />}
        />
        <StatCard
          label="Total Gain/Loss"
          value={fmt.format(portfolio.total_gain_loss)}
          subValue={`${portfolio.total_gain_loss_pct >= 0 ? "+" : ""}${portfolio.total_gain_loss_pct.toFixed(2)}%`}
          icon={<TrendingUp size={16} />}
          trend={portfolio.total_gain_loss >= 0 ? "up" : "down"}
        />
        <StatCard
          label="Cash Balance"
          value={fmt.format(portfolio.cash_balance)}
          subValue={`${portfolio.position_count} positions`}
          icon={<Activity size={16} />}
          trend="neutral"
        />
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-4 mb-6">
        {/* Positions Chart */}
        <div className="bg-[#1E293B] rounded-xl p-5 border border-white/5">
          <h2 className="text-sm font-medium text-[#E2E8F0] mb-4">Holdings Overview</h2>
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={chartData} margin={{ left: 10, right: 10 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
              <XAxis
                dataKey="name"
                tick={{ fill: "#94A3B8", fontSize: 10 }}
                angle={-30}
                textAnchor="end"
                height={60}
              />
              <YAxis tick={{ fill: "#94A3B8", fontSize: 11 }} tickFormatter={(v: number) => `$${(v / 1000).toFixed(0)}k`} />
              <Tooltip
                contentStyle={{ background: "#1E293B", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 8, color: "#fff" }}
                formatter={(v) => fmt.format(Number(v))}
              />
              <Bar dataKey="market_value" name="Market Value" fill="#22D3EE" radius={[4, 4, 0, 0]} />
              <Bar dataKey="cost_basis" name="Cost Basis" fill="#6366F1" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Gain/Loss Chart */}
        <div className="bg-[#1E293B] rounded-xl p-5 border border-white/5">
          <h2 className="text-sm font-medium text-[#E2E8F0] mb-4">Gain/Loss by Holding</h2>
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={chartData} margin={{ left: 10, right: 10 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
              <XAxis
                dataKey="name"
                tick={{ fill: "#94A3B8", fontSize: 10 }}
                angle={-30}
                textAnchor="end"
                height={60}
              />
              <YAxis tick={{ fill: "#94A3B8", fontSize: 11 }} tickFormatter={(v: number) => `$${(v / 1000).toFixed(0)}k`} />
              <Tooltip
                contentStyle={{ background: "#1E293B", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 8, color: "#fff" }}
                formatter={(v) => fmt.format(Number(v))}
              />
              <Bar dataKey="gain_loss" name="Gain/Loss" radius={[4, 4, 0, 0]}>
                {chartData.map((entry, i) => (
                  <Cell key={i} fill={entry.gain_loss >= 0 ? "#4ADE80" : "#F87171"} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Positions Table */}
      <div className="bg-[#1E293B] rounded-xl border border-white/5 mb-6">
        <div className="p-5 border-b border-white/5">
          <h2 className="text-sm font-medium text-[#E2E8F0]">Positions</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-[#94A3B8] text-xs border-b border-white/5">
                <th className="px-5 py-3 font-medium">Investment</th>
                <th className="px-5 py-3 font-medium text-right">Quantity</th>
                <th className="px-5 py-3 font-medium text-right">Cost Basis</th>
                <th className="px-5 py-3 font-medium text-right">Market Value</th>
                <th className="px-5 py-3 font-medium text-right">Gain/Loss</th>
                <th className="px-5 py-3 font-medium">Status</th>
              </tr>
            </thead>
            <tbody>
              {portfolio.positions.map((pos) => (
                <tr key={pos.id} className="border-b border-white/5 hover:bg-white/[0.02] text-[#CBD5E1]">
                  <td className="px-5 py-3">
                    <div className="font-medium text-white">{pos.investment_name}</div>
                    <div className="text-xs text-[#94A3B8]">{pos.investment_id}</div>
                  </td>
                  <td className="px-5 py-3 text-right font-mono">{pos.quantity.toFixed(4)}</td>
                  <td className="px-5 py-3 text-right font-mono">{fmt.format(pos.cost_basis)}</td>
                  <td className="px-5 py-3 text-right font-mono">{fmt.format(pos.market_value)}</td>
                  <td className="px-5 py-3 text-right">
                    <GainLoss value={pos.gain_loss} percent={pos.gain_loss_pct} size="sm" />
                  </td>
                  <td className="px-5 py-3"><StatusBadge code={pos.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Recent Transactions */}
      <div className="bg-[#1E293B] rounded-xl border border-white/5">
        <div className="p-5 border-b border-white/5">
          <h2 className="text-sm font-medium text-[#E2E8F0]">Recent Transactions</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-[#94A3B8] text-xs border-b border-white/5">
                <th className="px-5 py-3 font-medium">Date</th>
                <th className="px-5 py-3 font-medium">Type</th>
                <th className="px-5 py-3 font-medium">Investment</th>
                <th className="px-5 py-3 font-medium text-right">Quantity</th>
                <th className="px-5 py-3 font-medium text-right">Price</th>
                <th className="px-5 py-3 font-medium text-right">Amount</th>
                <th className="px-5 py-3 font-medium">Status</th>
              </tr>
            </thead>
            <tbody>
              {portfolio.recent_transactions.map((txn) => (
                <tr key={txn.transaction_id} className="border-b border-white/5 hover:bg-white/[0.02] text-[#CBD5E1]">
                  <td className="px-5 py-3">{txn.transaction_date}</td>
                  <td className="px-5 py-3">
                    <span className={`text-xs font-medium ${txn.transaction_type === "BU" ? "text-[#4ADE80]" : txn.transaction_type === "SL" ? "text-[#F87171]" : "text-[#94A3B8]"}`}>
                      {TXN_TYPE_LABELS[txn.transaction_type] ?? txn.transaction_type}
                    </span>
                  </td>
                  <td className="px-5 py-3">{txn.investment_id}</td>
                  <td className="px-5 py-3 text-right font-mono">{txn.quantity.toFixed(2)}</td>
                  <td className="px-5 py-3 text-right font-mono">{fmt.format(txn.price)}</td>
                  <td className="px-5 py-3 text-right font-mono">{fmt.format(txn.amount)}</td>
                  <td className="px-5 py-3"><StatusBadge code={txn.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  PieChart, Pie, Cell, ResponsiveContainer, Tooltip,
  BarChart, Bar, XAxis, YAxis, CartesianGrid,
} from "recharts";
import {
  Briefcase, TrendingUp, BarChart3,
  DollarSign, Activity,
} from "lucide-react";
import { api } from "../api/client";
import type { DashboardStats } from "../types";
import { TXN_TYPE_LABELS } from "../types";
import StatCard from "../components/StatCard";
import StatusBadge from "../components/StatusBadge";


const CHART_COLORS = ["#22D3EE", "#6366F1", "#F59E0B", "#4ADE80", "#F87171", "#C084FC", "#FB923C", "#A78BFA"];

const fmt = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
  minimumFractionDigits: 0,
  maximumFractionDigits: 0,
});

const fmtFull = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
  minimumFractionDigits: 2,
});

export default function Dashboard() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.getDashboard().then(setStats).finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <div className="animate-spin w-8 h-8 border-2 border-[#22D3EE] border-t-transparent rounded-full" />
      </div>
    );
  }

  if (!stats) return <div className="p-8 text-[#F87171]">Failed to load dashboard data</div>;

  return (
    <div className="flex-1 bg-[#0F172A] p-6 overflow-auto">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-white">Dashboard</h1>
        <p className="text-sm text-[#94A3B8]">Investment Portfolio Management System</p>
      </div>

      {/* Stat Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
        <StatCard
          label="Total Portfolios"
          value={stats.total_portfolios.toString()}
          subValue={`${stats.active_portfolios} active`}
          icon={<Briefcase size={16} />}
          trend="neutral"
        />
        <StatCard
          label="Market Value"
          value={fmt.format(stats.total_market_value)}
          subValue={`Cost basis: ${fmt.format(stats.total_cost_basis)}`}
          icon={<DollarSign size={16} />}
          trend="neutral"
        />
        <StatCard
          label="Total Gain/Loss"
          value={fmtFull.format(stats.total_gain_loss)}
          subValue={`${stats.total_gain_loss_pct >= 0 ? "+" : ""}${stats.total_gain_loss_pct.toFixed(2)}%`}
          icon={<TrendingUp size={16} />}
          trend={stats.total_gain_loss >= 0 ? "up" : "down"}
        />
        <StatCard
          label="Active Positions"
          value={stats.total_positions.toString()}
          subValue={`${stats.total_transactions} transactions`}
          icon={<Activity size={16} />}
          trend="neutral"
        />
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-4 mb-6">
        {/* Portfolio Breakdown Pie */}
        <div className="bg-[#1E293B] rounded-xl p-5 border border-white/5">
          <h2 className="text-sm font-medium text-[#E2E8F0] mb-4">Portfolio Type Breakdown</h2>
          <ResponsiveContainer width="100%" height={200}>
            <PieChart>
              <Pie
                data={stats.portfolio_breakdown}
                cx="50%"
                cy="50%"
                innerRadius={50}
                outerRadius={80}
                dataKey="value"
                stroke="#0F172A"
                strokeWidth={2}
              >
                {stats.portfolio_breakdown.map((_, i) => (
                  <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />
                ))}
              </Pie>
              <Tooltip
                contentStyle={{ background: "#1E293B", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 8, color: "#fff" }}
              />
            </PieChart>
          </ResponsiveContainer>
          <div className="flex justify-center gap-4 mt-2">
            {stats.portfolio_breakdown.map((item, i) => (
              <div key={item.name} className="flex items-center gap-2 text-xs text-[#CBD5E1]">
                <div className="w-2.5 h-2.5 rounded-full" style={{ background: CHART_COLORS[i] }} />
                {item.name} ({item.value})
              </div>
            ))}
          </div>
        </div>

        {/* Status Breakdown Pie */}
        <div className="bg-[#1E293B] rounded-xl p-5 border border-white/5">
          <h2 className="text-sm font-medium text-[#E2E8F0] mb-4">Portfolio Status</h2>
          <ResponsiveContainer width="100%" height={200}>
            <PieChart>
              <Pie
                data={stats.status_breakdown}
                cx="50%"
                cy="50%"
                innerRadius={50}
                outerRadius={80}
                dataKey="value"
                stroke="#0F172A"
                strokeWidth={2}
              >
                {stats.status_breakdown.map((entry) => (
                  <Cell
                    key={entry.name}
                    fill={
                      entry.name === "Active" ? "#4ADE80"
                        : entry.name === "Closed" ? "#94A3B8"
                          : "#FBBF24"
                    }
                  />
                ))}
              </Pie>
              <Tooltip
                contentStyle={{ background: "#1E293B", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 8, color: "#fff" }}
              />
            </PieChart>
          </ResponsiveContainer>
          <div className="flex justify-center gap-4 mt-2">
            {stats.status_breakdown.map((item) => (
              <div key={item.name} className="flex items-center gap-2 text-xs text-[#CBD5E1]">
                <div
                  className="w-2.5 h-2.5 rounded-full"
                  style={{
                    background: item.name === "Active" ? "#4ADE80"
                      : item.name === "Closed" ? "#94A3B8" : "#FBBF24"
                  }}
                />
                {item.name} ({item.value})
              </div>
            ))}
          </div>
        </div>

        {/* Top Performers Bar Chart */}
        <div className="bg-[#1E293B] rounded-xl p-5 border border-white/5">
          <h2 className="text-sm font-medium text-[#E2E8F0] mb-4">Top Performers</h2>
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={stats.top_performers} layout="vertical" margin={{ left: 10, right: 10 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
              <XAxis type="number" tick={{ fill: "#94A3B8", fontSize: 11 }} tickFormatter={(v: number) => fmt.format(v)} />
              <YAxis
                type="category"
                dataKey="investment_name"
                tick={{ fill: "#CBD5E1", fontSize: 11 }}
                width={100}
              />
              <Tooltip
                contentStyle={{ background: "#1E293B", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 8, color: "#fff" }}
                formatter={(v) => fmtFull.format(Number(v))}
              />
              <Bar dataKey="gain_loss" radius={[0, 4, 4, 0]}>
                {stats.top_performers.map((entry, i) => (
                  <Cell key={i} fill={entry.gain_loss >= 0 ? "#4ADE80" : "#F87171"} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Recent Transactions Table */}
      <div className="bg-[#1E293B] rounded-xl border border-white/5">
        <div className="flex items-center justify-between p-5 border-b border-white/5">
          <h2 className="text-sm font-medium text-[#E2E8F0] flex items-center gap-2">
            <BarChart3 size={16} className="text-[#22D3EE]" />
            Recent Transactions
          </h2>
          <Link to="/transactions" className="text-xs text-[#22D3EE] hover:underline">View all</Link>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-[#94A3B8] text-xs border-b border-white/5">
                <th className="px-5 py-3 font-medium">Date</th>
                <th className="px-5 py-3 font-medium">Portfolio</th>
                <th className="px-5 py-3 font-medium">Investment</th>
                <th className="px-5 py-3 font-medium">Type</th>
                <th className="px-5 py-3 font-medium text-right">Quantity</th>
                <th className="px-5 py-3 font-medium text-right">Amount</th>
                <th className="px-5 py-3 font-medium">Status</th>
              </tr>
            </thead>
            <tbody>
              {stats.recent_transactions.map((txn) => (
                <tr key={txn.transaction_id} className="border-b border-white/5 hover:bg-white/[0.02] text-[#CBD5E1]">
                  <td className="px-5 py-3">{txn.transaction_date}</td>
                  <td className="px-5 py-3">
                    <Link to={`/portfolios/${txn.portfolio_id}`} className="text-[#22D3EE] hover:underline">
                      {txn.portfolio_id}
                    </Link>
                  </td>
                  <td className="px-5 py-3">{txn.investment_id}</td>
                  <td className="px-5 py-3">
                    <span className={`text-xs font-medium ${txn.transaction_type === "BU" ? "text-[#4ADE80]" : txn.transaction_type === "SL" ? "text-[#F87171]" : "text-[#94A3B8]"}`}>
                      {TXN_TYPE_LABELS[txn.transaction_type] ?? txn.transaction_type}
                    </span>
                  </td>
                  <td className="px-5 py-3 text-right font-mono">{txn.quantity.toFixed(2)}</td>
                  <td className="px-5 py-3 text-right font-mono">{fmtFull.format(txn.amount)}</td>
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

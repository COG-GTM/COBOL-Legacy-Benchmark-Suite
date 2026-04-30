import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Search, Briefcase, Filter } from "lucide-react";
import { api } from "../api/client";
import type { PortfolioSummary } from "../types";
import { CLIENT_TYPE_LABELS, RISK_LABELS } from "../types";
import StatusBadge from "../components/StatusBadge";
import GainLoss from "../components/GainLoss";

const fmt = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD", minimumFractionDigits: 2 });

export default function Portfolios() {
  const [portfolios, setPortfolios] = useState<PortfolioSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [typeFilter, setTypeFilter] = useState("");

  useEffect(() => {
    const params: Record<string, string> = {};
    if (search) params.search = search;
    if (statusFilter) params.status = statusFilter;
    if (typeFilter) params.client_type = typeFilter;
    setLoading(true);
    api.getPortfolios(params).then(setPortfolios).finally(() => setLoading(false));
  }, [search, statusFilter, typeFilter]);

  return (
    <div className="flex-1 bg-[#0F172A] p-6 overflow-auto">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-semibold text-white">Portfolios</h1>
          <p className="text-sm text-[#94A3B8]">
            Replaces PORTMSTR VSAM file & INQPORT inquiry
          </p>
        </div>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap gap-3 mb-6">
        <div className="relative flex-1 min-w-[200px] max-w-md">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-[#94A3B8]" />
          <input
            type="text"
            placeholder="Search portfolios..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full bg-[#1E293B] border border-white/10 rounded-lg pl-9 pr-4 py-2 text-sm text-white placeholder-[#94A3B8] focus:outline-none focus:border-[#22D3EE]/50"
          />
        </div>
        <div className="flex items-center gap-2">
          <Filter size={14} className="text-[#94A3B8]" />
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="bg-[#1E293B] border border-white/10 rounded-lg px-3 py-2 text-sm text-[#CBD5E1] focus:outline-none focus:border-[#22D3EE]/50"
          >
            <option value="">All Status</option>
            <option value="A">Active</option>
            <option value="C">Closed</option>
            <option value="S">Suspended</option>
          </select>
          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
            className="bg-[#1E293B] border border-white/10 rounded-lg px-3 py-2 text-sm text-[#CBD5E1] focus:outline-none focus:border-[#22D3EE]/50"
          >
            <option value="">All Types</option>
            <option value="I">Individual</option>
            <option value="C">Corporate</option>
            <option value="T">Trust</option>
          </select>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <div className="animate-spin w-8 h-8 border-2 border-[#22D3EE] border-t-transparent rounded-full" />
        </div>
      ) : portfolios.length === 0 ? (
        <div className="text-center py-20 text-[#94A3B8]">
          <Briefcase size={48} className="mx-auto mb-4 opacity-50" />
          <p>No portfolios found</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {portfolios.map((p) => (
            <Link
              key={p.portfolio_id}
              to={`/portfolios/${p.portfolio_id}`}
              className="block bg-[#1E293B] rounded-xl p-5 border border-white/5 hover:border-[#22D3EE]/30 transition-colors"
            >
              <div className="flex items-start justify-between mb-3">
                <div>
                  <h3 className="text-base font-medium text-white">{p.client_name}</h3>
                  <p className="text-xs text-[#94A3B8] mt-0.5">
                    {p.portfolio_id} &middot; Acct: {p.account_no}
                  </p>
                </div>
                <StatusBadge code={p.status} />
              </div>
              <div className="grid grid-cols-3 gap-4 mb-3">
                <div>
                  <p className="text-[10px] uppercase text-[#94A3B8] tracking-wider">Market Value</p>
                  <p className="text-sm font-medium text-white">{fmt.format(p.total_market_value)}</p>
                </div>
                <div>
                  <p className="text-[10px] uppercase text-[#94A3B8] tracking-wider">Cash Balance</p>
                  <p className="text-sm font-medium text-white">{fmt.format(p.cash_balance)}</p>
                </div>
                <div>
                  <p className="text-[10px] uppercase text-[#94A3B8] tracking-wider">Gain/Loss</p>
                  <GainLoss value={p.total_gain_loss} percent={p.total_gain_loss_pct} size="sm" />
                </div>
              </div>
              <div className="flex items-center gap-3 text-xs text-[#94A3B8]">
                <span>{CLIENT_TYPE_LABELS[p.client_type] ?? p.client_type}</span>
                <span>&middot;</span>
                <span>Risk: {RISK_LABELS[p.risk_level] ?? p.risk_level}</span>
                <span>&middot;</span>
                <span>{p.position_count} positions</span>
                <span>&middot;</span>
                <span>Opened {p.open_date}</span>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}

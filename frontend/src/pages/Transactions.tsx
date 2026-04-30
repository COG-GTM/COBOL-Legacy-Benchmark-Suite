import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowLeftRight, Filter } from "lucide-react";
import { api } from "../api/client";
import type { Transaction } from "../types";
import { TXN_TYPE_LABELS } from "../types";
import StatusBadge from "../components/StatusBadge";

const fmt = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD", minimumFractionDigits: 2 });

export default function Transactions() {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [typeFilter, setTypeFilter] = useState("");
  const [portFilter, setPortFilter] = useState("");

  useEffect(() => {
    const params: Record<string, string> = { limit: "100" };
    if (typeFilter) params.transaction_type = typeFilter;
    if (portFilter) params.portfolio_id = portFilter;
    setLoading(true);
    api.getTransactions(params).then(setTransactions).finally(() => setLoading(false));
  }, [typeFilter, portFilter]);

  return (
    <div className="flex-1 bg-[#0F172A] p-6 overflow-auto">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-white flex items-center gap-2">
          <ArrowLeftRight size={22} className="text-[#22D3EE]" />
          Transactions
        </h1>
        <p className="text-sm text-[#94A3B8]">
          Replaces INQHIST (DB2 history inquiry) & TRNVAL00 (validation)
        </p>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap gap-3 mb-6">
        <div className="flex items-center gap-2">
          <Filter size={14} className="text-[#94A3B8]" />
          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
            className="bg-[#1E293B] border border-white/10 rounded-lg px-3 py-2 text-sm text-[#CBD5E1] focus:outline-none focus:border-[#22D3EE]/50"
          >
            <option value="">All Types</option>
            <option value="BU">Buy</option>
            <option value="SL">Sell</option>
            <option value="TR">Transfer</option>
            <option value="FE">Fee</option>
          </select>
          <input
            type="text"
            placeholder="Filter by Portfolio ID..."
            value={portFilter}
            onChange={(e) => setPortFilter(e.target.value.toUpperCase())}
            className="bg-[#1E293B] border border-white/10 rounded-lg px-3 py-2 text-sm text-white placeholder-[#94A3B8] focus:outline-none focus:border-[#22D3EE]/50 w-48"
          />
        </div>
        <div className="ml-auto text-sm text-[#94A3B8]">
          {transactions.length} transactions
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <div className="animate-spin w-8 h-8 border-2 border-[#22D3EE] border-t-transparent rounded-full" />
        </div>
      ) : (
        <div className="bg-[#1E293B] rounded-xl border border-white/5">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-[#94A3B8] text-xs border-b border-white/5">
                  <th className="px-5 py-3 font-medium">Date</th>
                  <th className="px-5 py-3 font-medium">Transaction ID</th>
                  <th className="px-5 py-3 font-medium">Portfolio</th>
                  <th className="px-5 py-3 font-medium">Investment</th>
                  <th className="px-5 py-3 font-medium">Type</th>
                  <th className="px-5 py-3 font-medium text-right">Quantity</th>
                  <th className="px-5 py-3 font-medium text-right">Price</th>
                  <th className="px-5 py-3 font-medium text-right">Amount</th>
                  <th className="px-5 py-3 font-medium">Status</th>
                </tr>
              </thead>
              <tbody>
                {transactions.map((txn) => (
                  <tr key={txn.transaction_id} className="border-b border-white/5 hover:bg-white/[0.02] text-[#CBD5E1]">
                    <td className="px-5 py-3">{txn.transaction_date}</td>
                    <td className="px-5 py-3 font-mono text-xs">{txn.transaction_id}</td>
                    <td className="px-5 py-3">
                      <Link to={`/portfolios/${txn.portfolio_id}`} className="text-[#22D3EE] hover:underline">
                        {txn.portfolio_id}
                      </Link>
                    </td>
                    <td className="px-5 py-3">{txn.investment_id}</td>
                    <td className="px-5 py-3">
                      <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${
                        txn.transaction_type === "BU" ? "bg-[#4ADE80]/15 text-[#4ADE80]"
                        : txn.transaction_type === "SL" ? "bg-[#F87171]/15 text-[#F87171]"
                        : txn.transaction_type === "FE" ? "bg-[#FBBF24]/15 text-[#FBBF24]"
                        : "bg-[#60A5FA]/15 text-[#60A5FA]"
                      }`}>
                        {TXN_TYPE_LABELS[txn.transaction_type] ?? txn.transaction_type}
                      </span>
                    </td>
                    <td className="px-5 py-3 text-right font-mono">{txn.quantity.toFixed(4)}</td>
                    <td className="px-5 py-3 text-right font-mono">{fmt.format(txn.price)}</td>
                    <td className="px-5 py-3 text-right font-mono">{fmt.format(txn.amount)}</td>
                    <td className="px-5 py-3"><StatusBadge code={txn.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}

"use client";

import { useState } from "react";
import useSWR from "swr";
import { submitTransaction, swrFetcher } from "@/lib/api";
import type { TransactionType, InvestmentType, PortfolioListResponse } from "@/types";
import toast from "react-hot-toast";

interface Props {
  onSuccess: () => void;
  onCancel: () => void;
}

export function SubmitTransactionForm({ onSuccess, onCancel }: Props) {
  const { data } = useSWR<PortfolioListResponse>("/api/portfolios", swrFetcher);
  const [portfolioId, setPortfolioId] = useState("");
  const [transactionType, setTransactionType] = useState<TransactionType>("BUY");
  const [investmentType, setInvestmentType] = useState<InvestmentType>("STK");
  const [units, setUnits] = useState("");
  const [price, setPrice] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");

    if (!portfolioId) { setError("Select a portfolio"); return; }
    const u = parseFloat(units);
    const p = parseFloat(price);
    if (isNaN(u) || u <= 0) { setError("Units must be greater than 0"); return; }
    if (isNaN(p) || p <= 0) { setError("Price must be greater than 0"); return; }

    setSubmitting(true);
    try {
      await submitTransaction({ portfolioId, transactionType, investmentType, units: u, price: p });
      toast.success("Transaction submitted successfully");
      onSuccess();
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Failed to submit transaction";
      setError(msg);
      toast.error(msg);
    } finally {
      setSubmitting(false);
    }
  }

  const amount = (parseFloat(units) || 0) * (parseFloat(price) || 0);

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <h3 className="text-lg font-semibold text-gray-900">Submit Transaction</h3>

      {error && (
        <div className="rounded-md bg-red-50 p-3 text-sm text-red-700" role="alert">{error}</div>
      )}

      <div>
        <label htmlFor="portfolio" className="block text-sm font-medium text-gray-700">Portfolio</label>
        <select
          id="portfolio"
          value={portfolioId}
          onChange={(e) => setPortfolioId(e.target.value)}
          className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          required
        >
          <option value="">Select a portfolio...</option>
          {data?.portfolios
            .filter((p) => p.status === "A")
            .map((p) => (
              <option key={p.id} value={p.id}>{p.accountNo} - {p.clientName}</option>
            ))}
        </select>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label htmlFor="txType" className="block text-sm font-medium text-gray-700">Transaction Type</label>
          <select
            id="txType"
            value={transactionType}
            onChange={(e) => setTransactionType(e.target.value as TransactionType)}
            className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          >
            <option value="BUY">Buy</option>
            <option value="SELL">Sell</option>
            <option value="TRANSFER">Transfer</option>
            <option value="FEE">Fee</option>
          </select>
        </div>
        <div>
          <label htmlFor="invType" className="block text-sm font-medium text-gray-700">Investment Type</label>
          <select
            id="invType"
            value={investmentType}
            onChange={(e) => setInvestmentType(e.target.value as InvestmentType)}
            className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          >
            <option value="STK">Stock</option>
            <option value="BND">Bond</option>
            <option value="MMF">Money Market Fund</option>
            <option value="ETF">Exchange Traded Fund</option>
          </select>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label htmlFor="units" className="block text-sm font-medium text-gray-700">Units</label>
          <input
            id="units"
            type="number"
            step="0.01"
            min="0.01"
            value={units}
            onChange={(e) => setUnits(e.target.value)}
            className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            required
          />
        </div>
        <div>
          <label htmlFor="price" className="block text-sm font-medium text-gray-700">Price</label>
          <input
            id="price"
            type="number"
            step="0.01"
            min="0.01"
            value={price}
            onChange={(e) => setPrice(e.target.value)}
            className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            required
          />
        </div>
      </div>

      {amount > 0 && (
        <div className="rounded-md bg-blue-50 p-3 text-sm text-blue-700">
          Estimated Amount: <span className="font-semibold">${amount.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
        </div>
      )}

      <div className="flex justify-end gap-3 pt-2">
        <button type="button" onClick={onCancel} className="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50">
          Cancel
        </button>
        <button type="submit" disabled={submitting} className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50">
          {submitting ? "Submitting..." : "Submit Transaction"}
        </button>
      </div>
    </form>
  );
}

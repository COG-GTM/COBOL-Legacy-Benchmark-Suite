"use client";

import { useState } from "react";
import { createPortfolio } from "@/lib/api";
import type { ClientType } from "@/types";
import toast from "react-hot-toast";

interface Props {
  onSuccess: () => void;
  onCancel: () => void;
}

export function CreatePortfolioForm({ onSuccess, onCancel }: Props) {
  const [accountNo, setAccountNo] = useState("");
  const [clientName, setClientName] = useState("");
  const [clientType, setClientType] = useState<ClientType>("I");
  const [cashBalance, setCashBalance] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");

    if (!/^\d{10}$/.test(accountNo)) {
      setError("Account number must be exactly 10 digits");
      return;
    }
    if (!clientName.trim()) {
      setError("Client name is required");
      return;
    }

    setSubmitting(true);
    try {
      await createPortfolio({
        accountNo,
        clientName: clientName.trim(),
        clientType,
        cashBalance: cashBalance ? parseFloat(cashBalance) : 0,
      });
      toast.success("Portfolio created successfully");
      onSuccess();
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Failed to create portfolio";
      setError(msg);
      toast.error(msg);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <h3 className="text-lg font-semibold text-gray-900">Create Portfolio</h3>

      {error && (
        <div className="rounded-md bg-red-50 p-3 text-sm text-red-700" role="alert">{error}</div>
      )}

      <div>
        <label htmlFor="accountNo" className="block text-sm font-medium text-gray-700">Account Number</label>
        <input
          id="accountNo"
          type="text"
          maxLength={10}
          value={accountNo}
          onChange={(e) => setAccountNo(e.target.value.replace(/\D/g, ""))}
          placeholder="1000000009"
          className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          required
        />
      </div>

      <div>
        <label htmlFor="clientName" className="block text-sm font-medium text-gray-700">Client Name</label>
        <input
          id="clientName"
          type="text"
          value={clientName}
          onChange={(e) => setClientName(e.target.value)}
          placeholder="John Doe Investment Fund"
          className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          required
        />
      </div>

      <div>
        <label htmlFor="clientType" className="block text-sm font-medium text-gray-700">Client Type</label>
        <select
          id="clientType"
          value={clientType}
          onChange={(e) => setClientType(e.target.value as ClientType)}
          className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
        >
          <option value="I">Individual</option>
          <option value="C">Corporate</option>
          <option value="T">Trust</option>
        </select>
      </div>

      <div>
        <label htmlFor="cashBalance" className="block text-sm font-medium text-gray-700">Initial Cash Balance</label>
        <input
          id="cashBalance"
          type="number"
          step="0.01"
          min="0"
          value={cashBalance}
          onChange={(e) => setCashBalance(e.target.value)}
          placeholder="0.00"
          className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
        />
      </div>

      <div className="flex justify-end gap-3 pt-2">
        <button type="button" onClick={onCancel} className="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50">
          Cancel
        </button>
        <button type="submit" disabled={submitting} className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50">
          {submitting ? "Creating..." : "Create Portfolio"}
        </button>
      </div>
    </form>
  );
}

"use client";

import { useState } from "react";
import useSWR from "swr";
import { swrFetcher } from "@/lib/api";
import type { ReportStats, PositionReport, AuditReport } from "@/types";
import { LoadingState } from "@/components/ui/LoadingState";
import { ErrorDisplay } from "@/components/ui/ErrorDisplay";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { StatCard } from "@/components/ui/StatCard";
import {
  BarChart, Bar, LineChart, Line, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend,
} from "recharts";

const COLORS = ["#0033FF", "#110081", "#00874D", "#FF4D00", "#8032DF", "#0058DB", "#FFCD34", "#BA0000"];

function formatCurrency(value: number): string {
  return new Intl.NumberFormat("en-US", { style: "currency", currency: "USD", maximumFractionDigits: 0 }).format(value);
}

function StatisticsTab() {
  const { data, error, isLoading } = useSWR<ReportStats>("/api/reports?type=statistics", swrFetcher);

  if (isLoading) return <LoadingState message="Loading statistics..." />;
  if (error) return <ErrorDisplay message={error.message} />;
  if (!data) return null;

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Total Portfolios" value={String(data.totalPortfolios)} />
        <StatCard label="Total AUM" value={formatCurrency(data.totalAUM)} />
        <StatCard label="Total Transactions" value={String(data.totalTransactions)} />
        <StatCard label="Avg Portfolio Value" value={formatCurrency(data.avgPortfolioValue)} />
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div className="rounded-lg border border-gray-200 bg-white p-5">
          <h4 className="mb-4 text-base font-semibold text-gray-900">Portfolio Value Trend</h4>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={data.portfolioValueTrend}>
              <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
              <XAxis dataKey="month" tick={{ fontSize: 12 }} />
              <YAxis tickFormatter={(v) => `$${(v / 1000000).toFixed(1)}M`} tick={{ fontSize: 12 }} />
              <Tooltip formatter={(v) => formatCurrency(Number(v))} />
              <Line type="monotone" dataKey="value" stroke="#0033FF" strokeWidth={2} dot={{ r: 3 }} />
            </LineChart>
          </ResponsiveContainer>
        </div>

        <div className="rounded-lg border border-gray-200 bg-white p-5">
          <h4 className="mb-4 text-base font-semibold text-gray-900">Transaction Volume</h4>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={data.transactionVolumeTrend}>
              <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
              <XAxis dataKey="month" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} />
              <Tooltip />
              <Bar dataKey="count" fill="#110081" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="rounded-lg border border-gray-200 bg-white p-5">
          <h4 className="mb-4 text-base font-semibold text-gray-900">Transactions by Type</h4>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={data.transactionsByType}
                cx="50%"
                cy="50%"
                outerRadius={100}
                dataKey="count"
                nameKey="type"
                label={({ name, value }) => `${name}: ${value}`}
              >
                {data.transactionsByType.map((_, i) => (
                  <Cell key={i} fill={COLORS[i % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        </div>

        <div className="rounded-lg border border-gray-200 bg-white p-5">
          <h4 className="mb-4 text-base font-semibold text-gray-900">Top Holdings</h4>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={data.topHoldings.slice(0, 8)} layout="vertical">
              <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
              <XAxis type="number" tickFormatter={(v) => `$${(v / 1000).toFixed(0)}K`} tick={{ fontSize: 12 }} />
              <YAxis type="category" dataKey="fundId" width={60} tick={{ fontSize: 12 }} />
              <Tooltip formatter={(v) => formatCurrency(Number(v))} />
              <Bar dataKey="totalMarketValue" fill="#00874D" radius={[0, 4, 4, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
}

function PositionsTab() {
  const { data, error, isLoading } = useSWR<PositionReport>("/api/reports?type=positions", swrFetcher);

  if (isLoading) return <LoadingState message="Loading position report..." />;
  if (error) return <ErrorDisplay message={error.message} />;
  if (!data) return null;

  return (
    <div className="space-y-6">
      {data.portfolios.map((p) => (
        <div key={p.id} className="rounded-lg border border-gray-200 bg-white p-5">
          <h4 className="mb-3 text-base font-semibold text-gray-900">
            {p.clientName} <span className="font-mono text-sm text-gray-500">({p.accountNo})</span>
          </h4>
          {p.positions.length === 0 ? (
            <p className="text-sm text-gray-500">No positions</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200 text-sm">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-3 py-2 text-left text-xs font-medium uppercase text-gray-500">Fund</th>
                    <th className="px-3 py-2 text-right text-xs font-medium uppercase text-gray-500">Units</th>
                    <th className="px-3 py-2 text-right text-xs font-medium uppercase text-gray-500">Cost Basis</th>
                    <th className="px-3 py-2 text-right text-xs font-medium uppercase text-gray-500">Market Value</th>
                    <th className="px-3 py-2 text-right text-xs font-medium uppercase text-gray-500">G/L</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {p.positions.map((pos) => {
                    const gl = pos.marketValue - pos.costBasis;
                    return (
                      <tr key={pos.id}>
                        <td className="px-3 py-2 font-mono">{pos.fundId} — {pos.fundName}</td>
                        <td className="px-3 py-2 text-right">{pos.units.toLocaleString()}</td>
                        <td className="px-3 py-2 text-right">{formatCurrency(pos.costBasis)}</td>
                        <td className="px-3 py-2 text-right font-medium">{formatCurrency(pos.marketValue)}</td>
                        <td className={`px-3 py-2 text-right font-medium ${gl >= 0 ? "text-green-600" : "text-red-600"}`}>
                          {gl >= 0 ? "+" : ""}{formatCurrency(gl)}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      ))}
    </div>
  );
}

function AuditTab() {
  const { data, error, isLoading } = useSWR<AuditReport>("/api/reports?type=audit", swrFetcher);

  if (isLoading) return <LoadingState message="Loading audit report..." />;
  if (error) return <ErrorDisplay message={error.message} />;
  if (!data) return null;

  return (
    <div className="overflow-hidden rounded-lg border border-gray-200 bg-white">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Timestamp</th>
            <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Action</th>
            <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Key</th>
            <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Reason</th>
            <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Status</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-200">
          {data.logs.map((log) => (
            <tr key={log.id} className="hover:bg-gray-50">
              <td className="px-4 py-3 text-sm text-gray-500">{new Date(log.createdAt).toLocaleString()}</td>
              <td className="px-4 py-3 text-sm font-medium text-gray-900">{log.action}</td>
              <td className="px-4 py-3 text-sm font-mono text-gray-700">{log.key}</td>
              <td className="px-4 py-3 text-sm text-gray-500">{log.reason}</td>
              <td className="px-4 py-3"><StatusBadge status={log.status} /></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default function ReportsPage() {
  const [tab, setTab] = useState<"statistics" | "positions" | "audit">("statistics");

  const tabs = [
    { key: "statistics" as const, label: "Statistics" },
    { key: "positions" as const, label: "Position Report" },
    { key: "audit" as const, label: "Audit Report" },
  ];

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold text-gray-900">Reports</h2>

      <div className="border-b border-gray-200">
        <nav className="-mb-px flex space-x-8">
          {tabs.map((t) => (
            <button
              key={t.key}
              onClick={() => setTab(t.key)}
              className={`whitespace-nowrap border-b-2 px-1 py-3 text-sm font-medium ${
                tab === t.key
                  ? "border-blue-500 text-blue-600"
                  : "border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700"
              }`}
            >
              {t.label}
            </button>
          ))}
        </nav>
      </div>

      {tab === "statistics" && <StatisticsTab />}
      {tab === "positions" && <PositionsTab />}
      {tab === "audit" && <AuditTab />}
    </div>
  );
}

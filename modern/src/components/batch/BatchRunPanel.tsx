"use client";

import { useState } from "react";
import useSWR from "swr";
import { triggerBatch, swrFetcher } from "@/lib/api";
import type { BatchRun } from "@/types";
import { StatusBadge } from "@/components/ui/StatusBadge";
import toast from "react-hot-toast";

export function BatchRunPanel() {
  const { data: runs, mutate } = useSWR<BatchRun[]>("/api/batch", swrFetcher, {
    refreshInterval: 5000,
  });
  const [running, setRunning] = useState(false);

  async function handleTrigger() {
    setRunning(true);
    try {
      await triggerBatch();
      toast.success("Batch pipeline completed");
      mutate();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Batch run failed");
    } finally {
      setRunning(false);
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold text-gray-900">Batch Pipeline</h3>
        <button
          onClick={handleTrigger}
          disabled={running}
          className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {running ? (
            <span className="flex items-center gap-2">
              <span className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
              Running...
            </span>
          ) : (
            "Trigger Batch Run"
          )}
        </button>
      </div>

      <p className="text-sm text-gray-500">
        Recalculates portfolio total values based on current position market values.
      </p>

      {runs && runs.length > 0 && (
        <div className="overflow-hidden rounded-lg border border-gray-200">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Status</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Items</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Processed</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Errors</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Started</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 bg-white">
              {runs.map((run) => (
                <tr key={run.id}>
                  <td className="px-4 py-3"><StatusBadge status={run.status} /></td>
                  <td className="px-4 py-3 text-sm text-gray-900">{run.totalItems}</td>
                  <td className="px-4 py-3 text-sm text-gray-900">{run.processed}</td>
                  <td className="px-4 py-3 text-sm text-gray-900">{run.errors}</td>
                  <td className="px-4 py-3 text-sm text-gray-500">
                    {new Date(run.startedAt).toLocaleString()}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

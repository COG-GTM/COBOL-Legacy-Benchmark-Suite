import { useState, useMemo } from "react";
import { ChevronDown, ChevronUp, Filter } from "lucide-react";
import type { BatchRun, BatchStep } from "../../types";
import { batchRuns } from "../../mock/batchJobsData";
import PipelineStepCard from "./PipelineStepCard";

type SortField = "runDate" | "overallStatus" | "totalRecordsProcessed";
type SortDir = "asc" | "desc";

const statusBadge: Record<
  BatchRun["overallStatus"],
  { bg: string; text: string; label: string }
> = {
  complete: { bg: "bg-green-100", text: "text-green-800", label: "Complete" },
  running: { bg: "bg-blue-100", text: "text-blue-800", label: "Running" },
  failed: { bg: "bg-red-100", text: "text-red-800", label: "Failed" },
  scheduled: { bg: "bg-gray-100", text: "text-gray-800", label: "Scheduled" },
  partial: { bg: "bg-yellow-100", text: "text-yellow-800", label: "Partial" },
};

function computeDuration(start: string | null, end: string | null): string {
  if (!start || !end) return "--";
  const [sh, sm, ss] = start.split(":").map(Number);
  const [eh, em, es] = end.split(":").map(Number);
  const totalSec = eh * 3600 + em * 60 + es - (sh * 3600 + sm * 60 + ss);
  const mins = Math.floor(totalSec / 60);
  const secs = totalSec % 60;
  if (mins >= 60) {
    const hrs = Math.floor(mins / 60);
    return `${hrs}h ${mins % 60}m ${secs}s`;
  }
  return `${mins}m ${secs}s`;
}

function SortIcon({
  field,
  sortField,
  sortDir,
}: {
  field: SortField;
  sortField: SortField;
  sortDir: SortDir;
}) {
  if (field !== sortField) return null;
  return sortDir === "asc" ? (
    <ChevronUp size={12} className="inline" />
  ) : (
    <ChevronDown size={12} className="inline" />
  );
}

function ExpandedRunDetail({
  run,
}: {
  run: BatchRun;
}) {
  const [selectedStepId, setSelectedStepId] = useState<string | null>(null);

  const phases = ["start-of-day", "main-process", "end-of-day"] as const;
  const phaseLabels: Record<string, string> = {
    "start-of-day": "Start of Day",
    "main-process": "Main Process",
    "end-of-day": "End of Day",
  };

  const selectedStep: BatchStep | null = selectedStepId
    ? run.steps.find((s) => s.stepId === selectedStepId) ?? null
    : null;

  return (
    <div className="space-y-3 bg-gray-50 px-4 py-3">
      <div className="flex flex-wrap gap-1">
        {phases.map((phase) => {
          const steps = run.steps.filter((s) => s.phase === phase);
          if (steps.length === 0) return null;
          return (
            <div key={phase} className="flex items-center gap-1">
              <span className="mr-1 text-[9px] font-semibold uppercase text-gray-400">
                {phaseLabels[phase]}:
              </span>
              {steps.map((step) => (
                <PipelineStepCard
                  key={step.stepId}
                  step={step}
                  isSelected={selectedStepId === step.stepId}
                  onClick={() =>
                    setSelectedStepId(
                      selectedStepId === step.stepId ? null : step.stepId
                    )
                  }
                  compact
                />
              ))}
            </div>
          );
        })}
      </div>

      {selectedStep && (
        <div className="rounded-md border border-gray-200 bg-white p-3">
          <div className="grid grid-cols-2 gap-2 text-xs sm:grid-cols-4">
            <div>
              <span className="text-gray-500">Step:</span>
              <p className="font-mono font-medium text-gray-900">
                {selectedStep.stepId}
              </p>
            </div>
            <div>
              <span className="text-gray-500">Status:</span>
              <p className="font-medium text-gray-900">
                {selectedStep.status}
              </p>
            </div>
            <div>
              <span className="text-gray-500">Return Code:</span>
              <p className="font-mono font-medium text-gray-900">
                {selectedStep.returnCode !== null
                  ? `RC=${selectedStep.returnCode}`
                  : "--"}
              </p>
            </div>
            <div>
              <span className="text-gray-500">Records:</span>
              <p className="font-mono font-medium text-gray-900">
                {selectedStep.recordsProcessed !== null
                  ? `${selectedStep.recordsProcessed.toLocaleString()}/${selectedStep.recordsRead?.toLocaleString()}`
                  : "--"}
              </p>
            </div>
          </div>
          {selectedStep.errorMessage && (
            <div className="mt-2 rounded border border-red-200 bg-red-50 p-2">
              <p className="font-mono text-[10px] text-red-800">
                {selectedStep.errorMessage}
              </p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default function RunHistoryTab() {
  const [sortField, setSortField] = useState<SortField>("runDate");
  const [sortDir, setSortDir] = useState<SortDir>("desc");
  const [statusFilter, setStatusFilter] = useState<string>("all");
  const [expandedRunId, setExpandedRunId] = useState<string | null>(null);

  const toggleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDir(sortDir === "asc" ? "desc" : "asc");
    } else {
      setSortField(field);
      setSortDir("desc");
    }
  };

  const filtered = useMemo(() => {
    let runs = [...batchRuns];
    if (statusFilter !== "all") {
      runs = runs.filter((r) => r.overallStatus === statusFilter);
    }
    runs.sort((a, b) => {
      let cmp = 0;
      if (sortField === "runDate") {
        cmp = a.runDate.localeCompare(b.runDate);
      } else if (sortField === "overallStatus") {
        cmp = a.overallStatus.localeCompare(b.overallStatus);
      } else {
        cmp = a.totalRecordsProcessed - b.totalRecordsProcessed;
      }
      return sortDir === "asc" ? cmp : -cmp;
    });
    return runs;
  }, [sortField, sortDir, statusFilter]);

  return (
    <div className="space-y-4">
      {/* Filters */}
      <div className="flex items-center gap-3">
        <Filter size={14} className="text-gray-400" />
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="rounded-md border border-gray-300 bg-white px-2 py-1.5 text-xs font-medium text-gray-700"
          aria-label="Filter by status"
        >
          <option value="all">All Statuses</option>
          <option value="complete">Complete</option>
          <option value="failed">Failed</option>
          <option value="partial">Partial</option>
          <option value="running">Running</option>
          <option value="scheduled">Scheduled</option>
        </select>
      </div>

      {/* Run List Table */}
      <div className="rounded-lg border border-gray-200 bg-white shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-gray-200 bg-gray-50">
                <th
                  scope="col"
                  className="cursor-pointer px-3 py-2 font-semibold text-gray-600 select-none"
                  onClick={() => toggleSort("runDate")}
                  aria-sort={
                    sortField === "runDate"
                      ? sortDir === "asc"
                        ? "ascending"
                        : "descending"
                      : undefined
                  }
                >
                  Run ID / Date{" "}
                  <SortIcon
                    field="runDate"
                    sortField={sortField}
                    sortDir={sortDir}
                  />
                </th>
                <th scope="col" className="px-3 py-2 font-semibold text-gray-600">
                  Scheduled Start
                </th>
                <th scope="col" className="px-3 py-2 font-semibold text-gray-600">
                  Actual Start
                </th>
                <th scope="col" className="px-3 py-2 font-semibold text-gray-600">
                  Actual End
                </th>
                <th scope="col" className="px-3 py-2 font-semibold text-gray-600">
                  Duration
                </th>
                <th
                  scope="col"
                  className="cursor-pointer px-3 py-2 font-semibold text-gray-600 select-none"
                  onClick={() => toggleSort("overallStatus")}
                  aria-sort={
                    sortField === "overallStatus"
                      ? sortDir === "asc"
                        ? "ascending"
                        : "descending"
                      : undefined
                  }
                >
                  Status{" "}
                  <SortIcon
                    field="overallStatus"
                    sortField={sortField}
                    sortDir={sortDir}
                  />
                </th>
                <th
                  scope="col"
                  className="cursor-pointer px-3 py-2 font-semibold text-gray-600 select-none"
                  onClick={() => toggleSort("totalRecordsProcessed")}
                  aria-sort={
                    sortField === "totalRecordsProcessed"
                      ? sortDir === "asc"
                        ? "ascending"
                        : "descending"
                      : undefined
                  }
                >
                  Records{" "}
                  <SortIcon
                    field="totalRecordsProcessed"
                    sortField={sortField}
                    sortDir={sortDir}
                  />
                </th>
                <th scope="col" className="px-3 py-2 font-semibold text-gray-600">
                  Errors
                </th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((run) => {
                const isExpanded = expandedRunId === run.runId;
                const badge = statusBadge[run.overallStatus];
                return (
                  <tbody key={run.runId}>
                    <tr
                      onClick={() =>
                        setExpandedRunId(isExpanded ? null : run.runId)
                      }
                      className="cursor-pointer border-b border-gray-100 transition-colors hover:bg-blue-50"
                    >
                      <td className="px-3 py-2">
                        <div className="flex items-center gap-1.5">
                          {isExpanded ? (
                            <ChevronUp size={12} className="text-gray-400" />
                          ) : (
                            <ChevronDown size={12} className="text-gray-400" />
                          )}
                          <div>
                            <span className="font-mono font-medium text-gray-900">
                              {run.runId}
                            </span>
                            <span className="ml-2 text-gray-500">
                              {run.runDate}
                            </span>
                          </div>
                        </div>
                      </td>
                      <td className="px-3 py-2 font-mono text-gray-700">
                        {run.scheduledStart}
                      </td>
                      <td className="px-3 py-2 font-mono text-gray-700">
                        {run.actualStart ?? "--"}
                      </td>
                      <td className="px-3 py-2 font-mono text-gray-700">
                        {run.actualEnd ?? "--"}
                      </td>
                      <td className="px-3 py-2 font-mono text-gray-700">
                        {computeDuration(run.actualStart, run.actualEnd)}
                      </td>
                      <td className="px-3 py-2">
                        <span
                          className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${badge.bg} ${badge.text}`}
                        >
                          {badge.label}
                        </span>
                      </td>
                      <td className="px-3 py-2 font-mono text-gray-700">
                        {run.totalRecordsProcessed.toLocaleString()}
                      </td>
                      <td className="px-3 py-2 font-mono">
                        <span
                          className={
                            run.totalErrors > 0
                              ? "text-red-600"
                              : "text-gray-700"
                          }
                        >
                          {run.totalErrors}
                        </span>
                      </td>
                    </tr>
                    {isExpanded && (
                      <tr>
                        <td colSpan={8} className="p-0">
                          <ExpandedRunDetail run={run} />
                        </td>
                      </tr>
                    )}
                  </tbody>
                );
              })}
              {filtered.length === 0 && (
                <tr>
                  <td
                    colSpan={8}
                    className="px-3 py-8 text-center text-sm text-gray-400"
                  >
                    No runs match the selected filter.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

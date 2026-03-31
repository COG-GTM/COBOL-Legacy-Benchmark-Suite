import {
  CheckCircle2,
  Loader2,
  Clock,
  XCircle,
  SkipForward,
} from "lucide-react";
import type { BatchStep } from "../../types";

interface PipelineStepCardProps {
  step: BatchStep;
  isSelected: boolean;
  onClick: () => void;
  compact?: boolean;
}

const statusConfig: Record<
  BatchStep["status"],
  { icon: typeof CheckCircle2; color: string; border: string; label: string }
> = {
  complete: {
    icon: CheckCircle2,
    color: "text-green-600",
    border: "border-green-500",
    label: "Complete",
  },
  running: {
    icon: Loader2,
    color: "text-blue-600",
    border: "border-blue-500",
    label: "Running",
  },
  pending: {
    icon: Clock,
    color: "text-gray-400",
    border: "border-gray-300",
    label: "Pending",
  },
  waiting: {
    icon: Clock,
    color: "text-yellow-500",
    border: "border-yellow-400",
    label: "Waiting",
  },
  failed: {
    icon: XCircle,
    color: "text-red-600",
    border: "border-red-500",
    label: "Failed",
  },
  skipped: {
    icon: SkipForward,
    color: "text-gray-400",
    border: "border-gray-400",
    label: "Skipped",
  },
};

function rcBadge(rc: number | null) {
  if (rc === null) return null;
  let bg = "bg-gray-100 text-gray-600";
  if (rc === 0) bg = "bg-green-100 text-green-800";
  else if (rc <= 4) bg = "bg-yellow-100 text-yellow-800";
  else bg = "bg-red-100 text-red-800";
  return (
    <span className={`rounded-full px-2 py-0.5 text-xs font-mono ${bg}`}>
      RC={rc}
    </span>
  );
}

export default function PipelineStepCard({
  step,
  isSelected,
  onClick,
  compact = false,
}: PipelineStepCardProps) {
  const cfg = statusConfig[step.status];
  const Icon = cfg.icon;
  const isRunning = step.status === "running";

  if (compact) {
    return (
      <button
        onClick={onClick}
        role="button"
        aria-label={`${step.stepName} - ${cfg.label}${step.returnCode !== null ? `, Return Code ${step.returnCode}` : ""}`}
        className={`flex items-center gap-2 rounded-md border px-2 py-1.5 text-left text-xs transition-colors ${cfg.border} ${
          isSelected ? "ring-2 ring-blue-400" : ""
        } bg-white hover:bg-gray-50`}
      >
        <Icon
          size={14}
          className={`${cfg.color} shrink-0 ${isRunning ? "animate-spin" : ""}`}
        />
        <span className="font-mono font-medium text-gray-900">
          {step.stepId}
        </span>
        {rcBadge(step.returnCode)}
      </button>
    );
  }

  return (
    <button
      onClick={onClick}
      role="button"
      aria-label={`${step.stepName} - ${cfg.label}${step.returnCode !== null ? `, Return Code ${step.returnCode}` : ""}`}
      className={`flex min-w-[160px] flex-col rounded-lg border-2 bg-white p-3 text-left transition-all ${cfg.border} ${
        isSelected ? "ring-2 ring-blue-400 shadow-md" : "shadow-sm"
      } ${isRunning ? "animate-pulse" : ""} hover:shadow-md focus:outline-none focus:ring-2 focus:ring-blue-400`}
    >
      <div className="mb-1 flex items-center gap-2">
        <Icon
          size={16}
          className={`${cfg.color} shrink-0 ${isRunning ? "animate-spin" : ""}`}
        />
        <span className="font-mono text-xs font-bold text-gray-900">
          {step.stepId}
        </span>
        {rcBadge(step.returnCode)}
      </div>

      <span className="mb-1.5 text-xs font-medium text-gray-700">
        {step.stepName}
      </span>

      <span className="font-mono text-[10px] text-gray-500">
        {step.actualStart && step.actualEnd
          ? `${step.actualStart} - ${step.actualEnd}`
          : step.actualStart
            ? `${step.actualStart} - ...`
            : "Pending"}
      </span>

      {step.recordsProcessed !== null && step.recordsRead !== null && step.recordsRead > 0 && (
        <span className="mt-1 font-mono text-[10px] text-gray-500">
          {step.recordsProcessed.toLocaleString()}/{step.recordsRead.toLocaleString()} records
        </span>
      )}
    </button>
  );
}

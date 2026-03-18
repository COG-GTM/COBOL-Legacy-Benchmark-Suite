import {
  CheckCircle,
  Loader2,
  Clock,
  XCircle,
  PauseCircle,
  Calendar,
} from "lucide-react";
import type { BatchPipelineStep } from "../../types";

interface BatchPipelineStatusProps {
  steps: BatchPipelineStep[];
  lastRun: string;
  nextScheduled: string;
}

const statusConfig = {
  complete: {
    icon: CheckCircle,
    color: "text-green-500",
    bg: "bg-green-50 border-green-200",
    label: "Complete",
  },
  running: {
    icon: Loader2,
    color: "text-blue-500",
    bg: "bg-blue-50 border-blue-200",
    label: "Running",
  },
  pending: {
    icon: Clock,
    color: "text-gray-400",
    bg: "bg-gray-50 border-gray-200",
    label: "Pending",
  },
  error: {
    icon: XCircle,
    color: "text-red-500",
    bg: "bg-red-50 border-red-200",
    label: "Error",
  },
  suspended: {
    icon: PauseCircle,
    color: "text-yellow-500",
    bg: "bg-yellow-50 border-yellow-200",
    label: "Suspended",
  },
};

function formatTime(time: string | null) {
  if (!time) return "—";
  return time.split(" ")[1] || time;
}

export default function BatchPipelineStatus({
  steps,
  lastRun,
  nextScheduled,
}: BatchPipelineStatusProps) {
  return (
    <div className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
      <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <h3 className="text-sm font-semibold text-gray-700">
          Batch Pipeline Status
        </h3>
        <div className="flex flex-wrap gap-3 text-xs text-gray-500">
          <span className="flex items-center gap-1">
            <Calendar size={12} />
            Last Run: <strong>{lastRun}</strong>
          </span>
          <span className="flex items-center gap-1">
            <Clock size={12} />
            Next: <strong>{nextScheduled}</strong>
          </span>
        </div>
      </div>

      {/* Desktop: horizontal pipeline */}
      <div className="hidden overflow-x-auto md:block">
        <div className="flex items-start gap-0 min-w-max">
          {steps.map((step, idx) => {
            const config = statusConfig[step.status];
            const Icon = config.icon;
            return (
              <div key={step.name} className="flex items-start">
                <div className="flex flex-col items-center" style={{ width: "130px" }}>
                  <div
                    className={`flex h-10 w-10 items-center justify-center rounded-full border-2 ${config.bg}`}
                    aria-label={`${step.name}: ${config.label}`}
                  >
                    <Icon
                      size={20}
                      className={`${config.color} ${step.status === "running" ? "animate-spin" : ""}`}
                    />
                  </div>
                  <p className="mt-2 text-center text-xs font-medium text-gray-700 leading-tight">
                    {step.name}
                  </p>
                  {step.startTime && (
                    <p className="mt-0.5 text-[10px] text-gray-400">
                      {formatTime(step.startTime)}
                    </p>
                  )}
                  {step.recordsProcessed !== null && (
                    <p className="text-[10px] text-gray-400">
                      {step.recordsProcessed.toLocaleString()} records
                    </p>
                  )}
                </div>
                {idx < steps.length - 1 && (
                  <div className="mt-5 flex items-center">
                    <div
                      className={`h-0.5 w-6 ${
                        step.status === "complete"
                          ? "bg-green-300"
                          : "bg-gray-200"
                      }`}
                    />
                    <div
                      className={`h-0 w-0 border-y-4 border-l-4 border-y-transparent ${
                        step.status === "complete"
                          ? "border-l-green-300"
                          : "border-l-gray-200"
                      }`}
                    />
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Mobile: vertical timeline */}
      <div className="md:hidden">
        <div className="space-y-3">
          {steps.map((step) => {
            const config = statusConfig[step.status];
            const Icon = config.icon;
            return (
              <div
                key={step.name}
                className={`flex items-center gap-3 rounded-md border p-3 ${config.bg}`}
              >
                <Icon
                  size={18}
                  className={`shrink-0 ${config.color} ${step.status === "running" ? "animate-spin" : ""}`}
                />
                <div className="min-w-0 flex-1">
                  <p className="text-xs font-medium text-gray-700">
                    {step.name}
                  </p>
                  <div className="flex flex-wrap gap-x-3 text-[10px] text-gray-500">
                    {step.startTime && (
                      <span>{formatTime(step.startTime)}</span>
                    )}
                    {step.recordsProcessed !== null && (
                      <span>
                        {step.recordsProcessed.toLocaleString()} records
                      </span>
                    )}
                    {step.returnCode !== null && (
                      <span>RC={step.returnCode}</span>
                    )}
                  </div>
                </div>
                <span
                  className={`shrink-0 text-[10px] font-medium ${config.color}`}
                >
                  {config.label}
                </span>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

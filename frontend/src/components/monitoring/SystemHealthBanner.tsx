import { Shield, CheckCircle, XCircle } from "lucide-react";
import type { SystemHealth } from "../../types";

interface SystemHealthBannerProps {
  health: SystemHealth;
}

const statusConfig = {
  healthy: {
    bg: "bg-green-50 border-green-200",
    dot: "bg-green-500",
    text: "text-green-800",
    label: "All Systems Operational",
  },
  degraded: {
    bg: "bg-yellow-50 border-yellow-200",
    dot: "bg-yellow-500",
    text: "text-yellow-800",
    label: "Degraded Performance",
  },
  critical: {
    bg: "bg-red-50 border-red-200",
    dot: "bg-red-500",
    text: "text-red-800",
    label: "Critical Issues Detected",
  },
};

const subsystems = [
  { key: "cicsRegionStatus" as const, label: "CICS Region" },
  { key: "db2Status" as const, label: "DB2" },
  { key: "vsamStatus" as const, label: "VSAM" },
  { key: "mqStatus" as const, label: "MQ" },
];

export default function SystemHealthBanner({ health }: SystemHealthBannerProps) {
  const config = statusConfig[health.overallStatus];

  return (
    <div
      className={`rounded-lg border p-4 ${config.bg}`}
      role="status"
      aria-label={`System status: ${config.label}`}
    >
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <Shield size={24} className={config.text} />
          <div>
            <div className="flex items-center gap-2">
              <span
                className={`inline-block h-3 w-3 rounded-full ${config.dot}`}
                aria-hidden="true"
              />
              <h2 className={`text-lg font-semibold ${config.text}`}>
                {config.label}
              </h2>
            </div>
            <div className="mt-1 flex flex-wrap gap-x-4 gap-y-1 text-sm text-gray-600">
              <span>
                Uptime: <strong>{health.uptime}</strong>
              </span>
              <span>
                Last Incident: <strong>{health.lastIncident}</strong>
              </span>
            </div>
          </div>
        </div>

        <div className="flex flex-wrap gap-2">
          {subsystems.map((sub) => {
            const isActive = health[sub.key] === "active";
            return (
              <div
                key={sub.key}
                className={`flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-medium ${
                  isActive
                    ? "bg-green-100 text-green-800"
                    : "bg-red-100 text-red-800"
                }`}
              >
                {isActive ? (
                  <CheckCircle size={12} aria-hidden="true" />
                ) : (
                  <XCircle size={12} aria-hidden="true" />
                )}
                <span>{sub.label}</span>
                <span>{isActive ? "Active" : "Inactive"}</span>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

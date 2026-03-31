import { NavLink, Outlet, useLocation, Navigate } from "react-router-dom";
import { Calendar, Clock } from "lucide-react";
import { batchRuns } from "../../mock/batchJobsData";

const tabs = [
  { label: "Pipeline Status", path: "/batch-jobs/status" },
  { label: "Pipeline Definition", path: "/batch-jobs/definition" },
  { label: "Run History", path: "/batch-jobs/history" },
];

export default function BatchJobsPage() {
  const location = useLocation();

  // Redirect bare /batch-jobs to /batch-jobs/status
  if (location.pathname === "/batch-jobs") {
    return <Navigate to="/batch-jobs/status" replace />;
  }

  const lastComplete = batchRuns.find((r) => r.overallStatus === "complete");

  return (
    <div className="space-y-6">
      {/* Page header */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            Batch Job Monitoring
          </h1>
          <div className="mt-1 flex flex-wrap items-center gap-4 text-xs text-gray-500">
            <span className="flex items-center gap-1">
              <Calendar size={12} />
              Next Scheduled Run: 2024-01-16 18:00:00
            </span>
            {lastComplete && (
              <span className="flex items-center gap-1">
                <Clock size={12} />
                Last Completed: {lastComplete.runDate}{" "}
                {lastComplete.actualEnd} ({lastComplete.runId})
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Tab navigation */}
      <nav className="flex gap-1 border-b border-gray-200" aria-label="Batch job tabs">
        {tabs.map((tab) => (
          <NavLink
            key={tab.path}
            to={tab.path}
            className={({ isActive }) =>
              `px-4 py-2 text-sm font-medium transition-colors ${
                isActive
                  ? "border-b-2 border-blue-600 text-blue-600"
                  : "text-gray-500 hover:text-gray-700"
              }`
            }
          >
            {tab.label}
          </NavLink>
        ))}
      </nav>

      {/* Tab content */}
      <Outlet />
    </div>
  );
}

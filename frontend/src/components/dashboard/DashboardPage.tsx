import { useState } from "react";
import SummaryCards from "./SummaryCards";
import QuickActions from "./QuickActions";
import RecentActivity from "./RecentActivity";
import ErrorBanner from "../common/ErrorBanner";
import { systemStatus } from "../../mock/dashboardData";

export default function DashboardPage() {
  const [banner, setBanner] = useState<{
    message: string;
    severity: "error" | "warning" | "info" | "success";
  } | null>(null);

  const currentDate = new Date().toLocaleDateString("en-US", {
    weekday: "long",
    year: "numeric",
    month: "long",
    day: "numeric",
  });

  return (
    <div className="space-y-6">
      {banner && (
        <ErrorBanner
          message={banner.message}
          severity={banner.severity}
          onDismiss={() => setBanner(null)}
        />
      )}

      <div className="flex flex-col justify-between gap-2 sm:flex-row sm:items-center">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            Portfolio Management System
          </h1>
          <p className="text-sm text-gray-500">
            {currentDate} &middot; Welcome, <span className="font-medium">USER001</span>
          </p>
        </div>
        <div className="flex items-center gap-2 text-sm text-gray-500">
          <span className="inline-flex items-center gap-1.5 rounded-full bg-green-50 px-3 py-1 text-green-700">
            <span className="h-2 w-2 rounded-full bg-green-500" />
            System Operational
          </span>
          <span className="text-xs text-gray-400">
            Last batch: {systemStatus.lastBatchRun}
          </span>
        </div>
      </div>

      <SummaryCards />
      <QuickActions />
      <RecentActivity />
    </div>
  );
}

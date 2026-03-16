export interface SummaryMetrics {
  totalAccounts: number;
  totalPositions: number;
  todayTransactions: number;
  systemStatus: string;
}

export interface Transaction {
  date: string;
  account: string;
  type: "BUY" | "SELL" | "XFER" | "FEE";
  fund: string;
  units: number;
  price: number;
  amount: number;
}

export interface SystemStatus {
  status: "operational" | "degraded" | "down";
  lastBatchRun: string;
}

export interface ErrorBannerProps {
  message: string;
  severity: "error" | "warning" | "info" | "success";
  onDismiss: () => void;
}

export interface NavItem {
  label: string;
  path: string;
  icon: string;
  badge?: string;
}

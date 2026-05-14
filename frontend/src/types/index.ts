export interface Position {
  id: number;
  portfolio_id: string;
  investment_id: string;
  investment_name: string;
  position_date: string;
  quantity: number;
  cost_basis: number;
  market_value: number;
  gain_loss: number;
  gain_loss_pct: number;
  currency_code: string;
  status: string;
  last_maint_date: string;
}

export interface Transaction {
  id: number;
  transaction_id: string;
  portfolio_id: string;
  investment_id: string;
  transaction_date: string;
  transaction_type: string;
  quantity: number;
  price: number;
  amount: number;
  currency_code: string;
  status: string;
  process_date: string;
}

export interface PortfolioSummary {
  id: number;
  portfolio_id: string;
  account_no: string;
  client_name: string;
  client_type: string;
  currency_code: string;
  risk_level: string;
  status: string;
  total_value: number;
  cash_balance: number;
  open_date: string;
  close_date: string | null;
  last_maint_date: string;
  position_count: number;
  total_market_value: number;
  total_cost_basis: number;
  total_gain_loss: number;
  total_gain_loss_pct: number;
}

export interface PortfolioDetail extends PortfolioSummary {
  positions: Position[];
  recent_transactions: Transaction[];
}

export interface DashboardStats {
  total_portfolios: number;
  active_portfolios: number;
  total_market_value: number;
  total_cost_basis: number;
  total_gain_loss: number;
  total_gain_loss_pct: number;
  total_positions: number;
  total_transactions: number;
  recent_transactions: Transaction[];
  portfolio_breakdown: { name: string; value: number }[];
  status_breakdown: { name: string; value: number }[];
  top_performers: {
    investment_id: string;
    investment_name: string;
    market_value: number;
    gain_loss: number;
    gain_loss_pct: number;
  }[];
}

export const TXN_TYPE_LABELS: Record<string, string> = {
  BU: "Buy",
  SL: "Sell",
  TR: "Transfer",
  FE: "Fee",
};

export const STATUS_LABELS: Record<string, string> = {
  A: "Active",
  C: "Closed",
  S: "Suspended",
  D: "Done",
  P: "Pending",
  F: "Failed",
  R: "Reversed",
};

export const CLIENT_TYPE_LABELS: Record<string, string> = {
  I: "Individual",
  C: "Corporate",
  T: "Trust",
};

export const RISK_LABELS: Record<string, string> = {
  L: "Low",
  M: "Medium",
  H: "High",
  A: "Aggressive",
};

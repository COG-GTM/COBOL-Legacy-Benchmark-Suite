export interface Portfolio {
  portfolio_id: string;
  account_number: string;
  client_name: string;
  client_type: string;
  portfolio_name: string;
  currency_code: string;
  risk_level: string;
  status: string;
  total_value: number;
  cash_balance: number;
  open_date: string;
  close_date: string | null;
  last_maint_user: string;
  created_at: string;
  updated_at: string;
}

export interface PositionSummary {
  investment_id: string;
  symbol: string;
  name: string;
  quantity: number;
  cost_basis: number;
  current_price: number;
  market_value: number;
  gain_loss: number;
  gain_loss_percent: number;
  status: string;
}

export interface PortfolioDetail extends Portfolio {
  positions: PositionSummary[];
  total_gain_loss: number;
  total_gain_loss_percent: number;
  position_count: number;
}

export interface Transaction {
  transaction_id: string;
  portfolio_id: string;
  investment_id: string;
  transaction_date: string;
  transaction_time: string;
  sequence_no: string;
  transaction_type: string;
  quantity: number;
  price: number;
  amount: number;
  currency: string;
  status: string;
  process_date: string | null;
  process_user: string;
  created_at: string;
}

export interface PositionReport {
  report_date: string;
  report_type: string;
  total_portfolios: number;
  total_positions: number;
  total_market_value: number;
  total_cost_basis: number;
  total_gain_loss: number;
  items: PositionReportItem[];
}

export interface PositionReportItem {
  portfolio_id: string;
  portfolio_name: string;
  investment_id: string;
  symbol: string;
  name: string;
  quantity: number;
  cost_basis: number;
  market_value: number;
  gain_loss: number;
  gain_loss_percent: number;
}

export interface Statistics {
  report_date: string;
  total_portfolios: number;
  active_portfolios: number;
  total_positions: number;
  total_transactions: number;
  transactions_today: number;
  total_market_value: number;
  total_gain_loss: number;
  avg_portfolio_value: number;
  system_health: string;
}

export interface AuditReport {
  report_date: string;
  report_type: string;
  total_entries: number;
  error_count: number;
  warning_count: number;
  entries: AuditEntry[];
}

export interface AuditEntry {
  timestamp: string;
  program_id: string;
  error_code: string;
  account_number: string | null;
  portfolio_id: string | null;
  description: string;
  severity: string;
}

export const TXN_TYPE_LABELS: Record<string, string> = {
  BU: 'Buy',
  SL: 'Sell',
  TR: 'Transfer',
  FE: 'Fee',
};

export const STATUS_LABELS: Record<string, string> = {
  P: 'Pending',
  D: 'Done',
  F: 'Failed',
  R: 'Reversed',
  A: 'Active',
  C: 'Closed',
  S: 'Suspended',
};

export const RISK_LABELS: Record<string, string> = {
  L: 'Low',
  M: 'Medium',
  H: 'High',
};

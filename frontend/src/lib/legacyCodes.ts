import type { ClientType, PortfolioStatus } from '@/types/portfolio'
import type { TransactionStatus, TransactionType } from '@/types/transaction'

export const CLIENT_TYPE_LABELS: Record<ClientType, string> = {
  I: 'Individual',
  C: 'Corporate',
  T: 'Trust',
}

export const PORTFOLIO_STATUS_LABELS: Record<PortfolioStatus, string> = {
  A: 'Active',
  C: 'Closed',
  S: 'Suspended',
}

export const TRANSACTION_TYPE_LABELS: Record<TransactionType, string> = {
  BU: 'Buy',
  SL: 'Sell',
  TR: 'Transfer',
  FE: 'Fee',
}

export const TRANSACTION_STATUS_LABELS: Record<TransactionStatus, string> = {
  P: 'Pending',
  D: 'Done',
  F: 'Failed',
  R: 'Reversed',
}

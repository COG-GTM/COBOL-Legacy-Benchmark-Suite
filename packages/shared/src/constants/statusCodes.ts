export const PORTFOLIO_STATUS = {
  A: { code: 'A', label: 'Active' },
  C: { code: 'C', label: 'Closed' },
  S: { code: 'S', label: 'Suspended' },
} as const;

export const TRANSACTION_STATUS = {
  P: { code: 'P', label: 'Pending' },
  D: { code: 'D', label: 'Done' },
  F: { code: 'F', label: 'Failed' },
  R: { code: 'R', label: 'Reversed' },
} as const;

export const BATCH_STATUS = {
  P: { code: 'P', label: 'Pending' },
  R: { code: 'R', label: 'Running' },
  C: { code: 'C', label: 'Complete' },
  F: { code: 'F', label: 'Failed' },
  A: { code: 'A', label: 'Aborted' },
} as const;

export type PortfolioStatusCode = keyof typeof PORTFOLIO_STATUS;
export type TransactionStatusCode = keyof typeof TRANSACTION_STATUS;
export type BatchStatusCode = keyof typeof BATCH_STATUS;

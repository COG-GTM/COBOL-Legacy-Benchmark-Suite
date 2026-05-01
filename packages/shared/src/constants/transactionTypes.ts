export const TRANSACTION_TYPES = {
  BU: { code: 'BU', label: 'Buy' },
  SL: { code: 'SL', label: 'Sell' },
  TR: { code: 'TR', label: 'Transfer' },
  FE: { code: 'FE', label: 'Fee' },
} as const;

export type TransactionTypeCode = keyof typeof TRANSACTION_TYPES;

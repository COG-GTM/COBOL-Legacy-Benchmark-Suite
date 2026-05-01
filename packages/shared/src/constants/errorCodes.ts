export const ERROR_CODES = {
  E001: { code: 'E001', message: 'Invalid Account', severity: 'E' as const },
  E002: { code: 'E002', message: 'Invalid Fund', severity: 'E' as const },
  E003: { code: 'E003', message: 'Invalid Date', severity: 'E' as const },
  E004: { code: 'E004', message: 'Invalid Amount', severity: 'E' as const },
  E005: { code: 'E005', message: 'Insufficient Shares', severity: 'E' as const },
  E006: { code: 'E006', message: 'Portfolio Not Found', severity: 'E' as const },
  E007: { code: 'E007', message: 'Portfolio Closed', severity: 'E' as const },
  E008: { code: 'E008', message: 'Duplicate Portfolio', severity: 'E' as const },
  E009: { code: 'E009', message: 'Database Error', severity: 'S' as const },
  E010: { code: 'E010', message: 'Authorization Failed', severity: 'E' as const },
  W001: { code: 'W001', message: 'Duplicate Transaction', severity: 'W' as const },
  W002: { code: 'W002', message: 'High Value Transaction', severity: 'W' as const },
  I001: { code: 'I001', message: 'Record Created', severity: 'I' as const },
  I002: { code: 'I002', message: 'Record Updated', severity: 'I' as const },
  I003: { code: 'I003', message: 'Record Deleted', severity: 'I' as const },
} as const;

export type ErrorCode = keyof typeof ERROR_CODES;

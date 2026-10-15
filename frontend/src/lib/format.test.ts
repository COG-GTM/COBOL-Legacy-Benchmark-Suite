import { describe, expect, it } from 'vitest'

import { formatCurrency, formatDate, formatQuantity } from './format'

describe('format', () => {
  it('formats money with exactly two decimals', () => {
    expect(formatCurrency(1234.5)).toBe('$1,234.50')
    expect(formatCurrency(78410, 'GBP')).toBe('£78,410.00')
  })

  it('formats quantities with exactly four decimals', () => {
    expect(formatQuantity(150.25)).toBe('150.2500')
  })

  it('formats ISO dates without a time zone shift', () => {
    expect(formatDate('2026-10-01')).toBe('Oct 01, 2026')
  })
})

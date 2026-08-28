import { describe, expect, it } from 'vitest'
import {
  validateAccountNumber,
  validateAmount,
  validateInvestmentType,
  validatePortfolioId,
} from './portvald'

describe('PORTVALD rules', () => {
  it('accepts PORT followed by four digits', () => {
    expect(validatePortfolioId('PORT0001')).toEqual({ code: 0, message: '' })
    expect(validatePortfolioId('PORT01')).toEqual({ code: 1, message: 'Invalid Portfolio ID format' })
  })
  it('validates ten digit accounts and rejects zero', () => {
    expect(validateAccountNumber('1234567890').code).toBe(0)
    expect(validateAccountNumber('0000000000')).toEqual({ code: 2, message: 'Invalid Account Number format' })
    expect(validateAccountNumber('123')).toEqual({ code: 2, message: 'Invalid Account Number format' })
  })
  it('only permits the four investment types', () => {
    expect(validateInvestmentType('ETF').code).toBe(0)
    expect(validateInvestmentType('MUT').message).toBe('Invalid Investment Type')
  })
  it('enforces the signed COBOL amount boundaries', () => {
    expect(validateAmount(9999999999999.99).code).toBe(0)
    expect(validateAmount(-9999999999999.99).code).toBe(0)
    expect(validateAmount(10000000000000).code).toBe(4)
  })
})

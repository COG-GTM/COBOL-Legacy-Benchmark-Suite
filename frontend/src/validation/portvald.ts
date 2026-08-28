import type { InvestmentType } from '../api/types'

export interface ValidationResult {
  code: number
  message: string
}

const valid = (): ValidationResult => ({ code: 0, message: '' })

export function validatePortfolioId(value: string): ValidationResult {
  return /^PORT\d{4}$/.test(value) ? valid() : { code: 1, message: 'Invalid Portfolio ID format' }
}

export function validateAccountNumber(value: string): ValidationResult {
  return /^\d{10}$/.test(value) && !/^0+$/.test(value)
    ? valid()
    : { code: 2, message: 'Invalid Account Number format' }
}

export function validateInvestmentType(value: string): ValidationResult {
  return (['STK', 'BND', 'MMF', 'ETF'] as InvestmentType[]).includes(value as InvestmentType)
    ? valid()
    : { code: 3, message: 'Invalid Investment Type' }
}

export function validateAmount(value: number): ValidationResult {
  return Number.isFinite(value) && value >= -9999999999999.99 && value <= 9999999999999.99
    ? valid()
    : { code: 4, message: 'Amount outside valid range' }
}

export function validatePortfolioFields(fields: {
  portfolioId?: string
  accountNo?: string
  investmentType?: string
  amount?: number
}): ValidationResult {
  if (fields.portfolioId !== undefined) {
    const result = validatePortfolioId(fields.portfolioId)
    if (result.code) return result
  }
  if (fields.accountNo !== undefined) {
    const result = validateAccountNumber(fields.accountNo)
    if (result.code) return result
  }
  if (fields.investmentType !== undefined) {
    const result = validateInvestmentType(fields.investmentType)
    if (result.code) return result
  }
  if (fields.amount !== undefined) return validateAmount(fields.amount)
  return valid()
}

/**
 * Data Validation Utilities
 * Migrated from: TRNVAL00 batch program, PORTVAL.cpy, data-dictionary.md
 * 
 * Provides validation functions for all data entities based on COBOL validation rules.
 */

import { TransactionType, TransactionStatus, PositionStatus, PortfolioStatus } from '../types';
import { CreateTransactionRequest, TransactionValidationResult } from '../models/Transaction';
import { CreatePortfolioRequest } from '../models/Portfolio';
import { CreatePositionRequest } from '../models/Position';

/**
 * Validation error codes
 * From: data-dictionary.md Error Codes section
 */
export const ValidationErrorCodes = {
  E001: 'E001', // Invalid Account Number
  E002: 'E002', // Invalid Fund ID
  E003: 'E003', // Invalid Transaction Type
  E004: 'E004', // Insufficient Position Balance
  W001: 'W001', // Zero Dollar Transaction
  W002: 'W002', // Duplicate Transaction ID
} as const;

/**
 * Validation error messages
 */
export const ValidationErrorMessages: Record<string, string> = {
  E001: 'Invalid Account Number',
  E002: 'Invalid Fund ID',
  E003: 'Invalid Transaction Type',
  E004: 'Insufficient Position Balance',
  W001: 'Zero Dollar Transaction',
  W002: 'Duplicate Transaction ID',
};

/**
 * Validation result interface
 */
export interface ValidationResult {
  isValid: boolean;
  errors: ValidationError[];
  warnings: ValidationWarning[];
}

/**
 * Validation error interface
 */
export interface ValidationError {
  code: string;
  field: string;
  message: string;
}

/**
 * Validation warning interface
 */
export interface ValidationWarning {
  code: string;
  field: string;
  message: string;
}

/**
 * Account number validation
 * From: data-dictionary.md - Account Number must be numeric and 9 digits, range 100000000-999999999
 */
export function validateAccountNumber(accountNumber: string): ValidationResult {
  const errors: ValidationError[] = [];
  const warnings: ValidationWarning[] = [];

  if (!accountNumber) {
    errors.push({
      code: ValidationErrorCodes.E001,
      field: 'accountNumber',
      message: 'Account number is required',
    });
    return { isValid: false, errors, warnings };
  }

  // Check if numeric
  if (!/^\d+$/.test(accountNumber)) {
    errors.push({
      code: ValidationErrorCodes.E001,
      field: 'accountNumber',
      message: 'Account number must be numeric',
    });
    return { isValid: false, errors, warnings };
  }

  // Check length (9 digits)
  if (accountNumber.length !== 9) {
    errors.push({
      code: ValidationErrorCodes.E001,
      field: 'accountNumber',
      message: 'Account number must be 9 digits',
    });
    return { isValid: false, errors, warnings };
  }

  // Check range
  const accountNum = parseInt(accountNumber, 10);
  if (accountNum < 100000000 || accountNum > 999999999) {
    errors.push({
      code: ValidationErrorCodes.E001,
      field: 'accountNumber',
      message: 'Account number must be between 100000000 and 999999999',
    });
    return { isValid: false, errors, warnings };
  }

  return { isValid: true, errors, warnings };
}

/**
 * Fund ID validation
 * From: data-dictionary.md - Fund ID must be 6 alphanumeric characters
 */
export function validateFundId(fundId: string): ValidationResult {
  const errors: ValidationError[] = [];
  const warnings: ValidationWarning[] = [];

  if (!fundId) {
    errors.push({
      code: ValidationErrorCodes.E002,
      field: 'fundId',
      message: 'Fund ID is required',
    });
    return { isValid: false, errors, warnings };
  }

  // Check alphanumeric
  if (!/^[A-Za-z0-9]+$/.test(fundId)) {
    errors.push({
      code: ValidationErrorCodes.E002,
      field: 'fundId',
      message: 'Fund ID must be alphanumeric',
    });
    return { isValid: false, errors, warnings };
  }

  // Check length (6 characters)
  if (fundId.length !== 6) {
    errors.push({
      code: ValidationErrorCodes.E002,
      field: 'fundId',
      message: 'Fund ID must be 6 characters',
    });
    return { isValid: false, errors, warnings };
  }

  return { isValid: true, errors, warnings };
}

/**
 * Portfolio ID validation
 * From: PORTVAL.cpy - Portfolio ID should start with 'PORT' prefix
 */
export function validatePortfolioId(portfolioId: string): ValidationResult {
  const errors: ValidationError[] = [];
  const warnings: ValidationWarning[] = [];

  if (!portfolioId) {
    errors.push({
      code: 'E005',
      field: 'portfolioId',
      message: 'Portfolio ID is required',
    });
    return { isValid: false, errors, warnings };
  }

  // Check length (8 characters)
  if (portfolioId.length !== 8) {
    errors.push({
      code: 'E005',
      field: 'portfolioId',
      message: 'Portfolio ID must be 8 characters',
    });
    return { isValid: false, errors, warnings };
  }

  // Check alphanumeric
  if (!/^[A-Za-z0-9]+$/.test(portfolioId)) {
    errors.push({
      code: 'E005',
      field: 'portfolioId',
      message: 'Portfolio ID must be alphanumeric',
    });
    return { isValid: false, errors, warnings };
  }

  return { isValid: true, errors, warnings };
}

/**
 * Investment ID validation
 * From: POSREC.cpy - Investment ID is 10 characters
 */
export function validateInvestmentId(investmentId: string): ValidationResult {
  const errors: ValidationError[] = [];
  const warnings: ValidationWarning[] = [];

  if (!investmentId) {
    errors.push({
      code: 'E006',
      field: 'investmentId',
      message: 'Investment ID is required',
    });
    return { isValid: false, errors, warnings };
  }

  // Check length (10 characters)
  if (investmentId.length !== 10) {
    errors.push({
      code: 'E006',
      field: 'investmentId',
      message: 'Investment ID must be 10 characters',
    });
    return { isValid: false, errors, warnings };
  }

  return { isValid: true, errors, warnings };
}

/**
 * CUSIP validation
 * From: data-dictionary.md - CUSIP must be 9 alphanumeric characters
 */
export function validateCusip(cusip: string): ValidationResult {
  const errors: ValidationError[] = [];
  const warnings: ValidationWarning[] = [];

  if (!cusip) {
    errors.push({
      code: 'E007',
      field: 'cusip',
      message: 'CUSIP is required',
    });
    return { isValid: false, errors, warnings };
  }

  // Check alphanumeric
  if (!/^[A-Za-z0-9]+$/.test(cusip)) {
    errors.push({
      code: 'E007',
      field: 'cusip',
      message: 'CUSIP must be alphanumeric',
    });
    return { isValid: false, errors, warnings };
  }

  // Check length (9 characters)
  if (cusip.length !== 9) {
    errors.push({
      code: 'E007',
      field: 'cusip',
      message: 'CUSIP must be 9 characters',
    });
    return { isValid: false, errors, warnings };
  }

  return { isValid: true, errors, warnings };
}

/**
 * Transaction date validation
 * From: data-dictionary.md - Transaction Date must not be future date
 */
export function validateTransactionDate(transactionDate: string): ValidationResult {
  const errors: ValidationError[] = [];
  const warnings: ValidationWarning[] = [];

  if (!transactionDate) {
    errors.push({
      code: 'E008',
      field: 'transactionDate',
      message: 'Transaction date is required',
    });
    return { isValid: false, errors, warnings };
  }

  // Check format (YYYYMMDD)
  if (!/^\d{8}$/.test(transactionDate)) {
    errors.push({
      code: 'E008',
      field: 'transactionDate',
      message: 'Transaction date must be in YYYYMMDD format',
    });
    return { isValid: false, errors, warnings };
  }

  // Parse date
  const year = parseInt(transactionDate.substring(0, 4), 10);
  const month = parseInt(transactionDate.substring(4, 6), 10);
  const day = parseInt(transactionDate.substring(6, 8), 10);

  // Validate date components
  if (month < 1 || month > 12) {
    errors.push({
      code: 'E008',
      field: 'transactionDate',
      message: 'Invalid month in transaction date',
    });
    return { isValid: false, errors, warnings };
  }

  if (day < 1 || day > 31) {
    errors.push({
      code: 'E008',
      field: 'transactionDate',
      message: 'Invalid day in transaction date',
    });
    return { isValid: false, errors, warnings };
  }

  // Check not future date
  const transDate = new Date(year, month - 1, day);
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  if (transDate > today) {
    errors.push({
      code: 'E008',
      field: 'transactionDate',
      message: 'Transaction date cannot be in the future',
    });
    return { isValid: false, errors, warnings };
  }

  return { isValid: true, errors, warnings };
}

/**
 * Amount validation
 * From: PORTVAL.cpy - Amount must be within valid range
 */
export function validateAmount(amount: number, fieldName: string = 'amount'): ValidationResult {
  const errors: ValidationError[] = [];
  const warnings: ValidationWarning[] = [];

  const MAX_AMOUNT = 9999999999999.99;
  const MIN_AMOUNT = -9999999999999.99;

  if (amount < MIN_AMOUNT || amount > MAX_AMOUNT) {
    errors.push({
      code: 'E009',
      field: fieldName,
      message: `Amount must be between ${MIN_AMOUNT} and ${MAX_AMOUNT}`,
    });
    return { isValid: false, errors, warnings };
  }

  // Warning for zero amount
  if (amount === 0) {
    warnings.push({
      code: ValidationErrorCodes.W001,
      field: fieldName,
      message: 'Zero dollar transaction',
    });
  }

  return { isValid: true, errors, warnings };
}

/**
 * Quantity validation
 * From: data-dictionary.md - Share Quantity must not be zero for BY/SL
 */
export function validateQuantity(
  quantity: number,
  transactionType: TransactionType
): ValidationResult {
  const errors: ValidationError[] = [];
  const warnings: ValidationWarning[] = [];

  // For Buy/Sell, quantity must not be zero
  if (
    (transactionType === TransactionType.BUY || transactionType === TransactionType.SELL) &&
    quantity === 0
  ) {
    errors.push({
      code: 'E010',
      field: 'quantity',
      message: 'Quantity cannot be zero for Buy/Sell transactions',
    });
    return { isValid: false, errors, warnings };
  }

  // Quantity must be positive for Buy
  if (transactionType === TransactionType.BUY && quantity < 0) {
    errors.push({
      code: 'E010',
      field: 'quantity',
      message: 'Quantity must be positive for Buy transactions',
    });
    return { isValid: false, errors, warnings };
  }

  return { isValid: true, errors, warnings };
}

/**
 * Price validation
 * From: data-dictionary.md - Price must be greater than zero for BY/SL
 */
export function validatePrice(price: number, transactionType: TransactionType): ValidationResult {
  const errors: ValidationError[] = [];
  const warnings: ValidationWarning[] = [];

  // For Buy/Sell, price must be greater than zero
  if (
    (transactionType === TransactionType.BUY || transactionType === TransactionType.SELL) &&
    price <= 0
  ) {
    errors.push({
      code: 'E011',
      field: 'price',
      message: 'Price must be greater than zero for Buy/Sell transactions',
    });
    return { isValid: false, errors, warnings };
  }

  return { isValid: true, errors, warnings };
}

/**
 * Transaction validation
 * From: TRNVAL00 batch program - Complete transaction validation
 */
export function validateTransaction(request: CreateTransactionRequest): TransactionValidationResult {
  const warnings: string[] = [];
  
  // Validate portfolio ID
  const portfolioResult = validatePortfolioId(request.portfolioId);
  if (!portfolioResult.isValid) {
    return {
      isValid: false,
      errorCode: portfolioResult.errors[0]?.code ?? null,
      errorMessage: portfolioResult.errors[0]?.message ?? null,
      warnings,
    };
  }

  // Validate investment ID
  const investmentResult = validateInvestmentId(request.investmentId);
  if (!investmentResult.isValid) {
    return {
      isValid: false,
      errorCode: investmentResult.errors[0]?.code ?? null,
      errorMessage: investmentResult.errors[0]?.message ?? null,
      warnings,
    };
  }

  // Validate quantity
  const quantityResult = validateQuantity(request.quantity, request.transactionType);
  if (!quantityResult.isValid) {
    return {
      isValid: false,
      errorCode: quantityResult.errors[0]?.code ?? null,
      errorMessage: quantityResult.errors[0]?.message ?? null,
      warnings,
    };
  }

  // Validate price
  const priceResult = validatePrice(request.price, request.transactionType);
  if (!priceResult.isValid) {
    return {
      isValid: false,
      errorCode: priceResult.errors[0]?.code ?? null,
      errorMessage: priceResult.errors[0]?.message ?? null,
      warnings,
    };
  }

  // Calculate and validate amount
  const amount = request.quantity * request.price;
  const amountResult = validateAmount(amount);
  if (!amountResult.isValid) {
    return {
      isValid: false,
      errorCode: amountResult.errors[0]?.code ?? null,
      errorMessage: amountResult.errors[0]?.message ?? null,
      warnings,
    };
  }
  
  // Collect warnings
  amountResult.warnings.forEach(w => warnings.push(w.message));

  return {
    isValid: true,
    errorCode: null,
    errorMessage: null,
    warnings,
  };
}

/**
 * Portfolio validation
 */
export function validatePortfolio(request: CreatePortfolioRequest): ValidationResult {
  const errors: ValidationError[] = [];
  const warnings: ValidationWarning[] = [];

  // Validate portfolio ID
  const portfolioResult = validatePortfolioId(request.portfolioId);
  errors.push(...portfolioResult.errors);
  warnings.push(...portfolioResult.warnings);

  // Validate account number
  const accountResult = validateAccountNumber(request.accountNumber);
  errors.push(...accountResult.errors);
  warnings.push(...accountResult.warnings);

  // Validate client name
  if (!request.clientName || request.clientName.trim().length === 0) {
    errors.push({
      code: 'E012',
      field: 'clientName',
      message: 'Client name is required',
    });
  } else if (request.clientName.length > 30) {
    errors.push({
      code: 'E012',
      field: 'clientName',
      message: 'Client name must be 30 characters or less',
    });
  }

  // Validate risk level
  if (!request.riskLevel || request.riskLevel.length !== 1) {
    errors.push({
      code: 'E013',
      field: 'riskLevel',
      message: 'Risk level must be 1 character',
    });
  }

  // Validate branch ID
  if (!request.branchId || request.branchId.length !== 2) {
    errors.push({
      code: 'E014',
      field: 'branchId',
      message: 'Branch ID must be 2 characters',
    });
  }

  return {
    isValid: errors.length === 0,
    errors,
    warnings,
  };
}

/**
 * Position validation
 */
export function validatePosition(request: CreatePositionRequest): ValidationResult {
  const errors: ValidationError[] = [];
  const warnings: ValidationWarning[] = [];

  // Validate portfolio ID
  const portfolioResult = validatePortfolioId(request.portfolioId);
  errors.push(...portfolioResult.errors);
  warnings.push(...portfolioResult.warnings);

  // Validate investment ID
  const investmentResult = validateInvestmentId(request.investmentId);
  errors.push(...investmentResult.errors);
  warnings.push(...investmentResult.warnings);

  // Validate quantity (must be positive)
  if (request.quantity < 0) {
    errors.push({
      code: 'E015',
      field: 'quantity',
      message: 'Position quantity cannot be negative',
    });
  }

  // Validate cost basis
  const costBasisResult = validateAmount(request.costBasis, 'costBasis');
  errors.push(...costBasisResult.errors);
  warnings.push(...costBasisResult.warnings);

  // Validate market value
  const marketValueResult = validateAmount(request.marketValue, 'marketValue');
  errors.push(...marketValueResult.errors);
  warnings.push(...marketValueResult.warnings);

  return {
    isValid: errors.length === 0,
    errors,
    warnings,
  };
}

/**
 * Position balance validation
 * From: data-dictionary.md - Share Balance must not go negative
 */
export function validatePositionBalance(
  currentBalance: number,
  transactionQuantity: number,
  transactionType: TransactionType
): ValidationResult {
  const errors: ValidationError[] = [];
  const warnings: ValidationWarning[] = [];

  let newBalance = currentBalance;

  if (transactionType === TransactionType.BUY) {
    newBalance = currentBalance + transactionQuantity;
  } else if (transactionType === TransactionType.SELL) {
    newBalance = currentBalance - transactionQuantity;
  }

  if (newBalance < 0) {
    errors.push({
      code: ValidationErrorCodes.E004,
      field: 'quantity',
      message: 'Insufficient position balance',
    });
  }

  return {
    isValid: errors.length === 0,
    errors,
    warnings,
  };
}

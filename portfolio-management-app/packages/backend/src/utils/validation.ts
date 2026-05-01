import { z } from 'zod';
import { TransactionType, PortfolioStatus } from '../types/index.js';

// Portfolio ID must start with 'PORT' and have 4 numeric digits (from PORTVALD.cbl)
const portfolioIdRegex = /^PORT\d{4}$/;

// Account number must be 10 numeric digits (from PORTVALD.cbl)
const accountNoRegex = /^\d{10}$/;

export const createPortfolioSchema = z.object({
  portfolioId: z
    .string()
    .length(8)
    .regex(portfolioIdRegex, 'Portfolio ID must start with PORT followed by 4 digits'),
  accountType: z.string().length(2),
  branchId: z.string().length(2),
  clientId: z
    .string()
    .length(10)
    .regex(accountNoRegex, 'Client ID must be 10 numeric digits'),
  portfolioName: z.string().min(1).max(50),
  currencyCode: z.string().length(3),
  riskLevel: z.enum(['1', '2', '3', '4', '5']),
});

export const updatePortfolioSchema = z.object({
  portfolioName: z.string().min(1).max(50).optional(),
  status: z.nativeEnum(PortfolioStatus).optional(),
  riskLevel: z.enum(['1', '2', '3', '4', '5']).optional(),
  cashBalance: z.number().optional(),
});

export const createTransactionSchema = z.object({
  portfolioId: z
    .string()
    .length(8)
    .regex(portfolioIdRegex, 'Portfolio ID must start with PORT followed by 4 digits'),
  investmentId: z.string().min(1).max(10),
  transactionType: z.nativeEnum(TransactionType),
  quantity: z.number().positive('Quantity must be positive'),
  price: z.number().positive('Price must be positive'),
  currencyCode: z.string().length(3).default('USD'),
});

// Amount validation from PORTVALD.cbl — range check
const MAX_AMOUNT = 9999999999999.99;
const MIN_AMOUNT = -9999999999999.99;

export function validateAmount(amount: number): boolean {
  return amount >= MIN_AMOUNT && amount <= MAX_AMOUNT;
}

// Investment type validation from PORTVALD.cbl
const VALID_INVESTMENT_TYPES = ['STK', 'BND', 'MMF', 'ETF'];
export function validateInvestmentType(type: string): boolean {
  return VALID_INVESTMENT_TYPES.includes(type);
}

export const loginSchema = z.object({
  username: z.string().min(1).max(50),
  password: z.string().min(1),
});

export const batchRunSchema = z.object({
  jobName: z.string().max(8).optional(),
  processDate: z.string().optional(),
});

export const paginationSchema = z.object({
  page: z.coerce.number().int().positive().default(1),
  pageSize: z.coerce.number().int().min(1).max(100).default(10),
});

export const reportQuerySchema = z.object({
  startDate: z.string().optional(),
  endDate: z.string().optional(),
  portfolioId: z.string().optional(),
  format: z.enum(['json', 'csv']).default('json'),
});

export const portfolioListSchema = z.object({
  page: z.coerce.number().int().positive().default(1),
  pageSize: z.coerce.number().int().min(1).max(100).default(10),
  status: z.nativeEnum(PortfolioStatus).optional(),
  clientId: z.string().optional(),
  search: z.string().optional(),
});

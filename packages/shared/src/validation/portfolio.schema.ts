import { z } from 'zod';

export const createPortfolioSchema = z.object({
  portfolioId: z.string().length(8, 'Portfolio ID must be 8 characters'),
  accountType: z.string().min(1).max(4),
  branchId: z.string().min(1).max(4),
  clientId: z.string().length(10, 'Client ID must be 10 characters'),
  name: z.string().min(1).max(50),
  currencyCode: z.string().length(3, 'Currency code must be 3 characters'),
  riskLevel: z.enum(['1', '2', '3', '4', '5']),
});

export const updatePortfolioSchema = z.object({
  name: z.string().min(1).max(50).optional(),
  currencyCode: z.string().length(3).optional(),
  riskLevel: z.enum(['1', '2', '3', '4', '5']).optional(),
  branchId: z.string().min(1).max(4).optional(),
  accountType: z.string().min(1).max(4).optional(),
});

export const portfolioFilterSchema = z.object({
  status: z.enum(['A', 'C', 'S']).optional(),
  branch: z.string().optional(),
  client: z.string().optional(),
  page: z.coerce.number().int().positive().default(1),
  limit: z.coerce.number().int().positive().max(100).default(20),
});

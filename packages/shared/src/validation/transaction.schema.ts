import { z } from 'zod';

export const createTransactionSchema = z
  .object({
    portfolioId: z.string().length(8),
    investmentId: z.string().length(6, 'Fund ID must be 6 alphanumeric characters'),
    transactionType: z.enum(['BU', 'SL', 'TR', 'FE']),
    quantity: z.number(),
    price: z.number().positive('Price must be positive'),
    currencyCode: z.string().length(3),
    transactionDate: z.string().refine(
      (val) => new Date(val) <= new Date(),
      'Transaction date cannot be in the future'
    ),
  })
  .refine(
    (data) => {
      if (data.transactionType === 'BU' || data.transactionType === 'SL') {
        return data.quantity !== 0;
      }
      return true;
    },
    { message: 'Quantity must be non-zero for BU/SL transactions', path: ['quantity'] }
  );

export const transactionFilterSchema = z.object({
  page: z.coerce.number().int().positive().default(1),
  limit: z.coerce.number().int().positive().max(100).default(10),
  startDate: z.string().optional(),
  endDate: z.string().optional(),
  type: z.enum(['BU', 'SL', 'TR', 'FE']).optional(),
});

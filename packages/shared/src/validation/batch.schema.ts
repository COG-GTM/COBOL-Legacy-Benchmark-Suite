import { z } from 'zod';

export const batchControlSchema = z.object({
  processDate: z.string(),
  processId: z.string().min(1).max(10),
  status: z.enum(['P', 'R', 'C', 'F', 'A']),
  recordCount: z.number().int().nonnegative(),
  errorCount: z.number().int().nonnegative(),
  returnCode: z.number().int().nonnegative(),
  message: z.string().max(80),
});

export const startBatchSchema = z.object({
  processDate: z.string().optional(),
});

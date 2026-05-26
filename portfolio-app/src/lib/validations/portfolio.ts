import { z } from "zod";

/**
 * Validation schemas ported from PORTVALD.cbl and PORTVAL.cpy.
 */

/** Portfolio ID: starts with 'PORT' + 4 numeric digits (PORTVALD.cbl 1000-VALIDATE-ID) */
const portfolioIdSchema = z
  .string()
  .length(8, "Portfolio ID must be exactly 8 characters")
  .regex(
    /^PORT\d{4}$/,
    "Portfolio ID must start with 'PORT' followed by 4 digits"
  );

/** Account number: exactly 10 numeric digits, not all zeros (PORTVALD.cbl 2000-VALIDATE-ACCOUNT) */
const accountNoSchema = z
  .string()
  .length(10, "Account number must be exactly 10 digits")
  .regex(/^\d{10}$/, "Account number must be 10 numeric digits")
  .refine((val) => val !== "0000000000", {
    message: "Account number cannot be all zeros",
  });

/** Client type: 'I', 'C', or 'T' (PORTFLIO.cpy lines 17-20) */
const clientTypeSchema = z.enum(["I", "C", "T"], {
  errorMap: () => ({
    message: "Client type must be 'I' (Individual), 'C' (Corporate), or 'T' (Trust)",
  }),
});

/** Status: 'A', 'C', or 'S' (PORTFLIO.cpy lines 24-27) */
const statusSchema = z.enum(["A", "C", "S"], {
  errorMap: () => ({
    message: "Status must be 'A' (Active), 'C' (Closed), or 'S' (Suspended)",
  }),
});

/** Amount: between -9999999999999.99 and +9999999999999.99 (PORTVAL.cpy lines 35-36) */
const amountSchema = z
  .string()
  .regex(/^-?\d+(\.\d{1,2})?$/, "Amount must be a valid decimal number")
  .refine(
    (val) => {
      const num = parseFloat(val);
      return num >= -9999999999999.99 && num <= 9999999999999.99;
    },
    { message: "Amount outside valid range" }
  );

/** Client name: required, non-empty (PORTMSTR.cbl line 149) */
const clientNameSchema = z
  .string()
  .min(1, "Client name is required")
  .max(30, "Client name must be at most 30 characters");

/** Delete reason code: '01', '02', or '03' (PORTDEL.cbl lines 50-52) */
export const deleteReasonCodeSchema = z.enum(["01", "02", "03"], {
  errorMap: () => ({
    message:
      "Reason code must be '01' (Closed), '02' (Transferred), or '03' (Requested)",
  }),
});

/** Schema for creating a new portfolio */
export const createPortfolioSchema = z.object({
  portfolio_id: portfolioIdSchema,
  account_no: accountNoSchema,
  client_name: clientNameSchema,
  client_type: clientTypeSchema,
  portfolio_name: z.string().max(50).optional().default(""),
  currency_code: z.string().length(3).default("USD"),
  risk_level: z.string().max(1).optional().default(""),
  branch_id: z.string().max(2).optional().default(""),
  total_value: amountSchema.optional().default("0"),
  cash_balance: amountSchema.optional().default("0"),
  status: statusSchema.optional().default("A"),
  updated_by: z.string().min(1).max(8),
});

/** Schema for updating an existing portfolio */
export const updatePortfolioSchema = z.object({
  portfolio_id: portfolioIdSchema,
  client_name: clientNameSchema.optional(),
  client_type: clientTypeSchema.optional(),
  portfolio_name: z.string().max(50).optional(),
  currency_code: z.string().length(3).optional(),
  risk_level: z.string().max(1).optional(),
  branch_id: z.string().max(2).optional(),
  total_value: amountSchema.optional(),
  cash_balance: amountSchema.optional(),
  status: statusSchema.optional(),
  close_date: z.string().optional(),
  updated_by: z.string().min(1).max(8),
});

/** Schema for deleting a portfolio */
export const deletePortfolioSchema = z.object({
  portfolio_id: portfolioIdSchema,
  reason_code: deleteReasonCodeSchema,
});

export type CreatePortfolioInput = z.infer<typeof createPortfolioSchema>;
export type UpdatePortfolioInput = z.infer<typeof updatePortfolioSchema>;
export type DeletePortfolioInput = z.infer<typeof deletePortfolioSchema>;

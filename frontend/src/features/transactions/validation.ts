import {
  SUBMITTABLE_TRANSACTION_TYPES,
  TRANSACTION_CURRENCIES,
  TRANSACTION_DECIMALS,
  TRANSACTION_FIELD_LENGTHS,
  type TransactionInput,
  type TransactionType,
} from '../../types/transaction';
import { compareDecimals, validateDecimal } from '../../utils/decimal';
import { calculateAmount } from './amount';

export type TransactionErrors = Partial<
  Record<keyof TransactionInput | 'amount', string>
>;

/** PORTVALD 1000-VALIDATE-ID: 'PORT' prefix followed by 4 numeric digits. */
const PORTFOLIO_ID_RE = /^PORT\d{4}$/;
const ALPHANUM_RE = /^[A-Za-z0-9]+$/;

/**
 * Validates a transaction submission against the TRNREC.cpy field definitions
 * and the checks PORTTRAN performs before a record is accepted:
 *
 *   2110-CHECK-PORTFOLIO       — portfolio id required (format per PORTVALD)
 *   2120-CHECK-TRANSACTION-TYPE — TRN-TYPE must be a known 88-level value
 *   2130-CHECK-AMOUNTS         — quantity > 0; price and amount > 0 unless the
 *                                type is 'TR' (transfer)
 *
 * Checks that need server data — that the portfolio exists and that a SELL is
 * covered by the units held — are handled by the service layer, since a purely
 * client-side form cannot know them.
 */
export function validateTransaction(
  values: TransactionInput,
): TransactionErrors {
  const errors: TransactionErrors = {};

  const portfolioId = values.portfolioId.trim();
  if (!portfolioId) {
    errors.portfolioId = 'Portfolio ID is required.';
  } else if (portfolioId.length > TRANSACTION_FIELD_LENGTHS.portfolioId) {
    errors.portfolioId = `Up to ${TRANSACTION_FIELD_LENGTHS.portfolioId} characters.`;
  } else if (!PORTFOLIO_ID_RE.test(portfolioId)) {
    errors.portfolioId = 'Invalid Portfolio ID format (e.g. PORT0001).';
  }

  const investmentId = values.investmentId.trim();
  if (!investmentId) {
    errors.investmentId = 'Investment ID is required.';
  } else if (investmentId.length > TRANSACTION_FIELD_LENGTHS.investmentId) {
    errors.investmentId = `Up to ${TRANSACTION_FIELD_LENGTHS.investmentId} characters.`;
  } else if (!ALPHANUM_RE.test(investmentId)) {
    errors.investmentId = 'Letters and numbers only.';
  }

  if (
    !SUBMITTABLE_TRANSACTION_TYPES.includes(values.type as TransactionType)
  ) {
    errors.type = 'Select a transaction type.';
  }

  const quantityError = validatePositiveDecimal(
    values.quantity,
    'Quantity',
    TRANSACTION_DECIMALS.quantity,
  );
  if (quantityError) {
    errors.quantity = quantityError;
  }

  // Transfers move units between portfolios and carry no price or amount.
  const priceRequired = values.type !== 'TR';
  if (priceRequired) {
    const priceError = validatePositiveDecimal(
      values.price,
      'Price',
      TRANSACTION_DECIMALS.price,
    );
    if (priceError) {
      errors.price = priceError;
    }
  }

  if (!TRANSACTION_CURRENCIES.includes(values.currency as never)) {
    errors.currency = 'Select a currency.';
  }

  if (priceRequired && !errors.quantity && !errors.price) {
    const amount = calculateAmount(values.type, values.quantity, values.price);
    const amountError =
      validateDecimal(amount, TRANSACTION_DECIMALS.amount) ??
      (compareDecimals(amount, '0', 2) <= 0
        ? 'Amount must be greater than zero.'
        : null);
    if (amountError) {
      errors.amount = amountError;
    }
  }

  return errors;
}

function validatePositiveDecimal(
  value: string,
  label: string,
  constraints: { maxIntDigits: number; maxFracDigits: number },
): string | null {
  const trimmed = value.trim();
  if (!trimmed) {
    return `${label} is required.`;
  }
  const formatError = validateDecimal(trimmed, constraints);
  if (formatError) {
    return formatError;
  }
  if (compareDecimals(trimmed, '0', constraints.maxFracDigits) <= 0) {
    return `${label} must be greater than zero.`;
  }
  return null;
}

export function hasErrors(errors: TransactionErrors): boolean {
  return Object.keys(errors).length > 0;
}

import { isDecimalString, multiplyDecimals } from '../../utils/decimal';
import {
  TRANSACTION_DECIMALS,
  type TransactionType,
} from '../../types/transaction';

/**
 * Computes TRN-AMOUNT from TRN-QUANTITY x TRN-PRICE.
 *
 * The product of two `V9(4)` operands carries 8 fraction digits; PORTTRAN's
 * amount field is `S9(13)V9(2)`, so the extra digits are truncated (see
 * multiplyDecimals). Transfers carry no price — PORTTRAN 2130-CHECK-AMOUNTS
 * exempts TRN-TYPE 'TR' from the price/amount checks — so their amount is zero.
 *
 * Returns an empty string while either operand is still blank or malformed, so
 * the form can leave the amount field empty rather than showing a bogus total.
 */
export function calculateAmount(
  type: TransactionType,
  quantity: string,
  price: string,
): string {
  if (type === 'TR') {
    return '0.00';
  }
  if (!isDecimalString(quantity) || !isDecimalString(price)) {
    return '';
  }
  return multiplyDecimals(quantity, price, {
    scale: TRANSACTION_DECIMALS.amount.maxFracDigits,
    aScale: TRANSACTION_DECIMALS.quantity.maxFracDigits,
    bScale: TRANSACTION_DECIMALS.price.maxFracDigits,
  });
}

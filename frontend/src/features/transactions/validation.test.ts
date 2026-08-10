import { describe, expect, it } from 'vitest';
import type { TransactionInput } from '../../types/transaction';
import { hasErrors, validateTransaction } from './validation';

const VALID: TransactionInput = {
  portfolioId: 'PORT0001',
  investmentId: 'FND0000001',
  type: 'BU',
  quantity: '250.0000',
  price: '409.8200',
  currency: 'USD',
};

describe('validateTransaction', () => {
  it('accepts a well-formed buy', () => {
    expect(hasErrors(validateTransaction(VALID))).toBe(false);
  });

  it('requires portfolio, investment, quantity and price', () => {
    const errors = validateTransaction({
      ...VALID,
      portfolioId: '',
      investmentId: '',
      quantity: '',
      price: '',
    });
    expect(errors.portfolioId).toMatch(/required/);
    expect(errors.investmentId).toMatch(/required/);
    expect(errors.quantity).toMatch(/required/);
    expect(errors.price).toMatch(/required/);
  });

  it('enforces the PORTVALD portfolio id format', () => {
    expect(
      validateTransaction({ ...VALID, portfolioId: 'ACCT0001' }).portfolioId,
    ).toMatch(/format/);
    expect(
      validateTransaction({ ...VALID, portfolioId: 'PORTABCD' }).portfolioId,
    ).toMatch(/format/);
  });

  it('rejects non-positive quantity and price (PORTTRAN 2130-CHECK-AMOUNTS)', () => {
    expect(validateTransaction({ ...VALID, quantity: '0' }).quantity).toMatch(
      /greater than zero/,
    );
    expect(validateTransaction({ ...VALID, price: '-1' }).price).toMatch(
      /greater than zero/,
    );
  });

  it('enforces the COMP-3 digit limits from TRNREC.cpy', () => {
    expect(
      validateTransaction({ ...VALID, quantity: '1.00005' }).quantity,
    ).toMatch(/4 decimal places/);
    expect(
      validateTransaction({ ...VALID, price: '123456789012' }).price,
    ).toMatch(/11 digits/);
  });

  it('rejects an amount that overflows TRN-AMOUNT S9(13)V9(2)', () => {
    const errors = validateTransaction({
      ...VALID,
      quantity: '99999999999',
      price: '99999999999',
    });
    expect(errors.amount).toMatch(/13 digits/);
  });

  it('exempts transfers from the price and amount checks', () => {
    const errors = validateTransaction({ ...VALID, type: 'TR', price: '' });
    expect(hasErrors(errors)).toBe(false);
  });

  it('rejects an unsupported currency', () => {
    expect(validateTransaction({ ...VALID, currency: 'XYZ' }).currency).toMatch(
      /currency/,
    );
  });
});

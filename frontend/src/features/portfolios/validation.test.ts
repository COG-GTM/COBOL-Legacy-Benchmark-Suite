import { describe, expect, it } from 'vitest';
import type { PortfolioInput } from '../../types/portfolio';
import { validatePortfolio } from './validation';

const valid: PortfolioInput = {
  portId: 'PORT9999',
  accountNo: 'ACCT999999',
  clientName: 'Test Client',
  clientType: 'I',
  status: 'A',
  totalValue: '1000.00',
  cashBalance: '50.00',
};

describe('validatePortfolio', () => {
  it('returns no errors for a valid create input', () => {
    expect(validatePortfolio(valid, true)).toEqual({});
  });

  it('requires PORT-ID on create but not on edit', () => {
    const input = { ...valid, portId: '' };
    expect(validatePortfolio(input, true).portId).toBeDefined();
    expect(validatePortfolio(input, false).portId).toBeUndefined();
  });

  it('enforces PIC X length limits', () => {
    expect(
      validatePortfolio({ ...valid, portId: 'TOOLONG12' }, true).portId,
    ).toMatch(/8 characters/);
    expect(
      validatePortfolio({ ...valid, accountNo: 'ACCT99999999' }, true)
        .accountNo,
    ).toMatch(/10 characters/);
    expect(
      validatePortfolio({ ...valid, clientName: 'x'.repeat(31) }, true)
        .clientName,
    ).toMatch(/30 characters/);
  });

  it('requires a valid client type and status', () => {
    expect(
      validatePortfolio(
        { ...valid, clientType: 'Z' as PortfolioInput['clientType'] },
        true,
      ).clientType,
    ).toBeDefined();
    expect(
      validatePortfolio(
        { ...valid, status: 'X' as PortfolioInput['status'] },
        true,
      ).status,
    ).toBeDefined();
  });

  it('requires monetary fields to be valid decimals', () => {
    expect(validatePortfolio({ ...valid, totalValue: '' }, true).totalValue).toMatch(
      /required/,
    );
    expect(
      validatePortfolio({ ...valid, cashBalance: 'abc' }, true).cashBalance,
    ).toBeDefined();
  });
});

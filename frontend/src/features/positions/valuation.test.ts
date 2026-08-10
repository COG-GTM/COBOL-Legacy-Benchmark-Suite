import { describe, expect, it } from 'vitest';
import type { Position } from '../../types/position';
import { summarizePositions } from './valuation';

const audit = { lastMaintDate: '2024-04-01-09.00.00.000000', lastMaintUser: 'X' };

function pos(
  costBasis: string,
  marketValue: string,
  status: Position['status'] = 'A',
): Position {
  return {
    portfolioId: 'PORT0001',
    date: '20240401',
    investmentId: 'FND0000001',
    fundName: 'Fund',
    quantity: '1.0000',
    costBasis,
    marketValue,
    currency: 'USD',
    status,
    ...audit,
  };
}

describe('summarizePositions', () => {
  it('totals market value and cost basis and computes gain', () => {
    const result = summarizePositions([
      pos('420150.00', '512300.75'),
      pos('158900.00', '172480.20'),
    ]);
    expect(result.totalCostBasis).toBe('579050.00');
    expect(result.totalMarketValue).toBe('684780.95');
    expect(result.gainLoss).toBe('105730.95');
  });

  it('reports a loss as a negative gainLoss', () => {
    const result = summarizePositions([pos('61200.00', '58940.10')]);
    expect(result.gainLoss).toBe('-2259.90');
  });

  it('returns zeroed totals for an empty set', () => {
    expect(summarizePositions([])).toEqual({
      totalMarketValue: '0.00',
      totalCostBasis: '0.00',
      gainLoss: '0.00',
    });
  });
});

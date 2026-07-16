import type { Position } from '../../types/position';
import { subtractDecimals, sumDecimals } from '../../utils/decimal';

/** Portfolio valuation totals derived from a set of positions. */
export interface PositionValuation {
  /** Sum of POS-MARKET-VALUE across the positions. */
  totalMarketValue: string;
  /** Sum of POS-COST-BASIS across the positions. */
  totalCostBasis: string;
  /** totalMarketValue − totalCostBasis (positive = unrealized gain). */
  gainLoss: string;
}

/**
 * Aggregates a set of positions into the valuation summary shown at the top of
 * the inquiry screen. All arithmetic runs through the decimal-string helpers so
 * `S9(13)V9(2)` money totals never lose COMP-3 precision to float math.
 */
export function summarizePositions(
  positions: readonly Position[],
): PositionValuation {
  const totalMarketValue = sumDecimals(positions.map((p) => p.marketValue));
  const totalCostBasis = sumDecimals(positions.map((p) => p.costBasis));
  return {
    totalMarketValue,
    totalCostBasis,
    gainLoss: subtractDecimals(totalMarketValue, totalCostBasis),
  };
}

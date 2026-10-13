import type { AccountDetail, AssetClass, Position } from '../api/types';

export interface Totals {
  marketValue: number;
  costBasis: number;
  dayChange: number;
  dayChangePct: number;
  unrealized: number;
  unrealizedPct: number;
  realized: number;
  activeCount: number;
  pendingCount: number;
  pendingNet: number;
}

export const isActive = (p: Position) => p.status === 'Active';

export function computeTotals(a: AccountDetail): Totals {
  const active = a.positions.filter(isActive);
  const marketValue = sum(active.map((p) => p.marketValue));
  const costBasis = sum(active.map((p) => p.costBasis));
  const prev = sum(active.map((p) => p.previousValue));
  const pending = a.transactions.filter((t) => t.status === 'Pending');
  return {
    marketValue,
    costBasis,
    dayChange: marketValue - prev,
    dayChangePct: prev ? (marketValue - prev) / prev : 0,
    unrealized: marketValue - costBasis,
    unrealizedPct: costBasis ? (marketValue - costBasis) / costBasis : 0,
    realized: sum(a.positions.map((p) => p.realizedGain)),
    activeCount: active.length,
    pendingCount: pending.length,
    pendingNet: sum(pending.map((t) => t.amount * t.direction)),
  };
}

export function allocation(positions: Position[]): { assetClass: AssetClass; value: number; weight: number }[] {
  const by = new Map<AssetClass, number>();
  for (const p of positions.filter(isActive)) by.set(p.assetClass, (by.get(p.assetClass) ?? 0) + p.marketValue);
  const total = sum([...by.values()]);
  return [...by.entries()]
    .map(([assetClass, value]) => ({ assetClass, value, weight: total ? value / total : 0 }))
    .sort((x, y) => y.value - x.value);
}

/** Gain for display: realized for closed positions, unrealized otherwise. */
export function positionGain(p: Position): { gain: number; pct: number | null } {
  if (p.status === 'Closed') return { gain: p.realizedGain, pct: null };
  const gain = p.marketValue - p.costBasis;
  return { gain, pct: p.costBasis ? gain / p.costBasis : null };
}

export const sum = (xs: number[]) => xs.reduce((s, x) => s + x, 0);

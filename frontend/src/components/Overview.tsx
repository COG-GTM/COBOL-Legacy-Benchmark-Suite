import { useMemo, useState } from 'react';
import { ASSET_CLASS_LABELS, type AccountDetail } from '../api/types';
import { formatDate, GainLoss, GainPct, pct2, signed, units3, usd, usd0 } from '../lib/format';
import { allocation, computeTotals } from '../lib/metrics';
import { StatusPill, TypeCell } from './shared';
import { ValueChart } from './ValueChart';

const RANGES = [
  { id: '3M', days: 90 },
  { id: '6M', days: 182 },
  { id: '1Y', days: 365 },
  { id: 'All', days: Infinity },
] as const;
type RangeId = (typeof RANGES)[number]['id'];

export function Overview({ account, onViewTransactions }: { account: AccountDetail; onViewTransactions: () => void }) {
  const totals = useMemo(() => computeTotals(account), [account]);
  const alloc = useMemo(() => allocation(account.positions), [account]);
  const [range, setRange] = useState<RangeId>('1Y');
  const points = useMemo(() => {
    const days = RANGES.find((r) => r.id === range)!.days;
    const cutoff = Date.parse(account.asOfDate) - days * 86_400_000;
    return account.valueHistory.filter((p) => Date.parse(p.date) >= cutoff);
  }, [account, range]);
  const recent = account.transactions.slice(0, 6);

  return (
    <div style={{ display: 'grid', gap: 20 }}>
      <div className="kpis" data-testid="kpis">
        <div className="kpi">
          <span className="eyebrow">Total market value</span>
          <span className="v">{usd0.format(totals.marketValue)}</span>
          <span className="d"><GainLoss value={totals.dayChange} format={usd0} /> <GainPct value={totals.dayChangePct} /> today</span>
        </div>
        <div className="kpi">
          <span className="eyebrow">Cost basis</span>
          <span className="v">{usd0.format(totals.costBasis)}</span>
          <span className="d">{totals.activeCount} active positions</span>
        </div>
        <div className="kpi">
          <span className="eyebrow">Unrealized gain/loss</span>
          <span className="v"><GainLoss value={totals.unrealized} format={usd0} /></span>
          <span className="d"><GainPct value={totals.unrealizedPct} /> · Realized <GainLoss value={totals.realized} format={usd0} /></span>
        </div>
        <div className="kpi">
          <span className="eyebrow">Pending</span>
          <span className="v">{totals.pendingCount}</span>
          <span className="d">{totals.pendingCount ? `${signed(totals.pendingNet, usd0)} net, settling` : 'Nothing settling'}</span>
        </div>
      </div>

      <div className="grid2">
        <div className="panel">
          <div className="panel-h">
            <h2>Portfolio value</h2>
            <div className="legend"><span><i />Market value</span><span><i className="dash" />Cost basis</span></div>
            <div className="seg" role="group" aria-label="Chart range">
              {RANGES.map((r) => (
                <button key={r.id} type="button" aria-pressed={range === r.id} onClick={() => setRange(r.id)}>{r.id}</button>
              ))}
            </div>
          </div>
          <ValueChart points={points} />
        </div>
        <div className="panel">
          <div className="panel-h"><h2>Allocation</h2><span className="sub">by asset class, active positions</span></div>
          <div className="panel-b">
            <div className="alloc-bar" aria-hidden="true">
              {alloc.map((a) => <span key={a.assetClass} style={{ width: `${a.weight * 100}%`, background: `var(--c-${a.assetClass})` }} />)}
            </div>
            <ul className="alloc-list">
              {alloc.map((a) => (
                <li key={a.assetClass}>
                  <span className="sw" style={{ background: `var(--c-${a.assetClass})` }} />
                  <span>{ASSET_CLASS_LABELS[a.assetClass]}</span>
                  <span className="num">{usd.format(a.value)}</span>
                  <span className="num pct">{pct2.format(a.weight)}</span>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>

      <div className="panel">
        <div className="panel-h">
          <h2>Recent activity</h2>
          <span className="sub">Latest {recent.length} of {account.transactions.length} transactions</span>
          <button className="link" style={{ marginLeft: 'auto' }} type="button" onClick={onViewTransactions}>View all transactions →</button>
        </div>
        <ul className="recent">
          {recent.map((t) => (
            <li key={t.id}>
              <TypeCell type={t.type} direction={t.direction} />
              <div style={{ minWidth: 0 }}>
                <div className="fund">{t.fundName}</div>
                <div className="meta"><span className="mono">{t.fundCode}</span> · {formatDate(t.date)} · {units3.format(t.units)} units @ {usd.format(t.price)}</div>
              </div>
              <div className="amt">
                <span className={`num ${t.status === 'Failed' || t.status === 'Reversed' ? 'struck' : ''}`}>{signed(t.amount * t.direction)}</span>
                <StatusPill status={t.status} />
              </div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}

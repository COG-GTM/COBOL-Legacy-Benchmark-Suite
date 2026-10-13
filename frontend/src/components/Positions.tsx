import { useMemo, useState, type RefObject } from 'react';
import { ASSET_CLASS_LABELS, type AccountDetail, type Position, type PositionStatus } from '../api/types';
import { GainLoss, GainPct, pct2, units3, usd } from '../lib/format';
import { isActive, positionGain, sum } from '../lib/metrics';
import { nextSort } from '../lib/sort';
import { Empty, SearchIcon, SortHeader, StatusPill } from './shared';

type SortKey = 'fund' | 'assetClass' | 'units' | 'nav' | 'costBasis' | 'marketValue' | 'gain' | 'weight' | 'status';
type StatusFilter = 'All' | PositionStatus;
type Layout = 'table' | 'cards';
const LAYOUT_KEY = 'portfolio-desk.positions-layout';
const STATUSES: StatusFilter[] = ['All', 'Active', 'Pending', 'Closed'];
const TEXT_KEYS: SortKey[] = ['fund', 'assetClass', 'status'];

function initialLayout(): Layout {
  const saved = localStorage.getItem(LAYOUT_KEY);
  if (saved === 'table' || saved === 'cards') return saved;
  return typeof window.matchMedia === 'function' && window.matchMedia('(max-width: 680px)').matches ? 'cards' : 'table';
}

export function Positions({ account, searchRef }: { account: AccountDetail; searchRef: RefObject<HTMLInputElement> }) {
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState<StatusFilter>('All');
  const [sort, setSort] = useState<{ key: SortKey; dir: 'asc' | 'desc' }>({ key: 'marketValue', dir: 'desc' });
  const [layout, setLayoutState] = useState<Layout>(initialLayout);
  const setLayout = (l: Layout) => { setLayoutState(l); localStorage.setItem(LAYOUT_KEY, l); };

  const activeValue = useMemo(() => sum(account.positions.filter(isActive).map((p) => p.marketValue)), [account]);
  const weight = (p: Position) => (isActive(p) && activeValue ? p.marketValue / activeValue : 0);
  const counts = useMemo(() => {
    const c: Record<StatusFilter, number> = { All: account.positions.length, Active: 0, Pending: 0, Closed: 0 };
    for (const p of account.positions) c[p.status]++;
    return c;
  }, [account]);

  const rows = useMemo(() => {
    const q = query.trim().toLowerCase();
    const value = (p: Position): string | number => {
      switch (sort.key) {
        case 'fund': return p.fundName;
        case 'assetClass': return ASSET_CLASS_LABELS[p.assetClass];
        case 'gain': return positionGain(p).gain;
        case 'weight': return isActive(p) && activeValue ? p.marketValue / activeValue : 0;
        case 'status': return p.status;
        default: return p[sort.key];
      }
    };
    return account.positions
      .filter((p) => status === 'All' || p.status === status)
      .filter((p) => !q || p.fundName.toLowerCase().includes(q) || p.fundCode.toLowerCase().includes(q))
      .sort((a, b) => {
        const va = value(a), vb = value(b);
        const c = typeof va === 'string' ? va.localeCompare(vb as string) : va - (vb as number);
        return sort.dir === 'asc' ? c : -c;
      });
  }, [account, query, status, sort, activeValue]);

  const active = rows.filter(isActive);
  const tot = { cost: sum(active.map((p) => p.costBasis)), mv: sum(active.map((p) => p.marketValue)) };
  const onSort = (k: SortKey) => setSort((s) => nextSort(s, k, TEXT_KEYS));
  const h = (label: string, key: SortKey, align?: 'r') => <SortHeader label={label} sortKey={key} sort={sort} onSort={onSort} align={align} />;

  return (
    <div className="panel">
      <div className="toolbar">
        <div className="field">
          <SearchIcon />
          <input ref={searchRef} value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search fund name or code" aria-label="Search positions" />
        </div>
        <div className="chips" role="group" aria-label="Position status">
          {STATUSES.map((s) => (
            <button key={s} type="button" className="chip" aria-pressed={status === s} onClick={() => setStatus(s)}>{s} <span className="num">{counts[s]}</span></button>
          ))}
        </div>
        <div className="seg" role="group" aria-label="Layout">
          <button type="button" aria-pressed={layout === 'table'} onClick={() => setLayout('table')}>Table</button>
          <button type="button" aria-pressed={layout === 'cards'} onClick={() => setLayout('cards')}>Cards</button>
        </div>
      </div>
      {rows.length === 0 ? (
        <Empty title="No positions match">
          <span>Try a different search or status filter.</span>
          <button className="link" type="button" onClick={() => { setQuery(''); setStatus('All'); }}>Clear filters</button>
        </Empty>
      ) : layout === 'table' ? (
        <div className="tbl-scroll">
          <table aria-label="Positions">
            <thead>
              <tr>
                {h('Fund', 'fund')}{h('Class', 'assetClass')}{h('Units', 'units', 'r')}{h('NAV', 'nav', 'r')}
                {h('Cost basis', 'costBasis', 'r')}{h('Market value', 'marketValue', 'r')}{h('Gain/loss', 'gain', 'r')}
                {h('Weight', 'weight', 'r')}{h('Status', 'status')}
              </tr>
            </thead>
            <tbody>
              {rows.map((p) => {
                const g = positionGain(p);
                return (
                  <tr key={p.fundCode}>
                    <td className="fundcell"><b>{p.fundName}</b><small>{p.fundCode}</small></td>
                    <td className="muted">{ASSET_CLASS_LABELS[p.assetClass]}</td>
                    <td className="r num">{units3.format(p.units)}{p.settlingUnits > 0 && <div className="muted" style={{ fontSize: 11.5 }}>+{units3.format(p.settlingUnits)} settling</div>}</td>
                    <td className="r num">{usd.format(p.nav)}</td>
                    <td className="r num">{p.status === 'Closed' ? '—' : usd.format(p.costBasis)}</td>
                    <td className="r num">{p.status === 'Closed' ? '—' : usd.format(p.marketValue)}</td>
                    <td className="r num"><GainLoss value={g.gain} /><div style={{ fontSize: 11.5 }}>{p.status === 'Closed' ? <span className="muted">realized</span> : <GainPct value={g.pct} />}</div></td>
                    <td className="r num">{isActive(p) ? <span className="weight">{pct2.format(weight(p))}<span className="bar"><i style={{ width: `${weight(p) * 100}%` }} /></span></span> : <span className="muted">—</span>}</td>
                    <td><StatusPill status={p.status} /></td>
                  </tr>
                );
              })}
            </tbody>
            {active.length > 0 && (
              <tfoot>
                <tr>
                  <td colSpan={4}>Active totals · {active.length} positions</td>
                  <td className="r num">{usd.format(tot.cost)}</td>
                  <td className="r num">{usd.format(tot.mv)}</td>
                  <td className="r num"><GainLoss value={tot.mv - tot.cost} /></td>
                  <td colSpan={2} />
                </tr>
              </tfoot>
            )}
          </table>
        </div>
      ) : (
        <div className="cards">
          {rows.map((p) => {
            const g = positionGain(p);
            return (
              <article className="pcard" key={p.fundCode} aria-label={p.fundName}>
                <div className="pcard-h"><div><b>{p.fundName}</b><small>{p.fundCode} · {ASSET_CLASS_LABELS[p.assetClass]}</small></div><StatusPill status={p.status} /></div>
                <div className="mv">{p.status === 'Closed' ? '—' : usd.format(p.marketValue)}</div>
                <dl>
                  <div><dt>Units</dt><dd>{units3.format(p.units)}{p.settlingUnits > 0 && <div className="muted">+{units3.format(p.settlingUnits)} settling</div>}</dd></div>
                  <div><dt>NAV</dt><dd>{usd.format(p.nav)}</dd></div>
                  <div><dt>Cost basis</dt><dd>{p.status === 'Closed' ? '—' : usd.format(p.costBasis)}</dd></div>
                  <div><dt>{p.status === 'Closed' ? 'Realized' : 'Gain/loss'}</dt><dd><GainLoss value={g.gain} /> {p.status !== 'Closed' && <GainPct value={g.pct} />}</dd></div>
                  <div><dt>Weight</dt><dd>{isActive(p) ? pct2.format(weight(p)) : '—'}</dd></div>
                </dl>
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
}

import { useEffect, useMemo, useState, type RefObject } from 'react';
import type { AccountDetail, Transaction, TransactionStatus, TransactionType } from '../api/types';
import { formatDate, signed, units3, usd } from '../lib/format';
import { sum } from '../lib/metrics';
import { nextSort } from '../lib/sort';
import { Empty, SearchIcon, SortHeader, StatusPill, TypeCell } from './shared';

type SortKey = 'date' | 'id' | 'type' | 'fund' | 'units' | 'price' | 'amount' | 'status';
const TYPES: TransactionType[] = ['Buy', 'Sell', 'Transfer', 'Fee'];
const STATUSES: TransactionStatus[] = ['Pending', 'Done', 'Failed', 'Reversed'];
const PAGE_SIZES = [10, 25, 50];
const TEXT_KEYS: SortKey[] = ['id', 'type', 'fund', 'status'];

function pageList(page: number, pages: number): (number | 'gap')[] {
  if (pages <= 7) return Array.from({ length: pages }, (_, i) => i + 1);
  const out: (number | 'gap')[] = [1];
  const lo = Math.max(2, page - 1), hi = Math.min(pages - 1, page + 1);
  if (lo > 2) out.push('gap');
  for (let i = lo; i <= hi; i++) out.push(i);
  if (hi < pages - 1) out.push('gap');
  out.push(pages);
  return out;
}

export function Transactions({ account, searchRef, active }: { account: AccountDetail; searchRef: RefObject<HTMLInputElement>; active: boolean }) {
  const [query, setQuery] = useState('');
  const [types, setTypes] = useState<TransactionType[]>([]);
  const [status, setStatus] = useState<'all' | TransactionStatus>('all');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [sort, setSort] = useState<{ key: SortKey; dir: 'asc' | 'desc' }>({ key: 'date', dir: 'desc' });
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    const value = (t: Transaction): string | number => {
      switch (sort.key) {
        case 'fund': return t.fundName;
        case 'units': return t.units * t.direction;
        case 'amount': return t.amount * t.direction;
        default: return t[sort.key];
      }
    };
    return account.transactions
      .filter((t) => types.length === 0 || types.includes(t.type))
      .filter((t) => status === 'all' || t.status === status)
      .filter((t) => (!from || t.date >= from) && (!to || t.date <= to))
      .filter((t) => !q || [t.fundName, t.fundCode, t.id].some((s) => s.toLowerCase().includes(q)))
      .sort((a, b) => {
        const va = value(a), vb = value(b);
        let c = typeof va === 'string' ? va.localeCompare(vb as string) : va - (vb as number);
        if (c === 0) c = a.id.localeCompare(b.id);
        return sort.dir === 'asc' ? c : -c;
      });
  }, [account, query, types, status, from, to, sort]);

  const pages = Math.max(1, Math.ceil(filtered.length / pageSize));
  const current = Math.min(page, pages);
  const rows = filtered.slice((current - 1) * pageSize, current * pageSize);
  const settled = filtered.filter((t) => t.status === 'Done');
  const totals = {
    inflow: sum(settled.filter((t) => t.direction > 0).map((t) => t.amount)),
    outflow: sum(settled.filter((t) => t.direction < 0 && t.type !== 'Fee').map((t) => t.amount)),
    fees: sum(settled.filter((t) => t.type === 'Fee').map((t) => t.amount)),
  };
  const filtersOn = query || types.length || status !== 'all' || from || to;

  const update = <T,>(set: (v: T) => void) => (v: T) => { set(v); setPage(1); };
  const clear = () => { setQuery(''); setTypes([]); setStatus('all'); setFrom(''); setTo(''); setPage(1); };
  const onSort = (k: SortKey) => { setSort((s) => nextSort(s, k, TEXT_KEYS)); setPage(1); };
  const h = (label: string, key: SortKey, align?: 'r') => <SortHeader label={label} sortKey={key} sort={sort} onSort={onSort} align={align} />;

  useEffect(() => {
    if (!active) return;
    const onKey = (e: KeyboardEvent) => {
      const el = e.target as HTMLElement;
      if (e.metaKey || e.ctrlKey || e.altKey || el.closest('input,select,textarea')) return;
      if (e.key === '[') setPage((p) => Math.max(1, Math.min(p, pages) - 1));
      if (e.key === ']') setPage((p) => Math.min(pages, p + 1));
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [active, pages]);

  return (
    <div className="panel">
      <div className="toolbar">
        <div className="field">
          <SearchIcon />
          <input ref={searchRef} value={query} onChange={(e) => update(setQuery)(e.target.value)} placeholder="Search fund or transaction ID" aria-label="Search transactions" />
        </div>
        <div className="chips" role="group" aria-label="Transaction type">
          {TYPES.map((t) => (
            <button key={t} type="button" className="chip" aria-pressed={types.includes(t)}
              onClick={() => update(setTypes)(types.includes(t) ? types.filter((x) => x !== t) : [...types, t])}>{t}</button>
          ))}
        </div>
        <select className="ctl" aria-label="Status" value={status} onChange={(e) => update(setStatus)(e.target.value as 'all' | TransactionStatus)}>
          <option value="all">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
        <div className="ctl-group">
          <label htmlFor="tx-from">From</label>
          <input className="ctl" type="date" id="tx-from" value={from} max={to || undefined} onChange={(e) => update(setFrom)(e.target.value)} />
          <label htmlFor="tx-to">to</label>
          <input className="ctl" type="date" id="tx-to" value={to} min={from || undefined} onChange={(e) => update(setTo)(e.target.value)} />
        </div>
        {filtersOn ? <button className="link" type="button" onClick={clear}>Clear filters</button> : null}
      </div>
      <div className="summary" data-testid="tx-summary">
        <span><b>{filtered.length}</b> of {account.transactions.length} transactions</span>
        <span>Settled in <b>{usd.format(totals.inflow)}</b></span>
        <span>Settled out <b>{usd.format(totals.outflow)}</b></span>
        <span>Fees <b>{usd.format(totals.fees)}</b></span>
      </div>
      {rows.length === 0 ? (
        <Empty title="No transactions match these filters">
          <button className="link" type="button" onClick={clear}>Clear filters</button>
        </Empty>
      ) : (
        <div className="tbl-scroll">
          <table id="tx-table" aria-label="Transactions">
            <thead>
              <tr>{h('Date', 'date')}{h('ID', 'id')}{h('Type', 'type')}{h('Fund', 'fund')}{h('Units', 'units', 'r')}{h('Price', 'price', 'r')}{h('Amount', 'amount', 'r')}{h('Status', 'status')}</tr>
            </thead>
            <tbody>
              {rows.map((t) => {
                const voided = t.status === 'Failed' || t.status === 'Reversed';
                return (
                  <tr key={t.id}>
                    <td className="num">{formatDate(t.date)}</td>
                    <td className="mono muted">{t.id}</td>
                    <td><TypeCell type={t.type} direction={t.direction} /></td>
                    <td className="fundcell wide"><b>{t.fundName}</b><small>{t.fundCode}</small></td>
                    <td className="r num" data-l="Units">{signed(t.units * t.direction, units3)}</td>
                    <td className="r num" data-l="Price">{usd.format(t.price)}</td>
                    <td className={`r num ${voided ? 'struck' : ''}`} data-l="Amount">{signed(t.amount * t.direction)}</td>
                    <td><StatusPill status={t.status} /></td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
      <div className="pager">
        <span>
          {filtered.length ? `${(current - 1) * pageSize + 1}–${Math.min(current * pageSize, filtered.length)} of ${filtered.length}` : '0 results'}
        </span>
        <label className="ctl-group">
          Rows
          <select className="ctl" value={pageSize} onChange={(e) => { setPageSize(Number(e.target.value)); setPage(1); }} aria-label="Rows per page">
            {PAGE_SIZES.map((n) => <option key={n} value={n}>{n}</option>)}
          </select>
        </label>
        <nav className="pages" aria-label="Pagination">
          <button type="button" onClick={() => setPage(current - 1)} disabled={current <= 1} aria-label="Previous page">‹</button>
          {pageList(current, pages).map((p, i) =>
            p === 'gap' ? <span key={`g${i}`} className="gap">…</span> : (
              <button key={p} type="button" aria-current={p === current ? 'page' : undefined} onClick={() => setPage(p)}>{p}</button>
            ),
          )}
          <button type="button" onClick={() => setPage(current + 1)} disabled={current >= pages} aria-label="Next page">›</button>
        </nav>
      </div>
    </div>
  );
}

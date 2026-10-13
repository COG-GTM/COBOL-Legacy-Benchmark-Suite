/**
 * Synthetic data for local development and demos. The legacy repo has no account or
 * position test data, so funds, accounts and prices here are fictional.
 */
import {
  ApiError,
  type AccountDetail,
  type AccountSummary,
  type AssetClass,
  type PortfolioApi,
  type Position,
  type Transaction,
  type TransactionStatus,
  type TransactionType,
  type ValuePoint,
} from './types';

const DAY = 86_400_000;
export const AS_OF = Date.UTC(2026, 9, 13);

interface Fund { name: string; cls: AssetClass; nav: number; mu: number; a: number; b: number; ph: number }
const FUNDS: Record<string, Fund> = {
  NTMIX: { name: 'Northfield Total Market Index', cls: 'us', nav: 142.18, mu: 0.11, a: 0.05, b: 0.012, ph: 0.3 },
  STLFX: { name: 'Summit Tech Leaders', cls: 'us', nav: 211.66, mu: 0.21, a: 0.08, b: 0.02, ph: 1.1 },
  MSCVX: { name: 'Meridian Small Cap Value', cls: 'us', nav: 27.91, mu: -0.05, a: 0.06, b: 0.015, ph: 2.2 },
  BDGFX: { name: 'Bluestem Dividend Growth', cls: 'us', nav: 64.3, mu: 0.07, a: 0.03, b: 0.01, ph: 0.9 },
  CIEFX: { name: 'Calder International Equity', cls: 'intl', nav: 38.64, mu: 0.08, a: 0.05, b: 0.012, ph: 2.8 },
  AEMKX: { name: 'Altamira Emerging Markets', cls: 'intl', nav: 21.47, mu: -0.07, a: 0.07, b: 0.02, ph: 0.5 },
  HPBIX: { name: 'Harbor Point Aggregate Bond', cls: 'bond', nav: 10.42, mu: 0.015, a: 0.012, b: 0.004, ph: 1.7 },
  GSTTX: { name: 'Granite Short-Term Treasury', cls: 'cash', nav: 9.98, mu: 0.004, a: 0.003, b: 0.001, ph: 0.2 },
  CRIFX: { name: 'Copperline REIT Index', cls: 're', nav: 18.05, mu: -0.03, a: 0.06, b: 0.015, ph: 3.1 },
};

type Plan = 'hold' | 'close' | 'pending';
interface Seed { no: string; name: string; type: string; opened: number; seed: number; funds: [string, Plan][] }
const SEEDS: Seed[] = [
  { no: '4471002893', name: 'Okafor Family Trust', type: 'Revocable trust', opened: -560, seed: 11,
    funds: [['NTMIX', 'hold'], ['STLFX', 'hold'], ['CIEFX', 'hold'], ['HPBIX', 'hold'], ['MSCVX', 'close'], ['CRIFX', 'hold'], ['AEMKX', 'pending']] },
  { no: '4471005512', name: 'R. Lindqvist Rollover IRA', type: 'Traditional IRA', opened: -500, seed: 29,
    funds: [['BDGFX', 'hold'], ['HPBIX', 'hold'], ['GSTTX', 'hold'], ['AEMKX', 'hold'], ['STLFX', 'close']] },
  { no: '4471009047', name: 'Delgado Joint Brokerage', type: 'Joint brokerage', opened: -420, seed: 47,
    funds: [['STLFX', 'hold'], ['NTMIX', 'hold'], ['MSCVX', 'hold'], ['CRIFX', 'close'], ['GSTTX', 'pending'], ['CIEFX', 'hold']] },
];

/** Accounts that exercise the legacy error paths (SECMGR deny, ERRHNDL system error). */
export const DENIED_ACCOUNT = '4471000403';
export const ERROR_ACCOUNT = '4471000500';

function rng(seed: number) {
  let a = seed;
  return () => {
    a |= 0; a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
function price(code: string, t: number) {
  const f = FUNDS[code];
  return f.nav * Math.exp((f.mu * t) / 365) * (1 + f.a * (Math.sin(t / 37 + f.ph) - Math.sin(f.ph)) + f.b * (Math.sin(t / 6.3 + f.ph * 2) - Math.sin(f.ph * 2)));
}
const r2 = (x: number) => Math.round(x * 100) / 100;
const r3 = (x: number) => Math.round(x * 1000) / 1000;
const r4 = (x: number) => Math.round(x * 10000) / 10000;
const weekday = (t: number) => new Date(AS_OF + t * DAY).getUTCDay();
const bday = (t: number) => { while (weekday(t) === 0 || weekday(t) === 6) t--; return t; };
export const isoDay = (t: number) => new Date(AS_OF + t * DAY).toISOString().slice(0, 10);

interface Ev { t: number; type: TransactionType; c: string; amt?: number; frac?: number; dir?: 1 | -1; force?: boolean }

export function buildAccount(s: Seed): AccountDetail {
  const r = rng(s.seed);
  const pick = <T,>(xs: T[]) => xs[Math.floor(r() * xs.length)];
  const small = () => pick([500, 750, 1000, 1500, 2000, 2500]);
  const big = () => pick([5000, 7500, 10000, 15000, 20000, 25000]);
  const O = s.opened;
  const ev: Ev[] = [];
  for (const [c, plan] of s.funds) {
    if (plan === 'hold') {
      ev.push({ t: bday(O + Math.floor(r() * 80)), type: 'Buy', c, amt: big() * (c === 'STLFX' || c === 'NTMIX' ? 2 : 1) });
      for (let t = O + 100; t < 0; t += 28 + Math.floor(r() * 10)) if (r() < 0.5) ev.push({ t: bday(t), type: 'Buy', c, amt: small() });
      if (r() < 0.45) ev.push({ t: bday(-60 - Math.floor(r() * 250)), type: 'Sell', c, frac: 0.15 + r() * 0.25 });
      if (r() < 0.4) ev.push({ t: bday(O + 20 + Math.floor(r() * 60)), type: 'Transfer', c, amt: pick([3000, 6000, 9000, 12000]), dir: 1 });
      if (r() < 0.2) ev.push({ t: bday(-30 - Math.floor(r() * 100)), type: 'Transfer', c, frac: 0.1, dir: -1 });
    } else if (plan === 'close') {
      ev.push({ t: bday(O + Math.floor(r() * 40)), type: 'Buy', c, amt: big() });
      ev.push({ t: bday(O + 70 + Math.floor(r() * 40)), type: 'Buy', c, amt: small() * 2 });
      ev.push({ t: bday(-120 - Math.floor(r() * 80)), type: 'Sell', c, frac: 1, force: true });
    } else {
      ev.push({ t: bday(-1 - Math.floor(r() * 3)), type: 'Buy', c, amt: pick([2500, 5000, 7500]) });
    }
  }
  const topUp = s.funds.find(([, p]) => p === 'hold');
  if (topUp) ev.push({ t: bday(-2), type: 'Buy', c: topUp[0], amt: 2500 });
  const feeFund = (s.funds.find(([c, p]) => p === 'hold' && (FUNDS[c].cls === 'bond' || FUNDS[c].cls === 'cash')) ?? s.funds[0])[0];
  for (let t = O + 91; t < 0; t += 91) ev.push({ t: bday(t), type: 'Fee', c: feeFund, amt: r2(18 + r() * 42) });
  ev.sort((a, b) => a.t - b.t);

  const held: Record<string, number> = {}, cost: Record<string, number> = {}, real: Record<string, number> = {};
  const pend: Record<string, { u: number; a: number }> = {};
  let id = Number(s.no.slice(-4)) * 100;
  const tx: (Transaction & { t: number })[] = [];
  for (const e of ev) {
    const p = r4(price(e.c, e.t));
    const inbound = e.type === 'Buy' || (e.type === 'Transfer' && e.dir === 1);
    let units: number, amt: number;
    if (inbound) { units = r3(e.amt! / p); amt = r2(units * p); }
    else if (e.type === 'Fee') { units = r3(e.amt! / p); amt = e.amt!; }
    else { units = r3((held[e.c] ?? 0) * e.frac!); amt = r2(units * p); }
    if (units <= 0) continue;
    let status: TransactionStatus = 'Done';
    if (e.t >= -4) status = 'Pending';
    else if (!e.force && (e.type === 'Buy' || e.type === 'Sell')) { const x = r(); if (x < 0.035) status = 'Failed'; else if (x < 0.06) status = 'Reversed'; }
    const direction: 1 | -1 = inbound ? 1 : -1;
    tx.push({ id: 'TX' + String(++id).padStart(7, '0'), t: e.t, date: isoDay(e.t), type: e.type, fundCode: e.c, fundName: FUNDS[e.c].name, units, price: p, amount: amt, status, direction });
    if (status === 'Pending' && inbound) { pend[e.c] ??= { u: 0, a: 0 }; pend[e.c].u += units; pend[e.c].a += amt; }
    if (status !== 'Done') continue;
    const h = held[e.c] ?? 0, cb = cost[e.c] ?? 0;
    if (inbound) { held[e.c] = h + units; cost[e.c] = cb + amt; }
    else {
      const share = h ? Math.min(1, units / h) : 0, cpart = cb * share;
      held[e.c] = Math.max(0, h - units); cost[e.c] = cb - cpart;
      if (e.type === 'Sell') real[e.c] = (real[e.c] ?? 0) + amt - cpart;
    }
  }

  const positions: Position[] = s.funds.map(([c]) => {
    const f = FUNDS[c], u = held[c] ?? 0;
    const base = { fundCode: c, fundName: f.name, assetClass: f.cls, nav: f.nav, realizedGain: real[c] ?? 0, settlingUnits: 0 };
    if (u > 0.0005) return { ...base, status: 'Active', units: u, costBasis: cost[c], marketValue: u * f.nav, previousValue: u * price(c, bday(-1)), settlingUnits: pend[c]?.u ?? 0 };
    if (pend[c]) return { ...base, status: 'Pending', units: pend[c].u, costBasis: pend[c].a, marketValue: pend[c].u * f.nav, previousValue: 0 };
    return { ...base, status: 'Closed', units: 0, costBasis: 0, marketValue: 0, previousValue: 0 };
  });

  const done = tx.filter((x) => x.status === 'Done').sort((a, b) => a.t - b.t);
  const valueHistory: ValuePoint[] = [];
  const H: Record<string, number> = {}, C: Record<string, number> = {};
  let i = 0;
  for (let t = O; ; t += 3) {
    if (t > 0) t = 0;
    while (i < done.length && done[i].t <= t) {
      const x = done[i++], h = H[x.fundCode] ?? 0, cb = C[x.fundCode] ?? 0;
      if (x.direction > 0) { H[x.fundCode] = h + x.units; C[x.fundCode] = cb + x.amount; }
      else { const sh = h ? Math.min(1, x.units / h) : 0; H[x.fundCode] = Math.max(0, h - x.units); C[x.fundCode] = cb * (1 - sh); }
    }
    let v = 0, cs = 0;
    for (const k in H) { v += H[k] * price(k, t); cs += C[k]; }
    valueHistory.push({ date: isoDay(t), marketValue: v, costBasis: cs });
    if (t === 0) break;
  }
  tx.sort((a, b) => b.t - a.t || b.id.localeCompare(a.id));
  return {
    accountNo: s.no, name: s.name, accountType: s.type, openedDate: isoDay(O), asOfDate: isoDay(0),
    positions, transactions: tx.map(({ t: _t, ...rest }) => rest), valueHistory,
  };
}

const delay = (ms: number) => new Promise((res) => setTimeout(res, ms));

export function createMockApi(latencyMs = 250): PortfolioApi {
  const cache = new Map<string, AccountDetail>();
  return {
    async listAccounts(): Promise<AccountSummary[]> {
      await delay(latencyMs / 2);
      return SEEDS.map((s) => ({ accountNo: s.no, name: s.name }));
    },
    async getAccount(accountNo) {
      await delay(latencyMs);
      if (accountNo === DENIED_ACCOUNT) throw new ApiError('ACCESS_DENIED', 'Access denied');
      if (accountNo === ERROR_ACCOUNT) throw new ApiError('SYSTEM', 'Error accessing position data', 'DB2-0904', 'A7F3C2E19B04D611');
      const seed = SEEDS.find((s) => s.no === accountNo);
      if (!seed) throw new ApiError('NOT_FOUND', 'Position not found for account');
      if (!cache.has(accountNo)) cache.set(accountNo, buildAccount(seed));
      return structuredClone(cache.get(accountNo)!);
    },
  };
}

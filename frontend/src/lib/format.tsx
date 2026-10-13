export const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });
export const usd0 = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 });
export const usdCompact = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', notation: 'compact', maximumFractionDigits: 1 });
export const units3 = new Intl.NumberFormat('en-US', { minimumFractionDigits: 3, maximumFractionDigits: 3 });
export const pct2 = new Intl.NumberFormat('en-US', { style: 'percent', minimumFractionDigits: 2, maximumFractionDigits: 2 });
const dateFmt = new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric', timeZone: 'UTC' });
export const monthFmt = new Intl.DateTimeFormat('en-US', { month: 'short', timeZone: 'UTC' });
export const monthYearFmt = new Intl.DateTimeFormat('en-US', { month: 'short', year: '2-digit', timeZone: 'UTC' });

export const formatDate = (iso: string) => dateFmt.format(new Date(`${iso}T00:00:00Z`));

export function signed(v: number, f: Intl.NumberFormat = usd): string {
  return (v > 0 ? '+' : v < 0 ? '−' : '') + f.format(Math.abs(v));
}

const tone = (v: number) => (v > 0 ? 'up' : v < 0 ? 'down' : 'muted');

export function GainLoss({ value, format = usd }: { value: number; format?: Intl.NumberFormat }) {
  return (
    <span className={tone(value)}>
      {value > 0 ? '▲ ' : value < 0 ? '▼ ' : ''}
      {signed(value, format)}
    </span>
  );
}

export function GainPct({ value }: { value: number | null }) {
  if (value == null) return <span className="muted">—</span>;
  return <span className={tone(value)}>{signed(value, pct2)}</span>;
}

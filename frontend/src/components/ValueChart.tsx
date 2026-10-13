import { useLayoutEffect, useMemo, useRef, useState } from 'react';
import type { ValuePoint } from '../api/types';
import { formatDate, monthFmt, monthYearFmt, usd0, usdCompact } from '../lib/format';

const H = 250;
const PAD = { l: 56, r: 12, t: 12, b: 26 };

export function ValueChart({ points }: { points: ValuePoint[] }) {
  const wrap = useRef<HTMLDivElement>(null);
  const [width, setWidth] = useState(720);
  const [hover, setHover] = useState<number | null>(null);

  useLayoutEffect(() => {
    const el = wrap.current;
    if (!el) return;
    const measure = () => setWidth(Math.max(280, el.clientWidth - 20));
    measure();
    if (typeof ResizeObserver === 'undefined') return;
    const ro = new ResizeObserver(measure);
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  const geo = useMemo(() => {
    const vals = points.flatMap((p) => [p.marketValue, p.costBasis]);
    let lo = Math.min(...vals), hi = Math.max(...vals);
    const pad = (hi - lo) * 0.08 || hi * 0.05 || 1;
    lo = Math.max(0, lo - pad); hi += pad;
    const iw = width - PAD.l - PAD.r, ih = H - PAD.t - PAD.b;
    const x = (i: number) => PAD.l + (points.length > 1 ? (i / (points.length - 1)) * iw : iw / 2);
    const y = (v: number) => PAD.t + ih - ((v - lo) / (hi - lo)) * ih;
    const line = (k: 'marketValue' | 'costBasis') => points.map((p, i) => `${i ? 'L' : 'M'}${x(i).toFixed(1)},${y(p[k]).toFixed(1)}`).join('');
    const ticks = Array.from({ length: 4 }, (_, i) => lo + ((hi - lo) * i) / 3);
    const span = points.length > 1 ? (Date.parse(points[points.length - 1].date) - Date.parse(points[0].date)) / 86_400_000 : 0;
    const fmt = span > 400 ? monthYearFmt : monthFmt;
    const xlabels: { i: number; label: string }[] = [];
    let lastMonth = '';
    points.forEach((p, i) => {
      const m = p.date.slice(0, 7);
      if (m !== lastMonth) { lastMonth = m; if (i > 0) xlabels.push({ i, label: fmt.format(new Date(`${p.date}T00:00:00Z`)) }); }
    });
    const step = Math.ceil(xlabels.length / Math.max(2, Math.floor(iw / 70)));
    return { x, y, mv: line('marketValue'), cb: line('costBasis'), ticks, xlabels: xlabels.filter((_, k) => k % step === 0), iw, ih };
  }, [points, width]);

  if (points.length === 0) return null;
  const area = `${geo.mv}L${geo.x(points.length - 1)},${PAD.t + geo.ih}L${geo.x(0)},${PAD.t + geo.ih}Z`;
  const hp = hover != null ? points[hover] : null;

  function onMove(e: React.PointerEvent<SVGSVGElement>) {
    const rect = e.currentTarget.getBoundingClientRect();
    const px = ((e.clientX - rect.left) / rect.width) * width;
    const i = Math.round(((px - PAD.l) / geo.iw) * (points.length - 1));
    setHover(Math.min(points.length - 1, Math.max(0, i)));
  }

  return (
    <div className="chart-wrap" ref={wrap}>
      <svg id="chart" viewBox={`0 0 ${width} ${H}`} role="img" aria-label={`Market value from ${formatDate(points[0].date)} to ${formatDate(points[points.length - 1].date)}`} onPointerMove={onMove} onPointerLeave={() => setHover(null)}>
        <defs>
          <linearGradient id="mvfill" x1="0" x2="0" y1="0" y2="1">
            <stop offset="0" stopColor="var(--accent)" stopOpacity=".18" />
            <stop offset="1" stopColor="var(--accent)" stopOpacity="0" />
          </linearGradient>
        </defs>
        {geo.ticks.map((t) => (
          <g key={t}>
            <line x1={PAD.l} x2={width - PAD.r} y1={geo.y(t)} y2={geo.y(t)} stroke="var(--line)" />
            <text x={PAD.l - 8} y={geo.y(t) + 3} textAnchor="end">{usdCompact.format(t)}</text>
          </g>
        ))}
        {geo.xlabels.map((l) => <text key={l.i} x={geo.x(l.i)} y={H - 8} textAnchor="middle">{l.label}</text>)}
        <path d={area} fill="url(#mvfill)" />
        <path d={geo.cb} fill="none" stroke="var(--faint)" strokeWidth="1.5" strokeDasharray="4 4" />
        <path d={geo.mv} fill="none" stroke="var(--accent)" strokeWidth="2" strokeLinejoin="round" />
        {hp && hover != null && (
          <g>
            <line x1={geo.x(hover)} x2={geo.x(hover)} y1={PAD.t} y2={PAD.t + geo.ih} stroke="var(--faint)" />
            <circle cx={geo.x(hover)} cy={geo.y(hp.marketValue)} r="4" fill="var(--accent)" stroke="var(--surface)" strokeWidth="2" />
          </g>
        )}
      </svg>
      {hp && hover != null && (
        <div className="tip" style={{ left: `${(geo.x(hover) / width) * 100}%`, top: geo.y(hp.marketValue) + 8 }}>
          <span>{formatDate(hp.date)}</span>
          <span className="mono">{usd0.format(hp.marketValue)}</span>
          <span className="mono" style={{ opacity: 0.7 }}>Cost {usd0.format(hp.costBasis)}</span>
        </div>
      )}
    </div>
  );
}

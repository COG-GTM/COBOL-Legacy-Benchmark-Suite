import { toIsoDate } from '../../utils/date';

/**
 * Colour class for a signed decimal string. Break-even (0.00) stays neutral,
 * so only real gains and losses are highlighted.
 */
export function valueTone(value: string | null): string {
  if (value === null) return '';
  if (value.startsWith('-')) return 'value--negative';
  if (/^0(\.0+)?$/.test(value)) return '';
  return 'value--positive';
}

/**
 * Colour class for a rate where a higher number is worse (error and failure
 * rates), inverted relative to {@link valueTone}.
 */
export function rateTone(value: string | null): string {
  if (value === null || /^0(\.0+)?$/.test(value)) return '';
  return value.startsWith('-') ? 'value--positive' : 'value--negative';
}

/** Human-readable period label for report headings and exports. */
export function periodLabel(fromDate?: string, toDate?: string): string {
  const from = toIsoDate(fromDate ?? '');
  const to = toIsoDate(toDate ?? '');
  if (!from && !to) return 'All dates';
  if (!from) return `Through ${to}`;
  if (!to) return `From ${from}`;
  return `${from} to ${to}`;
}

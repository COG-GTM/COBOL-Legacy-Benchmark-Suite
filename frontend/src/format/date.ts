const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

/** Dates stay ISO YYYY-MM-DD (FR-019). Compact legacy YYYYMMDD (POS-DATE) is converted. */
export function formatIsoDate(value: string | null | undefined): string {
  if (!value) return '';
  if (ISO_DATE_RE.test(value)) return value;
  if (/^\d{8}$/.test(value)) return `${value.slice(0, 4)}-${value.slice(4, 6)}-${value.slice(6, 8)}`;
  return value;
}

export function formatTime(value: string | null | undefined): string {
  return value ?? '';
}

/** RFC 3339 timestamp shown as "YYYY-MM-DD HH:MM:SS UTC" without locale/timezone drift. */
export function formatTimestamp(value: string | null | undefined): string {
  if (!value) return '';
  const match = /^(\d{4}-\d{2}-\d{2})T(\d{2}:\d{2}:\d{2})/.exec(value);
  return match ? `${match[1]} ${match[2]} UTC` : value;
}

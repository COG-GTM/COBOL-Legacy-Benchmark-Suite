/**
 * Helpers for the COBOL `PIC 9(8)` date fields (YYYYMMDD) used by
 * PORT-CREATE-DATE / PORT-LAST-MAINT.
 */

const YYYYMMDD_RE = /^\d{8}$/;

/** Formats a YYYYMMDD string as `YYYY-MM-DD`; returns input if malformed. */
export function formatCobolDate(value: string): string {
  if (!YYYYMMDD_RE.test(value)) {
    return value || '—';
  }
  return `${value.slice(0, 4)}-${value.slice(4, 6)}-${value.slice(6, 8)}`;
}

/** Returns today's date as a YYYYMMDD string. */
export function todayCobolDate(now: Date = new Date()): string {
  const yyyy = now.getFullYear().toString().padStart(4, '0');
  const mm = (now.getMonth() + 1).toString().padStart(2, '0');
  const dd = now.getDate().toString().padStart(2, '0');
  return `${yyyy}${mm}${dd}`;
}

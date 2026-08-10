/**
 * Helpers for the COBOL date/time fields: `PIC 9(8)` / `PIC X(8)` dates
 * (YYYYMMDD, e.g. HIST-DATE, PORT-CREATE-DATE) and `PIC X(6)` times
 * (HHMMSS, e.g. HIST-TIME).
 */

const YYYYMMDD_RE = /^\d{8}$/;
const HHMMSS_RE = /^\d{6}$/;
const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

/** Formats a YYYYMMDD string as `YYYY-MM-DD`; returns input if malformed. */
export function formatCobolDate(value: string): string {
  if (!YYYYMMDD_RE.test(value)) {
    return value || '—';
  }
  return `${value.slice(0, 4)}-${value.slice(4, 6)}-${value.slice(6, 8)}`;
}

/** Formats an HHMMSS string as `HH:MM:SS`; returns input if malformed. */
export function formatCobolTime(value: string): string {
  if (!HHMMSS_RE.test(value)) {
    return value || '—';
  }
  return `${value.slice(0, 2)}:${value.slice(2, 4)}:${value.slice(4, 6)}`;
}

/**
 * Converts the `YYYY-MM-DD` value of an `<input type="date">` into the
 * YYYYMMDD form used by the COBOL records. Empty/malformed input yields ''.
 */
export function toCobolDate(isoDate: string): string {
  return ISO_DATE_RE.test(isoDate) ? isoDate.replace(/-/g, '') : '';
}

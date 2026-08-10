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

/**
 * Formats a YYYYMMDD string as `YYYY-MM-DD`, or as an empty string when the
 * value is missing or malformed — for `<input type="date">` values and other
 * places where a placeholder dash would be wrong.
 */
export function toIsoDate(value: string): string {
  return YYYYMMDD_RE.test(value) ? formatCobolDate(value) : '';
}

/**
 * Extracts the YYYYMMDD date from a `PIC X(26)` DB2 timestamp of the form
 * `YYYY-MM-DD-hh.mm.ss.ffffff` (AUD-TIMESTAMP, POS-LAST-MAINT-DATE).
 */
export function cobolTimestampDate(timestamp: string): string {
  return timestamp.slice(0, 10).replace(/-/g, '');
}

/** Renders a `PIC X(26)` timestamp as `YYYY-MM-DD hh:mm:ss` for display. */
export function formatCobolTimestamp(timestamp: string): string {
  const date = timestamp.slice(0, 10);
  const time = timestamp.slice(11, 19).replace(/\./g, ':');
  if (!YYYYMMDD_RE.test(date.replace(/-/g, '')) || time.length < 8) {
    return timestamp || '—';
  }
  return `${date} ${time}`;
}

/** Shifts a YYYYMMDD date by `days` (negative shifts backwards). */
export function shiftCobolDate(value: string, days: number): string {
  if (!YYYYMMDD_RE.test(value)) {
    return value;
  }
  const date = new Date(
    Date.UTC(
      Number(value.slice(0, 4)),
      Number(value.slice(4, 6)) - 1,
      Number(value.slice(6, 8)),
    ),
  );
  date.setUTCDate(date.getUTCDate() + days);
  const yyyy = date.getUTCFullYear().toString().padStart(4, '0');
  const mm = (date.getUTCMonth() + 1).toString().padStart(2, '0');
  const dd = date.getUTCDate().toString().padStart(2, '0');
  return `${yyyy}${mm}${dd}`;
}

/** Number of days covered by an inclusive YYYYMMDD range (minimum 1). */
export function inclusiveDayCount(fromDate: string, toDate: string): number {
  if (!YYYYMMDD_RE.test(fromDate) || !YYYYMMDD_RE.test(toDate)) {
    return 1;
  }
  const from = Date.UTC(
    Number(fromDate.slice(0, 4)),
    Number(fromDate.slice(4, 6)) - 1,
    Number(fromDate.slice(6, 8)),
  );
  const to = Date.UTC(
    Number(toDate.slice(0, 4)),
    Number(toDate.slice(4, 6)) - 1,
    Number(toDate.slice(6, 8)),
  );
  const days = Math.floor((to - from) / 86_400_000) + 1;
  return days > 0 ? days : 1;
}

/** Returns today's date as a YYYYMMDD string. */
export function todayCobolDate(now: Date = new Date()): string {
  const yyyy = now.getFullYear().toString().padStart(4, '0');
  const mm = (now.getMonth() + 1).toString().padStart(2, '0');
  const dd = now.getDate().toString().padStart(2, '0');
  return `${yyyy}${mm}${dd}`;
}

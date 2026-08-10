import type { ReportDocument } from './reportDocument';

/** Quotes a CSV field only when it contains a delimiter, quote or newline. */
function escapeField(value: string): string {
  return /[",\r\n]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value;
}

/**
 * Renders a report as RFC 4180 CSV: the meta entries as `label,value` lines,
 * a blank separator line, then the column headings, detail lines and totals.
 * Cells are exported exactly as displayed so the spreadsheet matches the screen.
 */
export function toCsv(document: ReportDocument): string {
  const lines: string[] = [];
  lines.push(escapeField(document.title));
  for (const entry of document.meta ?? []) {
    lines.push([entry.label, entry.value].map(escapeField).join(','));
  }
  lines.push('');
  lines.push(
    document.columns.map((column) => escapeField(column.label)).join(','),
  );
  for (const row of document.rows) {
    lines.push(row.map(escapeField).join(','));
  }
  if (document.totalsRow) {
    lines.push(document.totalsRow.map(escapeField).join(','));
  }
  return `${lines.join('\r\n')}\r\n`;
}

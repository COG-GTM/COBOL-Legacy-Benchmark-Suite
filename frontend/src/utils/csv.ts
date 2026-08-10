/** RFC 4180 CSV helpers for exporting inquiry results. */

/** Quotes a field when it contains a delimiter, quote, or line break. */
function escapeField(value: string): string {
  return /[",\r\n]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value;
}

/** Serializes a header row plus data rows into CSV text (CRLF line endings). */
export function toCsv(
  headers: readonly string[],
  rows: readonly (readonly string[])[],
): string {
  return [headers, ...rows]
    .map((row) => row.map(escapeField).join(','))
    .join('\r\n');
}

/** Triggers a browser download of `content` as `filename`. */
export function downloadCsv(filename: string, content: string): void {
  const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

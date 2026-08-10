/**
 * A rendered report, independent of the view that produced it.
 *
 * Each report page builds one of these and hands it to the CSV / PDF export
 * helpers, mirroring the way the batch programs write a single RPTFILE record
 * stream that can be printed or archived.
 */
export interface ReportDocument {
  /** Report heading (WS-HEADER2), also used as the export file name stem. */
  title: string;
  /** Column headings, left to right. */
  columns: readonly ReportColumn[];
  /** Detail lines; one array of already-formatted cells per row. */
  rows: readonly (readonly string[])[];
  /** Optional totals line rendered after the detail lines. */
  totalsRow?: readonly string[];
  /** Filter/period annotations printed under the heading (WS-HEADER3). */
  meta?: readonly ReportMetaEntry[];
}

export interface ReportColumn {
  label: string;
  /** Right-aligned columns hold numeric (edited PIC) values. */
  align?: 'left' | 'right';
}

export interface ReportMetaEntry {
  label: string;
  value: string;
}

/** Slugifies a report title into a file name stem, e.g. `position-report`. */
export function reportFileStem(title: string): string {
  return (
    title
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-|-$/g, '') || 'report'
  );
}

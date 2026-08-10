import type { ReportDocument } from './reportDocument';

/**
 * Minimal PDF writer for the report exports.
 *
 * The batch programs write a fixed-width `PIC X(132)` print line stream to
 * RPTFILE, so the PDF reproduces that layout: Courier text, column-aligned
 * detail lines, page breaks every {@link LINES_PER_PAGE} lines. Emitting the
 * PDF by hand keeps the frontend dependency-free — the alternative (jsPDF and
 * friends) would add a renderer we do not otherwise need.
 */

/** US Letter, landscape — wide enough for a 132-column print line at 8pt. */
const PAGE_WIDTH = 792;
const PAGE_HEIGHT = 612;
const MARGIN = 36;
const FONT_SIZE = 8;
const LINE_HEIGHT = 10;
const LINES_PER_PAGE = 52;
const COLUMN_GAP = 2;

/** Right-pads (or left-pads, for numeric columns) a cell to `width`. */
function pad(value: string, width: number, right: boolean): string {
  const clipped = value.length > width ? value.slice(0, width) : value;
  return right ? clipped.padStart(width) : clipped.padEnd(width);
}

/**
 * Renders a report as fixed-width print lines, the modern stand-in for the
 * WS-HEADER / WS-DETAIL record layouts written to RPTFILE.
 */
export function toPrintLines(document: ReportDocument): string[] {
  const widths = document.columns.map((column, index) =>
    [
      column.label,
      ...document.rows.map((row) => row[index] ?? ''),
      ...(document.totalsRow ? [document.totalsRow[index] ?? ''] : []),
    ].reduce((max, cell) => Math.max(max, cell.length), 0),
  );
  const separator = ' '.repeat(COLUMN_GAP);
  const renderRow = (cells: readonly string[]) =>
    document.columns
      .map((column, index) =>
        pad(cells[index] ?? '', widths[index], column.align === 'right'),
      )
      .join(separator)
      .trimEnd();
  const rule = '-'.repeat(
    widths.reduce((sum, width) => sum + width, 0) +
      COLUMN_GAP * Math.max(0, widths.length - 1),
  );

  const lines = [document.title];
  for (const entry of document.meta ?? []) {
    lines.push(`${entry.label}: ${entry.value}`);
  }
  lines.push('', rule, renderRow(document.columns.map((c) => c.label)), rule);
  for (const row of document.rows) {
    lines.push(renderRow(row));
  }
  if (document.totalsRow) {
    lines.push(rule, renderRow(document.totalsRow));
  }
  lines.push(rule);
  return lines;
}

/** Replaces characters the PDF's WinAnsi Courier font cannot represent. */
function toWinAnsi(text: string): string {
  return text
    .replace(/[—–]/g, '-')
    .replace(/[‹«]/g, '<')
    .replace(/[›»]/g, '>')
    .replace(/[^\x20-\x7e\xa0-\xff]/g, '?');
}

function escapeText(text: string): string {
  return toWinAnsi(text).replace(/([\\()])/g, '\\$1');
}

function pageContent(lines: readonly string[]): string {
  const body = lines
    .map((line, index) => {
      const y = PAGE_HEIGHT - MARGIN - (index + 1) * LINE_HEIGHT;
      return `BT /F1 ${FONT_SIZE} Tf ${MARGIN} ${y} Td (${escapeText(line)}) Tj ET`;
    })
    .join('\n');
  return `${body}\n`;
}

function chunk<T>(values: readonly T[], size: number): T[][] {
  const pages: T[][] = [];
  for (let index = 0; index < values.length; index += size) {
    pages.push(values.slice(index, index + size));
  }
  return pages.length ? pages : [[]];
}

/** Encodes a PDF string as bytes; all content is WinAnsi (single byte). */
function toBytes(pdf: string): Uint8Array {
  const bytes = new Uint8Array(pdf.length);
  for (let index = 0; index < pdf.length; index += 1) {
    bytes[index] = pdf.charCodeAt(index) & 0xff;
  }
  return bytes;
}

/**
 * Serializes a report into a single-file PDF (one indirect object per page and
 * per content stream, with a conventional xref table).
 */
export function toPdf(document: ReportDocument): Uint8Array {
  const pages = chunk(toPrintLines(document), LINES_PER_PAGE);
  const pageObjectStart = 4;
  const pageIds = pages.map((_, index) => pageObjectStart + index * 2);
  const contentIds = pageIds.map((id) => id + 1);

  const objects: string[] = [
    '<< /Type /Catalog /Pages 2 0 R >>',
    `<< /Type /Pages /Count ${pages.length} /Kids [${pageIds
      .map((id) => `${id} 0 R`)
      .join(' ')}] >>`,
    '<< /Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding >>',
  ];
  pages.forEach((lines, index) => {
    objects.push(
      `<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${PAGE_WIDTH} ${PAGE_HEIGHT}] ` +
        `/Resources << /Font << /F1 3 0 R >> >> /Contents ${contentIds[index]} 0 R >>`,
    );
    const stream = pageContent(lines);
    objects.push(`<< /Length ${stream.length} >>\nstream\n${stream}endstream`);
  });

  let pdf = '%PDF-1.4\n';
  const offsets: number[] = [];
  objects.forEach((object, index) => {
    offsets.push(pdf.length);
    pdf += `${index + 1} 0 obj\n${object}\nendobj\n`;
  });

  const xrefOffset = pdf.length;
  pdf += `xref\n0 ${objects.length + 1}\n0000000000 65535 f \n`;
  for (const offset of offsets) {
    pdf += `${offset.toString().padStart(10, '0')} 00000 n \n`;
  }
  pdf +=
    `trailer\n<< /Size ${objects.length + 1} /Root 1 0 R >>\n` +
    `startxref\n${xrefOffset}\n%%EOF\n`;

  return toBytes(pdf);
}

import { toCsv } from './csv';
import { toPdf } from './pdf';
import { reportFileStem, type ReportDocument } from './reportDocument';

/** Triggers a browser download for `blob` under the given file name. */
export function downloadBlob(fileName: string, blob: Blob): void {
  const url = URL.createObjectURL(blob);
  const anchor = window.document.createElement('a');
  anchor.href = url;
  anchor.download = fileName;
  window.document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}

/** Exports a report as a CSV file. */
export function downloadReportCsv(report: ReportDocument): void {
  downloadBlob(
    `${reportFileStem(report.title)}.csv`,
    new Blob([toCsv(report)], { type: 'text/csv;charset=utf-8' }),
  );
}

/** Exports a report as a print-layout PDF file. */
export function downloadReportPdf(report: ReportDocument): void {
  downloadBlob(
    `${reportFileStem(report.title)}.pdf`,
    new Blob([toPdf(report)], { type: 'application/pdf' }),
  );
}

import { downloadReportCsv, downloadReportPdf } from '../../utils/download';
import type { ReportDocument } from '../../utils/reportDocument';

/**
 * Export controls shared by the report views. `buildReport` is called on click
 * so a report is only serialized when the user actually asks for it.
 */
export function ReportExportBar({
  buildReport,
  disabled = false,
}: {
  buildReport: () => ReportDocument;
  disabled?: boolean;
}) {
  return (
    <div className="report-exports">
      <button
        type="button"
        className="btn btn--ghost"
        disabled={disabled}
        onClick={() => downloadReportCsv(buildReport())}
      >
        Export CSV
      </button>
      <button
        type="button"
        className="btn btn--ghost"
        disabled={disabled}
        onClick={() => downloadReportPdf(buildReport())}
      >
        Export PDF
      </button>
    </div>
  );
}

"""Audit Report module - replaces RPTAUD00.cbl.

Generates audit trail reports showing security events,
data changes, and system activities.

COBOL program flow:
- 0000-MAIN: Initialize -> Process -> Terminate
- 1000-INITIALIZE: Open files, print headers
- 2000-PROCESS: Read audit records, format lines
- 2100-FORMAT-DETAIL: Format audit detail line
- 3000-TERMINATE: Print summary, close files
"""

import logging
from datetime import datetime
from typing import Any

from python_app.common.db2 import StatisticsCollector
from python_app.common.errors import ErrorHandler
from python_app.models.audit import AuditLogRecord
from python_app.models.return_code import RC_SUCCESS

logger = logging.getLogger("portfolio.batch.rptaud00")


class AuditReportGenerator:
    """Audit report generator replacing RPTAUD00.cbl."""

    REPORT_TITLE = "AUDIT TRAIL REPORT"
    LINE_WIDTH = 132

    def __init__(self) -> None:
        self.error_handler = ErrorHandler("RPTAUD00")
        self.stats = StatisticsCollector("RPTAUD00")
        self.report_lines: list[str] = []
        self.records_processed = 0
        self.type_counts: dict[str, int] = {}

    def initialize(self, report_date: str = "") -> None:
        """Initialize report - replaces 1000-INITIALIZE."""
        self.stats.initialize()
        self.report_lines.clear()
        self.records_processed = 0
        self.type_counts.clear()

        if not report_date:
            report_date = datetime.now().strftime("%Y%m%d")

        self.report_lines.extend([
            "=" * self.LINE_WIDTH,
            f" {self.REPORT_TITLE}".center(self.LINE_WIDTH),
            f" Report Date: {report_date}".center(self.LINE_WIDTH),
            "=" * self.LINE_WIDTH,
            f" {'Timestamp':<26} {'User':<10} {'Program':<10} "
            f"{'Type':<6} {'Action':<10} {'Status':<6} {'Portfolio':<10} {'Message':<40}",
            "-" * self.LINE_WIDTH,
        ])

    def format_detail(self, record: AuditLogRecord) -> None:
        """Format and add audit detail line - replaces 2100-FORMAT-DETAIL."""
        line = (
            f" {record.timestamp:<26} {record.user_id:<10} {record.program:<10} "
            f"{record.audit_type:<6} {record.action:<10} {record.status:<6} "
            f"{record.portfolio_id:<10} {record.message:<40}"
        )
        self.report_lines.append(line)
        self.records_processed += 1
        self.stats.update("reads")

        # Track counts by type
        self.type_counts[record.audit_type] = self.type_counts.get(record.audit_type, 0) + 1

    def process_batch(self, audit_records: list[AuditLogRecord], report_date: str = "") -> int:
        """Generate the full audit report - replaces 0000-MAIN."""
        self.initialize(report_date)

        for record in audit_records:
            self.format_detail(record)

        return self.terminate()

    def terminate(self) -> int:
        """Print summary and finalize - replaces 3000-TERMINATE."""
        self.report_lines.extend([
            "=" * self.LINE_WIDTH,
            f" SUMMARY: Total Records = {self.records_processed}",
        ])
        for audit_type, count in sorted(self.type_counts.items()):
            self.report_lines.append(f"   Type '{audit_type}': {count:>8} records")

        self.report_lines.extend([
            "=" * self.LINE_WIDTH,
            f" Report generated: {datetime.now().isoformat()}",
        ])

        self.stats.terminate()
        logger.info("RPTAUD00 complete: %d records processed", self.records_processed)
        return RC_SUCCESS

    def get_report(self) -> str:
        """Get the formatted report as a string."""
        return "\n".join(self.report_lines)

    def get_report_data(self) -> dict[str, Any]:
        """Get report data as structured dict (for JSON API)."""
        return {
            "title": self.REPORT_TITLE,
            "generated_at": datetime.now().isoformat(),
            "records_processed": self.records_processed,
            "type_counts": dict(self.type_counts),
        }

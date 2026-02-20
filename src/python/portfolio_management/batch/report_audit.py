"""Audit Report Generator - migrated from RPTAUD00.cbl.

Generates comprehensive audit report including security audit trails,
process audit reporting, error summary reporting, and control verification.
"""

import logging
from datetime import datetime
from typing import Optional

from portfolio_management.models.audit import AuditLogRecord
from portfolio_management.models.error_handling import ErrorMessage
from portfolio_management.models.common import ReturnCode

logger = logging.getLogger(__name__)

PROGRAM_ID = "RPTAUD00"
REPORT_WIDTH = 132


class AuditReportGenerator:
    def __init__(self):
        self._report_lines: list[str] = []
        self._page_number = 0
        self._total_audit_records = 0
        self._total_error_records = 0

    def generate(
        self,
        audit_records: list[AuditLogRecord],
        error_records: list[ErrorMessage],
        report_date: Optional[str] = None,
    ) -> int:
        if report_date is None:
            report_date = datetime.now().strftime("%Y-%m-%d")

        self._report_lines = []
        self._page_number = 0
        self._total_audit_records = 0
        self._total_error_records = 0

        self._write_report_header(report_date)
        self._write_security_audit(audit_records)
        self._write_process_audit(audit_records)
        self._write_error_summary(error_records)
        self._write_control_verification(audit_records, error_records)
        self._write_report_footer()

        return ReturnCode.SUCCESS

    def _write_report_header(self, report_date: str) -> None:
        self._page_number += 1
        self._write_line("=" * REPORT_WIDTH)
        self._write_line(f"{'COMPREHENSIVE AUDIT REPORT':^{REPORT_WIDTH}}")
        self._write_line(f"{'Report Date: ' + report_date:^{REPORT_WIDTH}}")
        self._write_line("=" * REPORT_WIDTH)
        self._write_line("")

    def _write_security_audit(self, audit_records: list[AuditLogRecord]) -> None:
        self._write_line("SECTION 1: SECURITY AUDIT TRAIL")
        self._write_line("-" * REPORT_WIDTH)
        self._write_line(
            f"{'Timestamp':<28} {'User':<10} {'Program':<10} "
            f"{'Type':<6} {'Action':<8} {'Key Info':<30}"
        )
        self._write_line("-" * REPORT_WIDTH)

        security_records = [r for r in audit_records if r.audit_type in ("S", "L", "A")]
        for record in security_records:
            self._total_audit_records += 1
            self._write_line(
                f"{record.timestamp:<28} {record.user_id:<10} "
                f"{record.program:<10} {record.audit_type:<6} "
                f"{record.audit_action:<8} {record.key_info:<30}"
            )

        self._write_line(f"Total security audit records: {len(security_records)}")
        self._write_line("")

    def _write_process_audit(self, audit_records: list[AuditLogRecord]) -> None:
        self._write_line("SECTION 2: PROCESS AUDIT")
        self._write_line("-" * REPORT_WIDTH)

        process_records = [r for r in audit_records if r.audit_type in ("P", "B")]
        for record in process_records:
            self._total_audit_records += 1
            self._write_line(
                f"  {record.timestamp} {record.program:<10} "
                f"{record.audit_action:<8} {record.audit_status}"
            )

        self._write_line(f"Total process audit records: {len(process_records)}")
        self._write_line("")

    def _write_error_summary(self, error_records: list[ErrorMessage]) -> None:
        self._write_line("SECTION 3: ERROR SUMMARY")
        self._write_line("-" * REPORT_WIDTH)

        severity_counts: dict[int, int] = {}
        category_counts: dict[str, int] = {}

        for error in error_records:
            self._total_error_records += 1
            severity_counts[error.severity] = severity_counts.get(error.severity, 0) + 1
            category_counts[error.category] = category_counts.get(error.category, 0) + 1

        self._write_line("  Error Summary by Severity:")
        for severity, count in sorted(severity_counts.items()):
            self._write_line(f"    Severity {severity}: {count}")

        self._write_line("  Error Summary by Category:")
        for category, count in sorted(category_counts.items()):
            self._write_line(f"    Category {category}: {count}")

        self._write_line(f"Total error records: {self._total_error_records}")
        self._write_line("")

    def _write_control_verification(
        self,
        audit_records: list[AuditLogRecord],
        error_records: list[ErrorMessage],
    ) -> None:
        self._write_line("SECTION 4: CONTROL VERIFICATION")
        self._write_line("-" * REPORT_WIDTH)
        self._write_line(f"  Total Audit Records:  {self._total_audit_records}")
        self._write_line(f"  Total Error Records:  {self._total_error_records}")
        self._write_line(f"  Audit Trail Complete: {'YES' if self._total_audit_records > 0 else 'NO'}")
        self._write_line("")

    def _write_report_footer(self) -> None:
        self._write_line("=" * REPORT_WIDTH)
        self._write_line(
            f"Report Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}  |  "
            f"Pages: {self._page_number}"
        )
        self._write_line("=" * REPORT_WIDTH)

    def _write_line(self, text: str) -> None:
        self._report_lines.append(text)

    def save_report(self, file_path: str) -> int:
        try:
            with open(file_path, "w") as f:
                for line in self._report_lines:
                    f.write(line + "\n")
            return ReturnCode.SUCCESS
        except Exception as e:
            logger.error("Error saving audit report: %s", e)
            return ReturnCode.ERROR

    def get_report_text(self) -> str:
        return "\n".join(self._report_lines)

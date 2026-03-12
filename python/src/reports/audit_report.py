"""
Audit report generator translated from COBOL program RPTAUD00.cbl.

Replaces:
  - RPTAUD00.cbl: Audit trail report generation
  - Lists audit entries for a specified date range
"""

import json
import logging
from datetime import datetime

from sqlalchemy.orm import Session

from src.db.repository import AuditRepository

logger = logging.getLogger(__name__)


class AuditReportGenerator:
    """
    Audit report generator.

    Translates RPTAUD00.cbl paragraph structure.
    """

    def __init__(self, session: Session) -> None:
        self._session = session
        self._audit_repo = AuditRepository(session)

    def generate(
        self,
        start_date: datetime,
        end_date: datetime,
        output_format: str = "json",
    ) -> str:
        """
        Generate audit trail report.

        Args:
            start_date: Start of date range.
            end_date: End of date range.
            output_format: Output format - 'json' or 'text'.

        Returns:
            Formatted report string.
        """
        logger.info("Generating audit report from %s to %s", start_date, end_date)

        records = self._audit_repo.list_by_date_range(start_date, end_date)

        entries: list[dict[str, object]] = []
        for record in records:
            entries.append({
                "timestamp": record.timestamp.isoformat(),
                "user_id": record.user_id,
                "action": record.action,
                "audit_type": record.audit_type,
                "portfolio_id": record.portfolio_id,
                "status": record.status,
                "message": record.message,
                "before_image": record.before_image,
                "after_image": record.after_image,
            })

        report = {
            "report_type": "Audit Trail",
            "start_date": start_date.isoformat(),
            "end_date": end_date.isoformat(),
            "total_entries": len(entries),
            "entries": entries,
        }

        if output_format.lower() == "text":
            return self._format_text(report)
        return json.dumps(report, indent=2)

    def _format_text(self, report: dict[str, object]) -> str:
        """Format report as text."""
        lines: list[str] = []
        lines.append("=" * 80)
        lines.append(
            f"  AUDIT TRAIL REPORT: {report.get('start_date', '')} to {report.get('end_date', '')}"
        )
        lines.append("=" * 80)
        lines.append("")
        lines.append(
            f"  {'Timestamp':<22} {'User':<10} {'Action':<10} "
            f"{'Portfolio':<10} {'Status':<6} Message"
        )
        lines.append("-" * 80)

        entries = report.get("entries", [])
        if isinstance(entries, list):
            for entry in entries:
                if isinstance(entry, dict):
                    lines.append(
                        f"  {str(entry.get('timestamp', ''))[:19]:<22} "
                        f"{str(entry.get('user_id', '')):<10} "
                        f"{str(entry.get('action', '')):<10} "
                        f"{str(entry.get('portfolio_id', '')):<10} "
                        f"{str(entry.get('status', '')):<6} "
                        f"{str(entry.get('message', ''))}"
                    )

        lines.append("")
        lines.append(f"  Total entries: {report.get('total_entries', 0)}")
        lines.append("=" * 80)
        return "\n".join(lines)

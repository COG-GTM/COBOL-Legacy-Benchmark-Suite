"""
Audit report generator translated from COBOL program RPTAUD00.cbl.
"""

import csv
import io
import logging
from datetime import datetime

from sqlalchemy.orm import Session

from src.db.repository import AuditRepository

logger = logging.getLogger(__name__)


class AuditReportGenerator:
    """Generate audit trail reports. Translates RPTAUD00.cbl."""

    def __init__(self, session: Session):
        self.session = session
        self.audit_repo = AuditRepository(session)

    def generate(
        self,
        start: datetime | None = None,
        end: datetime | None = None,
        audit_type: str | None = None,
        output_format: str = "json",
    ) -> str | dict:
        """Generate audit report."""
        start = start or datetime.min
        end = end or datetime.now()

        records = self.audit_repo.list_by_date_range(start, end, audit_type)

        entries = []
        for rec in records:
            entries.append({
                "timestamp": rec.timestamp.isoformat(),
                "system_id": rec.system_id,
                "user_id": rec.user_id,
                "program": rec.program,
                "audit_type": rec.audit_type,
                "action": rec.action,
                "status": rec.status,
                "key_info": rec.key_info,
                "message": rec.message,
            })

        report = {
            "report_title": "AUDIT TRAIL REPORT",
            "generated_at": datetime.now().isoformat(),
            "period_start": start.isoformat(),
            "period_end": end.isoformat(),
            "audit_type_filter": audit_type or "ALL",
            "entries": entries,
            "summary": {
                "total_entries": len(entries),
                "by_type": self._count_by_field(entries, "audit_type"),
                "by_action": self._count_by_field(entries, "action"),
                "by_status": self._count_by_field(entries, "status"),
            },
        }

        if output_format == "csv":
            return self._to_csv(entries)
        return report

    def _count_by_field(self, entries: list[dict], field: str) -> dict[str, int]:
        counts: dict[str, int] = {}
        for entry in entries:
            val = entry.get(field, "UNKNOWN")
            counts[val] = counts.get(val, 0) + 1
        return counts

    def _to_csv(self, entries: list[dict]) -> str:
        if not entries:
            return ""
        output = io.StringIO()
        writer = csv.DictWriter(output, fieldnames=entries[0].keys())
        writer.writeheader()
        writer.writerows(entries)
        return output.getvalue()

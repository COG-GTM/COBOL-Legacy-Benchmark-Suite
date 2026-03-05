"""
Statistics report generator translated from COBOL program RPTSTA00.cbl.
"""

import csv
import io
import logging
from datetime import datetime
from decimal import Decimal

from sqlalchemy.orm import Session

from src.db.repository import BatchControlRepository

logger = logging.getLogger(__name__)


class StatsReportGenerator:
    """Generate processing statistics reports. Translates RPTSTA00.cbl."""

    def __init__(self, session: Session):
        self.session = session
        self.batch_repo = BatchControlRepository(session)

    def generate(
        self,
        process_date: str | None = None,
        output_format: str = "json",
    ) -> str | dict:
        """Generate statistics report."""
        if process_date:
            records = self.batch_repo.list_by_date(process_date)
        else:
            records = self.batch_repo.list_all()

        jobs = []
        total_read = 0
        total_written = 0
        total_errors = 0
        total_elapsed = Decimal("0.00")

        for rec in records:
            elapsed = Decimal("0.00")
            if rec.start_time and rec.end_time:
                delta = rec.end_time - rec.start_time
                elapsed = Decimal(str(delta.total_seconds())).quantize(Decimal("0.01"))

            jobs.append({
                "job_name": rec.job_name,
                "process_date": rec.process_date,
                "status": rec.status,
                "return_code": rec.return_code,
                "records_read": rec.records_read,
                "records_written": rec.records_written,
                "error_count": rec.error_count,
                "elapsed_seconds": str(elapsed),
                "start_time": rec.start_time.isoformat() if rec.start_time else None,
                "end_time": rec.end_time.isoformat() if rec.end_time else None,
                "restart_count": rec.restart_count,
            })

            total_read += rec.records_read
            total_written += rec.records_written
            total_errors += rec.error_count
            total_elapsed += elapsed

        # Count by status
        status_counts: dict[str, int] = {}
        for job in jobs:
            s = job["status"]
            status_counts[s] = status_counts.get(s, 0) + 1

        report = {
            "report_title": "BATCH PROCESSING STATISTICS",
            "generated_at": datetime.now().isoformat(),
            "process_date": process_date or "ALL",
            "jobs": jobs,
            "summary": {
                "total_jobs": len(jobs),
                "total_records_read": total_read,
                "total_records_written": total_written,
                "total_errors": total_errors,
                "total_elapsed_seconds": str(total_elapsed),
                "by_status": status_counts,
            },
        }

        if output_format == "csv":
            return self._to_csv(jobs)
        return report

    def _to_csv(self, jobs: list[dict]) -> str:
        if not jobs:
            return ""
        output = io.StringIO()
        writer = csv.DictWriter(output, fieldnames=jobs[0].keys())
        writer.writeheader()
        writer.writerows(jobs)
        return output.getvalue()

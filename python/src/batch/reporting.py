"""
Batch reporting translated from COBOL programs:
- RPTPOS00.cbl (Position Report Generator)
- RPTAUD00.cbl (Audit Report Generator)
- RPTSTA00.cbl (Statistics Report Generator)
"""

import logging
from datetime import datetime
from decimal import Decimal

from sqlalchemy.orm import Session

from src.db.repository import AuditRepository, BatchControlRepository, PositionRepository

logger = logging.getLogger(__name__)


class BatchReporting:
    """
    Generate batch reports.
    Translates RPTPOS00, RPTAUD00, RPTSTA00 report generation.
    """

    def __init__(self, session: Session):
        self.session = session
        self.position_repo = PositionRepository(session)
        self.audit_repo = AuditRepository(session)
        self.batch_repo = BatchControlRepository(session)

    def generate_position_report(self, portfolio_id: str | None = None) -> dict:
        """
        Translates RPTPOS00.cbl position report.
        Iterates positions, formats output, computes totals.
        """
        if portfolio_id:
            positions = self.position_repo.list_by_portfolio(portfolio_id)
        else:
            positions = self.position_repo.list_all_active()

        total_cost = Decimal("0.00")
        total_market = Decimal("0.00")
        details = []

        for pos in positions:
            cost = Decimal(str(pos.cost_basis))
            market = Decimal(str(pos.market_value))
            change_pct = Decimal("0.00")
            if cost > 0:
                change_pct = (((market - cost) / cost) * Decimal("100")).quantize(Decimal("0.01"))

            details.append({
                "portfolio_id": pos.portfolio_id,
                "investment_id": pos.investment_id,
                "position_date": str(pos.position_date),
                "quantity": str(Decimal(str(pos.quantity))),
                "cost_basis": str(cost),
                "market_value": str(market),
                "change_pct": str(change_pct),
                "currency": pos.currency_code,
                "status": pos.status,
            })

            total_cost += cost
            total_market += market

        total_change_pct = Decimal("0.00")
        if total_cost > 0:
            total_change_pct = (((total_market - total_cost) / total_cost) * Decimal("100")).quantize(Decimal("0.01"))

        report = {
            "report_type": "POSITION",
            "generated_at": datetime.now().isoformat(),
            "portfolio_id": portfolio_id or "ALL",
            "positions": details,
            "summary": {
                "total_positions": len(details),
                "total_cost_basis": str(total_cost),
                "total_market_value": str(total_market),
                "total_change_pct": str(total_change_pct),
            },
        }

        logger.info("Position report generated: %d positions", len(details))
        return report

    def generate_audit_report(
        self,
        start: datetime | None = None,
        end: datetime | None = None,
        audit_type: str | None = None,
    ) -> dict:
        """Translates RPTAUD00.cbl audit report."""
        start = start or datetime.min
        end = end or datetime.now()

        records = self.audit_repo.list_by_date_range(start, end, audit_type)

        entries = []
        for rec in records:
            entries.append({
                "timestamp": rec.timestamp.isoformat(),
                "user_id": rec.user_id,
                "audit_type": rec.audit_type,
                "action": rec.action,
                "status": rec.status,
                "key_info": rec.key_info,
                "message": rec.message,
            })

        report = {
            "report_type": "AUDIT",
            "generated_at": datetime.now().isoformat(),
            "period_start": start.isoformat(),
            "period_end": end.isoformat(),
            "entries": entries,
            "summary": {
                "total_entries": len(entries),
            },
        }

        logger.info("Audit report generated: %d entries", len(entries))
        return report

    def generate_stats_report(self, process_date: str | None = None) -> dict:
        """Translates RPTSTA00.cbl statistics report."""
        if process_date:
            records = self.batch_repo.list_by_date(process_date)
        else:
            records = self.batch_repo.list_all()

        jobs = []
        total_read = 0
        total_written = 0
        total_errors = 0

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
            })

            total_read += rec.records_read
            total_written += rec.records_written
            total_errors += rec.error_count

        report = {
            "report_type": "STATISTICS",
            "generated_at": datetime.now().isoformat(),
            "process_date": process_date or "ALL",
            "jobs": jobs,
            "summary": {
                "total_jobs": len(jobs),
                "total_records_read": total_read,
                "total_records_written": total_written,
                "total_errors": total_errors,
            },
        }

        logger.info("Statistics report generated: %d jobs", len(jobs))
        return report

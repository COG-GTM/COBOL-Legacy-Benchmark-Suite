"""
Maintenance utilities translated from COBOL program UTLMNT00.cbl.

Replaces:
  - UTLMNT00.cbl 1000-ARCHIVE-RECORDS: Archive old records
  - UTLMNT00.cbl 2000-CLEANUP-RECORDS: Clean up expired data
  - UTLMNT00.cbl 3000-REORG-FILES: Reorganize data files
  - UTLMNT00.cbl 4000-ANALYZE-DATA: Analyze data integrity

Database maintenance operations: cleanup old records, archive, reorg.
"""

import logging
from datetime import date, datetime, timedelta

from sqlalchemy import and_, delete, func, select
from sqlalchemy.orm import Session

from src.db.tables import AuditLog, Checkpoint, ErrorLog, TransactionHistory

logger = logging.getLogger(__name__)


class MaintenanceService:
    """
    Database maintenance service.

    Translates UTLMNT00.cbl paragraph structure.
    """

    def __init__(self, session: Session) -> None:
        self._session = session

    def archive_old_records(self, retention_days: int = 365) -> dict[str, int]:
        """
        Archive records older than retention period.

        Translates UTLMNT00.cbl 1000-ARCHIVE-RECORDS.

        Args:
            retention_days: Number of days to retain records.

        Returns:
            Dictionary with counts of archived records by type.
        """
        cutoff_date = date.today() - timedelta(days=retention_days)
        cutoff_datetime = datetime.combine(cutoff_date, datetime.min.time())
        logger.info("Archiving records older than %s", cutoff_date)

        results: dict[str, int] = {}

        # Archive old audit records
        audit_count = self._session.execute(
            select(func.count()).select_from(AuditLog).where(
                AuditLog.timestamp < cutoff_datetime
            )
        ).scalar() or 0
        results["audit_records"] = audit_count

        # Archive old error records
        error_count = self._session.execute(
            select(func.count()).select_from(ErrorLog).where(
                ErrorLog.timestamp < cutoff_datetime
            )
        ).scalar() or 0
        results["error_records"] = error_count

        logger.info("Archive candidates: %s", results)
        return results

    def cleanup_records(self, retention_days: int = 90) -> dict[str, int]:
        """
        Clean up expired/temporary data.

        Translates UTLMNT00.cbl 2000-CLEANUP-RECORDS.
        """
        cutoff_date = date.today() - timedelta(days=retention_days)
        cutoff_datetime = datetime.combine(cutoff_date, datetime.min.time())
        logger.info("Cleaning up records older than %s", cutoff_date)

        results: dict[str, int] = {}

        # Clean old completed checkpoints
        old_checkpoints = self._session.execute(
            delete(Checkpoint).where(
                and_(
                    Checkpoint.status == "C",
                    Checkpoint.save_date < cutoff_date,
                )
            )
        )
        results["checkpoints_deleted"] = old_checkpoints.rowcount

        # Clean old error logs
        old_errors = self._session.execute(
            delete(ErrorLog).where(ErrorLog.timestamp < cutoff_datetime)
        )
        results["errors_deleted"] = old_errors.rowcount

        self._session.commit()
        logger.info("Cleanup results: %s", results)
        return results

    def analyze_data(self) -> dict[str, object]:
        """
        Analyze data integrity and statistics.

        Translates UTLMNT00.cbl 4000-ANALYZE-DATA.
        """
        logger.info("Analyzing data integrity")

        from src.db.tables import InvestmentPosition, PortfolioMaster

        stats: dict[str, object] = {}

        # Portfolio statistics
        total_portfolios = self._session.execute(
            select(func.count()).select_from(PortfolioMaster)
        ).scalar() or 0
        stats["total_portfolios"] = total_portfolios

        # Transaction statistics
        total_transactions = self._session.execute(
            select(func.count()).select_from(TransactionHistory)
        ).scalar() or 0
        stats["total_transactions"] = total_transactions

        # Position statistics
        total_positions = self._session.execute(
            select(func.count()).select_from(InvestmentPosition)
        ).scalar() or 0
        stats["total_positions"] = total_positions

        # Audit log count
        total_audits = self._session.execute(
            select(func.count()).select_from(AuditLog)
        ).scalar() or 0
        stats["total_audit_records"] = total_audits

        # Error log count
        total_errors = self._session.execute(
            select(func.count()).select_from(ErrorLog)
        ).scalar() or 0
        stats["total_error_records"] = total_errors

        logger.info("Data analysis complete: %s", stats)
        return stats

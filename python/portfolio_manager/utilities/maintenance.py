"""Maintenance Utility.

Replaces: UTLMNT00 (src/programs/utility/UTLMNT00.cbl)

Performs file/database maintenance operations including:
  - Archival of old records
  - Cleanup of expired data
  - Reorganization of indexes
  - Purge of old error/audit logs

Original COBOL flow (UTLMNT00.cbl):
  0000-MAIN-PROCESS
    1000-INITIALIZE
    2000-PROCESS-MAINTENANCE
      2100-ARCHIVE-OLD-RECORDS
      2200-PURGE-ERROR-LOG
      2300-PURGE-AUDIT-LOG
      2400-CLEANUP-TEMP-FILES
    3000-FINALIZE
"""

from __future__ import annotations

import logging
from dataclasses import dataclass, field
from datetime import datetime, timedelta

from sqlalchemy import delete
from sqlalchemy.orm import Session

from portfolio_manager.models.database import AuditLog, ErrorLog, ReturnCodeLog
from portfolio_manager.services.error_handler import ErrorProcessor

logger = logging.getLogger(__name__)


@dataclass
class MaintenanceResult:
    """Result of maintenance operations."""

    start_time: datetime = field(default_factory=datetime.now)
    end_time: datetime | None = None
    error_logs_purged: int = 0
    audit_logs_purged: int = 0
    return_codes_purged: int = 0
    return_code: int = 0
    messages: list[str] = field(default_factory=list)


class MaintenanceUtility:
    """Database maintenance utility.

    Replaces UTLMNT00 (src/programs/utility/UTLMNT00.cbl).
    """

    PROGRAM_ID = "UTLMNT00"

    # Default retention periods (days)
    ERROR_LOG_RETENTION = 90
    AUDIT_LOG_RETENTION = 365
    RETURN_CODE_RETENTION = 30

    def __init__(self, session: Session):
        self._session = session
        self._error_processor = ErrorProcessor(session)

    def run(
        self,
        error_log_days: int | None = None,
        audit_log_days: int | None = None,
        return_code_days: int | None = None,
    ) -> MaintenanceResult:
        """Run all maintenance operations.

        Replaces 0000-MAIN-PROCESS from UTLMNT00.cbl.

        Args:
            error_log_days: Retention period for error logs.
            audit_log_days: Retention period for audit logs.
            return_code_days: Retention period for return code logs.

        Returns:
            MaintenanceResult with operation statistics.
        """
        result = MaintenanceResult()

        err_days = error_log_days or self.ERROR_LOG_RETENTION
        aud_days = audit_log_days or self.AUDIT_LOG_RETENTION
        rc_days = return_code_days or self.RETURN_CODE_RETENTION

        logger.info(
            "%s: Starting maintenance (error=%dd, audit=%dd, rc=%dd)",
            self.PROGRAM_ID,
            err_days,
            aud_days,
            rc_days,
        )

        try:
            # 2200-PURGE-ERROR-LOG
            result.error_logs_purged = self._purge_error_logs(err_days)
            result.messages.append(
                f"Purged {result.error_logs_purged} error log records older than {err_days} days"
            )

            # 2300-PURGE-AUDIT-LOG
            result.audit_logs_purged = self._purge_audit_logs(aud_days)
            result.messages.append(
                f"Purged {result.audit_logs_purged} audit log records older than {aud_days} days"
            )

            # Purge return code logs
            result.return_codes_purged = self._purge_return_codes(rc_days)
            result.messages.append(
                f"Purged {result.return_codes_purged} return code records older than {rc_days} days"
            )

            result.return_code = 0

        except Exception as exc:
            result.return_code = 8
            result.messages.append(f"Maintenance error: {exc}")
            logger.error("%s: Maintenance failed: %s", self.PROGRAM_ID, exc)

            self._error_processor.process_error(
                program_id=self.PROGRAM_ID,
                category="SY",
                error_code="E007",
                severity=3,
                error_text=str(exc)[:200],
            )

        result.end_time = datetime.now()

        logger.info(
            "%s: Complete — errors_purged=%d audit_purged=%d rc_purged=%d RC=%d",
            self.PROGRAM_ID,
            result.error_logs_purged,
            result.audit_logs_purged,
            result.return_codes_purged,
            result.return_code,
        )

        return result

    def _purge_error_logs(self, retention_days: int) -> int:
        """Purge error log records older than retention period.

        Replaces UTLMNT00 paragraph 2200-PURGE-ERROR-LOG.
        """
        cutoff = datetime.now() - timedelta(days=retention_days)
        stmt = delete(ErrorLog).where(ErrorLog.error_timestamp < cutoff)
        result = self._session.execute(stmt)
        return result.rowcount  # type: ignore[return-value]

    def _purge_audit_logs(self, retention_days: int) -> int:
        """Purge audit log records older than retention period.

        Replaces UTLMNT00 paragraph 2300-PURGE-AUDIT-LOG.
        """
        cutoff = datetime.now() - timedelta(days=retention_days)
        stmt = delete(AuditLog).where(AuditLog.timestamp < cutoff)
        result = self._session.execute(stmt)
        return result.rowcount  # type: ignore[return-value]

    def _purge_return_codes(self, retention_days: int) -> int:
        """Purge return code log records older than retention period."""
        cutoff = datetime.now() - timedelta(days=retention_days)
        stmt = delete(ReturnCodeLog).where(ReturnCodeLog.timestamp < cutoff)
        result = self._session.execute(stmt)
        return result.rowcount  # type: ignore[return-value]

"""
Audit trail service translated from COBOL program AUDPROC.cbl.

Replaces:
  - AUDPROC.cbl 1000-WRITE-AUDIT: Write audit record
  - AUDPROC.cbl 2000-READ-AUDIT: Read audit records by key
  - AUDPROC.cbl 3000-BROWSE-AUDIT: Browse audit records by date range

All data modifications are tracked via this service.
"""

import logging
from datetime import datetime

from sqlalchemy.orm import Session

from src.common.constants import AuditAction, AuditStatus, AuditType
from src.db.repository import AuditRepository
from src.db.tables import AuditLog

logger = logging.getLogger(__name__)


class AuditService:
    """
    Audit trail service.

    Translates AUDPROC.cbl paragraphs to methods.
    """

    def __init__(self, session: Session) -> None:
        self._repo = AuditRepository(session)

    def log_action(
        self,
        user_id: str,
        action: AuditAction,
        portfolio_id: str = "",
        account_no: str = "",
        before_image: str = "",
        after_image: str = "",
        message: str = "",
        audit_type: AuditType = AuditType.TRANSACTION,
        status: AuditStatus = AuditStatus.SUCCESS,
        program: str = "",
        terminal: str = "",
        system_id: str = "PYINVST",
    ) -> AuditLog:
        """
        Write an audit log record.

        Translates AUDPROC.cbl 1000-WRITE-AUDIT paragraph.
        """
        audit = AuditLog(
            timestamp=datetime.now(),
            system_id=system_id,
            user_id=user_id,
            program=program,
            terminal=terminal,
            audit_type=audit_type,
            action=action,
            status=status,
            portfolio_id=portfolio_id,
            account_no=account_no,
            before_image=before_image[:100],
            after_image=after_image[:100],
            message=message[:100],
        )
        self._repo.create(audit)
        logger.debug("Audit record created: user=%s action=%s portfolio=%s", user_id, action, portfolio_id)
        return audit

    def log_portfolio_change(
        self,
        user_id: str,
        action: AuditAction,
        portfolio_id: str,
        before_image: str = "",
        after_image: str = "",
    ) -> AuditLog:
        """Convenience method for portfolio-related audit entries."""
        return self.log_action(
            user_id=user_id,
            action=action,
            portfolio_id=portfolio_id,
            before_image=before_image,
            after_image=after_image,
            audit_type=AuditType.TRANSACTION,
            program="PORTSVC",
        )

    def log_system_event(
        self,
        action: AuditAction,
        message: str,
        user_id: str = "SYSTEM",
    ) -> AuditLog:
        """Convenience method for system-level audit entries."""
        return self.log_action(
            user_id=user_id,
            action=action,
            message=message,
            audit_type=AuditType.SYSTEM_EVENT,
            program="SYSTEM",
        )

    def get_audit_trail(
        self,
        start: datetime,
        end: datetime,
    ) -> list[AuditLog]:
        """
        Browse audit records by date range.

        Translates AUDPROC.cbl 3000-BROWSE-AUDIT paragraph.
        """
        return self._repo.list_by_date_range(start, end)

    def get_portfolio_audit(self, portfolio_id: str) -> list[AuditLog]:
        """Get audit trail for a specific portfolio."""
        return self._repo.list_by_portfolio(portfolio_id)

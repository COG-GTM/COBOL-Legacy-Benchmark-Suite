"""Audit processing module - replaces AUDPROC.cbl.

Provides audit trail processing for security and data change tracking.

AUDPROC.cbl functions: INIT, WRIT, READ
"""

import logging
from datetime import datetime
from typing import Any

from python_app.models.audit import AuditLogRecord

logger = logging.getLogger("portfolio.audit")


class AuditProcessor:
    """Audit trail processor replacing AUDPROC.cbl.

    Provides:
    - P100-INITIALIZE: Reset audit context
    - P200-WRITE-AUDIT: Write an audit record
    - P300-READ-AUDIT: Read audit records (for reporting)
    """

    def __init__(self, system_id: str = "PORTMGMT") -> None:
        self.system_id = system_id
        self.audit_count = 0
        self.records: list[AuditLogRecord] = []

    def initialize(self) -> None:
        """Initialize audit processor - replaces P100-INITIALIZE."""
        self.audit_count = 0
        self.records.clear()

    def write_audit(
        self,
        *,
        user_id: str,
        program: str,
        audit_type: str,
        action: str,
        status: str = "OK",
        portfolio_id: str = "",
        account_no: str = "",
        before_image: str = "",
        after_image: str = "",
        message: str = "",
    ) -> AuditLogRecord:
        """Write an audit record - replaces P200-WRITE-AUDIT.

        In COBOL this executed:
        EXEC SQL INSERT INTO AUDITLOG (...) VALUES (...) END-EXEC
        """
        record = AuditLogRecord(
            timestamp=datetime.now().isoformat(),
            system_id=self.system_id,
            user_id=user_id,
            program=program,
            audit_type=audit_type,
            action=action,
            status=status,
            portfolio_id=portfolio_id,
            account_no=account_no,
            before_image=before_image,
            after_image=after_image,
            message=message,
        )
        self.records.append(record)
        self.audit_count += 1

        logger.info(
            "AUDIT: user=%s program=%s type=%s action=%s status=%s portfolio=%s",
            user_id, program, audit_type, action, status, portfolio_id,
        )
        return record

    def read_audit(
        self,
        *,
        user_id: str | None = None,
        audit_type: str | None = None,
        limit: int = 100,
    ) -> list[AuditLogRecord]:
        """Read audit records with optional filters - replaces P300-READ-AUDIT."""
        results = self.records
        if user_id:
            results = [r for r in results if r.user_id == user_id]
        if audit_type:
            results = [r for r in results if r.audit_type == audit_type]
        return results[:limit]

    def get_stats(self) -> dict[str, Any]:
        """Get audit processing statistics."""
        return {
            "system_id": self.system_id,
            "total_records": self.audit_count,
            "buffered_records": len(self.records),
        }

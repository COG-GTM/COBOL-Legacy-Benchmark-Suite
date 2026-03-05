"""
Audit trail processing translated from COBOL program AUDPROC.cbl.

Translates:
- 1000-INITIALIZE: timestamp capture
- 2000-PROCESS-AUDIT: build and write audit record
- 3000-TERMINATE: close resources
"""

import logging
from datetime import datetime

from src.common.constants import AuditAction, AuditStatus, AuditType

logger = logging.getLogger(__name__)


def write_audit_record(
    session,
    audit_type: AuditType,
    action: AuditAction,
    user_id: str = "",
    program: str = "",
    key_info: str = "",
    before_image: str = "",
    after_image: str = "",
    message: str = "",
    status: AuditStatus = AuditStatus.SUCCESS,
    system_id: str = "SYSTEM",
    terminal: str = "",
) -> None:
    """
    Write an audit trail record.
    Translates AUDPROC.cbl 2000-PROCESS-AUDIT paragraph.
    """
    from src.db.tables import AuditLog

    try:
        record = AuditLog(
            timestamp=datetime.now(),
            system_id=system_id,
            user_id=user_id,
            program=program,
            terminal=terminal,
            audit_type=audit_type.value,
            action=action.value,
            status=status.value,
            key_info=key_info[:50] if key_info else "",
            before_image=before_image,
            after_image=after_image,
            message=message,
        )
        session.add(record)
        session.flush()
        logger.debug(
            "Audit record written: type=%s action=%s key=%s",
            audit_type.value,
            action.value,
            key_info,
        )
    except Exception as e:
        # AUDPROC.cbl: IF WS-FILE-STATUS NOT = '00' DISPLAY error
        logger.warning("Error writing audit record: %s", e)

"""Audit Trail Processing Subroutine - migrated from AUDPROC.cbl.

Processes audit trail records and writes to audit file.
"""

import logging
from dataclasses import dataclass
from datetime import datetime

from portfolio_management.models.audit import AuditLogRecord

logger = logging.getLogger(__name__)


@dataclass
class AuditRequest:
    system_id: str = ""
    user_id: str = ""
    program: str = ""
    terminal: str = ""
    audit_type: str = ""
    audit_action: str = ""
    key_info: str = ""
    before_image: str = ""
    after_image: str = ""


class AuditProcessor:
    def __init__(self):
        self._audit_file = None
        self._audit_count = 0

    def initialize(self, audit_file_path: str) -> int:
        try:
            self._audit_file = open(audit_file_path, "a")
            return 0
        except OSError as e:
            logger.error("Error opening audit file: %s - %s", audit_file_path, e)
            return 8

    def process_audit(self, request: AuditRequest) -> int:
        now = datetime.now()
        timestamp = now.strftime("%Y-%m-%d-%H.%M.%S.%f")

        record = AuditLogRecord(
            timestamp=timestamp,
            system_id=request.system_id,
            user_id=request.user_id,
            program=request.program,
            terminal=request.terminal,
            audit_type=request.audit_type,
            audit_action=request.audit_action,
            audit_status="A",
            key_info=request.key_info,
            before_image=request.before_image,
            after_image=request.after_image,
        )

        self._write_audit_record(record)
        self._audit_count += 1

        return 0

    def _write_audit_record(self, record: AuditLogRecord) -> None:
        if self._audit_file is None:
            return

        audit_line = (
            f"{record.timestamp} {record.system_id:<8s} "
            f"{record.user_id:<8s} {record.program:<8s} "
            f"{record.terminal:<8s} {record.audit_type} "
            f"{record.audit_action} {record.audit_status} "
            f"{record.key_info}\n"
        )
        try:
            self._audit_file.write(audit_line)
            self._audit_file.flush()
        except OSError as e:
            logger.error("Error writing audit record: %s", e)

    def terminate(self) -> int:
        if self._audit_file is not None:
            try:
                self._audit_file.close()
            except OSError:
                pass
            self._audit_file = None

        logger.info("Total audit records processed: %d", self._audit_count)
        return 0

    @property
    def audit_count(self) -> int:
        return self._audit_count

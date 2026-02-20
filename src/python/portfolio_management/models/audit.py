"""Audit Trail Record Definitions - migrated from AUDITLOG.cpy."""

from dataclasses import dataclass


@dataclass
class AuditLogRecord:
    timestamp: str = ""
    system_id: str = ""
    user_id: str = ""
    program: str = ""
    terminal: str = ""
    audit_type: str = ""
    audit_action: str = ""
    audit_status: str = ""
    key_info: str = ""
    before_image: str = ""
    after_image: str = ""

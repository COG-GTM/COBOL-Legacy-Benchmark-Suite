"""Audit Log Record model - translated from AUDITLOG.cpy copybook.

Mirrors the COBOL AUDIT-RECORD structure used by AUDPROC.cbl
for security and process audit trail logging.
"""

from datetime import datetime

from pydantic import BaseModel, Field


class AuditLogRecord(BaseModel):
    """Audit log record translated from COBOL AUDIT-RECORD.

    Maps to AUDITLOG.cpy copybook and AUDPROC.cbl linkage fields.
    """

    timestamp: str = Field(default_factory=lambda: datetime.now().isoformat(), max_length=26)
    system_id: str = Field(default="", max_length=8, description="LS-SYSTEM-ID")
    user_id: str = Field(default="", max_length=8, description="LS-USER-ID / AUD-USER-ID")
    program: str = Field(default="", max_length=8, description="LS-PROGRAM")
    terminal: str = Field(default="", max_length=8, description="LS-TERMINAL")
    audit_type: str = Field(default="", max_length=4, description="LS-TYPE / AUD-TYPE")
    action: str = Field(default="", max_length=8, description="LS-ACTION / AUD-ACTION")
    status: str = Field(default="", max_length=4, description="LS-STATUS / AUD-STATUS")
    portfolio_id: str = Field(default="", max_length=8, description="LS-PORT-ID")
    account_no: str = Field(default="", max_length=10, description="LS-ACCT-NO")
    before_image: str = Field(default="", max_length=100, description="LS-BEFORE-IMAGE")
    after_image: str = Field(default="", max_length=100, description="LS-AFTER-IMAGE")
    message: str = Field(default="", max_length=100, description="LS-MESSAGE / AUD-MESSAGE")

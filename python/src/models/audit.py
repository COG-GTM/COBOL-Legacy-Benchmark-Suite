"""
Audit log models translated from COBOL copybook AUDITLOG.cpy.

COBOL fields mapped:
  AUD-TIMESTAMP     PIC X(26)  -> datetime
  AUD-SYSTEM-ID     PIC X(8)   -> str, max_length=8
  AUD-USER-ID       PIC X(8)   -> str, max_length=8
  AUD-PROGRAM       PIC X(8)   -> str, max_length=8
  AUD-TERMINAL      PIC X(8)   -> str, max_length=8
  AUD-TYPE          PIC X(4)   -> AuditType enum
  AUD-ACTION        PIC X(8)   -> AuditAction enum
  AUD-STATUS        PIC X(4)   -> AuditStatus enum
  AUD-PORTFOLIO-ID  PIC X(8)   -> str, max_length=8
  AUD-ACCOUNT-NO    PIC X(10)  -> str, max_length=10
  AUD-BEFORE-IMAGE  PIC X(100) -> str, max_length=100
  AUD-AFTER-IMAGE   PIC X(100) -> str, max_length=100
  AUD-MESSAGE       PIC X(100) -> str, max_length=100
"""

from datetime import datetime

from pydantic import BaseModel, Field

from src.common.constants import AuditAction, AuditStatus, AuditType


class AuditLogRecord(BaseModel):
    """Audit trail record from AUDITLOG.cpy."""

    # Header
    timestamp: datetime = Field(default_factory=datetime.now, description="Audit event timestamp")
    system_id: str = Field(default="", max_length=8, description="Source system identifier")
    user_id: str = Field(default="", max_length=8, description="User who performed the action")
    program: str = Field(default="", max_length=8, description="Program that generated the record")
    terminal: str = Field(default="", max_length=8, description="Terminal identifier")

    # Event classification
    audit_type: AuditType = Field(default=AuditType.TRANSACTION, description="Audit event type")
    action: AuditAction = Field(default=AuditAction.INQUIRE, description="Action performed")
    status: AuditStatus = Field(default=AuditStatus.SUCCESS, description="Action result status")

    # Key info
    portfolio_id: str = Field(default="", max_length=8, description="Related portfolio ID")
    account_no: str = Field(default="", max_length=10, description="Related account number")

    # Change tracking
    before_image: str = Field(default="", max_length=100, description="Record state before change")
    after_image: str = Field(default="", max_length=100, description="Record state after change")
    message: str = Field(default="", max_length=100, description="Descriptive message")

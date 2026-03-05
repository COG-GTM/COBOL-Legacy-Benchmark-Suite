"""
Audit log models translated from COBOL copybook AUDITLOG.cpy.
"""

from datetime import datetime
from typing import Optional

from pydantic import BaseModel, Field

from src.common.constants import AuditAction, AuditStatus, AuditType


class AuditLogRecord(BaseModel):
    """Translates COBOL 01 AUDIT-RECORD from AUDITLOG.cpy."""

    id: Optional[int] = Field(default=None)
    timestamp: datetime = Field(description="PIC X(26)")
    system_id: str = Field(max_length=8, default="SYSTEM", description="PIC X(08)")
    user_id: str = Field(max_length=8, default="", description="PIC X(08)")
    program: str = Field(max_length=8, default="", description="PIC X(08)")
    terminal: str = Field(max_length=8, default="", description="PIC X(08)")
    audit_type: AuditType = Field(description="PIC X(04)")
    action: AuditAction = Field(description="PIC X(08)")
    status: AuditStatus = Field(
        default=AuditStatus.SUCCESS,
        description="PIC X(04)",
    )
    key_info: str = Field(max_length=50, default="", description="Portfolio ID + Account")
    before_image: str = Field(max_length=500, default="", description="PIC X(100)")
    after_image: str = Field(max_length=500, default="", description="PIC X(100)")
    message: str = Field(max_length=500, default="", description="PIC X(100)")

    model_config = {"from_attributes": True}

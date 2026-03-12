"""
Audit log data models translated from COBOL copybook.

Source copybook:
  - src/copybook/common/AUDITLOG.cpy  (Audit Trail Record Definitions)
"""

from __future__ import annotations

import datetime
from enum import Enum
from typing import Annotated, Optional

from pydantic import BaseModel, Field, field_validator


# ---------------------------------------------------------------------------
# Enums derived from AUDITLOG.cpy level-88 condition values
# ---------------------------------------------------------------------------

class AuditType(str, Enum):
    """AUD-TYPE level-88 values from AUDITLOG.cpy."""

    TRANSACTION = "TRAN"
    USER_ACTION = "USER"
    SYSTEM_EVENT = "SYST"


class AuditAction(str, Enum):
    """AUD-ACTION level-88 values from AUDITLOG.cpy.

    Values are left-padded to 8 chars in COBOL; we store stripped values.
    """

    CREATE = "CREATE"
    UPDATE = "UPDATE"
    DELETE = "DELETE"
    INQUIRE = "INQUIRE"
    LOGIN = "LOGIN"
    LOGOUT = "LOGOUT"
    STARTUP = "STARTUP"
    SHUTDOWN = "SHUTDOWN"


class AuditStatus(str, Enum):
    """AUD-STATUS level-88 values from AUDITLOG.cpy."""

    SUCCESS = "SUCC"
    FAILURE = "FAIL"
    WARNING = "WARN"


# ---------------------------------------------------------------------------
# Pydantic model
# ---------------------------------------------------------------------------

class AuditLogRecord(BaseModel):
    """Complete audit trail record.

    Maps to AUDITLOG.cpy  01 AUDIT-RECORD.

    Field sizes from PIC clauses:
      AUD-TIMESTAMP     PIC X(26)
      AUD-SYSTEM-ID     PIC X(8)
      AUD-USER-ID       PIC X(8)
      AUD-PROGRAM       PIC X(8)
      AUD-TERMINAL      PIC X(8)
      AUD-TYPE          PIC X(4)
      AUD-ACTION        PIC X(8)
      AUD-STATUS        PIC X(4)
      AUD-PORTFOLIO-ID  PIC X(8)
      AUD-ACCOUNT-NO    PIC X(10)
      AUD-BEFORE-IMAGE  PIC X(100)
      AUD-AFTER-IMAGE   PIC X(100)
      AUD-MESSAGE       PIC X(100)
    """

    # AUD-HEADER fields
    timestamp: datetime.datetime
    system_id: Annotated[str, Field(max_length=8)]
    user_id: Annotated[str, Field(max_length=8)]
    program: Annotated[str, Field(max_length=8)]
    terminal: Annotated[str, Field(max_length=8)] = ""

    # AUD-TYPE / AUD-ACTION / AUD-STATUS
    audit_type: AuditType
    action: AuditAction
    status: AuditStatus

    # AUD-KEY-INFO
    portfolio_id: Annotated[str, Field(max_length=8)] = ""
    account_no: Annotated[str, Field(max_length=10)] = ""

    # Change images and message
    before_image: Annotated[str, Field(max_length=100)] = ""
    after_image: Annotated[str, Field(max_length=100)] = ""
    message: Annotated[str, Field(max_length=100)] = ""

    @field_validator("timestamp", mode="before")
    @classmethod
    def _parse_cobol_timestamp(cls, value: object) -> object:
        """Accept COBOL PIC X(26) timestamp strings."""
        if isinstance(value, str):
            try:
                return datetime.datetime.fromisoformat(value.strip())
            except ValueError:
                pass
        return value

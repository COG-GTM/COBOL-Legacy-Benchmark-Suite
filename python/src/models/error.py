"""
Error handling data models translated from COBOL copybooks.

Source copybooks:
  - src/copybook/common/ERRHAND.cpy  (Standard Error Handling Definitions)
  - src/copybook/common/RETHND.cpy   (Return Code Handling Definitions)
"""

from __future__ import annotations

import datetime
from enum import Enum
from typing import Annotated, Optional

from pydantic import BaseModel, Field, field_validator


# ---------------------------------------------------------------------------
# Enums derived from RETHND.cpy level-88 condition values
# ---------------------------------------------------------------------------

class ErrorType(str, Enum):
    """ERROR-TYPE level-88 values from RETHND.cpy."""

    VALIDATION = "V"
    PROCESSING = "P"
    DATABASE = "D"
    FILE = "F"
    SECURITY = "S"


class ErrorSeverity(str, Enum):
    """Mapped from ERRHAND.cpy ERR-SEVERITY / ERR-RETURN-CODES.

    Combines the numeric return codes with descriptive labels.
    """

    SUCCESS = "SUCCESS"
    WARNING = "WARNING"
    ERROR = "ERROR"
    SEVERE = "SEVERE"
    TERMINAL = "TERMINAL"


class RecoveryAction(str, Enum):
    """ACTION-FLAG level-88 values from RETHND.cpy."""

    CONTINUE = "C"
    ABORT = "A"
    RETRY = "R"


class ErrorCategory(str, Enum):
    """ERR-CATEGORIES from ERRHAND.cpy."""

    VSAM = "VS"
    VALIDATION = "VL"
    PROCESSING = "PR"
    SYSTEM = "SY"


# ---------------------------------------------------------------------------
# Pydantic models
# ---------------------------------------------------------------------------

class ErrorLogRecord(BaseModel):
    """Error message record combining ERRHAND.cpy and RETHND.cpy structures.

    ERRHAND.cpy  01 ERR-MESSAGE:
      ERR-DATE        PIC X(10)
      ERR-TIME        PIC X(8)
      ERR-PROGRAM     PIC X(8)
      ERR-CATEGORY    PIC X(2)
      ERR-CODE        PIC X(4)
      ERR-SEVERITY    PIC S9(4) COMP
      ERR-TEXT        PIC X(80)
      ERR-DETAILS     PIC X(256)

    RETHND.cpy  01 RETURN-HANDLING:
      PROGRAM-NAME    PIC X(8)
      PARAGRAPH-NAME  PIC X(8)
      ERROR-ROUTINE   PIC X(8)
      ERROR-TYPE      PIC X(1)
      ERROR-CODE      PIC X(4)
      ERROR-TEXT       PIC X(80)
      SYSTEM-CODE     PIC X(4)
      SYSTEM-MSG      PIC X(80)
    """

    # Timestamp fields (from ERR-TIMESTAMP in ERRHAND.cpy)
    error_date: Annotated[str, Field(max_length=10)] = ""
    error_time: Annotated[str, Field(max_length=8)] = ""
    timestamp: Optional[datetime.datetime] = None

    # Program identification
    program: Annotated[str, Field(max_length=8)]
    paragraph_name: Annotated[str, Field(max_length=8)] = ""
    error_routine: Annotated[str, Field(max_length=8)] = ""

    # Error classification
    category: ErrorCategory
    error_type: ErrorType = ErrorType.PROCESSING
    error_code: Annotated[str, Field(max_length=4)]
    severity: int = 0

    # Messages
    error_text: Annotated[str, Field(max_length=80)]
    details: Annotated[str, Field(max_length=256)] = ""

    # System information (from RETHND.cpy SYSTEM-INFO)
    system_code: Annotated[str, Field(max_length=4)] = ""
    system_message: Annotated[str, Field(max_length=80)] = ""

    # Recovery (from RETHND.cpy RETURN-ACTIONS)
    action: RecoveryAction = RecoveryAction.CONTINUE
    retry_count: int = 0
    max_retries: int = 3

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


class ReturnStatus(BaseModel):
    """Return status structure from RETHND.cpy  05 RETURN-STATUS.

    Fields:
      RETURN-CODE   PIC S9(4) COMP
      REASON-CODE   PIC S9(4) COMP
      MODULE-ID     PIC X(8)
      FUNCTION-ID   PIC X(8)
    """

    return_code: int = 0
    reason_code: int = 0
    module_id: Annotated[str, Field(max_length=8)] = ""
    function_id: Annotated[str, Field(max_length=8)] = ""

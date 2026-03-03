"""Error Record model - translated from ERRHAND.cpy copybook.

Mirrors the COBOL error handling structures used by ERRPROC.cbl
and ERRHNDL.cbl for centralized error processing.
"""

from datetime import datetime
from enum import IntEnum, StrEnum

from pydantic import BaseModel, Field


class ErrorSeverity(IntEnum):
    """Error severity levels mapped from COBOL ERR-SEVERITY."""

    INFO = 1
    WARNING = 2
    ERROR = 3
    FATAL = 4


class ErrorAction(StrEnum):
    """Error recovery action from ERRHNDL COBOL 88-level values."""

    CONTINUE = "CONTINUE"
    RETURN = "RETURN"
    ABEND = "ABEND"


class ErrorRecord(BaseModel):
    """Error record translated from COBOL ERRHAND copybook and ERRPROC linkage.

    Maps to fields from ERRPROC.cbl LS-ERROR-REQUEST and
    ERRHNDL.cbl WS-ERRLOG-RECORD.
    """

    timestamp: str = Field(default_factory=lambda: datetime.now().isoformat(), max_length=26)
    program_id: str = Field(default="", max_length=8, description="ERR-PROGRAM / LS-PROGRAM-ID")
    paragraph: str = Field(default="", max_length=30, description="ERR-PARAGRAPH")
    category: str = Field(default="", max_length=2, description="ERR-CATEGORY / LS-CATEGORY")
    error_code: str = Field(default="", max_length=4, description="ERR-CODE / LS-ERROR-CODE")
    severity: ErrorSeverity = Field(default=ErrorSeverity.INFO, description="ERR-SEVERITY / LS-SEVERITY")
    message: str = Field(default="", max_length=80, description="ERR-TEXT / ERR-MESSAGE")
    details: str = Field(default="", max_length=256, description="ERR-DETAILS / LS-ERROR-DETAILS")
    sqlcode: int = Field(default=0, description="ERR-SQLCODE / LOG-SQLCODE")
    cics_resp: int = Field(default=0, description="ERR-CICS-RESP / LOG-CICS-RESP")
    trace_id: str = Field(default="", max_length=16, description="ERR-TRACE-ID / LOG-TRACE-ID")
    action: ErrorAction = Field(default=ErrorAction.CONTINUE, description="Recovery action")

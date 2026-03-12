"""
Error log models translated from COBOL copybook ERRLOG.cpy.

COBOL fields mapped:
  ERR-TIMESTAMP     PIC X(26)  -> datetime
  ERR-SYSTEM-ID     PIC X(8)   -> str, max_length=8
  ERR-PROGRAM       PIC X(8)   -> str, max_length=8
  ERR-PARAGRAPH     PIC X(30)  -> str, max_length=30
  ERR-SEVERITY      PIC 9(2)   -> int
  ERR-CATEGORY      PIC X(2)   -> ErrorCategory enum
  ERR-CODE          PIC X(4)   -> str, max_length=4
  ERR-MESSAGE       PIC X(80)  -> str, max_length=80
  ERR-SQLCODE       PIC S9(9)  -> int | None
  ERR-SQLSTATE      PIC X(5)   -> str, max_length=5
  ERR-DATA          PIC X(100) -> str, max_length=100
"""

from datetime import datetime

from pydantic import BaseModel, Field

from src.common.constants import ErrorCategory


class ErrorLogRecord(BaseModel):
    """Error log record from ERRLOG.cpy."""

    timestamp: datetime = Field(default_factory=datetime.now, description="Error occurrence timestamp")
    system_id: str = Field(default="", max_length=8, description="Source system identifier")
    program: str = Field(default="", max_length=8, description="Program where error occurred")
    paragraph: str = Field(default="", max_length=30, description="Paragraph/method where error occurred")
    severity: int = Field(default=0, ge=0, le=16, description="Error severity (0/4/8/12/16)")
    category: ErrorCategory = Field(default=ErrorCategory.PROCESSING, description="Error category")
    error_code: str = Field(default="", max_length=4, description="Application error code")
    message: str = Field(default="", max_length=80, description="Error message text")
    sqlcode: int | None = Field(default=None, description="DB2 SQLCODE if database error")
    sqlstate: str = Field(default="", max_length=5, description="DB2 SQLSTATE if database error")
    error_data: str = Field(default="", max_length=100, description="Additional error context data")

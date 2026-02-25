"""
Error Handling Data Model

Migrated from COBOL copybook: src/copybook/online/ERRHND.cpy

Original COBOL structure:
- ERR-AREA: Error handling communication area
  - ERR-PROGRAM: Program name where error occurred
  - ERR-PARAGRAPH: Paragraph name where error occurred
  - ERR-SQLCODE: SQL return code (for DB2 errors)
  - ERR-CICS-RESP: CICS response code
  - ERR-CICS-RESP2: CICS response code 2
  - ERR-SEVERITY: Error severity (F=Fatal, W=Warning, I=Info)
  - ERR-MESSAGE: Error message
  - ERR-ACTION: Action taken (R=Retry, C=Continue, A=Abort)
  - ERR-TRACE: Trace information (ID and timestamp)

This copybook is used for centralized error handling across all programs.

Note: This module contains Pydantic models that faithfully represent the COBOL
copybook structure for validation and data transfer. For database ORM operations,
use the ErrorLog model from src/models/database.py which matches the actual
PostgreSQL error_log table schema.
"""

from datetime import datetime
from enum import Enum
from typing import Optional
from pydantic import BaseModel, Field, field_validator


class ErrorSeverity(str, Enum):
    """
    Error severity codes.
    
    Migrated from COBOL 88-level conditions:
    - ERR-FATAL   VALUE 'F'
    - ERR-WARNING VALUE 'W'
    - ERR-INFO    VALUE 'I'
    """
    FATAL = "F"
    WARNING = "W"
    INFO = "I"


class ErrorAction(str, Enum):
    """
    Error action codes.
    
    Migrated from COBOL 88-level conditions:
    - ERR-RETRY    VALUE 'R'
    - ERR-CONTINUE VALUE 'C'
    - ERR-ABORT    VALUE 'A'
    """
    RETRY = "R"
    CONTINUE = "C"
    ABORT = "A"


class ErrorRecord(BaseModel):
    """
    Pydantic model for error record validation.
    
    Preserves all field definitions from ERRHND.cpy with Python type mappings.
    This model is used for error logging and handling across the application.
    """
    
    # Error location fields
    err_program: str = Field(
        ...,
        max_length=8,
        description="Program name where error occurred (PIC X(8))"
    )
    err_paragraph: Optional[str] = Field(
        None,
        max_length=30,
        description="Paragraph/function name where error occurred (PIC X(30))"
    )
    
    # Error code fields
    err_sqlcode: int = Field(
        0,
        description="SQL return code for DB2 errors (PIC S9(9) COMP)"
    )
    err_cics_resp: int = Field(
        0,
        description="CICS response code (PIC S9(8) COMP)"
    )
    err_cics_resp2: int = Field(
        0,
        description="CICS response code 2 (PIC S9(8) COMP)"
    )
    
    # Error details
    err_severity: ErrorSeverity = Field(
        ErrorSeverity.INFO,
        description="Error severity: F=Fatal, W=Warning, I=Info"
    )
    err_message: str = Field(
        ...,
        max_length=80,
        description="Error message (PIC X(80))"
    )
    err_action: ErrorAction = Field(
        ErrorAction.CONTINUE,
        description="Action taken: R=Retry, C=Continue, A=Abort"
    )
    
    # Trace information
    err_trace_id: Optional[str] = Field(
        None,
        max_length=16,
        description="Trace identifier (PIC X(16))"
    )
    err_timestamp: Optional[datetime] = Field(
        None,
        description="Error timestamp"
    )

    @field_validator("err_program", "err_paragraph", "err_trace_id")
    @classmethod
    def strip_and_upper(cls, v: Optional[str]) -> Optional[str]:
        """Strip whitespace and convert to uppercase for consistency with COBOL."""
        if v:
            return v.strip().upper()
        return v

    @property
    def is_fatal(self) -> bool:
        """Check if error is fatal."""
        return self.err_severity == ErrorSeverity.FATAL

    @property
    def is_warning(self) -> bool:
        """Check if error is a warning."""
        return self.err_severity == ErrorSeverity.WARNING

    @property
    def is_info(self) -> bool:
        """Check if error is informational."""
        return self.err_severity == ErrorSeverity.INFO

    @property
    def should_retry(self) -> bool:
        """Check if action indicates retry."""
        return self.err_action == ErrorAction.RETRY

    @property
    def should_abort(self) -> bool:
        """Check if action indicates abort."""
        return self.err_action == ErrorAction.ABORT

    class Config:
        """Pydantic configuration."""
        json_encoders = {
            datetime: lambda v: v.isoformat(),
        }


class ErrorArea(BaseModel):
    """
    Complete Error Area model.
    
    This model represents the full ERR-AREA structure used for
    error handling communication in CICS programs.
    
    Original COBOL structure:
    - 05 ERR-PROGRAM      PIC X(8)
    - 05 ERR-PARAGRAPH    PIC X(30)
    - 05 ERR-SQLCODE      PIC S9(9) COMP
    - 05 ERR-CICS-RESP    PIC S9(8) COMP
    - 05 ERR-CICS-RESP2   PIC S9(8) COMP
    - 05 ERR-SEVERITY     PIC X
    - 05 ERR-MESSAGE      PIC X(80)
    - 05 ERR-ACTION       PIC X
    - 05 ERR-TRACE
       - 10 ERR-TRACE-ID  PIC X(16)
       - 10 ERR-TIMESTAMP PIC X(26)
    """
    
    err_program: str = Field(
        ...,
        max_length=8,
        description="Program name"
    )
    err_paragraph: str = Field(
        "",
        max_length=30,
        description="Paragraph name"
    )
    err_sqlcode: int = Field(
        0,
        description="SQL return code"
    )
    err_cics_resp: int = Field(
        0,
        description="CICS response code"
    )
    err_cics_resp2: int = Field(
        0,
        description="CICS response code 2"
    )
    err_severity: str = Field(
        "I",
        max_length=1,
        description="Error severity (F/W/I)"
    )
    err_message: str = Field(
        "",
        max_length=80,
        description="Error message"
    )
    err_action: str = Field(
        "C",
        max_length=1,
        description="Action taken (R/C/A)"
    )
    err_trace_id: str = Field(
        "",
        max_length=16,
        description="Trace identifier"
    )
    err_timestamp: str = Field(
        "",
        max_length=26,
        description="Error timestamp (ISO format)"
    )

    @field_validator("err_program", "err_paragraph", "err_trace_id")
    @classmethod
    def strip_and_upper(cls, v: str) -> str:
        """Strip whitespace and convert to uppercase for consistency with COBOL."""
        if v:
            return v.strip().upper()
        return v

    @field_validator("err_severity")
    @classmethod
    def validate_severity(cls, v: str) -> str:
        """Validate severity is one of F, W, I."""
        v = v.strip().upper()
        if v not in ("F", "W", "I"):
            raise ValueError("Severity must be F, W, or I")
        return v

    @field_validator("err_action")
    @classmethod
    def validate_action(cls, v: str) -> str:
        """Validate action is one of R, C, A."""
        v = v.strip().upper()
        if v not in ("R", "C", "A"):
            raise ValueError("Action must be R, C, or A")
        return v

    def to_record(self) -> ErrorRecord:
        """Convert ErrorArea to ErrorRecord."""
        return ErrorRecord(
            err_program=self.err_program,
            err_paragraph=self.err_paragraph if self.err_paragraph else None,
            err_sqlcode=self.err_sqlcode,
            err_cics_resp=self.err_cics_resp,
            err_cics_resp2=self.err_cics_resp2,
            err_severity=ErrorSeverity(self.err_severity),
            err_message=self.err_message,
            err_action=ErrorAction(self.err_action),
            err_trace_id=self.err_trace_id if self.err_trace_id else None,
            err_timestamp=datetime.fromisoformat(self.err_timestamp) if self.err_timestamp else None,
        )

    @classmethod
    def from_record(cls, record: ErrorRecord) -> "ErrorArea":
        """Create ErrorArea from ErrorRecord."""
        return cls(
            err_program=record.err_program,
            err_paragraph=record.err_paragraph or "",
            err_sqlcode=record.err_sqlcode,
            err_cics_resp=record.err_cics_resp,
            err_cics_resp2=record.err_cics_resp2,
            err_severity=record.err_severity.value,
            err_message=record.err_message,
            err_action=record.err_action.value,
            err_trace_id=record.err_trace_id or "",
            err_timestamp=record.err_timestamp.isoformat() if record.err_timestamp else "",
        )

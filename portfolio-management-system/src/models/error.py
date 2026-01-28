"""
Error handling data models.
Migrated from COBOL copybook: src/copybook/online/ERRHND.cpy and src/copybook/common/ERRHAND.cpy

Original COBOL structure (ERRHND.cpy):
01  ERROR-HANDLING.
    05 ERR-PROGRAM          PIC X(8).
    05 ERR-PARAGRAPH        PIC X(30).
    05 ERR-SQLCODE          PIC S9(9) COMP.
    05 ERR-CICS-RESP        PIC S9(8) COMP.
    05 ERR-CICS-RESP2       PIC S9(8) COMP.
    05 ERR-SEVERITY         PIC X.
    05 ERR-MESSAGE          PIC X(80).
    05 ERR-ACTION           PIC X.
    05 ERR-TRACE.
       10 ERR-TRACE-ID      PIC X(16).
       10 ERR-TIMESTAMP     PIC X(26).

Original COBOL structure (ERRHAND.cpy):
- Error categories: VSAM, VALID, PROC, SYSTEM
- Return codes: SUCCESS=0, WARNING=4, ERROR=8, SEVERE=12, TERMINAL=16
"""

from datetime import datetime
from decimal import Decimal
from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field
from sqlalchemy import DateTime, Integer, Numeric, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from src.database.base import Base


class ErrorSeverity(str, Enum):
    """
    Error severity codes.
    Migrated from COBOL: ERR-SEVERITY values.
    """
    FATAL = "F"
    WARNING = "W"
    INFO = "I"


class ErrorAction(str, Enum):
    """
    Error action codes.
    Migrated from COBOL: ERR-ACTION values.
    """
    RETURN = "R"
    CONTINUE = "C"
    ABEND = "A"


class ReturnCode(int, Enum):
    """
    Return codes.
    Migrated from COBOL: ERRHAND.cpy return code definitions.
    """
    SUCCESS = 0
    WARNING = 4
    ERROR = 8
    SEVERE = 12
    TERMINAL = 16


class ErrorCategory(str, Enum):
    """
    Error categories.
    Migrated from COBOL: ERRHAND.cpy error category definitions.
    """
    VSAM = "VSAM"
    VALIDATION = "VALID"
    PROCESSING = "PROC"
    SYSTEM = "SYSTEM"
    DATABASE = "DB"
    SECURITY = "SEC"


class ErrorLogRecord(Base):
    """
    SQLAlchemy ORM model for error log records.
    Migrated from DB2 ERRLOG table.
    """
    __tablename__ = "error_log"
    
    error_timestamp: Mapped[datetime] = mapped_column(DateTime, primary_key=True)
    program_id: Mapped[str] = mapped_column(String(8), primary_key=True)
    error_type: Mapped[str] = mapped_column(String(1), nullable=False)
    error_severity: Mapped[int] = mapped_column(Integer, nullable=False)
    error_code: Mapped[str] = mapped_column(String(8), nullable=False)
    error_message: Mapped[str] = mapped_column(String(200), nullable=False)
    process_date: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    process_time: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    user_id: Mapped[str] = mapped_column(String(8), nullable=False)
    additional_info: Mapped[Optional[str]] = mapped_column(String(500), nullable=True)
    
    def __repr__(self) -> str:
        return (
            f"ErrorLogRecord(timestamp={self.error_timestamp}, program={self.program_id}, "
            f"severity={self.error_severity}, code={self.error_code})"
        )


class ErrorTrace(BaseModel):
    """
    Pydantic model for error trace (ERR-TRACE).
    """
    trace_id: str = Field(..., max_length=16, description="Trace identifier")
    timestamp: str = Field(..., max_length=26, description="Error timestamp")


class ErrorHandling(BaseModel):
    """
    Pydantic model for error handling (ERROR-HANDLING).
    Main error structure used throughout the application.
    """
    program: str = Field(..., max_length=8, description="Program name")
    paragraph: Optional[str] = Field(None, max_length=30, description="Paragraph/function name")
    sqlcode: Optional[int] = Field(None, description="SQL return code")
    cics_resp: Optional[int] = Field(None, description="CICS response code")
    cics_resp2: Optional[int] = Field(None, description="CICS response code 2")
    severity: ErrorSeverity = Field(..., description="Error severity")
    message: str = Field(..., max_length=80, description="Error message")
    action: ErrorAction = Field(..., description="Error action")
    trace: Optional[ErrorTrace] = Field(None, description="Error trace information")
    
    @classmethod
    def create_info(cls, program: str, message: str, paragraph: str = None) -> "ErrorHandling":
        """Create an informational error."""
        return cls(
            program=program,
            paragraph=paragraph,
            severity=ErrorSeverity.INFO,
            message=message[:80],
            action=ErrorAction.CONTINUE,
            trace=ErrorTrace(
                trace_id=f"{program[:8]:<8}",
                timestamp=datetime.utcnow().isoformat()
            )
        )
    
    @classmethod
    def create_warning(cls, program: str, message: str, paragraph: str = None) -> "ErrorHandling":
        """Create a warning error."""
        return cls(
            program=program,
            paragraph=paragraph,
            severity=ErrorSeverity.WARNING,
            message=message[:80],
            action=ErrorAction.CONTINUE,
            trace=ErrorTrace(
                trace_id=f"{program[:8]:<8}",
                timestamp=datetime.utcnow().isoformat()
            )
        )
    
    @classmethod
    def create_fatal(
        cls, 
        program: str, 
        message: str, 
        paragraph: str = None,
        sqlcode: int = None
    ) -> "ErrorHandling":
        """Create a fatal error."""
        return cls(
            program=program,
            paragraph=paragraph,
            sqlcode=sqlcode,
            severity=ErrorSeverity.FATAL,
            message=message[:80],
            action=ErrorAction.ABEND,
            trace=ErrorTrace(
                trace_id=f"{program[:8]:<8}",
                timestamp=datetime.utcnow().isoformat()
            )
        )


class ErrorLogCreate(BaseModel):
    """
    Pydantic model for creating error log entries.
    """
    program_id: str = Field(..., max_length=8, description="Program identifier")
    error_type: str = Field(..., max_length=1, description="Error type code")
    error_severity: ReturnCode = Field(..., description="Error severity level")
    error_code: str = Field(..., max_length=8, description="Error code")
    error_message: str = Field(..., max_length=200, description="Error message")
    user_id: str = Field(..., max_length=8, description="User identifier")
    additional_info: Optional[str] = Field(None, max_length=500, description="Additional information")


class ErrorLogResponse(ErrorLogCreate):
    """
    Pydantic model for error log API responses.
    """
    error_timestamp: datetime
    process_date: datetime
    process_time: datetime
    
    class Config:
        from_attributes = True


class ErrorSummary(BaseModel):
    """
    Pydantic model for error summary.
    Used in reports and monitoring.
    """
    total_errors: int
    errors_by_severity: dict[int, int]
    errors_by_program: dict[str, int]
    errors_by_type: dict[str, int]
    time_period_start: datetime
    time_period_end: datetime


class VSAMStatusCode(BaseModel):
    """
    Pydantic model for VSAM status codes.
    Migrated from ERRHAND.cpy VSAM status definitions.
    """
    status_1: str = Field(..., max_length=1, description="File status byte 1")
    status_2: str = Field(..., max_length=1, description="File status byte 2")
    
    @property
    def status_code(self) -> str:
        """Get combined status code."""
        return f"{self.status_1}{self.status_2}"
    
    @property
    def is_success(self) -> bool:
        """Check if status indicates success."""
        return self.status_code == "00"
    
    @property
    def is_end_of_file(self) -> bool:
        """Check if status indicates end of file."""
        return self.status_code == "10"
    
    @property
    def is_duplicate_key(self) -> bool:
        """Check if status indicates duplicate key."""
        return self.status_code == "22"
    
    @property
    def is_not_found(self) -> bool:
        """Check if status indicates record not found."""
        return self.status_code == "23"
    
    @property
    def description(self) -> str:
        """Get status description."""
        status_descriptions = {
            "00": "Successful completion",
            "02": "Duplicate key detected (non-unique alternate index)",
            "10": "End of file reached",
            "21": "Sequence error",
            "22": "Duplicate key",
            "23": "Record not found",
            "24": "Boundary violation",
            "30": "Permanent error",
            "34": "Boundary violation (sequential write)",
            "35": "File not found",
            "37": "File not open",
            "39": "File attribute conflict",
            "41": "File already open",
            "42": "File not open",
            "43": "Delete without prior read",
            "44": "Rewrite without prior read",
            "46": "Read without positioning",
            "47": "Read not permitted",
            "48": "Write not permitted",
            "49": "Delete/rewrite not permitted",
            "91": "VSAM password failure",
            "92": "Logic error",
            "93": "Resource not available",
            "94": "Sequential read after end of file",
            "95": "Invalid or incomplete file information",
            "96": "No DD statement",
            "97": "Open successful, file integrity verified",
        }
        return status_descriptions.get(self.status_code, f"Unknown status: {self.status_code}")

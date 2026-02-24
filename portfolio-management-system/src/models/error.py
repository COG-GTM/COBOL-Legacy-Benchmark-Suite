"""
Error Handling Data Model.

Translated from COBOL copybook: src/copybook/online/ERRHND.cpy

COBOL Source Structure:
    01 ERROR-HANDLING-AREA.
       05 ERR-PROGRAM           PIC X(8).       -> str
       05 ERR-PARAGRAPH         PIC X(30).      -> str
       05 ERR-SQLCODE           PIC S9(9) COMP. -> int
       05 ERR-CICS-RESP         PIC S9(8) COMP. -> int
       05 ERR-CICS-RESP2        PIC S9(8) COMP. -> int
       05 ERR-SEVERITY          PIC X(1).       -> ErrorSeverity enum
          88 ERR-FATAL          VALUE 'F'.
          88 ERR-WARNING        VALUE 'W'.
          88 ERR-INFO           VALUE 'I'.
       05 ERR-MESSAGE           PIC X(80).      -> str
       05 ERR-ACTION            PIC X(1).       -> ErrorAction enum
          88 ERR-RETRY          VALUE 'R'.
          88 ERR-CONTINUE       VALUE 'C'.
          88 ERR-ABEND          VALUE 'A'.
       05 ERR-TRACE-ID          PIC X(16).      -> str
       05 ERR-TIMESTAMP         PIC X(26).      -> str

Data Type Mapping Notes:
    PIC S9(9) COMP -> int
        Binary signed integer for SQL error codes.
        Stores the DB2 SQLCODE value when the error originates from SQL.
    PIC S9(8) COMP -> int
        Binary signed integer for CICS response codes.
        ERR-CICS-RESP: Primary CICS response (e.g., NOTFND, DUPREC).
        ERR-CICS-RESP2: Secondary CICS response for additional detail.
        In the Python migration, these map to HTTP status codes and
        application-specific error codes.
    PIC X(26) -> str with max_length=26
        Timestamp in IBM format: YYYY-MM-DD-HH.MM.SS.FFFFFF
        In Python, we use ISO 8601 format instead.

This copybook is used by the centralized error handler (ERRHNDL) to
process all errors from online CICS programs. The severity determines
the recovery action:
    Fatal (F)   -> Abend (terminate transaction)
    Warning (W) -> Continue with warning message
    Info (I)    -> Continue normally

In the Python migration, this maps to structured error handling with
logging levels and exception types.
"""

from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field


class ErrorSeverity(str, Enum):
    """Error severity levels.

    COBOL 88-level condition names from ERRHND copybook.
    Determines the system's response to the error.
    """

    FATAL = "F"
    WARNING = "W"
    INFO = "I"


class ErrorAction(str, Enum):
    """Error recovery action codes.

    COBOL 88-level condition names from ERRHND copybook.
    Specifies what action to take after the error is processed.
    """

    RETRY = "R"
    CONTINUE = "C"
    ABEND = "A"


class ErrorHandling(BaseModel):
    """Error handling area (ERROR-HANDLING-AREA).

    Translated from COBOL copybook ERRHND.cpy.
    Used by the centralized error handler (ERRHNDL) to capture,
    log, and manage errors from all online CICS programs.

    Error flow in the original system:
    1. Program detects error condition
    2. Populates ErrorHandling fields
    3. Calls ERRHNDL via EXEC CICS LINK
    4. ERRHNDL logs to DB2 ERRLOG table
    5. ERRHNDL determines action based on severity
    6. Control returns with action code set

    In the Python migration, this maps to:
    1. Exception raised or error detected
    2. ErrorHandling model populated
    3. Error logged via structlog
    4. Error persisted to error_log table
    5. HTTP response code determined by severity

    Usage:
        # SQL error
        error = ErrorHandling(
            program="INQHIST",
            paragraph="2100-FETCH-HISTORY",
            sqlcode=-911,
            severity=ErrorSeverity.WARNING,
            message="Deadlock detected during history fetch",
            action=ErrorAction.RETRY,
            trace_id="TRC20240115143022",
        )

        # CICS error (maps to HTTP error in Python)
        error = ErrorHandling(
            program="INQPORT",
            paragraph="2200-READ-POSITION",
            cics_resp=13,  # NOTFND
            severity=ErrorSeverity.INFO,
            message="Position record not found",
            action=ErrorAction.CONTINUE,
        )
    """

    program: str = Field(
        ...,
        max_length=8,
        description=(
            "Program where error occurred. "
            "COBOL: ERR-PROGRAM PIC X(8)."
        ),
    )
    paragraph: Optional[str] = Field(
        default=None,
        max_length=30,
        description=(
            "Paragraph/section where error occurred. "
            "COBOL: ERR-PARAGRAPH PIC X(30). "
            "Maps to function/method name in Python."
        ),
    )
    sqlcode: int = Field(
        default=0,
        description=(
            "DB2 SQL return code. "
            "COBOL: ERR-SQLCODE PIC S9(9) COMP. "
            "0=no SQL error, negative=SQL error."
        ),
    )
    cics_resp: int = Field(
        default=0,
        description=(
            "CICS primary response code. "
            "COBOL: ERR-CICS-RESP PIC S9(8) COMP. "
            "Maps to HTTP status codes in Python migration."
        ),
    )
    cics_resp2: int = Field(
        default=0,
        description=(
            "CICS secondary response code. "
            "COBOL: ERR-CICS-RESP2 PIC S9(8) COMP. "
            "Provides additional error detail."
        ),
    )
    severity: ErrorSeverity = Field(
        default=ErrorSeverity.INFO,
        description=(
            "Error severity level. "
            "COBOL: ERR-SEVERITY PIC X(1). "
            "F=Fatal, W=Warning, I=Info."
        ),
    )
    message: Optional[str] = Field(
        default=None,
        max_length=80,
        description=(
            "Human-readable error message. "
            "COBOL: ERR-MESSAGE PIC X(80). "
            "Sized for 80-column terminal display."
        ),
    )
    action: ErrorAction = Field(
        default=ErrorAction.CONTINUE,
        description=(
            "Recovery action to take. "
            "COBOL: ERR-ACTION PIC X(1). "
            "R=Retry, C=Continue, A=Abend."
        ),
    )
    trace_id: Optional[str] = Field(
        default=None,
        max_length=16,
        description=(
            "Trace identifier for error correlation. "
            "COBOL: ERR-TRACE-ID PIC X(16)."
        ),
    )
    timestamp: Optional[str] = Field(
        default=None,
        max_length=26,
        description=(
            "Error timestamp. "
            "COBOL: ERR-TIMESTAMP PIC X(26). "
            "IBM format: YYYY-MM-DD-HH.MM.SS.FFFFFF. "
            "Use ISO 8601 in Python."
        ),
    )

    @property
    def is_fatal(self) -> bool:
        """Check if error severity is Fatal (requires abend/termination)."""
        return self.severity == ErrorSeverity.FATAL

    @property
    def is_retryable(self) -> bool:
        """Check if the error action indicates retry is appropriate."""
        return self.action == ErrorAction.RETRY

    @property
    def has_sql_error(self) -> bool:
        """Check if a SQL error code is present."""
        return self.sqlcode != 0

    @property
    def has_cics_error(self) -> bool:
        """Check if a CICS response code is present."""
        return self.cics_resp != 0

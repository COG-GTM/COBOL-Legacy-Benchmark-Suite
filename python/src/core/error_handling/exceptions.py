"""Custom exception hierarchy mirroring COBOL error categories.

Maps COBOL error categories from ERRHAND.cpy and ERRHND.cpy:
  - VS (VSAM)      -> DataError (with DuplicateKeyError, RecordNotFoundError)
  - VL (Validation) -> ValidationError
  - PR (Processing) -> ProcessingError
  - SY (System)     -> SystemError

Severity levels from ERRHAND.cpy:
  - 0  (SUCCESS)   -> N/A (no exception)
  - 4  (WARNING)   -> severity="warning"
  - 8  (ERROR)     -> severity="error"
  - 12 (SEVERE)    -> severity="severe"
  - 16 (TERMINAL)  -> severity="terminal"

Recovery actions from ERRHND.cpy:
  - 'R' (RETURN)   -> action="return"
  - 'C' (CONTINUE) -> action="continue"
  - 'A' (ABEND)    -> action="abend"
"""

from enum import Enum
from typing import Optional


class ErrorSeverity(Enum):
    """Error severity levels matching COBOL ERRHAND.cpy return codes."""

    WARNING = 4
    ERROR = 8
    SEVERE = 12
    TERMINAL = 16


class ErrorCategory(Enum):
    """Error categories matching COBOL ERRHAND.cpy ERR-CATEGORIES."""

    VSAM = "VS"
    VALIDATION = "VL"
    PROCESSING = "PR"
    SYSTEM = "SY"
    CONNECTION = "CN"
    SECURITY = "SC"
    CURSOR = "CR"


class RecoveryAction(Enum):
    """Recovery actions matching COBOL ERRHND.cpy ERR-ACTION."""

    RETURN = "R"
    CONTINUE = "C"
    ABEND = "A"


class CLBSError(Exception):
    """Base exception for COBOL Legacy Benchmark Suite.

    All framework exceptions inherit from this class, mirroring the
    centralized error handling in ERRHNDL/ERRPROC.

    Attributes:
        message: Human-readable error description.
        category: Error category code (maps to COBOL ERR-CATEGORY).
        error_code: Application-specific error code (maps to ERR-CODE).
        severity: Error severity level (maps to ERR-SEVERITY).
        program: Originating program/module name (maps to ERR-PROGRAM).
        details: Extended error details (maps to ERR-DETAILS).
        trace_id: Trace identifier for error correlation (maps to ERR-TRACE-ID).
        recovery_action: Suggested recovery action (maps to ERR-ACTION).
    """

    def __init__(
        self,
        message: str,
        category: ErrorCategory = ErrorCategory.SYSTEM,
        error_code: str = "0000",
        severity: ErrorSeverity = ErrorSeverity.ERROR,
        program: str = "",
        details: str = "",
        trace_id: Optional[str] = None,
        recovery_action: RecoveryAction = RecoveryAction.RETURN,
    ) -> None:
        super().__init__(message)
        self.message = message
        self.category = category
        self.error_code = error_code
        self.severity = severity
        self.program = program
        self.details = details
        self.trace_id = trace_id
        self.recovery_action = recovery_action

    def to_dict(self) -> dict:
        """Serialize error to dictionary for structured logging."""
        return {
            "message": self.message,
            "category": self.category.value,
            "error_code": self.error_code,
            "severity": self.severity.name.lower(),
            "severity_value": self.severity.value,
            "program": self.program,
            "details": self.details,
            "trace_id": self.trace_id,
            "recovery_action": self.recovery_action.value,
        }


# --- Data Errors (VSAM category 'VS') ---


class DataError(CLBSError):
    """Base class for data-related errors (COBOL ERR-CAT-VSAM 'VS').

    Covers VSAM file operation errors and general data access failures.
    """

    def __init__(
        self,
        message: str,
        error_code: str = "VS00",
        severity: ErrorSeverity = ErrorSeverity.ERROR,
        **kwargs,
    ) -> None:
        super().__init__(
            message=message,
            category=ErrorCategory.VSAM,
            error_code=error_code,
            severity=severity,
            **kwargs,
        )


class DuplicateKeyError(DataError):
    """Duplicate record key error (COBOL VSAM status '22').

    Maps to ERR-VSAM-DUPKEY / ERR-VSAM-22 'Duplicate record key'.
    """

    def __init__(self, message: str = "Duplicate record key", **kwargs) -> None:
        super().__init__(
            message=message,
            error_code="VS22",
            severity=ErrorSeverity.ERROR,
            **kwargs,
        )


class RecordNotFoundError(DataError):
    """Record not found error (COBOL VSAM status '23').

    Maps to ERR-VSAM-NOTFND / ERR-VSAM-23 'Record not found'.
    """

    def __init__(self, message: str = "Record not found", **kwargs) -> None:
        super().__init__(
            message=message,
            error_code="VS23",
            severity=ErrorSeverity.WARNING,
            recovery_action=RecoveryAction.CONTINUE,
            **kwargs,
        )


# --- Validation Errors (category 'VL') ---


class ValidationError(CLBSError):
    """Validation error (COBOL ERR-CAT-VALID 'VL').

    Raised when input data fails validation checks,
    mirroring TRNVAL00's validation logic.
    """

    def __init__(
        self,
        message: str,
        error_code: str = "VL00",
        severity: ErrorSeverity = ErrorSeverity.ERROR,
        **kwargs,
    ) -> None:
        super().__init__(
            message=message,
            category=ErrorCategory.VALIDATION,
            error_code=error_code,
            severity=severity,
            **kwargs,
        )


# --- Connection Errors (DB2 connection failures) ---


class ConnectionError(CLBSError):
    """Database/service connection error.

    Maps to DB2RECV connection recovery patterns (RECV-CONNECTION 'C').
    Default recovery action is RETURN to trigger retry logic.
    """

    def __init__(
        self,
        message: str,
        error_code: str = "CN00",
        severity: ErrorSeverity = ErrorSeverity.SEVERE,
        sqlcode: Optional[int] = None,
        **kwargs,
    ) -> None:
        super().__init__(
            message=message,
            category=ErrorCategory.CONNECTION,
            error_code=error_code,
            severity=severity,
            **kwargs,
        )
        self.sqlcode = sqlcode


class CursorError(CLBSError):
    """Database cursor error.

    Maps to DB2RECV cursor recovery (RECV-CURSOR 'R').
    """

    def __init__(
        self,
        message: str,
        error_code: str = "CR00",
        severity: ErrorSeverity = ErrorSeverity.WARNING,
        cursor_name: str = "",
        sqlcode: Optional[int] = None,
        **kwargs,
    ) -> None:
        super().__init__(
            message=message,
            category=ErrorCategory.CURSOR,
            error_code=error_code,
            severity=severity,
            recovery_action=RecoveryAction.CONTINUE,
            **kwargs,
        )
        self.cursor_name = cursor_name
        self.sqlcode = sqlcode


# --- Security Errors ---


class SecurityError(CLBSError):
    """Security/authentication/authorization error.

    Maps to SECMGR error responses:
      - Response code 8: validation/authorization failure
      - Response code 12: system-level security failure
    """

    def __init__(
        self,
        message: str,
        error_code: str = "SC00",
        severity: ErrorSeverity = ErrorSeverity.ERROR,
        **kwargs,
    ) -> None:
        super().__init__(
            message=message,
            category=ErrorCategory.SECURITY,
            error_code=error_code,
            severity=severity,
            **kwargs,
        )


# --- Processing Errors (category 'PR') ---


class ProcessingError(CLBSError):
    """General processing error (COBOL ERR-CAT-PROC 'PR').

    Raised during business logic processing failures.
    """

    def __init__(
        self,
        message: str,
        error_code: str = "PR00",
        severity: ErrorSeverity = ErrorSeverity.ERROR,
        **kwargs,
    ) -> None:
        super().__init__(
            message=message,
            category=ErrorCategory.PROCESSING,
            error_code=error_code,
            severity=severity,
            **kwargs,
        )


# --- System Errors (category 'SY') ---


class SystemError(CLBSError):
    """System-level error (COBOL ERR-CAT-SYSTEM 'SY').

    Maps to ERR-FATAL severity with ERR-ABEND recovery action.
    These are unrecoverable errors that require system intervention.
    """

    def __init__(
        self,
        message: str,
        error_code: str = "SY00",
        severity: ErrorSeverity = ErrorSeverity.TERMINAL,
        **kwargs,
    ) -> None:
        super().__init__(
            message=message,
            category=ErrorCategory.SYSTEM,
            error_code=error_code,
            severity=severity,
            recovery_action=RecoveryAction.ABEND,
            **kwargs,
        )

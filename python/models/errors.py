"""Error handling models translated from src/copybook/common/ERRHAND.cpy and RTNCODE.cpy.

Provides a custom exception hierarchy mirroring the COBOL error handling patterns,
plus an ErrorMessage dataclass matching the ERR-MESSAGE structure.
"""

from dataclasses import dataclass, field
from datetime import datetime
from enum import IntEnum

from models.enums import ErrorCategory


class ErrorSeverity(IntEnum):
    """Error severity levels from ERRHAND.cpy ERR-RETURN-CODES."""

    SUCCESS = 0
    WARNING = 4
    ERROR = 8
    SEVERE = 12
    TERMINAL = 16


# ---------------------------------------------------------------------------
# Exception hierarchy
# ---------------------------------------------------------------------------


class AppError(Exception):
    """Base application error mirroring COBOL RETURN-HANDLING structure.

    Attributes:
        message: Human-readable description.
        severity: ErrorSeverity level.
        code: 4-character error code (e.g. 'E001').
        program: Originating program name (max 8 chars).
    """

    def __init__(
        self,
        message: str,
        severity: ErrorSeverity = ErrorSeverity.ERROR,
        code: str = "",
        program: str = "",
    ) -> None:
        super().__init__(message)
        self.severity = severity
        self.code = code
        self.program = program


class ValidationError(AppError):
    """Validation errors (ERRHAND.cpy ERR-CAT-VALID / RETHND.cpy ERR-VALIDATION)."""

    def __init__(self, message: str, code: str = "E008", program: str = "") -> None:
        super().__init__(message, severity=ErrorSeverity.ERROR, code=code, program=program)


class VsamError(AppError):
    """VSAM file errors (ERRHAND.cpy ERR-CAT-VSAM).

    The VSAM dual-storage pattern is collapsed, but the error semantics are preserved
    so that business logic depending on these error types continues to work.
    """

    def __init__(
        self,
        message: str,
        vsam_status: str = "",
        severity: ErrorSeverity = ErrorSeverity.ERROR,
        code: str = "E004",
        program: str = "",
    ) -> None:
        super().__init__(message, severity=severity, code=code, program=program)
        self.vsam_status = vsam_status


class DuplicateKeyError(VsamError):
    """VSAM duplicate key error (status '22')."""

    def __init__(self, message: str = "Duplicate record key", program: str = "") -> None:
        super().__init__(
            message, vsam_status="22", severity=ErrorSeverity.ERROR, code="E003", program=program
        )


class NotFoundError(VsamError):
    """VSAM record not found error (status '23')."""

    def __init__(self, message: str = "Record not found", program: str = "") -> None:
        super().__init__(
            message, vsam_status="23", severity=ErrorSeverity.WARNING, code="E002", program=program
        )


class ProcessingError(AppError):
    """Processing errors (ERRHAND.cpy ERR-CAT-PROC / RETHND.cpy ERR-PROCESSING)."""

    def __init__(self, message: str, code: str = "E007", program: str = "") -> None:
        super().__init__(message, severity=ErrorSeverity.ERROR, code=code, program=program)


class SystemError(AppError):
    """System-level errors (ERRHAND.cpy ERR-CAT-SYSTEM)."""

    def __init__(self, message: str, code: str = "E005", program: str = "") -> None:
        super().__init__(message, severity=ErrorSeverity.SEVERE, code=code, program=program)


# ---------------------------------------------------------------------------
# Error message dataclass (ERR-MESSAGE from ERRHAND.cpy)
# ---------------------------------------------------------------------------


@dataclass
class ErrorMessage:
    """Structured error message matching ERRHAND.cpy ERR-MESSAGE layout.

    Attributes:
        timestamp: When the error occurred.
        program: Originating program (PIC X(8)).
        category: Error category code (ErrorCategory enum).
        code: 4-character error code (PIC X(4)).
        severity: Numeric severity (ErrorSeverity).
        text: Short error description (max 80 chars, PIC X(80)).
        details: Extended details (max 256 chars, PIC X(256)).
    """

    timestamp: datetime = field(default_factory=datetime.now)
    program: str = ""
    category: ErrorCategory = ErrorCategory.SYSTEM
    code: str = ""
    severity: ErrorSeverity = ErrorSeverity.ERROR
    text: str = ""
    details: str = ""

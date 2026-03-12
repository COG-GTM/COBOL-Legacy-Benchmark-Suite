"""
Error handler translated from COBOL programs ERRPROC.cbl and ERRHNDL.cbl.

Replaces:
  - ERRPROC.cbl: Error processing and logging
  - ERRHNDL.cbl: Error severity classification
  - DB2ERR.cbl: DB2 SQL error mapping

Maps COBOL return codes (0/4/8/12/16) to a Python exception hierarchy.
"""

import logging
from datetime import datetime

from src.common.constants import (
    DB2_RETRYABLE_CODES,
    DB2_SEVERITY_MAP,
    ErrorCategory,
    ReturnCode,
    StandardErrorCode,
)

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Exception Hierarchy  (replaces COBOL RETURN-CODE convention)
# ---------------------------------------------------------------------------
class ApplicationError(Exception):
    """Base exception for all application errors. Maps to RETURN-CODE 8."""

    def __init__(
        self,
        message: str,
        error_code: str = "",
        severity: ReturnCode = ReturnCode.ERROR,
        category: ErrorCategory = ErrorCategory.PROCESSING,
        program: str = "",
        paragraph: str = "",
    ) -> None:
        super().__init__(message)
        self.error_code = error_code
        self.severity = severity
        self.category = category
        self.program = program
        self.paragraph = paragraph
        self.timestamp = datetime.now()


class ValidationError(ApplicationError):
    """Validation error. Maps to RETURN-CODE 4 (warning) or 8 (error)."""

    def __init__(self, message: str, field: str = "", **kwargs: object) -> None:
        super().__init__(
            message,
            error_code=kwargs.pop("error_code", StandardErrorCode.VALIDATION),  # type: ignore[arg-type]
            severity=kwargs.pop("severity", ReturnCode.WARNING),  # type: ignore[arg-type]
            category=ErrorCategory.VALIDATION,
            **kwargs,  # type: ignore[arg-type]
        )
        self.field = field


class DatabaseError(ApplicationError):
    """Database error. Maps to RETURN-CODE 8 or 12."""

    def __init__(
        self,
        message: str,
        sqlcode: int | None = None,
        sqlstate: str = "",
        **kwargs: object,
    ) -> None:
        severity = kwargs.pop("severity", None)
        if severity is None:
            severity = _sqlcode_to_severity(sqlcode) if sqlcode else ReturnCode.ERROR
        super().__init__(
            message,
            error_code=kwargs.pop("error_code", StandardErrorCode.DB_ERROR),  # type: ignore[arg-type]
            severity=severity,  # type: ignore[arg-type]
            category=ErrorCategory.SYSTEM,
            **kwargs,  # type: ignore[arg-type]
        )
        self.sqlcode = sqlcode
        self.sqlstate = sqlstate
        self.retryable = sqlcode in DB2_RETRYABLE_CODES if sqlcode else False


class BatchError(ApplicationError):
    """Batch processing error. Maps to RETURN-CODE 12 (severe)."""

    def __init__(self, message: str, step: str = "", **kwargs: object) -> None:
        super().__init__(
            message,
            error_code=kwargs.pop("error_code", StandardErrorCode.PROCESSING),  # type: ignore[arg-type]
            severity=kwargs.pop("severity", ReturnCode.SEVERE),  # type: ignore[arg-type]
            category=ErrorCategory.PROCESSING,
            **kwargs,  # type: ignore[arg-type]
        )
        self.step = step


class NotFoundError(ApplicationError):
    """Record not found. Maps to VSAM status 23 / SQLCODE +100."""

    def __init__(self, message: str, **kwargs: object) -> None:
        super().__init__(
            message,
            error_code=kwargs.pop("error_code", StandardErrorCode.NOT_FOUND),  # type: ignore[arg-type]
            severity=kwargs.pop("severity", ReturnCode.WARNING),  # type: ignore[arg-type]
            category=ErrorCategory.PROCESSING,
            **kwargs,  # type: ignore[arg-type]
        )


class DuplicateError(ApplicationError):
    """Duplicate record. Maps to VSAM status 22 / SQLCODE -803."""

    def __init__(self, message: str, **kwargs: object) -> None:
        super().__init__(
            message,
            error_code=kwargs.pop("error_code", StandardErrorCode.DUPLICATE),  # type: ignore[arg-type]
            severity=kwargs.pop("severity", ReturnCode.WARNING),  # type: ignore[arg-type]
            category=ErrorCategory.PROCESSING,
            **kwargs,  # type: ignore[arg-type]
        )


class SecurityError(ApplicationError):
    """Security/authorization error."""

    def __init__(self, message: str, **kwargs: object) -> None:
        super().__init__(
            message,
            error_code=kwargs.pop("error_code", StandardErrorCode.SECURITY),  # type: ignore[arg-type]
            severity=kwargs.pop("severity", ReturnCode.ERROR),  # type: ignore[arg-type]
            category=ErrorCategory.SYSTEM,
            **kwargs,  # type: ignore[arg-type]
        )


# ---------------------------------------------------------------------------
# Error handling functions  (from ERRPROC.cbl paragraphs)
# ---------------------------------------------------------------------------
def handle_error(error: ApplicationError) -> None:
    """
    Process and log an application error.

    Translates ERRPROC.cbl 1000-PROCESS-ERROR.
    """
    log_func = _get_log_function(error.severity)
    log_func(
        "Error [%s] severity=%d category=%s program=%s: %s",
        error.error_code,
        error.severity,
        error.category,
        error.program,
        str(error),
    )


def _get_log_function(severity: ReturnCode) -> logging.Logger.warning:  # type: ignore[name-defined]
    """Map COBOL severity to Python log level."""
    if severity >= ReturnCode.CRITICAL:
        return logger.critical
    if severity >= ReturnCode.SEVERE:
        return logger.error
    if severity >= ReturnCode.ERROR:
        return logger.error
    if severity >= ReturnCode.WARNING:
        return logger.warning
    return logger.info


def _sqlcode_to_severity(sqlcode: int | None) -> ReturnCode:
    """
    Map DB2 SQLCODE to severity level.

    Translates DB2ERR.cbl 1100-SET-SEVERITY.
    """
    if sqlcode is None:
        return ReturnCode.ERROR
    severity_val = DB2_SEVERITY_MAP.get(sqlcode, 3)
    if severity_val >= 4:
        return ReturnCode.CRITICAL
    if severity_val >= 3:
        return ReturnCode.SEVERE
    if severity_val >= 2:
        return ReturnCode.ERROR
    return ReturnCode.WARNING

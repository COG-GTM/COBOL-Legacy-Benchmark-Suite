"""
Error handling translated from COBOL programs:
- ERRPROC.cbl (Standard Error Processing Subroutine)
- ERRHNDL.cbl (Centralized Error Handler)

Maps COBOL return codes (0/4/8/12/16) to Python exception hierarchy.
"""

import logging
from datetime import datetime

from src.common.constants import ErrorCategory, ReturnCode

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Exception hierarchy (replaces COBOL return code convention)
# ---------------------------------------------------------------------------
class ApplicationError(Exception):
    """Base exception. Maps to RETURN-CODE > 0."""

    def __init__(
        self,
        message: str,
        severity: ReturnCode = ReturnCode.ERROR,
        category: ErrorCategory = ErrorCategory.PROCESSING,
        error_code: str = "0000",
        program: str = "",
        details: str = "",
    ):
        super().__init__(message)
        self.severity = severity
        self.category = category
        self.error_code = error_code
        self.program = program
        self.details = details
        self.timestamp = datetime.now()


class ValidationError(ApplicationError):
    """Validation errors. Maps to RETURN-CODE = 4 or 8."""

    def __init__(self, message: str, field: str = "", **kwargs):
        super().__init__(
            message,
            severity=kwargs.pop("severity", ReturnCode.WARNING),
            category=ErrorCategory.VALIDATION,
            **kwargs,
        )
        self.field = field


class DatabaseError(ApplicationError):
    """Database errors. Maps to RETURN-CODE = 8 or 12."""

    def __init__(self, message: str, sqlcode: int = 0, **kwargs):
        super().__init__(
            message,
            severity=kwargs.pop("severity", ReturnCode.ERROR),
            category=ErrorCategory.DATABASE,
            **kwargs,
        )
        self.sqlcode = sqlcode


class BatchError(ApplicationError):
    """Batch processing errors. Maps to RETURN-CODE = 8 or 12."""

    def __init__(self, message: str, job_name: str = "", **kwargs):
        super().__init__(
            message,
            severity=kwargs.pop("severity", ReturnCode.ERROR),
            category=ErrorCategory.PROCESSING,
            **kwargs,
        )
        self.job_name = job_name


class SecurityError(ApplicationError):
    """Security errors. Maps to RETURN-CODE = 8 or 12."""

    def __init__(self, message: str, user_id: str = "", **kwargs):
        super().__init__(
            message,
            severity=kwargs.pop("severity", ReturnCode.ERROR),
            category=ErrorCategory.SECURITY,
            **kwargs,
        )
        self.user_id = user_id


# ---------------------------------------------------------------------------
# Error processor (translates ERRPROC.cbl 2000-PROCESS-ERROR)
# ---------------------------------------------------------------------------
def process_error(
    error: ApplicationError,
    session=None,
) -> None:
    """
    Process and log an error.
    Translates ERRPROC.cbl: build error message, write to log, display.
    """
    # 2200-DISPLAY-ERROR equivalent
    logger.error(
        "ERROR DETECTED: %s | Program: %s | Category: %s | Code: %s | "
        "Severity: %s | Message: %s | Details: %s",
        error.timestamp.isoformat(),
        error.program,
        error.category.value,
        error.error_code,
        error.severity.value,
        str(error),
        error.details,
    )

    # 2100-WRITE-LOG equivalent — write to DB if session available
    if session is not None:
        try:
            from src.db.tables import ErrorLog

            record = ErrorLog(
                timestamp=error.timestamp,
                program=error.program,
                category=error.category.value,
                error_code=error.error_code,
                severity=error.severity.value,
                error_text=str(error)[:80],
                error_details=error.details[:256] if error.details else "",
            )
            session.add(record)
            session.flush()
        except Exception as log_err:
            logger.warning("Failed to write error to database: %s", log_err)

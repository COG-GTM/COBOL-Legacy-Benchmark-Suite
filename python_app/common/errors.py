"""Error handling module - replaces ERRPROC.cbl and ERRHNDL.cbl.

Provides centralized error processing with Python logging, custom exceptions,
and error recording matching the COBOL error handling patterns.

ERRPROC.cbl functions: INIT, LOG, RETN
ERRHNDL.cbl functions: LOG, DIAG, RETN (with DB2 ERRLOG insert)
"""

import logging
import traceback
from datetime import datetime
from enum import IntEnum
from typing import Any

logger = logging.getLogger("portfolio.errors")


class ErrorSeverity(IntEnum):
    """Error severity levels matching COBOL ERR-SEVERITY."""

    INFO = 1
    WARNING = 2
    ERROR = 3
    FATAL = 4


class PortfolioError(Exception):
    """Base exception for the Portfolio Management System.

    Replaces COBOL ERRPROC/ERRHNDL error record structure.
    """

    def __init__(
        self,
        message: str,
        *,
        program_id: str = "",
        error_code: str = "",
        severity: ErrorSeverity = ErrorSeverity.ERROR,
        category: str = "",
        details: str = "",
        sqlcode: int = 0,
    ) -> None:
        super().__init__(message)
        self.program_id = program_id
        self.error_code = error_code
        self.severity = severity
        self.category = category
        self.details = details
        self.sqlcode = sqlcode
        self.timestamp = datetime.now().isoformat()


class DatabaseError(PortfolioError):
    """Database-related errors (replaces DB2 SQLCODE error handling)."""

    def __init__(self, message: str, *, sqlcode: int = 0, **kwargs: Any) -> None:
        super().__init__(message, sqlcode=sqlcode, category="DB", **kwargs)


class ValidationError(PortfolioError):
    """Data validation errors (replaces TRNVAL00 validation failures)."""

    def __init__(self, message: str, **kwargs: Any) -> None:
        super().__init__(message, category="VL", **kwargs)


class BatchProcessingError(PortfolioError):
    """Batch processing errors."""

    def __init__(self, message: str, *, return_code: int = 8, **kwargs: Any) -> None:
        super().__init__(message, category="BT", **kwargs)
        self.return_code = return_code


class SecurityError(PortfolioError):
    """Security/authentication errors (replaces SECMGR failures)."""

    def __init__(self, message: str, **kwargs: Any) -> None:
        super().__init__(message, category="SC", **kwargs)


class RecoveryError(PortfolioError):
    """Recovery processing errors (replaces RCVPRC00 failures)."""

    def __init__(self, message: str, **kwargs: Any) -> None:
        super().__init__(message, category="RC", **kwargs)


class ErrorHandler:
    """Centralized error handler replacing ERRHNDL.cbl.

    Provides:
    - Error logging (P200-LOG-ERROR)
    - Error diagnostics (P300-DIAGNOSE-ERROR)
    - Error count tracking (P400-GET-COUNT / P500-RESET-COUNT)
    """

    def __init__(self, program_id: str) -> None:
        self.program_id = program_id
        self.error_count = 0
        self.warning_count = 0
        self.errors: list[dict[str, Any]] = []
        self._logger = logging.getLogger(f"portfolio.{program_id}")

    def log_error(
        self,
        message: str,
        *,
        severity: ErrorSeverity = ErrorSeverity.ERROR,
        error_code: str = "",
        paragraph: str = "",
        details: str = "",
        sqlcode: int = 0,
        exc: Exception | None = None,
    ) -> None:
        """Log an error - replaces ERRHNDL P200-LOG-ERROR.

        In COBOL, this inserted into the ERRLOG DB2 table.
        Here we log via Python logging and store for later DB persistence.
        """
        error_record = {
            "timestamp": datetime.now().isoformat(),
            "program_id": self.program_id,
            "error_code": error_code,
            "severity": severity,
            "message": message,
            "paragraph": paragraph,
            "details": details,
            "sqlcode": sqlcode,
            "traceback": traceback.format_exc() if exc else "",
        }
        self.errors.append(error_record)

        if severity >= ErrorSeverity.ERROR:
            self.error_count += 1
            self._logger.error(
                "[%s] %s (code=%s, sqlcode=%d) %s",
                self.program_id,
                message,
                error_code,
                sqlcode,
                details,
            )
        elif severity == ErrorSeverity.WARNING:
            self.warning_count += 1
            self._logger.warning("[%s] %s (code=%s)", self.program_id, message, error_code)
        else:
            self._logger.info("[%s] %s", self.program_id, message)

    def diagnose_error(self, exc: Exception) -> dict[str, Any]:
        """Diagnose an error - replaces ERRHNDL P300-DIAGNOSE-ERROR.

        Returns diagnostic information about the error.
        """
        diag: dict[str, Any] = {
            "type": type(exc).__name__,
            "message": str(exc),
            "traceback": traceback.format_exception(exc),
        }
        if isinstance(exc, PortfolioError):
            diag["program_id"] = exc.program_id
            diag["error_code"] = exc.error_code
            diag["severity"] = exc.severity
            diag["category"] = exc.category
            diag["sqlcode"] = exc.sqlcode
        return diag

    def get_counts(self) -> tuple[int, int]:
        """Get error and warning counts - replaces P400-GET-COUNT."""
        return self.error_count, self.warning_count

    def reset_counts(self) -> None:
        """Reset error counters - replaces P500-RESET-COUNT."""
        self.error_count = 0
        self.warning_count = 0
        self.errors.clear()


def setup_logging(level: int = logging.INFO) -> None:
    """Configure logging for the portfolio management system.

    Replaces COBOL DISPLAY and WRITE statements with structured logging.
    """
    logging.basicConfig(
        level=level,
        format="%(asctime)s [%(name)s] %(levelname)s: %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    )

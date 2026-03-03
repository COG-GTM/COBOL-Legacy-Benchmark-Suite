"""Error handling services.

Replaces:
  - ERRPROC (src/programs/common/ERRPROC.cbl) — batch error processor
  - ERRHNDL (src/programs/online/ERRHNDL.cbl) — online error handler

Provides structured Python logging + exception classes instead of
COBOL error log files and CICS error screen display.
"""

from __future__ import annotations

import logging
from datetime import datetime
from enum import IntEnum

from sqlalchemy.orm import Session

from portfolio_manager.models.database import AuditLog, ErrorLog

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Exception hierarchy (replaces COBOL return-code error signaling)
# ---------------------------------------------------------------------------


class PortfolioError(Exception):
    """Base exception for all portfolio application errors."""

    def __init__(
        self,
        message: str,
        error_code: str = "E000",
        program_id: str = "",
        severity: int = 3,
    ):
        super().__init__(message)
        self.error_code = error_code
        self.program_id = program_id
        self.severity = severity


class ValidationError(PortfolioError):
    """Validation error (replaces ERR-CAT-VALID / 'VL')."""

    def __init__(self, message: str, error_code: str = "E008", program_id: str = ""):
        super().__init__(message, error_code, program_id, severity=2)


class FileError(PortfolioError):
    """File I/O error (replaces ERR-CAT-VSAM / 'VS')."""

    def __init__(self, message: str, error_code: str = "E004", program_id: str = ""):
        super().__init__(message, error_code, program_id, severity=3)


class DatabaseError(PortfolioError):
    """Database error (replaces ERR-CAT-PROC for DB2 issues)."""

    def __init__(self, message: str, error_code: str = "E005", program_id: str = ""):
        super().__init__(message, error_code, program_id, severity=3)


class SecurityError(PortfolioError):
    """Security/authorization error."""

    def __init__(self, message: str, error_code: str = "E006", program_id: str = ""):
        super().__init__(message, error_code, program_id, severity=4)


class ProcessingError(PortfolioError):
    """General processing error."""

    def __init__(self, message: str, error_code: str = "E007", program_id: str = ""):
        super().__init__(message, error_code, program_id, severity=3)


# ---------------------------------------------------------------------------
# Severity levels (from ERRHAND.cpy / ERRHNDL.cbl)
# ---------------------------------------------------------------------------


class Severity(IntEnum):
    """Error severity levels matching COBOL EL-ERROR-SEVERITY."""

    INFO = 1
    WARNING = 2
    ERROR = 3
    SEVERE = 4


# ---------------------------------------------------------------------------
# Error Processor — replaces ERRPROC.cbl
# ---------------------------------------------------------------------------


class ErrorProcessor:
    """Batch error processor.

    Replaces ERRPROC (src/programs/common/ERRPROC.cbl).
    Logs errors to the database (ERRLOG table) and to Python logging.
    """

    def __init__(self, session: Session | None = None):
        self._session = session

    def process_error(
        self,
        program_id: str,
        category: str,
        error_code: str,
        severity: int,
        error_text: str,
        details: str = "",
        user_id: str = "SYSTEM",
    ) -> int:
        """Process and log an error.

        Mirrors ERRPROC paragraphs:
          1000-INITIALIZE  -> set timestamp
          2000-PROCESS-ERROR -> build and write error message
          2100-WRITE-LOG   -> insert into ERRLOG
          2200-DISPLAY-ERROR -> log to Python logger

        Args:
            program_id: Originating program identifier.
            category: Error category (VS, VL, PR, SY).
            error_code: Error code string.
            severity: Severity level (1-4).
            error_text: Human-readable error message.
            details: Additional error details.
            user_id: User ID associated with the error.

        Returns:
            The severity level (used as return code, mirroring ERRPROC behavior).
        """
        now = datetime.now()

        # 2200-DISPLAY-ERROR equivalent — structured logging
        log_method = {
            Severity.INFO: logger.info,
            Severity.WARNING: logger.warning,
            Severity.ERROR: logger.error,
            Severity.SEVERE: logger.critical,
        }.get(severity, logger.error)

        log_method(
            "ERROR DETECTED | program=%s category=%s code=%s severity=%d | %s | %s",
            program_id,
            category,
            error_code,
            severity,
            error_text,
            details,
        )

        # 2100-WRITE-LOG equivalent — persist to ERRLOG table
        if self._session is not None:
            try:
                error_record = ErrorLog(
                    error_timestamp=now,
                    program_id=program_id,
                    error_type=_category_to_type(category),
                    error_severity=severity,
                    error_code=error_code,
                    error_message=error_text[:200],
                    process_date=now.date(),
                    process_time=now.time(),
                    user_id=user_id,
                    additional_info=details[:500] if details else None,
                )
                with self._session.begin_nested():
                    self._session.add(error_record)
            except Exception as exc:
                logger.error("Failed to write error log to database: %s", exc)

        return severity


def _category_to_type(category: str) -> str:
    """Map COBOL error category to error type code.

    VS/VL -> D (Data), PR -> A (Application), SY -> S (System).
    """
    mapping = {"VS": "D", "VL": "D", "PR": "A", "SY": "S"}
    return mapping.get(category, "A")


# ---------------------------------------------------------------------------
# Online Error Handler — replaces ERRHNDL.cbl
# ---------------------------------------------------------------------------


class OnlineErrorHandler:
    """Online error handler.

    Replaces ERRHNDL (src/programs/online/ERRHNDL.cbl).
    Instead of CICS error screens, returns structured error responses
    for the FastAPI layer.
    """

    def __init__(self, session: Session | None = None):
        self._error_processor = ErrorProcessor(session)

    def handle_error(
        self,
        program: str,
        paragraph: str,
        message: str,
        severity: str = "W",
        sqlcode: int = 0,
    ) -> dict[str, object]:
        """Handle an online error.

        Replaces ERRHNDL CICS LINK processing. Instead of formatting
        an error screen, returns a structured dict for API responses.

        Args:
            program: Program name where error occurred.
            paragraph: Paragraph/function name.
            message: Error message.
            severity: F=Fatal, W=Warning, I=Info.
            sqlcode: SQL error code (0 if not DB2-related).

        Returns:
            Dict with error details for API response.
        """
        sev_int = {"F": Severity.SEVERE, "W": Severity.WARNING, "I": Severity.INFO}.get(
            severity, Severity.ERROR
        )

        self._error_processor.process_error(
            program_id=program,
            category="PR",
            error_code="E007",
            severity=sev_int,
            error_text=message,
            details=f"paragraph={paragraph} sqlcode={sqlcode}",
        )

        action = "abend" if severity == "F" else "continue"

        return {
            "error": True,
            "program": program,
            "paragraph": paragraph,
            "severity": severity,
            "message": message,
            "sqlcode": sqlcode,
            "action": action,
        }


# ---------------------------------------------------------------------------
# Audit logger — writes to audit_log table (replaces AUDPROC.cbl)
# ---------------------------------------------------------------------------


def log_audit_event(
    session: Session,
    user_id: str,
    program: str,
    audit_type: str,
    action: str,
    status: str = "SUCC",
    portfolio_id: str | None = None,
    account_no: str | None = None,
    before_image: str | None = None,
    after_image: str | None = None,
    message: str | None = None,
) -> None:
    """Write an audit log entry.

    Replaces AUDPROC (src/programs/common/AUDPROC.cbl).

    Args:
        session: Active database session.
        user_id: User performing the action.
        program: Program name.
        audit_type: TRAN, USER, or SYST.
        action: CREATE, UPDATE, DELETE, INQUIRE, LOGIN, LOGOUT, etc.
        status: SUCC, FAIL, or WARN.
        portfolio_id: Optional portfolio ID.
        account_no: Optional account number.
        before_image: Optional before-change snapshot.
        after_image: Optional after-change snapshot.
        message: Optional descriptive message.
    """
    record = AuditLog(
        user_id=user_id,
        program=program,
        audit_type=audit_type,
        action=action,
        status=status,
        portfolio_id=portfolio_id,
        account_no=account_no,
        before_image=before_image,
        after_image=after_image,
        message=message,
    )
    session.add(record)
    session.flush()
    logger.debug(
        "Audit: user=%s program=%s type=%s action=%s status=%s",
        user_id,
        program,
        audit_type,
        action,
        status,
    )

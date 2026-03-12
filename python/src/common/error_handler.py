"""
Error handling framework for the Investment Portfolio Management System.

Migrated from COBOL sources:
  - src/programs/common/ERRPROC.cbl   (Standard error processing subroutine)
  - src/programs/online/ERRHNDL.cbl   (Centralized online error handler)
  - src/programs/common/DB2ERR.cbl    (DB2 SQL error handler)
  - src/copybook/common/ERRHAND.cpy   (Error handling data structures)
  - src/copybook/common/RETHND.cpy    (Return handling / recovery actions)
  - src/copybook/online/ERRHND.cpy    (Online error handling structures)

Key COBOL patterns preserved:
  - ERRPROC.cbl:  0000-MAIN -> 1000-INITIALIZE -> 2000-PROCESS-ERROR
                  -> 2100-WRITE-LOG -> 2200-DISPLAY-ERROR -> 3000-TERMINATE
  - ERRHNDL.cbl:  P400-DETERMINE-ACTION EVALUATE TRUE for action routing
  - DB2ERR.cbl:   1100-SET-SEVERITY EVALUATE for SQL code classification
  - DB2ERR.cbl:   2000-DIAGNOSE-ERROR EVALUATE for SQL code diagnosis
  - RETHND.cpy:   ACTION-FLAG level-88 values (Continue/Abort/Retry)
"""

from __future__ import annotations

import logging
import uuid
from datetime import datetime, timezone
from dataclasses import dataclass
from typing import Any

from .constants import (
    DB2SqlCode,
    ErrorCategory,
    MAX_RETRY_COUNT,
    RecoveryAction,
    Severity,
    StandardErrorCode,
)

logger = logging.getLogger("portfolio.error")


# ============================================================
# Custom Exception Hierarchy
# ============================================================

class ApplicationError(Exception):
    """Base exception for all application errors.

    Mirrors the COBOL error record structure from ERRHAND.cpy:
    ERR-MESSAGE contains timestamp, program, category, code, severity,
    text, and details.

    Attributes:
        error_code: Application error code (e.g. ``"E001"``).
        message: Human-readable description.
        severity: Error severity level.
        source_program: Name of the originating program/module.
        timestamp: When the error occurred.
        trace_id: Unique trace identifier (mirrors ERRHNDL.cbl ERR-TRACE-ID).
        details: Additional context information.
    """

    def __init__(
        self,
        error_code: str = "",
        message: str = "",
        severity: Severity = Severity.ERROR,
        source_program: str = "",
        timestamp: datetime | None = None,
        trace_id: str | None = None,
        details: str = "",
    ) -> None:
        self.error_code = error_code
        self.severity = severity
        self.source_program = source_program
        self.timestamp = timestamp or datetime.now(timezone.utc)
        self.trace_id = trace_id or uuid.uuid4().hex[:16]
        self.details = details
        super().__init__(message)

    @property
    def message(self) -> str:
        """The error message text."""
        return str(self)

    def formatted_message(self) -> str:
        """Format the error like ERRHNDL.cbl P300-FORMAT-MESSAGE.

        Produces: ``Error in <program> - <message> (<trace_id>)``
        """
        return (
            f"Error in {self.source_program} - {self.message} "
            f"({self.trace_id})"
        )


class ValidationError(ApplicationError):
    """Input validation failures.

    Maps to RETHND.cpy ERR-VALIDATION (ERROR-TYPE = 'V')
    and STD-ERROR-CODES ERR-INVALID-DATA / ERR-VALIDATION.
    """

    def __init__(
        self,
        error_code: str = StandardErrorCode.VALIDATION,
        message: str = "Validation error",
        severity: Severity = Severity.ERROR,
        source_program: str = "",
        **kwargs: Any,
    ) -> None:
        super().__init__(
            error_code=error_code,
            message=message,
            severity=severity,
            source_program=source_program,
            **kwargs,
        )


class DatabaseError(ApplicationError):
    """Database / VSAM operation failures.

    Maps to RETHND.cpy ERR-DATABASE (ERROR-TYPE = 'D')
    and the DB2ERR.cbl error classification logic.

    Attributes:
        sqlcode: The DB2 SQL code (mirrors DB2ERR.cbl LS-SQLCODE).
        sqlstate: The DB2 SQL state (mirrors DB2ERR.cbl LS-SQLSTATE).
        should_retry: Whether the operation should be retried
            (mirrors DB2ERR.cbl LS-RETRY-FLAG).
    """

    def __init__(
        self,
        error_code: str = StandardErrorCode.DB_ERROR,
        message: str = "Database error",
        severity: Severity = Severity.ERROR,
        source_program: str = "",
        sqlcode: int | None = None,
        sqlstate: str = "",
        should_retry: bool = False,
        **kwargs: Any,
    ) -> None:
        super().__init__(
            error_code=error_code,
            message=message,
            severity=severity,
            source_program=source_program,
            **kwargs,
        )
        self.sqlcode = sqlcode
        self.sqlstate = sqlstate
        self.should_retry = should_retry


class BatchError(ApplicationError):
    """Batch processing failures.

    Maps to RETHND.cpy ERR-PROCESSING (ERROR-TYPE = 'P')
    and batch checkpoint/restart scenarios.
    """

    def __init__(
        self,
        error_code: str = StandardErrorCode.PROCESSING,
        message: str = "Batch processing error",
        severity: Severity = Severity.ERROR,
        source_program: str = "",
        **kwargs: Any,
    ) -> None:
        super().__init__(
            error_code=error_code,
            message=message,
            severity=severity,
            source_program=source_program,
            **kwargs,
        )


class SecurityError(ApplicationError):
    """Authentication and access control failures.

    Maps to RETHND.cpy ERR-SECURITY (ERROR-TYPE = 'S')
    and STD-ERROR-CODES ERR-SECURITY.
    """

    def __init__(
        self,
        error_code: str = StandardErrorCode.SECURITY,
        message: str = "Security error",
        severity: Severity = Severity.SEVERE,
        source_program: str = "",
        **kwargs: Any,
    ) -> None:
        super().__init__(
            error_code=error_code,
            message=message,
            severity=severity,
            source_program=source_program,
            **kwargs,
        )


class RecoveryError(ApplicationError):
    """Checkpoint / restart failures.

    Raised when the system cannot recover from a previous failure,
    e.g. checkpoint data is corrupted or restart sequencing fails.
    """

    def __init__(
        self,
        error_code: str = StandardErrorCode.PROCESSING,
        message: str = "Recovery error",
        severity: Severity = Severity.FATAL,
        source_program: str = "",
        **kwargs: Any,
    ) -> None:
        super().__init__(
            error_code=error_code,
            message=message,
            severity=severity,
            source_program=source_program,
            **kwargs,
        )


# ============================================================
# Error Context — passed alongside errors for handler decisions
# ============================================================

@dataclass
class ErrorContext:
    """Contextual information accompanying an error.

    Mirrors the COBOL LINKAGE SECTION fields from ERRPROC.cbl
    (LS-ERROR-REQUEST) and ERRHNDL.cbl (DFHCOMMAREA).
    """

    program_id: str = ""
    paragraph_name: str = ""
    category: str = ErrorCategory.PROCESSING
    additional_info: str = ""
    user_id: str = ""
    terminal_id: str = ""
    retry_count: int = 0
    max_retries: int = MAX_RETRY_COUNT


# ============================================================
# Recovery Result — returned by ErrorHandler.handle_error
# ============================================================

@dataclass
class RecoveryResult:
    """Outcome of error handling, including the recovery action to take.

    Mirrors RETHND.cpy RETURN-ACTIONS and ERRHNDL.cbl P400-DETERMINE-ACTION.
    """

    action: RecoveryAction = RecoveryAction.ABORT
    return_code: int = 0
    error_message: str = ""
    trace_id: str = ""
    should_retry: bool = False
    retry_count: int = 0


# ============================================================
# ErrorHandler — main error processing class
# ============================================================

class ErrorHandler:
    """Centralized error handler.

    Combines the responsibilities of:
      - ERRPROC.cbl  (logging errors to file)
      - ERRHNDL.cbl  (classifying errors, determining actions)
      - DB2ERR.cbl   (diagnosing DB2-specific errors)

    The COBOL PERFORM flow is preserved:
      ERRPROC.cbl:  1000-INITIALIZE -> 2000-PROCESS-ERROR
                    -> 2100-WRITE-LOG -> 2200-DISPLAY-ERROR
      ERRHNDL.cbl:  P100-INIT -> P200-LOG-ERROR -> P300-FORMAT-MESSAGE
                    -> P400-DETERMINE-ACTION

    Usage::

        handler = ErrorHandler()
        try:
            ...
        except ApplicationError as exc:
            result = handler.handle_error(exc, ErrorContext(program_id="TRNVAL00"))
            if result.action == RecoveryAction.RETRY and result.should_retry:
                ...  # retry logic
    """

    def __init__(self, log_to_file: bool = False, log_file: str = "") -> None:
        self._log_to_file = log_to_file
        self._log_file = log_file

    # ----------------------------------------------------------
    # Public methods
    # ----------------------------------------------------------

    def handle_error(
        self,
        error: ApplicationError,
        context: ErrorContext | None = None,
    ) -> RecoveryResult:
        """Process an error: log it and determine the recovery action.

        Mirrors ERRPROC.cbl 0000-MAIN:
          PERFORM 1000-INITIALIZE
          PERFORM 2000-PROCESS-ERROR
          PERFORM 3000-TERMINATE

        And ERRHNDL.cbl main flow:
          P100-INIT -> P200-LOG -> P300-FORMAT -> P400-DETERMINE-ACTION

        Args:
            error: The application error to handle.
            context: Optional contextual information.

        Returns:
            A :class:`RecoveryResult` describing what action to take.
        """
        ctx = context or ErrorContext()

        # P100 / 1000 — initialize
        if not error.source_program and ctx.program_id:
            error.source_program = ctx.program_id

        # P200 / 2000 — log the error
        self.log_error(error, ctx)

        # P400 — determine recovery action
        action = self.get_recovery_action(error, ctx)

        return action

    def log_error(
        self,
        error: ApplicationError,
        context: ErrorContext | None = None,
    ) -> None:
        """Write error to log.

        Mirrors ERRPROC.cbl 2100-WRITE-LOG / 2200-DISPLAY-ERROR
        and ERRHNDL.cbl P200-LOG-ERROR (INSERT INTO ERRLOG).

        In production this would write to a database table (like ERRHNDL.cbl);
        in development it writes to the Python logger (replacing DISPLAY).

        Args:
            error: The application error to log.
            context: Optional contextual information.
        """
        ctx = context or ErrorContext()
        from .logging_config import severity_to_log_level

        log_level = severity_to_log_level(error.severity)

        extra = {
            "error_code": error.error_code,
            "program": error.source_program,
            "category": ctx.category,
            "severity_code": error.severity.value,
            "trace_id": error.trace_id,
            "source_program": error.source_program,
            "details": error.details,
        }

        logger.log(log_level, error.formatted_message(), extra=extra)

        # File-backed logging (dev mode, mirrors ERRPROC.cbl WRITE ERROR-LOG)
        if self._log_to_file and self._log_file:
            self._write_to_file(error, ctx)

    def get_recovery_action(
        self,
        error: ApplicationError | str,
        context: ErrorContext | None = None,
    ) -> RecoveryResult:
        """Determine whether to retry, skip (continue), or abort.

        Mirrors ERRHNDL.cbl P400-DETERMINE-ACTION::

            EVALUATE TRUE
                WHEN ERR-FATAL
                     SET ERR-ABEND TO TRUE
                WHEN ERR-WARNING
                     SET ERR-CONTINUE TO TRUE
                WHEN ERR-INFO
                     SET ERR-CONTINUE TO TRUE
                WHEN OTHER
                     SET ERR-RETURN TO TRUE
            END-EVALUATE

        For :class:`DatabaseError`, also incorporates the DB2ERR.cbl
        1100-SET-SEVERITY logic for deadlock/timeout retry decisions.

        Args:
            error: An :class:`ApplicationError` or an error code string.
            context: Optional contextual information (for retry tracking).

        Returns:
            A :class:`RecoveryResult`.
        """
        ctx = context or ErrorContext()

        # If passed a raw error code string, build a minimal result
        if isinstance(error, str):
            return self._resolve_action_for_code(error, ctx)

        result = RecoveryResult(
            return_code=error.severity.value,
            error_message=error.formatted_message(),
            trace_id=error.trace_id,
        )

        # DB2-specific retry logic from DB2ERR.cbl 1100-SET-SEVERITY
        if isinstance(error, DatabaseError):
            return self._determine_db_action(error, ctx, result)

        # General EVALUATE TRUE from ERRHNDL.cbl P400-DETERMINE-ACTION
        return self._determine_general_action(error, ctx, result)

    # ----------------------------------------------------------
    # Private methods — COBOL paragraph equivalents
    # ----------------------------------------------------------

    def _determine_general_action(
        self,
        error: ApplicationError,
        context: ErrorContext,
        result: RecoveryResult,
    ) -> RecoveryResult:
        """Translate ERRHNDL.cbl P400-DETERMINE-ACTION EVALUATE TRUE.

        COBOL logic:
          WHEN ERR-FATAL       -> SET ERR-ABEND TO TRUE
          WHEN ERR-WARNING     -> SET ERR-CONTINUE TO TRUE
          WHEN ERR-INFO        -> SET ERR-CONTINUE TO TRUE
          WHEN OTHER           -> SET ERR-RETURN TO TRUE
        """
        if error.severity == Severity.FATAL:
            result.action = RecoveryAction.ABORT
        elif error.severity == Severity.SEVERE:
            result.action = RecoveryAction.ABORT
        elif error.severity == Severity.WARNING:
            result.action = RecoveryAction.CONTINUE
        elif error.severity == Severity.SUCCESS:
            result.action = RecoveryAction.CONTINUE
        else:
            # ERROR (8) — "WHEN OTHER -> SET ERR-RETURN TO TRUE"
            # Check if retry is viable
            if context.retry_count < context.max_retries:
                result.action = RecoveryAction.RETRY
                result.should_retry = True
                result.retry_count = context.retry_count + 1
            else:
                result.action = RecoveryAction.ABORT

        return result

    def _determine_db_action(
        self,
        error: DatabaseError,
        context: ErrorContext,
        result: RecoveryResult,
    ) -> RecoveryResult:
        """DB2-specific recovery logic from DB2ERR.cbl.

        Maps the EVALUATE LS-SQLCODE in 1100-SET-SEVERITY and
        2000-DIAGNOSE-ERROR:

          WHEN WS-DEADLOCK / WS-TIMEOUT:
              severity=2, SHOULD-RETRY='Y', return-code=4
          WHEN WS-CONNECTION-ERROR:
              severity=4, NO-RETRY, return-code=12
          WHEN WS-DUP-KEY:
              severity=1, NO-RETRY, return-code=8
          WHEN WS-NOT-FOUND:
              severity=1, NO-RETRY, return-code=4 (warning)
          WHEN OTHER (negative SQLCODE):
              severity=3, NO-RETRY, return-code=12
        """
        sqlcode = error.sqlcode

        if sqlcode in (DB2SqlCode.DEADLOCK, DB2SqlCode.TIMEOUT):
            # Deadlock/timeout — retryable
            if context.retry_count < context.max_retries:
                result.action = RecoveryAction.RETRY
                result.should_retry = True
                result.retry_count = context.retry_count + 1
                result.return_code = Severity.WARNING.value
            else:
                result.action = RecoveryAction.ABORT
                result.return_code = Severity.ERROR.value
                result.error_message = (
                    "Max retries exceeded for deadlock/timeout"
                )

        elif sqlcode == DB2SqlCode.CONNECTION_ERROR:
            result.action = RecoveryAction.ABORT
            result.return_code = Severity.SEVERE.value
            result.error_message = (
                "DB2 connection error - check availability"
            )

        elif sqlcode == DB2SqlCode.DUPLICATE_KEY:
            result.action = RecoveryAction.CONTINUE
            result.return_code = Severity.ERROR.value
            result.error_message = "Duplicate key violation"

        elif sqlcode == DB2SqlCode.NOT_FOUND:
            result.action = RecoveryAction.CONTINUE
            result.return_code = Severity.WARNING.value
            result.error_message = "Record not found"

        elif sqlcode is not None and sqlcode < 0:
            # Unhandled negative SQLCODE
            result.action = RecoveryAction.ABORT
            result.return_code = Severity.SEVERE.value
            result.error_message = "Unhandled DB2 error"

        else:
            # Positive / zero SQLCODE — warning
            result.action = RecoveryAction.CONTINUE
            result.return_code = Severity.WARNING.value
            result.error_message = "DB2 warning condition"

        return result

    def _resolve_action_for_code(
        self,
        error_code: str,
        context: ErrorContext,
    ) -> RecoveryResult:
        """Map a raw error code string to a recovery action.

        Uses the standard error codes from RETHND.cpy to determine
        severity and action.
        """
        result = RecoveryResult(trace_id=uuid.uuid4().hex[:16])

        # Codes that should abort
        abort_codes = {
            StandardErrorCode.DB_ERROR,
            StandardErrorCode.SECURITY,
            StandardErrorCode.FILE_ERROR,
        }
        # Codes that are retryable
        retry_codes = {
            StandardErrorCode.TIMEOUT,
        }
        # Codes that should continue
        continue_codes = {
            StandardErrorCode.NOT_FOUND,
            StandardErrorCode.DUPLICATE,
            StandardErrorCode.VERSION,
        }

        if error_code in retry_codes:
            if context.retry_count < context.max_retries:
                result.action = RecoveryAction.RETRY
                result.should_retry = True
                result.retry_count = context.retry_count + 1
                result.return_code = Severity.WARNING.value
            else:
                result.action = RecoveryAction.ABORT
                result.return_code = Severity.ERROR.value
        elif error_code in continue_codes:
            result.action = RecoveryAction.CONTINUE
            result.return_code = Severity.WARNING.value
        elif error_code in abort_codes:
            result.action = RecoveryAction.ABORT
            result.return_code = Severity.SEVERE.value
        else:
            # Default for unknown codes: ERROR severity, abort
            result.action = RecoveryAction.ABORT
            result.return_code = Severity.ERROR.value

        return result

    def _write_to_file(
        self,
        error: ApplicationError,
        context: ErrorContext,
    ) -> None:
        """Write error record to a file (dev mode).

        Mirrors ERRPROC.cbl 2100-WRITE-LOG / 2200-DISPLAY-ERROR:
            WRITE ERROR-LOG-RECORD
            DISPLAY '============================'
            DISPLAY 'ERROR DETECTED: ' ERR-TIMESTAMP
            ...
        """
        separator = "=" * 52
        lines = [
            separator,
            f"ERROR DETECTED: {error.timestamp.isoformat()}",
            f"PROGRAM:       {error.source_program}",
            f"CATEGORY:      {context.category}",
            f"CODE:          {error.error_code}",
            f"SEVERITY:      {error.severity.value}",
            f"MESSAGE:       {error.message}",
            f"DETAILS:       {error.details}",
            f"TRACE-ID:      {error.trace_id}",
            separator,
            "",
        ]
        try:
            with open(self._log_file, "a", encoding="utf-8") as fh:
                fh.write("\n".join(lines))
        except OSError as exc:
            # Mirror ERRPROC.cbl: DISPLAY 'Error writing to log: ' status
            logger.warning("Error writing to log file: %s", exc)

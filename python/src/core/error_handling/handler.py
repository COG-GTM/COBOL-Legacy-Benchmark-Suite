"""Centralized error handler with structured logging.

Mirrors COBOL ERRHNDL program logic:
  P100 - Initialize error context (timestamp, trace ID)
  P200 - Log error to persistent store (DB2 ERRLOG table)
  P300 - Format error message with context
  P400 - Determine recovery action based on severity

Also incorporates ERRPROC patterns for batch error processing.
"""

import logging
import uuid
from abc import ABC, abstractmethod
from datetime import datetime, timezone
from typing import Optional

from python.src.core.error_handling.exceptions import (
    CLBSError,
    ErrorSeverity,
    RecoveryAction,
)

logger = logging.getLogger("clbs.error_handler")


class ErrorLogStore(ABC):
    """Abstract interface for error log persistence.

    Mirrors the DB2 ERRLOG table INSERT in ERRHNDL P200-LOG-ERROR.
    Concrete implementations can use any database backend.
    """

    @abstractmethod
    def save_error_log(self, error_record: dict) -> bool:
        """Persist an error log record.

        Args:
            error_record: Dictionary containing error fields matching
                the ERRLOG table schema (timestamp, program, paragraph,
                sqlcode, severity, message, trace_id).

        Returns:
            True if the record was saved successfully, False otherwise.
        """
        ...


class ErrorHandler:
    """Centralized error handler mirroring COBOL ERRHNDL.

    Processes errors through the same pipeline as the COBOL program:
    1. Initialize error context (P100)
    2. Log error to store and structured logger (P200)
    3. Format error message with context (P300)
    4. Determine recovery action based on severity (P400)

    Attributes:
        error_store: Optional persistent store for error logs.
    """

    # Map severity to Python log levels
    _SEVERITY_LOG_LEVEL = {
        ErrorSeverity.WARNING: logging.WARNING,
        ErrorSeverity.ERROR: logging.ERROR,
        ErrorSeverity.SEVERE: logging.CRITICAL,
        ErrorSeverity.TERMINAL: logging.CRITICAL,
    }

    # Map severity to recovery action (P400 logic from ERRHNDL)
    _SEVERITY_ACTION = {
        ErrorSeverity.WARNING: RecoveryAction.CONTINUE,
        ErrorSeverity.ERROR: RecoveryAction.RETURN,
        ErrorSeverity.SEVERE: RecoveryAction.RETURN,
        ErrorSeverity.TERMINAL: RecoveryAction.ABEND,
    }

    def __init__(self, error_store: Optional[ErrorLogStore] = None) -> None:
        """Initialize the error handler.

        Args:
            error_store: Optional backend for persisting error logs.
                If not provided, errors are only logged via Python logging.
        """
        self._error_store = error_store

    def handle_error(
        self,
        error: Exception,
        program: str = "",
        paragraph: str = "",
        trace_id: Optional[str] = None,
    ) -> dict:
        """Process an error through the full ERRHNDL pipeline.

        Args:
            error: The exception to handle.
            program: Originating program/module name (ERR-PROGRAM).
            paragraph: Originating function/section name (ERR-PARAGRAPH).
            trace_id: Correlation ID; auto-generated if not provided.

        Returns:
            Dictionary with error details and determined recovery action.
        """
        # P100: Initialize error context
        context = self._init_error_context(error, program, paragraph, trace_id)

        # P200: Log error
        self._log_error(context)

        # P300: Format message
        context["formatted_message"] = self._format_message(context)

        # P400: Determine action
        context["recovery_action"] = self._determine_action(context)

        return context

    def handle_exception(
        self,
        error: Exception,
        program: str = "",
        paragraph: str = "",
        trace_id: Optional[str] = None,
    ) -> dict:
        """Alias for handle_error for non-CLBS exceptions.

        Wraps generic exceptions into the CLBS error context format.
        """
        return self.handle_error(error, program, paragraph, trace_id)

    def _init_error_context(
        self,
        error: Exception,
        program: str,
        paragraph: str,
        trace_id: Optional[str],
    ) -> dict:
        """P100: Initialize error handler context.

        Mirrors P100-INIT-ERROR-HANDLER:
        - Sets timestamp (FUNCTION CURRENT-DATE)
        - Generates trace ID if empty (FUNCTION RANDOM)
        - Copies error fields from DFHCOMMAREA
        """
        timestamp = datetime.now(timezone.utc).isoformat()

        if trace_id is None:
            trace_id = uuid.uuid4().hex[:16]

        if isinstance(error, CLBSError):
            context = {
                "timestamp": timestamp,
                "program": program or error.program,
                "paragraph": paragraph,
                "message": error.message,
                "category": error.category.value,
                "error_code": error.error_code,
                "severity": error.severity,
                "severity_name": error.severity.name.lower(),
                "severity_value": error.severity.value,
                "details": error.details,
                "trace_id": trace_id,
                "sqlcode": getattr(error, "sqlcode", None),
                "exception_type": type(error).__name__,
            }
        else:
            context = {
                "timestamp": timestamp,
                "program": program,
                "paragraph": paragraph,
                "message": str(error),
                "category": "SY",
                "error_code": "SY00",
                "severity": ErrorSeverity.ERROR,
                "severity_name": "error",
                "severity_value": ErrorSeverity.ERROR.value,
                "details": "",
                "trace_id": trace_id,
                "sqlcode": None,
                "exception_type": type(error).__name__,
            }

        return context

    def _log_error(self, context: dict) -> None:
        """P200: Log error to store and structured logger.

        Mirrors P200-LOG-ERROR which INSERTs into DB2 ERRLOG table.
        Falls back to logging if store write fails (matching ERRHNDL
        behavior: 'Error logging failed' -> ERR-FATAL).
        """
        severity = context["severity"]
        log_level = self._SEVERITY_LOG_LEVEL.get(severity, logging.ERROR)

        # Log via Python structured logging
        logger.log(
            log_level,
            context["message"],
            extra={
                "trace_id": context["trace_id"],
                "program": context["program"],
                "paragraph": context["paragraph"],
                "category": context["category"],
                "error_code": context["error_code"],
                "severity": context["severity_name"],
                "sqlcode": context["sqlcode"],
                "exception_type": context["exception_type"],
            },
        )

        # Persist to error store if available
        if self._error_store is not None:
            try:
                error_record = {
                    "timestamp": context["timestamp"],
                    "program": context["program"],
                    "paragraph": context["paragraph"],
                    "sqlcode": context["sqlcode"],
                    "severity": context["severity_name"],
                    "message": context["message"],
                    "trace_id": context["trace_id"],
                }
                success = self._error_store.save_error_log(error_record)
                if not success:
                    logger.error(
                        "Error logging failed",
                        extra={"trace_id": context["trace_id"]},
                    )
            except Exception as store_error:
                # Matches ERRHNDL: if logging INSERT fails, log the failure
                logger.error(
                    "Error logging failed: %s",
                    str(store_error),
                    extra={"trace_id": context["trace_id"]},
                )

    def _format_message(self, context: dict) -> str:
        """P300: Format error message with context.

        Mirrors P300-FORMAT-MESSAGE which builds:
        'Error in {PROGRAM} - {MESSAGE} ({TRACE-ID})'
        """
        program = context["program"]
        message = context["message"]
        trace_id = context["trace_id"]

        if program:
            return f"Error in {program} - {message} ({trace_id})"
        return f"{message} ({trace_id})"

    def _determine_action(self, context: dict) -> str:
        """P400: Determine recovery action based on severity.

        Mirrors P400-DETERMINE-ACTION EVALUATE:
          WHEN ERR-FATAL   -> ERR-ABEND   (abort)
          WHEN ERR-WARNING -> ERR-CONTINUE (continue)
          WHEN ERR-INFO    -> ERR-CONTINUE (continue)
          WHEN OTHER       -> ERR-RETURN   (return/retry)
        """
        severity = context["severity"]
        action = self._SEVERITY_ACTION.get(severity, RecoveryAction.RETURN)
        return action.value

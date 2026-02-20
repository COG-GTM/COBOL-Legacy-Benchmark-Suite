"""Centralized Error Handler - migrated from ERRHNDL.cbl.

Processes all online errors, logs errors to database, formats error
messages, and controls error recovery.
"""

import logging
from dataclasses import dataclass
from datetime import datetime

from portfolio_management.models.online import OnlineErrorHandling, ErrorSeverity, ErrorAction
from portfolio_management.models.common import ReturnCode

logger = logging.getLogger(__name__)

PROGRAM_ID = "ERRHNDL"


@dataclass
class ErrorLogEntry:
    timestamp: str = ""
    program: str = ""
    paragraph: str = ""
    severity: str = ""
    message: str = ""
    action_taken: str = ""


class OnlineErrorHandler:
    def __init__(self):
        self._error_log: list[ErrorLogEntry] = []
        self._error_count = 0

    def handle_error(self, error_info: OnlineErrorHandling) -> int:
        self._error_count += 1

        entry = ErrorLogEntry(
            timestamp=datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f"),
            program=error_info.program,
            paragraph=error_info.paragraph,
            severity=error_info.severity,
            message=error_info.message,
        )

        self._log_error(entry)
        formatted_msg = self._format_message(error_info)
        action = self._determine_action(error_info)
        entry.action_taken = action

        self._error_log.append(entry)

        if action == ErrorAction.ABEND:
            logger.critical("ABEND condition: %s - %s", error_info.program, formatted_msg)
            return ReturnCode.CRITICAL
        elif action == ErrorAction.RETURN:
            logger.error("Error (return): %s - %s", error_info.program, formatted_msg)
            return ReturnCode.ERROR
        else:
            logger.warning("Error (continue): %s - %s", error_info.program, formatted_msg)
            return ReturnCode.WARNING

    def _log_error(self, entry: ErrorLogEntry) -> None:
        logger.error(
            "Online Error - Program: %s, Paragraph: %s, Severity: %s - %s",
            entry.program,
            entry.paragraph,
            entry.severity,
            entry.message,
        )

    def _format_message(self, error_info: OnlineErrorHandling) -> str:
        parts = [f"Program: {error_info.program}"]

        if error_info.sqlcode != 0:
            parts.append(f"SQLCODE: {error_info.sqlcode}")
        if error_info.cics_resp != 0:
            parts.append(f"RESP: {error_info.cics_resp}")

        parts.append(f"Message: {error_info.message}")
        return " | ".join(parts)

    def _determine_action(self, error_info: OnlineErrorHandling) -> str:
        if error_info.action:
            return error_info.action

        if error_info.severity == ErrorSeverity.FATAL:
            return ErrorAction.ABEND
        elif error_info.severity == ErrorSeverity.WARNING:
            return ErrorAction.RETURN
        else:
            return ErrorAction.CONTINUE

    def get_error_log(self) -> list[ErrorLogEntry]:
        return list(self._error_log)

    @property
    def error_count(self) -> int:
        return self._error_count

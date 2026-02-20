"""Standard Error Processing Subroutine - migrated from ERRPROC.cbl.

Processes errors, writes to error log file, and displays error information.
"""

import logging
from dataclasses import dataclass
from datetime import datetime
from typing import Optional

from portfolio_management.models.error_handling import ErrorMessage

logger = logging.getLogger(__name__)


@dataclass
class ErrorRequest:
    program: str = ""
    category: str = ""
    code: str = ""
    severity: int = 0
    text: str = ""
    details: str = ""


class ErrorProcessor:
    def __init__(self, error_log_path: Optional[str] = None):
        self._error_log_path = error_log_path
        self._error_count = 0
        self._log_file = None

    def initialize(self, error_log_path: str) -> int:
        self._error_log_path = error_log_path
        try:
            self._log_file = open(error_log_path, "a")
            return 0
        except OSError as e:
            logger.error("Error opening error log file: %s - %s", error_log_path, e)
            return 8

    def process_error(self, request: ErrorRequest) -> int:
        now = datetime.now()
        error_msg = ErrorMessage(
            date=now.strftime("%Y-%m-%d"),
            time=now.strftime("%H:%M:%S"),
            program=request.program,
            category=request.category,
            code=request.code,
            severity=request.severity,
            text=request.text,
            details=request.details,
        )

        self._write_error_log(error_msg)
        self._display_error(error_msg)
        self._error_count += 1

        return 0

    def _write_error_log(self, error_msg: ErrorMessage) -> None:
        if self._log_file is None:
            return

        log_line = (
            f"{error_msg.date} {error_msg.time} "
            f"{error_msg.program:<8s} {error_msg.category} "
            f"{error_msg.code} {error_msg.severity:4d} "
            f"{error_msg.text}\n"
        )
        try:
            self._log_file.write(log_line)
            self._log_file.flush()
        except OSError as e:
            logger.error("Error writing to error log: %s", e)

    def _display_error(self, error_msg: ErrorMessage) -> None:
        logger.error(
            "ERROR: %s %s %s - %s",
            error_msg.program,
            error_msg.category,
            error_msg.code,
            error_msg.text,
        )
        if error_msg.details:
            logger.error("  Details: %s", error_msg.details)

    def terminate(self) -> int:
        if self._log_file is not None:
            try:
                self._log_file.close()
            except OSError:
                pass
            self._log_file = None

        logger.info("Total errors processed: %d", self._error_count)
        return 0

    @property
    def error_count(self) -> int:
        return self._error_count

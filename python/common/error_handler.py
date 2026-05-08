"""Replacement for ERRPROC.cbl.

Writes one fixed-width error record per call to a sequential ``ERROR-LOG``
file (default ``errlog.txt``) and emits structured log output via the
``logging`` module. The serialized record is exactly 400 characters wide,
matching the COBOL ``ERROR-LOG-RECORD.LOG-DATA PIC X(400)``.
"""

from __future__ import annotations

import logging
import os
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional, Union

from python.models.error_message import (
    ErrorCategory,
    ErrorMessage,
    ErrorSeverity,
)


LOGGER = logging.getLogger("HISTLD00")

# 10 + 8 + 8 + 2 + 4 + 4 + 80 + 256 = 372 chars; pad to 400 to mirror
# ERROR-LOG-RECORD.LOG-DATA PIC X(400) in ERRPROC.cbl.
LOG_RECORD_LENGTH = 400


class ErrorHandler:
    """Sequential error-log writer mirroring ERRPROC.cbl behavior."""

    def __init__(
        self,
        log_path: Union[str, os.PathLike, None] = None,
        program_id: str = "HISTLD00",
    ) -> None:
        self._log_path = Path(log_path or os.environ.get("ERRLOG_PATH") or "errlog.txt")
        self._program_id = program_id[:8]
        self._fh = None  # type: Optional[object]

    @property
    def program_id(self) -> str:
        return self._program_id

    @property
    def log_path(self) -> Path:
        return self._log_path

    def open(self) -> None:
        """Open the error log in append mode (matches ``OPEN EXTEND``)."""
        if self._fh is not None:
            return
        self._log_path.parent.mkdir(parents=True, exist_ok=True)
        self._fh = open(self._log_path, "a", encoding="utf-8")

    def close(self) -> None:
        if self._fh is not None:
            try:
                self._fh.flush()
            finally:
                self._fh.close()
                self._fh = None

    def __enter__(self) -> "ErrorHandler":
        self.open()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb) -> None:
        self.close()

    # ------------------------------------------------------------------
    # Public API: log_error mirrors ERRPROC PROCEDURE DIVISION USING ...
    # ------------------------------------------------------------------
    def log_error(
        self,
        program: Optional[str] = None,
        category: Union[str, ErrorCategory] = ErrorCategory.SYSTEM,
        code: str = "0000",
        severity: Union[int, ErrorSeverity] = ErrorSeverity.ERROR,
        text: str = "",
        details: str = "",
        timestamp: Optional[datetime] = None,
    ) -> ErrorMessage:
        """Append a formatted error record and emit a Python log entry."""
        msg = ErrorMessage(
            program=(program or self._program_id),
            category=(category.value if isinstance(category, ErrorCategory) else category),
            code=code,
            severity=int(severity),
            text=text,
            details=details,
            timestamp=timestamp or datetime.now(timezone.utc),
        )
        self._write_record(msg)
        self._emit_log(msg)
        return msg

    def log_message(self, msg: ErrorMessage) -> None:
        """Append a pre-built :class:`ErrorMessage`."""
        self._write_record(msg)
        self._emit_log(msg)

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------
    def _write_record(self, msg: ErrorMessage) -> None:
        record = self._format_record(msg)
        if self._fh is None:
            self.open()
        assert self._fh is not None
        self._fh.write(record + "\n")
        self._fh.flush()

    @staticmethod
    def _format_record(msg: ErrorMessage) -> str:
        """Format a single fixed-width error record (400 chars)."""
        body = (
            f"{msg.err_date:<10}"
            f"{msg.err_time:<8}"
            f"{msg.program:<8}"
            f"{msg.category:<2}"
            f"{msg.code:<4}"
            f"{msg.severity:>4}"
            f"{msg.text:<80}"
            f"{msg.details:<256}"
        )
        return body.ljust(LOG_RECORD_LENGTH)[:LOG_RECORD_LENGTH]

    @staticmethod
    def _emit_log(msg: ErrorMessage) -> None:
        level = logging.ERROR
        if msg.severity <= ErrorSeverity.WARNING:
            level = logging.WARNING
        elif msg.severity >= ErrorSeverity.SEVERE:
            level = logging.CRITICAL
        LOGGER.log(
            level,
            "[%s] %s/%s %s :: %s",
            msg.program,
            msg.category,
            msg.code,
            msg.text,
            msg.details,
        )

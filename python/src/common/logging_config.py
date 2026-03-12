"""
Logging configuration for the Investment Portfolio Management System.

Replaces COBOL DISPLAY statements, sysout/syslog writes, and the error log
file (ERRLOG) from ERRPROC.cbl with structured Python logging.

COBOL severity to Python log level mapping:
  0  (SUCCESS)  -> INFO
  4  (WARNING)  -> WARNING
  8  (ERROR)    -> ERROR
  12 (SEVERE)   -> CRITICAL
  16 (FATAL)    -> CRITICAL

Provides separate loggers for: batch, api, audit, error, db.
"""

from __future__ import annotations

import json
import logging
import logging.handlers
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from .constants import Severity

# ============================================================
# Logger Names — dedicated loggers for each subsystem
# ============================================================

LOGGER_BATCH = "portfolio.batch"
LOGGER_API = "portfolio.api"
LOGGER_AUDIT = "portfolio.audit"
LOGGER_ERROR = "portfolio.error"
LOGGER_DB = "portfolio.db"

_ALL_LOGGERS = (LOGGER_BATCH, LOGGER_API, LOGGER_AUDIT, LOGGER_ERROR, LOGGER_DB)

# ============================================================
# Default Configuration
# ============================================================

DEFAULT_LOG_DIR = Path("logs")
DEFAULT_MAX_BYTES = 10 * 1024 * 1024  # 10 MB
DEFAULT_BACKUP_COUNT = 5
DEFAULT_FORMAT = (
    "%(asctime)s | %(name)-20s | %(levelname)-8s | %(message)s"
)
DEFAULT_DATE_FORMAT = "%Y-%m-%d %H:%M:%S"

# ============================================================
# Severity -> Python log level mapping
# ============================================================

_SEVERITY_TO_LOG_LEVEL: dict[Severity, int] = {
    Severity.SUCCESS: logging.INFO,
    Severity.WARNING: logging.WARNING,
    Severity.ERROR: logging.ERROR,
    Severity.SEVERE: logging.CRITICAL,
    Severity.FATAL: logging.CRITICAL,
}


def severity_to_log_level(severity: Severity | int) -> int:
    """Convert a COBOL-style severity / return code to a Python log level.

    Args:
        severity: A ``Severity`` enum member or an integer return code
            (0, 4, 8, 12, 16).

    Returns:
        The corresponding Python ``logging`` level constant.
    """
    if isinstance(severity, int) and not isinstance(severity, Severity):
        try:
            severity = Severity(severity)
        except ValueError:
            # For non-standard codes, map by range
            if severity <= 0:
                return logging.INFO
            if severity <= 4:
                return logging.WARNING
            if severity <= 8:
                return logging.ERROR
            return logging.CRITICAL
    return _SEVERITY_TO_LOG_LEVEL.get(severity, logging.ERROR)


# ============================================================
# JSON Formatter — structured logging for production
# ============================================================

class JsonFormatter(logging.Formatter):
    """Emit log records as single-line JSON objects for production use.

    Replaces the flat COBOL error-log record format with a machine-readable
    JSON structure while preserving all fields captured by ERRPROC.cbl:
    timestamp, program, category, code, severity, text, and details.
    """

    def format(self, record: logging.LogRecord) -> str:
        log_entry: dict[str, Any] = {
            "timestamp": datetime.fromtimestamp(
                record.created, tz=timezone.utc
            ).isoformat(),
            "logger": record.name,
            "level": record.levelname,
            "message": record.getMessage(),
        }

        # Preserve extra fields that mirror COBOL error record structure
        for attr in (
            "error_code",
            "program",
            "category",
            "severity_code",
            "trace_id",
            "source_program",
            "details",
            "sqlcode",
            "user_id",
            "batch_id",
        ):
            value = getattr(record, attr, None)
            if value is not None:
                log_entry[attr] = value

        if record.exc_info and record.exc_info[1] is not None:
            log_entry["exception"] = self.formatException(record.exc_info)

        return json.dumps(log_entry, default=str)


# ============================================================
# Human-Readable Formatter — for development
# ============================================================

class HumanReadableFormatter(logging.Formatter):
    """Detailed, human-readable format mirroring COBOL DISPLAY output.

    Matches the format produced by ERRPROC.cbl paragraph 2200-DISPLAY-ERROR:
        ====================================================
        ERROR DETECTED: <timestamp>
        PROGRAM:       <program>
        ...
    """

    SEPARATOR = "=" * 60

    def format(self, record: logging.LogRecord) -> str:
        base = super().format(record)

        # If the record carries COBOL-style structured fields, add them
        program = getattr(record, "source_program", None)
        error_code = getattr(record, "error_code", None)
        details = getattr(record, "details", None)

        if program or error_code:
            parts = [self.SEPARATOR, base]
            if program:
                parts.append(f"  PROGRAM:  {program}")
            if error_code:
                parts.append(f"  CODE:     {error_code}")
            if details:
                parts.append(f"  DETAILS:  {details}")
            parts.append(self.SEPARATOR)
            return "\n".join(parts)

        return base


# ============================================================
# Public API
# ============================================================

def setup_logging(
    level: int | str = logging.INFO,
    log_file: str | Path | None = None,
    fmt: str | None = None,
    json_format: bool = False,
    log_dir: str | Path | None = None,
    max_bytes: int = DEFAULT_MAX_BYTES,
    backup_count: int = DEFAULT_BACKUP_COUNT,
) -> None:
    """Configure the root logger and all subsystem loggers.

    This is the main entry point that replaces the COBOL logging
    infrastructure (ERRPROC.cbl file-based logging, ERRHNDL.cbl DB2 logging,
    and DISPLAY statements).

    Args:
        level: Logging level (e.g. ``logging.INFO`` or ``"DEBUG"``).
        log_file: Optional path to a single log file.  If provided,
            a rotating file handler is created for this file.
        fmt: Optional format string for the human-readable formatter.
            Ignored when *json_format* is ``True``.
        json_format: If ``True``, use :class:`JsonFormatter` for all
            handlers (production mode).  Otherwise use
            :class:`HumanReadableFormatter` (development mode).
        log_dir: Directory for per-logger rotating log files.
            When set, each subsystem logger (batch, api, audit, error, db)
            gets its own rotating log file under this directory.
        max_bytes: Maximum bytes per log file before rotation.
        backup_count: Number of rotated backup files to keep.
    """
    root = logging.getLogger()
    root.setLevel(level)

    # Remove existing handlers to allow re-configuration
    root.handlers.clear()

    # Choose formatter
    if json_format:
        formatter: logging.Formatter = JsonFormatter()
    else:
        formatter = HumanReadableFormatter(
            fmt=fmt or DEFAULT_FORMAT,
            datefmt=DEFAULT_DATE_FORMAT,
        )

    # Console handler (always present)
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setLevel(level)
    console_handler.setFormatter(formatter)
    root.addHandler(console_handler)

    # Single-file handler
    if log_file is not None:
        log_path = Path(log_file)
        log_path.parent.mkdir(parents=True, exist_ok=True)
        file_handler = logging.handlers.RotatingFileHandler(
            filename=str(log_path),
            maxBytes=max_bytes,
            backupCount=backup_count,
            encoding="utf-8",
        )
        file_handler.setLevel(level)
        file_handler.setFormatter(formatter)
        root.addHandler(file_handler)

    # Per-logger rotating files
    if log_dir is not None:
        _setup_per_logger_files(
            log_dir=Path(log_dir),
            formatter=formatter,
            level=level if isinstance(level, int) else logging.getLevelName(level),
            max_bytes=max_bytes,
            backup_count=backup_count,
        )


def get_logger(name: str) -> logging.Logger:
    """Return a named logger within the portfolio hierarchy.

    Convenience wrapper so callers do not need to know the logger
    naming convention.

    Args:
        name: One of ``"batch"``, ``"api"``, ``"audit"``, ``"error"``,
            ``"db"``, or any dotted name.

    Returns:
        A :class:`logging.Logger` instance.
    """
    short_map = {
        "batch": LOGGER_BATCH,
        "api": LOGGER_API,
        "audit": LOGGER_AUDIT,
        "error": LOGGER_ERROR,
        "db": LOGGER_DB,
    }
    return logging.getLogger(short_map.get(name, name))


# ============================================================
# Internal helpers
# ============================================================

def _setup_per_logger_files(
    log_dir: Path,
    formatter: logging.Formatter,
    level: int,
    max_bytes: int,
    backup_count: int,
) -> None:
    """Create individual rotating file handlers for each subsystem logger."""
    log_dir.mkdir(parents=True, exist_ok=True)

    for logger_name in _ALL_LOGGERS:
        logger = logging.getLogger(logger_name)
        # Clear existing handlers to avoid duplicates on re-configuration
        logger.handlers.clear()
        # Derive filename from the last segment (e.g. "batch", "api")
        short_name = logger_name.rsplit(".", maxsplit=1)[-1]
        file_handler = logging.handlers.RotatingFileHandler(
            filename=str(log_dir / f"{short_name}.log"),
            maxBytes=max_bytes,
            backupCount=backup_count,
            encoding="utf-8",
        )
        file_handler.setLevel(level)
        file_handler.setFormatter(formatter)
        logger.addHandler(file_handler)

"""Structured logging configuration for the CLBS Python framework.

Maps COBOL severity levels to Python logging:
  SUCCESS (0)  -> DEBUG
  WARNING (4)  -> WARNING
  ERROR (8)    -> ERROR
  SEVERE (12)  -> CRITICAL
  TERMINAL (16)-> CRITICAL

Provides JSON-formatted output matching the ERRLOG/AUDITLOG
structured data patterns from ERRHNDL and SECMGR.
"""

import logging
import sys
from typing import Optional

from pythonjsonlogger import jsonlogger


# Custom fields matching COBOL error record structure
CLBS_LOG_FIELDS = [
    "timestamp",
    "trace_id",
    "program",
    "paragraph",
    "category",
    "error_code",
    "severity",
    "sqlcode",
    "user_id",
    "terminal_id",
    "event_type",
]


class CLBSJsonFormatter(jsonlogger.JsonFormatter):
    """Custom JSON formatter including CLBS-specific fields.

    Extends python-json-logger to include fields matching the
    COBOL ERRLOG and AUDITLOG table structures.
    """

    def add_fields(
        self,
        log_record: dict,
        record: logging.LogRecord,
        message_dict: dict,
    ) -> None:
        super().add_fields(log_record, record, message_dict)

        # Ensure standard fields are present
        if "timestamp" not in log_record:
            log_record["timestamp"] = self.formatTime(record)

        if "level" not in log_record:
            log_record["level"] = record.levelname

        if "logger" not in log_record:
            log_record["logger"] = record.name

        # Include CLBS-specific fields from the extra dict
        for field_name in CLBS_LOG_FIELDS:
            if hasattr(record, field_name) and field_name not in log_record:
                log_record[field_name] = getattr(record, field_name)


def configure_logging(
    level: int = logging.INFO,
    json_output: bool = True,
    log_file: Optional[str] = None,
    logger_name: str = "clbs",
) -> logging.Logger:
    """Configure structured logging for the CLBS framework.

    Sets up Python logging with:
    - JSON-formatted output (matching ERRLOG structured data)
    - Console handler (stdout)
    - Optional file handler
    - CLBS-specific log fields

    Args:
        level: Minimum log level (default INFO).
        json_output: Use JSON formatting (default True).
        log_file: Optional file path for log output.
        logger_name: Root logger name (default 'clbs').

    Returns:
        Configured logger instance.
    """
    root_logger = logging.getLogger(logger_name)
    root_logger.setLevel(level)

    # Remove existing handlers to avoid duplicates
    root_logger.handlers.clear()

    if json_output:
        formatter = CLBSJsonFormatter(
            fmt="%(timestamp)s %(level)s %(name)s %(message)s",
            rename_fields={
                "levelname": "level",
                "name": "logger",
            },
        )
    else:
        formatter = logging.Formatter(
            fmt="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
            datefmt="%Y-%m-%dT%H:%M:%S%z",
        )

    # Console handler
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setFormatter(formatter)
    root_logger.addHandler(console_handler)

    # File handler (optional)
    if log_file:
        file_handler = logging.FileHandler(log_file)
        file_handler.setFormatter(formatter)
        root_logger.addHandler(file_handler)

    return root_logger

"""
Logging configuration replacing COBOL DISPLAY statements and sysout/syslog writes.

Provides structured logging with configurable output (console, file).
"""

import logging
import os
import sys


def configure_logging(
    level: str | None = None,
    log_file: str | None = None,
) -> None:
    """
    Configure application logging.

    Translates COBOL DISPLAY statements to structured Python logging.

    Args:
        level: Logging level string (DEBUG, INFO, WARNING, ERROR, CRITICAL).
        log_file: Optional file path for log output.
    """
    log_level = getattr(logging, (level or os.environ.get("LOG_LEVEL", "INFO")).upper(), logging.INFO)

    formatter = logging.Formatter(
        fmt="%(asctime)s [%(levelname)-8s] %(name)-20s: %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    )

    root_logger = logging.getLogger()
    root_logger.setLevel(log_level)

    # Remove existing handlers to avoid duplicates on re-configuration
    for handler in root_logger.handlers[:]:
        root_logger.removeHandler(handler)

    # Console handler (replaces COBOL DISPLAY UPON CONSOLE)
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setFormatter(formatter)
    console_handler.setLevel(log_level)
    root_logger.addHandler(console_handler)

    # File handler (replaces COBOL sysout DD writes)
    if log_file:
        file_handler = logging.FileHandler(log_file)
        file_handler.setFormatter(formatter)
        file_handler.setLevel(log_level)
        root_logger.addHandler(file_handler)

    # Suppress noisy third-party loggers
    logging.getLogger("sqlalchemy.engine").setLevel(logging.WARNING)
    logging.getLogger("uvicorn.access").setLevel(logging.WARNING)

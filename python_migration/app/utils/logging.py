"""Logging configuration using structlog.

This module provides structured logging similar to the COBOL
error logging and audit trail functionality.
"""

import logging
import sys
from typing import Any

import structlog


def setup_logging(level: str = "INFO", json_format: bool = False) -> None:
    """Configure structured logging.

    Args:
        level: Log level (DEBUG, INFO, WARNING, ERROR)
        json_format: If True, output logs in JSON format
    """
    logging.basicConfig(
        format="%(message)s",
        stream=sys.stdout,
        level=getattr(logging, level.upper()),
    )

    processors = [
        structlog.contextvars.merge_contextvars,
        structlog.processors.add_log_level,
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.stdlib.PositionalArgumentsFormatter(),
        structlog.processors.StackInfoRenderer(),
        structlog.processors.format_exc_info,
        structlog.processors.UnicodeDecoder(),
    ]

    if json_format:
        processors.append(structlog.processors.JSONRenderer())
    else:
        processors.append(structlog.dev.ConsoleRenderer())

    structlog.configure(
        processors=processors,
        wrapper_class=structlog.stdlib.BoundLogger,
        context_class=dict,
        logger_factory=structlog.stdlib.LoggerFactory(),
        cache_logger_on_first_use=True,
    )


def get_logger(name: str) -> structlog.stdlib.BoundLogger:
    """Get a logger instance.

    Args:
        name: Logger name (typically __name__)

    Returns:
        Configured logger instance
    """
    return structlog.get_logger(name)


def log_batch_start(
    logger: structlog.stdlib.BoundLogger,
    program: str,
    job_name: str,
    process_date: str,
) -> None:
    """Log batch job start - similar to COBOL batch logging."""
    logger.info(
        "Batch job started",
        program=program,
        job_name=job_name,
        process_date=process_date,
    )


def log_batch_end(
    logger: structlog.stdlib.BoundLogger,
    program: str,
    job_name: str,
    return_code: int,
    records_read: int = 0,
    records_written: int = 0,
    errors: int = 0,
) -> None:
    """Log batch job end - similar to COBOL batch logging."""
    logger.info(
        "Batch job completed",
        program=program,
        job_name=job_name,
        return_code=return_code,
        records_read=records_read,
        records_written=records_written,
        errors=errors,
    )


def log_checkpoint(
    logger: structlog.stdlib.BoundLogger,
    program: str,
    checkpoint_id: str,
    records_processed: int,
) -> None:
    """Log checkpoint - similar to COBOL checkpoint logging."""
    logger.info(
        "Checkpoint saved",
        program=program,
        checkpoint_id=checkpoint_id,
        records_processed=records_processed,
    )


def log_transaction(
    logger: structlog.stdlib.BoundLogger,
    program: str,
    transaction_id: str,
    action: str,
    status: str,
    details: dict[str, Any] | None = None,
) -> None:
    """Log transaction - similar to COBOL transaction logging."""
    logger.info(
        "Transaction processed",
        program=program,
        transaction_id=transaction_id,
        action=action,
        status=status,
        **(details or {}),
    )


setup_logging()

"""
Logging framework for the Portfolio Management System.
Replaces COBOL error logging and provides structured logging.

This module implements centralized logging that replaces:
- ERRPROC (Batch Error Processor)
- ERRHNDL (Online Error Handler)
- DB2ERR (DB2 Error Handler)
"""

import logging
import sys
from datetime import datetime
from typing import Any

import structlog
from structlog.types import Processor

from app.config import get_settings


def add_timestamp(
    logger: logging.Logger,
    method_name: str,
    event_dict: dict[str, Any],
) -> dict[str, Any]:
    """Add timestamp to log events."""
    event_dict["timestamp"] = datetime.utcnow().isoformat()
    return event_dict


def add_program_info(
    logger: logging.Logger,
    method_name: str,
    event_dict: dict[str, Any],
) -> dict[str, Any]:
    """Add program information to log events (COBOL-style)."""
    if "program" not in event_dict:
        event_dict["program"] = "PORTMGMT"
    return event_dict


def setup_logging() -> None:
    """
    Set up structured logging for the application.
    Configures both structlog and standard logging.
    """
    settings = get_settings()

    shared_processors: list[Processor] = [
        structlog.contextvars.merge_contextvars,
        structlog.stdlib.add_log_level,
        structlog.stdlib.add_logger_name,
        add_timestamp,
        add_program_info,
        structlog.stdlib.PositionalArgumentsFormatter(),
        structlog.processors.StackInfoRenderer(),
        structlog.processors.UnicodeDecoder(),
    ]

    if settings.log_format == "json":
        shared_processors.append(structlog.processors.JSONRenderer())
    else:
        shared_processors.append(structlog.dev.ConsoleRenderer())

    structlog.configure(
        processors=shared_processors,
        wrapper_class=structlog.stdlib.BoundLogger,
        context_class=dict,
        logger_factory=structlog.stdlib.LoggerFactory(),
        cache_logger_on_first_use=True,
    )

    log_level = getattr(logging, settings.log_level.upper(), logging.INFO)

    logging.basicConfig(
        format="%(message)s",
        stream=sys.stdout,
        level=log_level,
    )


def get_logger(name: str | None = None) -> structlog.stdlib.BoundLogger:
    """
    Get a logger instance.

    Args:
        name: Logger name (typically module name)

    Returns:
        Configured structlog logger
    """
    return structlog.get_logger(name)


class ErrorLogger:
    """
    Error logger that mimics COBOL ERRHNDL functionality.
    Provides centralized error logging with COBOL-style error codes.

    Replaces:
    - P200-LOG-ERROR in ERRHNDL.cbl
    - DB2-ERROR-ROUTINE in DBPROC.cpy
    - 9000-ERROR-ROUTINE in batch programs
    """

    def __init__(self, program: str = "PORTMGMT"):
        self.logger = get_logger(program)
        self.program = program

    def log_error(
        self,
        message: str,
        error_code: str,
        severity: int,
        category: str = "PR",
        paragraph: str | None = None,
        sqlcode: int | None = None,
        trace_id: str | None = None,
        **kwargs: Any,
    ) -> None:
        """
        Log an error with COBOL-style error information.

        Replaces P200-LOG-ERROR in ERRHNDL.cbl:
        - LOG-TIMESTAMP -> timestamp (auto-added)
        - LOG-PROGRAM -> program
        - LOG-PARAGRAPH -> paragraph
        - LOG-SQLCODE -> sqlcode
        - LOG-SEVERITY -> severity
        - LOG-MESSAGE -> message
        - LOG-TRACE-ID -> trace_id
        """
        log_data = {
            "error_code": error_code,
            "severity": severity,
            "category": category,
            "program": self.program,
            **kwargs,
        }

        if paragraph:
            log_data["paragraph"] = paragraph

        if sqlcode is not None:
            log_data["sqlcode"] = sqlcode

        if trace_id:
            log_data["trace_id"] = trace_id

        if severity >= 12:
            self.logger.error(message, **log_data)
        elif severity >= 8:
            self.logger.warning(message, **log_data)
        else:
            self.logger.info(message, **log_data)

    def log_vsam_error(
        self,
        message: str,
        file_status: str,
        file_name: str | None = None,
        **kwargs: Any,
    ) -> None:
        """
        Log a VSAM file error.

        VSAM Status Codes (from ERRHAND.cpy):
        - 00: Success
        - 22: Duplicate key
        - 23: Record not found
        - 10: End of file
        """
        severity = 8
        if file_status in ("22", "23"):
            severity = 4
        elif file_status == "10":
            severity = 0

        self.log_error(
            message=message,
            error_code=f"VS{file_status}",
            severity=severity,
            category="VS",
            file_status=file_status,
            file_name=file_name,
            **kwargs,
        )

    def log_db_error(
        self,
        message: str,
        sqlcode: int,
        sqlstate: str | None = None,
        **kwargs: Any,
    ) -> None:
        """
        Log a database error.

        Replaces DB2-ERROR-ROUTINE in DBPROC.cpy.
        """
        severity = 12 if sqlcode < 0 else 4

        self.log_error(
            message=message,
            error_code="E005",
            severity=severity,
            category="SY",
            sqlcode=sqlcode,
            sqlstate=sqlstate,
            **kwargs,
        )

    def log_validation_error(
        self,
        message: str,
        field: str | None = None,
        value: Any | None = None,
        **kwargs: Any,
    ) -> None:
        """Log a validation error."""
        self.log_error(
            message=message,
            error_code="E008",
            severity=8,
            category="VL",
            field=field,
            value=str(value) if value is not None else None,
            **kwargs,
        )

    def log_security_error(
        self,
        message: str,
        user_id: str | None = None,
        resource: str | None = None,
        **kwargs: Any,
    ) -> None:
        """Log a security error."""
        self.log_error(
            message=message,
            error_code="E006",
            severity=8,
            category="SY",
            user_id=user_id,
            resource=resource,
            **kwargs,
        )

    def log_batch_start(
        self,
        job_name: str,
        step_name: str | None = None,
        **kwargs: Any,
    ) -> None:
        """Log batch job start."""
        self.logger.info(
            "Batch job started",
            job_name=job_name,
            step_name=step_name,
            program=self.program,
            **kwargs,
        )

    def log_batch_end(
        self,
        job_name: str,
        return_code: int,
        records_read: int = 0,
        records_written: int = 0,
        records_error: int = 0,
        **kwargs: Any,
    ) -> None:
        """Log batch job completion."""
        log_method = self.logger.info if return_code <= 4 else self.logger.warning
        log_method(
            "Batch job completed",
            job_name=job_name,
            return_code=return_code,
            records_read=records_read,
            records_written=records_written,
            records_error=records_error,
            program=self.program,
            **kwargs,
        )

    def log_checkpoint(
        self,
        checkpoint_key: str,
        records_processed: int,
        phase: str,
        **kwargs: Any,
    ) -> None:
        """Log checkpoint information."""
        self.logger.info(
            "Checkpoint taken",
            checkpoint_key=checkpoint_key,
            records_processed=records_processed,
            phase=phase,
            program=self.program,
            **kwargs,
        )

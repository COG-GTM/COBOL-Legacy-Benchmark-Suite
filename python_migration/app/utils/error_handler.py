"""Error handling utilities - replaces ERRPROC and ERRHNDL programs.

This module provides centralized error handling similar to the COBOL
error processing programs.
"""

from datetime import datetime
from typing import Optional

from sqlalchemy.orm import Session

from app.database.models import ErrorLog
from app.models.error import ErrorCategory, ErrorMessage, ReturnCode
from app.utils.logging import get_logger

logger = get_logger(__name__)


class ApplicationError(Exception):
    """Application-level error with structured information.

    This exception class provides error handling similar to the
    COBOL ERRPROC program.
    """

    def __init__(
        self,
        message: str,
        code: str = "0000",
        category: ErrorCategory = ErrorCategory.PROCESSING,
        severity: ReturnCode = ReturnCode.ERROR,
        program: str = "",
        details: str = "",
    ):
        self.message = message
        self.code = code
        self.category = category
        self.severity = severity
        self.program = program
        self.details = details
        super().__init__(self.message)

    def to_error_message(self) -> ErrorMessage:
        """Convert to ErrorMessage model."""
        return ErrorMessage(
            program=self.program,
            category=self.category,
            code=self.code,
            severity=self.severity,
            text=self.message,
            details=self.details,
        )

    def to_dict(self) -> dict:
        """Convert to dictionary for API responses."""
        return {
            "error": True,
            "code": self.code,
            "category": self.category.value,
            "severity": self.severity.value,
            "message": self.message,
            "details": self.details,
            "program": self.program,
        }


class ErrorHandler:
    """Centralized error handler - replaces ERRHNDL program.

    This class provides error logging and handling similar to the
    COBOL ERRHNDL program that manages error processing for online
    and batch programs.
    """

    def __init__(self, db: Optional[Session] = None, program: str = ""):
        self.db = db
        self.program = program
        self.error_count = 0
        self.warning_count = 0

    def handle_error(
        self,
        error: Exception,
        code: str = "0000",
        category: ErrorCategory = ErrorCategory.PROCESSING,
        severity: ReturnCode = ReturnCode.ERROR,
        details: str = "",
        user_id: str = "SYSTEM",
    ) -> ErrorMessage:
        """Handle an error - log it and create error message.

        Args:
            error: The exception that occurred
            code: Error code
            category: Error category
            severity: Error severity
            details: Additional details
            user_id: User ID for audit

        Returns:
            ErrorMessage with error details
        """
        error_msg = ErrorMessage(
            program=self.program,
            category=category,
            code=code,
            severity=severity,
            text=str(error)[:80],
            details=details or str(error),
        )

        if severity >= ReturnCode.ERROR:
            self.error_count += 1
            logger.error(
                "Error occurred",
                program=self.program,
                code=code,
                category=category.value,
                message=str(error),
                details=details,
            )
        else:
            self.warning_count += 1
            logger.warning(
                "Warning occurred",
                program=self.program,
                code=code,
                category=category.value,
                message=str(error),
            )

        if self.db:
            self._log_to_database(error_msg, user_id)

        return error_msg

    def handle_application_error(
        self,
        error: ApplicationError,
        user_id: str = "SYSTEM",
    ) -> ErrorMessage:
        """Handle an ApplicationError.

        Args:
            error: The ApplicationError
            user_id: User ID for audit

        Returns:
            ErrorMessage with error details
        """
        return self.handle_error(
            error,
            code=error.code,
            category=error.category,
            severity=error.severity,
            details=error.details,
            user_id=user_id,
        )

    def _log_to_database(self, error_msg: ErrorMessage, user_id: str) -> None:
        """Log error to database - similar to COBOL ERRLOG insert."""
        try:
            now = datetime.now()
            error_log = ErrorLog(
                error_timestamp=now,
                program_id=error_msg.program,
                error_type=error_msg.category.value[0],
                error_severity=error_msg.severity.value,
                error_code=error_msg.code,
                error_message=error_msg.text,
                process_date=now.date(),
                process_time=now.time(),
                user_id=user_id,
                additional_info=error_msg.details,
            )
            self.db.add(error_log)
            self.db.commit()
        except Exception as e:
            logger.error(f"Failed to log error to database: {e}")

    def create_validation_error(
        self,
        field: str,
        message: str,
        value: str = "",
    ) -> ApplicationError:
        """Create a validation error.

        Args:
            field: Field that failed validation
            message: Validation error message
            value: Invalid value

        Returns:
            ApplicationError for the validation failure
        """
        return ApplicationError(
            message=f"Validation failed for {field}: {message}",
            code="VL01",
            category=ErrorCategory.VALIDATION,
            severity=ReturnCode.ERROR,
            program=self.program,
            details=f"Field: {field}, Value: {value}",
        )

    def create_not_found_error(
        self,
        entity: str,
        key: str,
    ) -> ApplicationError:
        """Create a not found error.

        Args:
            entity: Entity type that was not found
            key: Key that was searched

        Returns:
            ApplicationError for the not found condition
        """
        return ApplicationError(
            message=f"{entity} not found",
            code="NF01",
            category=ErrorCategory.PROCESSING,
            severity=ReturnCode.ERROR,
            program=self.program,
            details=f"Key: {key}",
        )

    def create_database_error(
        self,
        operation: str,
        error: Exception,
    ) -> ApplicationError:
        """Create a database error.

        Args:
            operation: Database operation that failed
            error: The database exception

        Returns:
            ApplicationError for the database failure
        """
        return ApplicationError(
            message=f"Database error during {operation}",
            code="DB01",
            category=ErrorCategory.SYSTEM,
            severity=ReturnCode.SEVERE,
            program=self.program,
            details=str(error),
        )

    def get_stats(self) -> dict:
        """Get error statistics."""
        return {
            "error_count": self.error_count,
            "warning_count": self.warning_count,
            "program": self.program,
        }

    def reset_stats(self) -> None:
        """Reset error statistics."""
        self.error_count = 0
        self.warning_count = 0

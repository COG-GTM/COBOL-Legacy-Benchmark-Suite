"""
Exception hierarchy for the Portfolio Management System.
Migrated from COBOL error handling patterns in ERRHAND.cpy and RETHND.cpy.

Error Severity Mapping (from COBOL):
- 0: SUCCESS - Operation successful
- 4: WARNING - Non-fatal warning
- 8: ERROR - Recoverable error
- 12: SEVERE - Severe error
- 16: CRITICAL - Terminal error

Error Categories (from ERRHAND.cpy):
- VS: VSAM errors
- VL: Validation errors
- PR: Processing errors
- SY: System errors

Error Types (from RETHND.cpy):
- V: Validation
- P: Processing
- D: Database
- F: File
- S: Security
"""

from enum import IntEnum


class ErrorSeverity(IntEnum):
    """Error severity levels matching COBOL return codes."""
    SUCCESS = 0
    WARNING = 4
    ERROR = 8
    SEVERE = 12
    CRITICAL = 16


class ErrorCategory:
    """Error categories matching COBOL ERRHAND.cpy."""
    VSAM = "VS"
    VALIDATION = "VL"
    PROCESSING = "PR"
    SYSTEM = "SY"


class ErrorCode:
    """
    Standard error codes matching COBOL STD-ERROR-CODES in RETHND.cpy.
    """
    INVALID_DATA = "E001"
    NOT_FOUND = "E002"
    DUPLICATE = "E003"
    FILE_ERROR = "E004"
    DB_ERROR = "E005"
    SECURITY = "E006"
    PROCESSING = "E007"
    VALIDATION = "E008"
    VERSION = "E009"
    TIMEOUT = "E010"


class PortfolioError(Exception):
    """
    Base exception for all portfolio system errors.
    Replaces COBOL error handling structure from ERRHAND.cpy.
    """

    def __init__(
        self,
        message: str,
        code: str = ErrorCode.PROCESSING,
        severity: ErrorSeverity = ErrorSeverity.ERROR,
        category: str = ErrorCategory.PROCESSING,
        details: str | None = None,
        program: str | None = None,
    ):
        super().__init__(message)
        self.message = message
        self.code = code
        self.severity = severity
        self.category = category
        self.details = details
        self.program = program

    def to_dict(self) -> dict:
        """Convert exception to dictionary for API responses."""
        return {
            "error": self.__class__.__name__,
            "message": self.message,
            "code": self.code,
            "severity": self.severity.value,
            "category": self.category,
            "details": self.details,
            "program": self.program,
        }

    @property
    def return_code(self) -> int:
        """Get COBOL-style return code."""
        return self.severity.value


class ValidationError(PortfolioError):
    """
    Validation error - matches ERR-VALIDATION in RETHND.cpy.
    Used for input validation failures.
    """

    def __init__(
        self,
        message: str,
        details: str | None = None,
        program: str | None = None,
    ):
        super().__init__(
            message=message,
            code=ErrorCode.VALIDATION,
            severity=ErrorSeverity.ERROR,
            category=ErrorCategory.VALIDATION,
            details=details,
            program=program,
        )


class PortfolioNotFoundError(PortfolioError):
    """
    Portfolio not found error - matches ERR-NOT-FOUND in RETHND.cpy.
    Equivalent to VSAM status '23' (record not found).
    """

    def __init__(
        self,
        message: str,
        details: str | None = None,
        program: str | None = None,
    ):
        super().__init__(
            message=message,
            code=ErrorCode.NOT_FOUND,
            severity=ErrorSeverity.WARNING,
            category=ErrorCategory.VSAM,
            details=details,
            program=program,
        )


class PositionNotFoundError(PortfolioError):
    """
    Position not found error.
    Equivalent to P900-NOT-FOUND in INQPORT.cbl.
    """

    def __init__(
        self,
        message: str,
        details: str | None = None,
        program: str | None = None,
    ):
        super().__init__(
            message=message,
            code=ErrorCode.NOT_FOUND,
            severity=ErrorSeverity.WARNING,
            category=ErrorCategory.VSAM,
            details=details,
            program=program or "INQPORT",
        )


class TransactionNotFoundError(PortfolioError):
    """
    Transaction not found error.
    """

    def __init__(
        self,
        message: str,
        details: str | None = None,
        program: str | None = None,
    ):
        super().__init__(
            message=message,
            code=ErrorCode.NOT_FOUND,
            severity=ErrorSeverity.WARNING,
            category=ErrorCategory.VSAM,
            details=details,
            program=program or "INQHIST",
        )


class DuplicateRecordError(PortfolioError):
    """
    Duplicate record error - matches ERR-DUPLICATE in RETHND.cpy.
    Equivalent to VSAM status '22' (duplicate key).
    """

    def __init__(
        self,
        message: str,
        details: str | None = None,
        program: str | None = None,
    ):
        super().__init__(
            message=message,
            code=ErrorCode.DUPLICATE,
            severity=ErrorSeverity.ERROR,
            category=ErrorCategory.VSAM,
            details=details,
            program=program,
        )


class DatabaseError(PortfolioError):
    """
    Database error - matches ERR-DB-ERROR in RETHND.cpy.
    Replaces DB2 SQL errors.
    """

    def __init__(
        self,
        message: str,
        sqlcode: int | None = None,
        details: str | None = None,
        program: str | None = None,
    ):
        super().__init__(
            message=message,
            code=ErrorCode.DB_ERROR,
            severity=ErrorSeverity.SEVERE,
            category=ErrorCategory.SYSTEM,
            details=details or f"SQLCODE: {sqlcode}" if sqlcode else None,
            program=program,
        )
        self.sqlcode = sqlcode


class AuthenticationError(PortfolioError):
    """
    Authentication error - matches ERR-SECURITY in RETHND.cpy.
    Replaces CICS security validation failures.
    """

    def __init__(
        self,
        message: str,
        details: str | None = None,
        program: str | None = None,
    ):
        super().__init__(
            message=message,
            code=ErrorCode.SECURITY,
            severity=ErrorSeverity.ERROR,
            category=ErrorCategory.SYSTEM,
            details=details,
            program=program or "SECMGR",
        )


class AuthorizationError(PortfolioError):
    """
    Authorization error - access denied.
    Replaces SECMGR authorization check failures.
    """

    def __init__(
        self,
        message: str,
        resource: str | None = None,
        access_type: str | None = None,
        details: str | None = None,
        program: str | None = None,
    ):
        super().__init__(
            message=message,
            code=ErrorCode.SECURITY,
            severity=ErrorSeverity.ERROR,
            category=ErrorCategory.SYSTEM,
            details=details or f"Resource: {resource}, Access: {access_type}",
            program=program or "SECMGR",
        )
        self.resource = resource
        self.access_type = access_type


class UserNotFoundError(PortfolioError):
    """User not found error."""

    def __init__(
        self,
        message: str,
        details: str | None = None,
        program: str | None = None,
    ):
        super().__init__(
            message=message,
            code=ErrorCode.NOT_FOUND,
            severity=ErrorSeverity.WARNING,
            category=ErrorCategory.SYSTEM,
            details=details,
            program=program or "SECMGR",
        )


class FileError(PortfolioError):
    """
    File error - matches ERR-FILE-ERROR in RETHND.cpy.
    Replaces VSAM file errors.
    """

    def __init__(
        self,
        message: str,
        file_status: str | None = None,
        details: str | None = None,
        program: str | None = None,
    ):
        super().__init__(
            message=message,
            code=ErrorCode.FILE_ERROR,
            severity=ErrorSeverity.SEVERE,
            category=ErrorCategory.VSAM,
            details=details or f"File Status: {file_status}" if file_status else None,
            program=program,
        )
        self.file_status = file_status


class BatchProcessingError(PortfolioError):
    """
    Batch processing error.
    Used for errors during batch job execution.
    """

    def __init__(
        self,
        message: str,
        job_name: str | None = None,
        step_name: str | None = None,
        details: str | None = None,
        program: str | None = None,
    ):
        super().__init__(
            message=message,
            code=ErrorCode.PROCESSING,
            severity=ErrorSeverity.SEVERE,
            category=ErrorCategory.PROCESSING,
            details=details,
            program=program,
        )
        self.job_name = job_name
        self.step_name = step_name


class CheckpointError(PortfolioError):
    """
    Checkpoint/restart error.
    Used for errors during checkpoint operations.
    """

    def __init__(
        self,
        message: str,
        checkpoint_key: str | None = None,
        details: str | None = None,
        program: str | None = None,
    ):
        super().__init__(
            message=message,
            code=ErrorCode.PROCESSING,
            severity=ErrorSeverity.SEVERE,
            category=ErrorCategory.PROCESSING,
            details=details,
            program=program,
        )
        self.checkpoint_key = checkpoint_key


class TimeoutError(PortfolioError):
    """
    Timeout error - matches ERR-TIMEOUT in RETHND.cpy.
    """

    def __init__(
        self,
        message: str,
        details: str | None = None,
        program: str | None = None,
    ):
        super().__init__(
            message=message,
            code=ErrorCode.TIMEOUT,
            severity=ErrorSeverity.ERROR,
            category=ErrorCategory.SYSTEM,
            details=details,
            program=program,
        )

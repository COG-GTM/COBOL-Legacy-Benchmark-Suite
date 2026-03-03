"""Error handling framework mirroring COBOL ERRHNDL/ERRPROC patterns.

Provides custom exception hierarchy, centralized error handler,
and retry/recovery logic based on DB2RECV patterns.
"""

from python.src.core.error_handling.exceptions import (
    CLBSError,
    ConnectionError as CLBSConnectionError,
    CursorError,
    DataError,
    DuplicateKeyError,
    ProcessingError,
    RecordNotFoundError,
    SecurityError,
    SystemError as CLBSSystemError,
    ValidationError,
)
from python.src.core.error_handling.handler import ErrorHandler
from python.src.core.error_handling.recovery import (
    RetryConfig,
    retry_database_operation,
    retry_external_call,
)

__all__ = [
    "CLBSError",
    "DataError",
    "ValidationError",
    "DuplicateKeyError",
    "RecordNotFoundError",
    "CLBSConnectionError",
    "CursorError",
    "SecurityError",
    "ProcessingError",
    "CLBSSystemError",
    "ErrorHandler",
    "RetryConfig",
    "retry_database_operation",
    "retry_external_call",
]

"""
Common utility modules for the Investment Portfolio Management System.

This package provides shared infrastructure used across all subsystems
of the migrated COBOL application:

- **constants** — Enumerations, return codes, status codes, and field limits
  migrated from RTNCODE.cpy, COMMON.cpy, ERRHAND.cpy, and RETHND.cpy.
- **logging_config** — Structured logging replacing COBOL DISPLAY statements
  and sysout/syslog writes.
- **error_handler** — Exception hierarchy and centralized error processing
  migrated from ERRPROC.cbl, ERRHNDL.cbl, and DB2ERR.cbl.
- **audit** — Audit trail logging migrated from AUDPROC.cbl and AUDITLOG.cpy.
"""

from .constants import (
    AuditAction,
    AuditStatus,
    AuditType,
    CurrencyCode,
    DB2SqlCode,
    ErrorCategory,
    ErrorType,
    OnlineErrorAction,
    OnlineErrorSeverity,
    RecoveryAction,
    ReturnCode,
    ReturnCodeRequestType,
    ReturnCodeStatus,
    Severity,
    StandardErrorCode,
    StatusCode,
    TransactionType,
    VsamStatus,
)
from .error_handler import (
    ApplicationError,
    BatchError,
    DatabaseError,
    ErrorContext,
    ErrorHandler,
    RecoveryError,
    RecoveryResult,
    SecurityError,
    ValidationError,
)
from .audit import AuditLogger, AuditRecord
from .logging_config import get_logger, setup_logging

__all__ = [
    # constants
    "AuditAction",
    "AuditStatus",
    "AuditType",
    "CurrencyCode",
    "DB2SqlCode",
    "ErrorCategory",
    "ErrorType",
    "OnlineErrorAction",
    "OnlineErrorSeverity",
    "RecoveryAction",
    "ReturnCode",
    "ReturnCodeRequestType",
    "ReturnCodeStatus",
    "Severity",
    "StandardErrorCode",
    "StatusCode",
    "TransactionType",
    "VsamStatus",
    # error_handler
    "ApplicationError",
    "BatchError",
    "DatabaseError",
    "ErrorContext",
    "ErrorHandler",
    "RecoveryError",
    "RecoveryResult",
    "SecurityError",
    "ValidationError",
    # audit
    "AuditLogger",
    "AuditRecord",
    # logging
    "get_logger",
    "setup_logging",
]

"""
Common constants and enumerations for the Investment Portfolio Management System.

Migrated from COBOL sources:
  - src/copybook/common/RTNCODE.cpy  (Return code management)
  - src/copybook/common/COMMON.cpy   (Common definitions and constants)
  - src/copybook/common/ERRHAND.cpy  (Error handling definitions)
  - src/copybook/common/RETHND.cpy   (Return handling definitions)
"""

from enum import IntEnum, Enum


# ============================================================
# Return Codes — mapped from COMMON.cpy RETURN-CODES
# and ERRHAND.cpy ERR-RETURN-CODES
# COBOL convention: RETURN-CODE is set to 0/4/8/12/16
# ============================================================

class ReturnCode(IntEnum):
    """Standard return codes mirroring COBOL RETURN-CODE values."""

    SUCCESS = 0
    WARNING = 4
    ERROR = 8
    SEVERE = 12
    FATAL = 16


class Severity(IntEnum):
    """Error severity levels mapped from COBOL return code conventions.

    Maps to ERRHAND.cpy ERR-RETURN-CODES:
      ERR-SUCCESS  = +0
      ERR-WARNING  = +4
      ERR-ERROR    = +8
      ERR-SEVERE   = +12
      ERR-TERMINAL = +16
    """

    SUCCESS = 0
    WARNING = 4
    ERROR = 8
    SEVERE = 12
    FATAL = 16

    @property
    def label(self) -> str:
        """Human-readable label for the severity level."""
        return self.name


# ============================================================
# Error Categories — from ERRHAND.cpy ERR-CATEGORIES
# ============================================================

class ErrorCategory(str, Enum):
    """Error categories from ERRHAND.cpy ERR-CATEGORIES."""

    VSAM = "VS"
    VALIDATION = "VL"
    PROCESSING = "PR"
    SYSTEM = "SY"


# ============================================================
# Error Types — from RETHND.cpy ERROR-TYPE level-88 values
# ============================================================

class ErrorType(str, Enum):
    """Error type classification from RETHND.cpy ERROR-TYPE."""

    VALIDATION = "V"
    PROCESSING = "P"
    DATABASE = "D"
    FILE = "F"
    SECURITY = "S"


# ============================================================
# Standard Error Codes — from RETHND.cpy STD-ERROR-CODES
# ============================================================

class StandardErrorCode(str, Enum):
    """Standard error codes from RETHND.cpy STD-ERROR-CODES."""

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


# ============================================================
# Recovery Actions — from RETHND.cpy RETURN-ACTIONS
# ============================================================

class RecoveryAction(str, Enum):
    """Recovery action flags from RETHND.cpy ACTION-FLAG level-88 values."""

    CONTINUE = "C"
    ABORT = "A"
    RETRY = "R"


# ============================================================
# Return Code Status — from RTNCODE.cpy RC-STATUS level-88
# ============================================================

class ReturnCodeStatus(str, Enum):
    """Return code status from RTNCODE.cpy RC-STATUS."""

    SUCCESS = "S"
    WARNING = "W"
    ERROR = "E"
    SEVERE = "F"


# ============================================================
# Return Code Request Types — from RTNCODE.cpy RC-REQUEST-TYPE
# ============================================================

class ReturnCodeRequestType(str, Enum):
    """Request types for return code management from RTNCODE.cpy."""

    INITIALIZE = "I"
    SET_CODE = "S"
    GET_CODE = "G"
    LOG_CODE = "L"
    ANALYZE = "A"


# ============================================================
# Online Error Severity — from ERRHND.cpy (online) ERR-SEVERITY
# ============================================================

class OnlineErrorSeverity(str, Enum):
    """Online error severity levels from online/ERRHND.cpy."""

    FATAL = "F"
    WARNING = "W"
    INFO = "I"


# ============================================================
# Online Error Actions — from ERRHND.cpy (online) ERR-ACTION
# ============================================================

class OnlineErrorAction(str, Enum):
    """Online error actions from online/ERRHND.cpy."""

    RETURN = "R"
    CONTINUE = "C"
    ABEND = "A"


# ============================================================
# Status Codes — from COMMON.cpy STATUS-CODES
# ============================================================

class StatusCode(str, Enum):
    """Entity status codes from COMMON.cpy STATUS-CODES."""

    ACTIVE = "A"
    CLOSED = "C"
    PENDING = "P"
    SUSPENDED = "S"
    FAILED = "F"
    REVERSED = "R"


# ============================================================
# Transaction Types — from COMMON.cpy TRANSACTION-TYPES
# ============================================================

class TransactionType(str, Enum):
    """Transaction types from COMMON.cpy TRANSACTION-TYPES."""

    BUY = "BU"
    SELL = "SL"
    TRANSFER = "TR"
    FEE = "FE"


# ============================================================
# Audit Types — from AUDITLOG.cpy AUD-TYPE level-88 values
# ============================================================

class AuditType(str, Enum):
    """Audit record types from AUDITLOG.cpy AUD-TYPE."""

    TRANSACTION = "TRAN"
    USER_ACTION = "USER"
    SYSTEM_EVENT = "SYST"


# ============================================================
# Audit Actions — from AUDITLOG.cpy AUD-ACTION level-88 values
# ============================================================

class AuditAction(str, Enum):
    """Audit actions from AUDITLOG.cpy AUD-ACTION."""

    CREATE = "CREATE"
    UPDATE = "UPDATE"
    DELETE = "DELETE"
    READ = "INQUIRE"
    LOGIN = "LOGIN"
    LOGOUT = "LOGOUT"
    BATCH_START = "STARTUP"
    BATCH_END = "SHUTDOWN"
    ERROR = "ERROR"


# ============================================================
# Audit Status — from AUDITLOG.cpy AUD-STATUS level-88 values
# ============================================================

class AuditStatus(str, Enum):
    """Audit status values from AUDITLOG.cpy AUD-STATUS."""

    SUCCESS = "SUCC"
    FAILURE = "FAIL"
    WARNING = "WARN"


# ============================================================
# Currency Codes — from COMMON.cpy CURRENCY-CODES
# ============================================================

class CurrencyCode(str, Enum):
    """Supported currency codes from COMMON.cpy CURRENCY-CODES."""

    USD = "USD"
    EUR = "EUR"
    GBP = "GBP"
    JPY = "JPY"
    CAD = "CAD"


# ============================================================
# VSAM Status Codes — from ERRHAND.cpy ERR-VSAM-STATUSES
# ============================================================

class VsamStatus(str, Enum):
    """VSAM file status codes from ERRHAND.cpy ERR-VSAM-STATUSES."""

    SUCCESS = "00"
    DUPLICATE_KEY = "22"
    NOT_FOUND = "23"
    END_OF_FILE = "10"


VSAM_STATUS_MESSAGES: dict[str, str] = {
    VsamStatus.DUPLICATE_KEY: "Duplicate record key",
    VsamStatus.NOT_FOUND: "Record not found",
}

VSAM_DEFAULT_ERROR_MESSAGE = "Unexpected VSAM error"


# ============================================================
# DB2 SQL Code Categories — from DB2ERR.cbl WS-ERROR-CATEGORIES
# ============================================================

class DB2SqlCode(IntEnum):
    """Well-known DB2 SQL codes from DB2ERR.cbl."""

    DEADLOCK = -911
    TIMEOUT = -913
    CONNECTION_ERROR = -30081
    DUPLICATE_KEY = -803
    NOT_FOUND = 100


# ============================================================
# DB2 Error Handler Functions — from DB2ERR.cbl LS-FUNCTION
# ============================================================

class DB2ErrorFunction(str, Enum):
    """DB2 error handler function codes from DB2ERR.cbl."""

    LOG = "LOG"
    DIAGNOSE = "DIAG"
    RETRIEVE = "RETR"


# ============================================================
# COBOL Figurative Constants — Python equivalents
# ============================================================

SPACES = ""
"""COBOL SPACES: empty string (equivalent to PIC X filled with spaces)."""

ZEROS = 0
"""COBOL ZEROS: numeric zero."""

ZEROS_STR = "0"
"""COBOL ZEROS as string: for alphanumeric zero-fill."""

HIGH_VALUES = "\xff"
"""COBOL HIGH-VALUES: highest collating sequence value."""

LOW_VALUES = "\x00"
"""COBOL LOW-VALUES: lowest collating sequence value."""


# ============================================================
# Field Length Limits — derived from COBOL PIC clauses
# ============================================================

MAX_PROGRAM_ID_LENGTH = 8
"""COBOL PIC X(8) for program identifiers."""

MAX_ERROR_CODE_LENGTH = 4
"""COBOL PIC X(4) for error codes."""

MAX_ERROR_TEXT_LENGTH = 80
"""COBOL PIC X(80) for error message text."""

MAX_ERROR_DETAILS_LENGTH = 256
"""COBOL PIC X(256) for error detail text."""

MAX_USER_ID_LENGTH = 8
"""COBOL PIC X(8) for user identifiers."""

MAX_TERMINAL_ID_LENGTH = 8
"""COBOL PIC X(8) for terminal identifiers."""

MAX_SYSTEM_ID_LENGTH = 8
"""COBOL PIC X(8) for system identifiers."""

MAX_PORTFOLIO_ID_LENGTH = 8
"""COBOL PIC X(8) for portfolio identifiers."""

MAX_ACCOUNT_NO_LENGTH = 10
"""COBOL PIC X(10) for account numbers."""

MAX_AUDIT_MESSAGE_LENGTH = 100
"""COBOL PIC X(100) for audit messages."""

MAX_BEFORE_AFTER_IMAGE_LENGTH = 100
"""COBOL PIC X(100) for before/after images in audit records."""

MAX_PARAGRAPH_NAME_LENGTH = 30
"""COBOL PIC X(30) for paragraph names."""

MAX_TRACE_ID_LENGTH = 16
"""COBOL PIC X(16) for trace identifiers."""

MAX_TIMESTAMP_LENGTH = 26
"""COBOL PIC X(26) for timestamp fields."""

MAX_ADDITIONAL_INFO_LENGTH = 100
"""COBOL PIC X(100) for additional info in error records."""

MAX_SQLSTATE_LENGTH = 5
"""COBOL PIC X(5) for SQLSTATE codes."""

MAX_RETRY_COUNT = 3
"""Maximum retry count from RETHND.cpy MAX-RETRIES."""


# ============================================================
# Date/Time Format Strings
# ============================================================

DATE_FORMAT_ISO = "%Y-%m-%d"
"""ISO 8601 date format (maps to COBOL YYYY-MM-DD)."""

TIME_FORMAT_ISO = "%H:%M:%S"
"""ISO 8601 time format (maps to COBOL HH:MM:SS)."""

DATETIME_FORMAT_ISO = "%Y-%m-%d %H:%M:%S"
"""ISO 8601 datetime format."""

TIMESTAMP_FORMAT = "%Y-%m-%d %H:%M:%S.%f"
"""Full timestamp format with microseconds (maps to COBOL TIMESTAMP)."""


# ============================================================
# Default Values
# ============================================================

DEFAULT_SYSTEM_ID = "PORTMGMT"
"""Default system identifier for the portfolio management system."""

DEFAULT_CURRENCY = CurrencyCode.USD
"""Default currency code."""

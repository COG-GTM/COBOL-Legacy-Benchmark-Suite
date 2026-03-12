"""
Constants and enumerations translated from COBOL copybooks.

Source copybooks:
  - src/copybook/common/RTNCODE.cpy  (Return Code Management)
  - src/copybook/common/COMMON.cpy   (Common Definitions and Constants)
  - src/copybook/common/ERRHAND.cpy  (Standard Error Handling Definitions)
  - src/copybook/common/RETHND.cpy   (Return Code Handling Definitions)
"""

from enum import Enum, IntEnum


# ---------------------------------------------------------------------------
# Return Codes  (from COMMON.cpy and ERRHAND.cpy)
#   COBOL convention: 0=success, 4=warning, 8=error, 12=severe, 16=fatal
# ---------------------------------------------------------------------------

class ReturnCode(IntEnum):
    """Standard return codes mirroring COBOL ERR-RETURN-CODES / RETURN-CODES."""

    SUCCESS = 0
    WARNING = 4
    ERROR = 8
    SEVERE = 12
    CRITICAL = 16


# ---------------------------------------------------------------------------
# Return-code status letters  (from RTNCODE.cpy  RC-STATUS)
# ---------------------------------------------------------------------------

class ReturnCodeStatus(str, Enum):
    """Maps to RTNCODE.cpy RC-STATUS level-88 values."""

    SUCCESS = "S"
    WARNING = "W"
    ERROR = "E"
    SEVERE = "F"


# ---------------------------------------------------------------------------
# Return-code request types  (from RTNCODE.cpy  RC-REQUEST-TYPE)
# ---------------------------------------------------------------------------

class ReturnCodeRequestType(str, Enum):
    """Maps to RTNCODE.cpy RC-REQUEST-TYPE level-88 values."""

    INITIALIZE = "I"
    SET_CODE = "S"
    GET_CODE = "G"
    LOG_CODE = "L"
    ANALYZE = "A"


# ---------------------------------------------------------------------------
# Status codes  (from COMMON.cpy  STATUS-CODES)
# ---------------------------------------------------------------------------

class StatusCode(str, Enum):
    """Generic status codes from COMMON.cpy STATUS-CODES."""

    ACTIVE = "A"
    CLOSED = "C"
    PENDING = "P"
    SUSPENDED = "S"
    FAILED = "F"
    REVERSED = "R"


# ---------------------------------------------------------------------------
# Transaction types  (from COMMON.cpy  TRANSACTION-TYPES)
# ---------------------------------------------------------------------------

class TransactionTypeCode(str, Enum):
    """Transaction types from COMMON.cpy TRANSACTION-TYPES."""

    BUY = "BU"
    SELL = "SL"
    TRANSFER = "TR"
    FEE = "FE"


# ---------------------------------------------------------------------------
# Currency codes  (from COMMON.cpy  CURRENCY-CODES)
# ---------------------------------------------------------------------------

class CurrencyCode(str, Enum):
    """Supported currency codes from COMMON.cpy CURRENCY-CODES."""

    USD = "USD"
    EUR = "EUR"
    GBP = "GBP"
    JPY = "JPY"
    CAD = "CAD"


# ---------------------------------------------------------------------------
# Error categories  (from ERRHAND.cpy  ERR-CATEGORIES)
# ---------------------------------------------------------------------------

class ErrorCategory(str, Enum):
    """Error category codes from ERRHAND.cpy ERR-CATEGORIES."""

    VSAM = "VS"
    VALIDATION = "VL"
    PROCESSING = "PR"
    SYSTEM = "SY"


# ---------------------------------------------------------------------------
# Error types  (from RETHND.cpy  ERROR-TYPE level-88)
# ---------------------------------------------------------------------------

class ErrorType(str, Enum):
    """Error type codes from RETHND.cpy ERROR-TYPE level-88 values."""

    VALIDATION = "V"
    PROCESSING = "P"
    DATABASE = "D"
    FILE = "F"
    SECURITY = "S"


# ---------------------------------------------------------------------------
# Recovery action flags  (from RETHND.cpy  ACTION-FLAG level-88)
# ---------------------------------------------------------------------------

class ActionFlag(str, Enum):
    """Recovery action flags from RETHND.cpy ACTION-FLAG level-88 values."""

    CONTINUE = "C"
    ABORT = "A"
    RETRY = "R"


# ---------------------------------------------------------------------------
# Standard error codes  (from RETHND.cpy  STD-ERROR-CODES)
# ---------------------------------------------------------------------------

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


# ---------------------------------------------------------------------------
# VSAM status codes  (from ERRHAND.cpy  ERR-VSAM-STATUSES)
# ---------------------------------------------------------------------------

class VsamStatus(str, Enum):
    """VSAM file-status codes from ERRHAND.cpy ERR-VSAM-STATUSES."""

    SUCCESS = "00"
    DUPLICATE_KEY = "22"
    NOT_FOUND = "23"
    END_OF_FILE = "10"


VSAM_STATUS_MESSAGES: dict[str, str] = {
    VsamStatus.DUPLICATE_KEY: "Duplicate record key",
    VsamStatus.NOT_FOUND: "Record not found",
}

VSAM_DEFAULT_ERROR_MESSAGE = "Unexpected VSAM error"


# ---------------------------------------------------------------------------
# Portfolio validation  (from PORTVAL.cpy)
# ---------------------------------------------------------------------------

class ValidationReturnCode(IntEnum):
    """Validation-specific return codes from PORTVAL.cpy VAL-RETURN-CODES."""

    SUCCESS = 0
    INVALID_ID = 1
    INVALID_ACCOUNT = 2
    INVALID_TYPE = 3
    INVALID_AMOUNT = 4


VALIDATION_ERROR_MESSAGES: dict[int, str] = {
    ValidationReturnCode.INVALID_ID: "Invalid Portfolio ID format",
    ValidationReturnCode.INVALID_ACCOUNT: "Invalid Account Number format",
    ValidationReturnCode.INVALID_TYPE: "Invalid Investment Type",
    ValidationReturnCode.INVALID_AMOUNT: "Amount outside valid range",
}

# Portfolio ID prefix requirement (PORTVAL.cpy VAL-ID-PREFIX)
PORTFOLIO_ID_PREFIX = "PORT"

# ---------------------------------------------------------------------------
# Retry / control defaults  (from RETHND.cpy)
# ---------------------------------------------------------------------------
DEFAULT_MAX_RETRIES: int = 3

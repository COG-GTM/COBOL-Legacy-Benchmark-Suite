"""Constants and configuration values - converted from COMMON.cpy.

This module contains constants that were defined in the COBOL
COMMON.cpy copybook and other configuration values.
"""

from decimal import Decimal
from enum import Enum, IntEnum


class CurrencyCode(str, Enum):
    """Currency codes - from CURRENCY-CODES in COMMON.cpy."""

    USD = "USD"  # CURR-USD
    EUR = "EUR"  # CURR-EUR
    GBP = "GBP"  # CURR-GBP
    JPY = "JPY"  # CURR-JPY
    CAD = "CAD"  # CURR-CAD


CURRENCY_CODES = {
    "USD": "US Dollar",
    "EUR": "Euro",
    "GBP": "British Pound",
    "JPY": "Japanese Yen",
    "CAD": "Canadian Dollar",
}


class TransactionTypeCode(str, Enum):
    """Transaction type codes - from TRANSACTION-TYPES in COMMON.cpy."""

    BUY = "BU"  # TRN-TYPE-BUY
    SELL = "SL"  # TRN-TYPE-SELL
    TRANSFER = "TR"  # TRN-TYPE-TRANSFER
    FEE = "FE"  # TRN-TYPE-FEE


TRANSACTION_TYPES = {
    "BU": "Buy",
    "SL": "Sell",
    "TR": "Transfer",
    "FE": "Fee",
}


class StatusCode(str, Enum):
    """Status codes - from STATUS-CODES in COMMON.cpy."""

    ACTIVE = "A"  # STATUS-ACTIVE
    CLOSED = "C"  # STATUS-CLOSED
    PENDING = "P"  # STATUS-PENDING
    SUSPENDED = "S"  # STATUS-SUSPENDED
    FAILED = "F"  # STATUS-FAILED
    REVERSED = "R"  # STATUS-REVERSED


STATUS_CODES = {
    "A": "Active",
    "C": "Closed",
    "P": "Pending",
    "S": "Suspended",
    "F": "Failed",
    "R": "Reversed",
}


class ReturnCodeValue(IntEnum):
    """Return codes - from RETURN-CODES in COMMON.cpy."""

    SUCCESS = 0  # RC-SUCCESS
    WARNING = 4  # RC-WARNING
    ERROR = 8  # RC-ERROR
    SEVERE = 12  # RC-SEVERE
    CRITICAL = 16  # RC-CRITICAL


RETURN_CODES = {
    0: "Success",
    4: "Warning",
    8: "Error",
    12: "Severe",
    16: "Critical",
}


class ValidationConstants:
    """Validation constants - from PORTVAL.cpy."""

    MIN_AMOUNT = Decimal("-9999999999999.99")  # VAL-MIN-AMOUNT
    MAX_AMOUNT = Decimal("9999999999999.99")  # VAL-MAX-AMOUNT
    ID_PREFIX = "PORT"  # VAL-ID-PREFIX


class BatchConstants:
    """Batch processing constants."""

    DEFAULT_COMMIT_THRESHOLD = 1000
    MAX_RESTART_COUNT = 3
    MAX_ERROR_COUNT = 100
    CHECKPOINT_INTERVAL = 1000


class SecurityConstants:
    """Security constants."""

    MAX_LOGIN_ATTEMPTS = 3
    LOCKOUT_DURATION_MINUTES = 30
    TOKEN_EXPIRE_MINUTES = 60
    PASSWORD_MIN_LENGTH = 8


class SystemConstants:
    """System-wide constants."""

    SYSTEM_ID = "PORTMGMT"
    DEFAULT_CURRENCY = "USD"
    DATE_FORMAT = "%Y%m%d"
    TIME_FORMAT = "%H%M%S"
    TIMESTAMP_FORMAT = "%Y-%m-%d %H:%M:%S"


RISK_LEVELS = {
    "L": "Low",
    "M": "Medium",
    "H": "High",
}


CLIENT_TYPES = {
    "I": "Individual",
    "C": "Corporate",
    "T": "Trust",
}


ACCOUNT_TYPES = {
    "IN": "Individual",
    "JT": "Joint",
    "TR": "Trust",
    "CO": "Corporate",
    "RT": "Retirement",
}


ERROR_MESSAGES = {
    "VL01": "Invalid Portfolio ID format",
    "VL02": "Invalid Account Number format",
    "VL03": "Invalid Investment Type",
    "VL04": "Amount outside valid range",
    "NF01": "Record not found",
    "DB01": "Database error",
    "AU01": "Authentication failed",
    "AU02": "Authorization denied",
    "SY01": "System error",
}

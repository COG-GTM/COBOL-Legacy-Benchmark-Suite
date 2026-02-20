"""Common definitions and constants - migrated from COMMON.cpy."""

from dataclasses import dataclass
from enum import IntEnum, Enum


class ReturnCode(IntEnum):
    SUCCESS = 0
    WARNING = 4
    ERROR = 8
    SEVERE = 12
    CRITICAL = 16


class StatusCode(str, Enum):
    ACTIVE = "A"
    CLOSED = "C"
    PENDING = "P"
    SUSPENDED = "S"
    FAILED = "F"
    REVERSED = "R"


class TransactionType(str, Enum):
    BUY = "BU"
    SELL = "SL"
    TRANSFER = "TR"
    FEE = "FE"


class CurrencyCode(str, Enum):
    USD = "USD"
    EUR = "EUR"
    GBP = "GBP"
    JPY = "JPY"
    CAD = "CAD"


@dataclass
class CommonDatetime:
    year: str = ""
    month: str = ""
    day: str = ""
    hour: str = ""
    minute: str = ""
    second: str = ""
    millisecond: str = ""

    @property
    def date_string(self) -> str:
        return f"{self.year}{self.month}{self.day}"

    @property
    def time_string(self) -> str:
        return f"{self.hour}{self.minute}{self.second}{self.millisecond}"


@dataclass
class ErrorHandling:
    error_code: str = ""
    error_module: str = ""
    error_routine: str = ""
    error_message: str = ""


@dataclass
class AuditFields:
    timestamp: str = ""
    user: str = ""
    terminal: str = ""
    program: str = ""

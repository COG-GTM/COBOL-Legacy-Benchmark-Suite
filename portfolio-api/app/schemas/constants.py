from enum import Enum, IntEnum


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


class AccountType(str, Enum):
    INDIVIDUAL = "IN"
    JOINT = "JT"
    CORPORATE = "CO"
    TRUST = "TR"
    RETIREMENT = "RT"


class RiskLevel(str, Enum):
    LOW = "L"
    MEDIUM = "M"
    HIGH = "H"
    AGGRESSIVE = "A"

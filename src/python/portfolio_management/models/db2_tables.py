"""DB2 Table Definitions - migrated from DBTBLS.cpy."""

from dataclasses import dataclass
from decimal import Decimal
from enum import IntEnum, Enum


@dataclass
class PosHistRecord:
    account_no: str = ""
    portfolio_id: str = ""
    trans_date: str = ""
    trans_time: str = ""
    trans_type: str = ""
    security_id: str = ""
    quantity: Decimal = Decimal("0")
    price: Decimal = Decimal("0")
    amount: Decimal = Decimal("0")
    fees: Decimal = Decimal("0")
    total_amount: Decimal = Decimal("0")
    cost_basis: Decimal = Decimal("0")
    gain_loss: Decimal = Decimal("0")
    process_date: str = ""
    process_time: str = ""
    program_id: str = ""
    user_id: str = ""
    audit_timestamp: str = ""


class ErrorLogType(str, Enum):
    SYSTEM = "S"
    APPLICATION = "A"
    DATA = "D"


class ErrorLogSeverity(IntEnum):
    INFO = 1
    WARNING = 2
    ERROR = 3
    SEVERE = 4


@dataclass
class ErrLogRecord:
    error_timestamp: str = ""
    program_id: str = ""
    error_type: str = ErrorLogType.APPLICATION
    error_severity: int = ErrorLogSeverity.ERROR
    error_code: str = ""
    error_message: str = ""
    process_date: str = ""
    process_time: str = ""
    user_id: str = ""
    additional_info: str = ""


class SqlStatusCode:
    SUCCESS = "00000"
    NOT_FOUND = "02000"
    DUP_KEY = "23505"
    DEADLOCK = "40001"
    TIMEOUT = "40003"
    CONNECTION_ERROR = "08001"
    DB_ERROR = "58004"

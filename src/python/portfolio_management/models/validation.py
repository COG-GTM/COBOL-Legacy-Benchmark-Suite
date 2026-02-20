"""Portfolio Validation Rules - migrated from PORTVAL.cpy."""

from dataclasses import dataclass
from decimal import Decimal


class ValidationReturnCode:
    SUCCESS = 0
    INVALID_ID = 1
    INVALID_ACCT = 2
    INVALID_TYPE = 3
    INVALID_AMT = 4


class ValidationErrorMessage:
    INVALID_ID = "Invalid Portfolio ID format"
    INVALID_ACCT = "Invalid Account Number format"
    INVALID_TYPE = "Invalid Investment Type"
    INVALID_AMT = "Amount outside valid range"


class ValidationConstants:
    MIN_AMOUNT = Decimal("-9999999999999.99")
    MAX_AMOUNT = Decimal("9999999999999.99")
    ID_PREFIX = "PORT"
    VALID_INVESTMENT_TYPES = ("STK", "BND", "MMF", "ETF")


@dataclass
class PortfolioValidation:
    numeric_check: str = ""
    temp_num: Decimal = Decimal("0")
    error_code: int = 0
    error_msg: str = ""

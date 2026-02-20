"""Portfolio Validation Subroutine - migrated from PORTVALD.cbl.

Validates portfolio data elements including ID format, account number,
investment type, and amount ranges.
"""

import logging
from decimal import Decimal, InvalidOperation

from portfolio_management.models.validation import (
    ValidationReturnCode,
    ValidationErrorMessage,
    ValidationConstants,
)

logger = logging.getLogger(__name__)

PROGRAM_ID = "PORTVALD"


class ValidationResult:
    def __init__(self, return_code: int = 0, error_msg: str = ""):
        self.return_code = return_code
        self.error_msg = error_msg

    @property
    def is_valid(self) -> bool:
        return self.return_code == ValidationReturnCode.SUCCESS


class PortfolioValidator:
    def validate(self, validate_type: str, input_value: str) -> ValidationResult:
        if validate_type == "I":
            return self._validate_id(input_value)
        elif validate_type == "A":
            return self._validate_account(input_value)
        elif validate_type == "T":
            return self._validate_type(input_value)
        elif validate_type == "M":
            return self._validate_amount(input_value)
        else:
            return ValidationResult(
                ValidationReturnCode.INVALID_ID,
                "Invalid validation type",
            )

    def _validate_id(self, value: str) -> ValidationResult:
        if len(value) < 8:
            return ValidationResult(
                ValidationReturnCode.INVALID_ID,
                ValidationErrorMessage.INVALID_ID,
            )

        if value[:4] != ValidationConstants.ID_PREFIX:
            return ValidationResult(
                ValidationReturnCode.INVALID_ID,
                ValidationErrorMessage.INVALID_ID,
            )

        if not value[4:8].isdigit():
            return ValidationResult(
                ValidationReturnCode.INVALID_ID,
                ValidationErrorMessage.INVALID_ID,
            )

        return ValidationResult(ValidationReturnCode.SUCCESS)

    def _validate_account(self, value: str) -> ValidationResult:
        if not value.strip().isdigit():
            return ValidationResult(
                ValidationReturnCode.INVALID_ACCT,
                ValidationErrorMessage.INVALID_ACCT,
            )

        if value.strip() == "0" * len(value.strip()):
            return ValidationResult(
                ValidationReturnCode.INVALID_ACCT,
                ValidationErrorMessage.INVALID_ACCT,
            )

        return ValidationResult(ValidationReturnCode.SUCCESS)

    def _validate_type(self, value: str) -> ValidationResult:
        if value.strip() not in ValidationConstants.VALID_INVESTMENT_TYPES:
            return ValidationResult(
                ValidationReturnCode.INVALID_TYPE,
                ValidationErrorMessage.INVALID_TYPE,
            )

        return ValidationResult(ValidationReturnCode.SUCCESS)

    def _validate_amount(self, value: str) -> ValidationResult:
        try:
            amount = Decimal(value.strip())
        except InvalidOperation:
            return ValidationResult(
                ValidationReturnCode.INVALID_AMT,
                ValidationErrorMessage.INVALID_AMT,
            )

        if amount < ValidationConstants.MIN_AMOUNT or amount > ValidationConstants.MAX_AMOUNT:
            return ValidationResult(
                ValidationReturnCode.INVALID_AMT,
                ValidationErrorMessage.INVALID_AMT,
            )

        return ValidationResult(ValidationReturnCode.SUCCESS)

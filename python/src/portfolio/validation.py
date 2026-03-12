"""
Portfolio validation translated from COBOL program PORTVALD.cbl.

Replaces:
  - PORTVALD.cbl 1000-VALIDATE-ID: Portfolio ID format validation
  - PORTVALD.cbl 2000-VALIDATE-ACCOUNT: Account number validation
  - PORTVALD.cbl 3000-VALIDATE-TYPE: Investment type validation
  - PORTVALD.cbl 4000-VALIDATE-AMOUNT: Amount range validation

Validation types from PORTVALD.cbl:
  'I' = ID validation
  'A' = Account validation
  'T' = Type validation
  'M' = Amount validation
"""

import logging
import re
from datetime import date
from decimal import Decimal

from src.common.constants import (
    MAX_AMOUNT,
    MIN_AMOUNT,
    PORTFOLIO_ID_PREFIX,
    AccountType,
    CurrencyCode,
    InvestmentType,
    PortfolioStatus,
    RiskLevel,
)
from src.common.error_handler import ValidationError

logger = logging.getLogger(__name__)

# Regex: must start with 'PORT' followed by exactly 4 numeric digits
_PORTFOLIO_ID_PATTERN = re.compile(r"^PORT\d{4}$")
_ACCOUNT_NO_PATTERN = re.compile(r"^\d{10}$")


def validate_portfolio_id(portfolio_id: str) -> None:
    """
    Validate portfolio ID format.

    Translates PORTVALD.cbl 1000-VALIDATE-ID:
      - Must start with 'PORT' prefix
      - Followed by 4 numeric digits
      - Total length = 8

    Raises:
        ValidationError: If format is invalid.
    """
    if not portfolio_id:
        raise ValidationError("Portfolio ID is required", field="portfolio_id")
    if len(portfolio_id) > 8:
        raise ValidationError(
            f"Portfolio ID exceeds maximum length of 8: '{portfolio_id}'",
            field="portfolio_id",
        )
    if not _PORTFOLIO_ID_PATTERN.match(portfolio_id):
        raise ValidationError(
            f"Portfolio ID must start with '{PORTFOLIO_ID_PREFIX}' followed by 4 digits: '{portfolio_id}'",
            field="portfolio_id",
        )


def validate_account_no(account_no: str) -> None:
    """
    Validate account number.

    Translates PORTVALD.cbl 2000-VALIDATE-ACCOUNT:
      - Must be 10 numeric digits
      - Must not be all zeros

    Raises:
        ValidationError: If format is invalid.
    """
    if not account_no:
        raise ValidationError("Account number is required", field="account_no")
    if not _ACCOUNT_NO_PATTERN.match(account_no):
        raise ValidationError(
            f"Account number must be 10 numeric digits: '{account_no}'",
            field="account_no",
        )
    if account_no == "0000000000":
        raise ValidationError(
            "Account number must not be all zeros",
            field="account_no",
        )


def validate_investment_type(inv_type: str) -> None:
    """
    Validate investment type code.

    Translates PORTVALD.cbl 3000-VALIDATE-TYPE:
      - Must be STK, BND, MMF, or ETF

    Raises:
        ValidationError: If type is invalid.
    """
    valid_types = {t.value for t in InvestmentType}
    if inv_type not in valid_types:
        raise ValidationError(
            f"Invalid investment type '{inv_type}'. Must be one of: {', '.join(sorted(valid_types))}",
            field="investment_type",
        )


def validate_amount(amount: Decimal) -> None:
    """
    Validate monetary amount range.

    Translates PORTVALD.cbl 4000-VALIDATE-AMOUNT:
      - Must be within MIN_AMOUNT..MAX_AMOUNT (PIC S9(13)V99 range)

    Raises:
        ValidationError: If amount is out of range.
    """
    if amount < MIN_AMOUNT or amount > MAX_AMOUNT:
        raise ValidationError(
            f"Amount {amount} is outside valid range ({MIN_AMOUNT} to {MAX_AMOUNT})",
            field="amount",
        )


def validate_currency_code(currency: str) -> None:
    """Validate currency code."""
    valid_codes = {c.value for c in CurrencyCode}
    if currency not in valid_codes:
        raise ValidationError(
            f"Invalid currency code '{currency}'. Must be one of: {', '.join(sorted(valid_codes))}",
            field="currency_code",
        )


def validate_account_type(account_type: str) -> None:
    """Validate account type code."""
    valid_types = {t.value for t in AccountType}
    if account_type not in valid_types:
        raise ValidationError(
            f"Invalid account type '{account_type}'. Must be one of: {', '.join(sorted(valid_types))}",
            field="account_type",
        )


def validate_risk_level(risk_level: str) -> None:
    """Validate risk level code."""
    valid_levels = {r.value for r in RiskLevel}
    if risk_level not in valid_levels:
        raise ValidationError(
            f"Invalid risk level '{risk_level}'. Must be one of: {', '.join(sorted(valid_levels))}",
            field="risk_level",
        )


def validate_status(status: str) -> None:
    """Validate portfolio status code."""
    valid_statuses = {s.value for s in PortfolioStatus}
    if status not in valid_statuses:
        raise ValidationError(
            f"Invalid status '{status}'. Must be one of: {', '.join(sorted(valid_statuses))}",
            field="status",
        )


def validate_dates(open_date: date, close_date: date | None = None) -> None:
    """Validate date fields and their relationships."""
    if close_date is not None and close_date < open_date:
        raise ValidationError(
            f"Close date ({close_date}) cannot be before open date ({open_date})",
            field="close_date",
        )


def validate_portfolio_for_closure(
    has_open_positions: bool,
    has_pending_transactions: bool,
) -> None:
    """
    Validate business rules for portfolio closure.

    Cannot close a portfolio with open positions or pending transactions.
    """
    if has_open_positions:
        raise ValidationError(
            "Cannot close portfolio with open positions",
            field="status",
        )
    if has_pending_transactions:
        raise ValidationError(
            "Cannot close portfolio with pending transactions",
            field="status",
        )

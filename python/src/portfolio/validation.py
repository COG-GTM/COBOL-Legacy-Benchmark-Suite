"""
Portfolio validation translated from COBOL program PORTVALD.cbl.

Validates portfolio fields: ID format, account type, risk level,
currency code, dates. Enforces business rules.
"""

from src.common.constants import (
    VALID_CURRENCIES,
    AccountType,
    PortfolioStatus,
    RiskLevel,
)
from src.common.error_handler import ValidationError


def validate_portfolio_create(
    portfolio_id: str,
    client_id: str,
    client_name: str,
    account_type: str = "IN",
    currency_code: str = "USD",
    risk_level: str = "M",
) -> None:
    """
    Validate portfolio creation fields.
    Translates PORTVALD.cbl validation paragraphs.
    """
    # Portfolio ID: PIC X(8), required, alphanumeric
    if not portfolio_id or not portfolio_id.strip():
        raise ValidationError("Portfolio ID is required", field="portfolio_id", error_code="V001")
    if len(portfolio_id) > 8:
        raise ValidationError("Portfolio ID must be at most 8 characters", field="portfolio_id", error_code="V002")
    if not portfolio_id.replace("-", "").replace("_", "").isalnum():
        raise ValidationError("Portfolio ID must be alphanumeric", field="portfolio_id", error_code="V003")

    # Client ID: PIC X(10), required
    if not client_id or not client_id.strip():
        raise ValidationError("Client ID is required", field="client_id", error_code="V004")
    if len(client_id) > 10:
        raise ValidationError("Client ID must be at most 10 characters", field="client_id", error_code="V005")

    # Client name: PIC X(30), required
    if not client_name or not client_name.strip():
        raise ValidationError("Client name is required", field="client_name", error_code="V006")
    if len(client_name) > 30:
        raise ValidationError("Client name must be at most 30 characters", field="client_name", error_code="V007")

    # Account type: PIC X(2), must be valid enum value
    valid_account_types = {e.value for e in AccountType}
    if account_type not in valid_account_types:
        raise ValidationError(
            f"Invalid account type: {account_type}. Must be one of {valid_account_types}",
            field="account_type",
            error_code="V008",
        )

    # Currency code: PIC X(3), must be valid
    if currency_code not in VALID_CURRENCIES:
        raise ValidationError(
            f"Invalid currency: {currency_code}. Must be one of {VALID_CURRENCIES}",
            field="currency_code",
            error_code="V009",
        )

    # Risk level: PIC X(1), must be valid enum value
    valid_risk_levels = {e.value for e in RiskLevel}
    if risk_level not in valid_risk_levels:
        raise ValidationError(
            f"Invalid risk level: {risk_level}. Must be one of {valid_risk_levels}",
            field="risk_level",
            error_code="V010",
        )


def validate_portfolio_update(portfolio, **kwargs) -> None:
    """
    Validate portfolio update fields.
    Translates PORTVALD.cbl update validation.
    """
    if "currency_code" in kwargs and kwargs["currency_code"] is not None:
        if kwargs["currency_code"] not in VALID_CURRENCIES:
            raise ValidationError(
                f"Invalid currency: {kwargs['currency_code']}",
                field="currency_code",
                error_code="V009",
            )

    if "risk_level" in kwargs and kwargs["risk_level"] is not None:
        valid_risk_levels = {e.value for e in RiskLevel}
        if kwargs["risk_level"] not in valid_risk_levels:
            raise ValidationError(
                f"Invalid risk level: {kwargs['risk_level']}",
                field="risk_level",
                error_code="V010",
            )

    if "status" in kwargs and kwargs["status"] is not None:
        valid_statuses = {e.value for e in PortfolioStatus}
        if kwargs["status"] not in valid_statuses:
            raise ValidationError(
                f"Invalid status: {kwargs['status']}",
                field="status",
                error_code="V011",
            )

    if "portfolio_name" in kwargs and kwargs["portfolio_name"] is not None:
        if len(kwargs["portfolio_name"]) > 50:
            raise ValidationError(
                "Portfolio name must be at most 50 characters",
                field="portfolio_name",
                error_code="V012",
            )

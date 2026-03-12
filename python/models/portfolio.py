"""Portfolio master record model translated from src/copybook/common/PORTFLIO.cpy.

Validation rules sourced from PORTMSTR.cbl and PORTVALD.cbl.
"""

import re
from datetime import date
from decimal import Decimal

from pydantic import BaseModel, field_validator

from models.enums import ClientType, PortfolioStatus


class Portfolio(BaseModel):
    """Portfolio master record (PORTFLIO.cpy PORT-RECORD).

    Financial fields use Decimal to match COBOL PIC S9(13)V99 COMP-3.
    """

    id: str
    account_no: str
    client_name: str
    client_type: ClientType
    create_date: date
    last_maint: date
    status: PortfolioStatus
    total_value: Decimal
    cash_balance: Decimal
    last_user: str
    last_trans: date

    @field_validator("id")
    @classmethod
    def validate_portfolio_id(cls, v: str) -> str:
        """Portfolio ID must start with 'PORT' followed by exactly 4 numeric digits.

        Mirrors PORTVALD.cbl lines 52-71: checks LS-INPUT-VALUE(5:4) for numeric.
        PORT-ID is PIC X(8) so total length must be exactly 8 characters.
        """
        if not re.match(r"^PORT\d{4}$", v):
            raise ValueError(
                "Portfolio ID must start with 'PORT' followed by exactly 4 numeric digits"
            )
        return v

    @field_validator("account_no")
    @classmethod
    def validate_account_no(cls, v: str) -> str:
        """Account number must be exactly 10 numeric digits.

        Mirrors PORTVALD.cbl lines 73-86.
        """
        if not re.match(r"^\d{10}$", v):
            raise ValueError("Account number must be exactly 10 numeric digits")
        return v

    @field_validator("client_name")
    @classmethod
    def validate_client_name(cls, v: str) -> str:
        """Client name max 30 characters (COBOL PIC X(30))."""
        if len(v) > 30:
            raise ValueError("Client name must not exceed 30 characters")
        return v

    @field_validator("last_user")
    @classmethod
    def validate_last_user(cls, v: str) -> str:
        """Last user max 8 characters (COBOL PIC X(8))."""
        if len(v) > 8:
            raise ValueError("Last user must not exceed 8 characters")
        return v

"""
Portfolio data models translated from COBOL copybooks.

Source copybooks:
  - src/copybook/common/PORTFLIO.cpy  (Portfolio Master Record Layout)
  - src/copybook/common/PORTVAL.cpy   (Portfolio Validation Rules)
"""

from __future__ import annotations

import datetime
from decimal import Decimal
from enum import Enum
from typing import Annotated, Optional

from pydantic import BaseModel, Field, field_validator


# ---------------------------------------------------------------------------
# Enums derived from PORTFLIO.cpy level-88 condition values
# ---------------------------------------------------------------------------

class ClientType(str, Enum):
    """PORT-CLIENT-TYPE level-88 values from PORTFLIO.cpy."""

    INDIVIDUAL = "I"
    CORPORATE = "C"
    TRUST = "T"


class PortfolioStatus(str, Enum):
    """PORT-STATUS level-88 values from PORTFLIO.cpy."""

    ACTIVE = "A"
    CLOSED = "C"
    SUSPENDED = "S"


# ---------------------------------------------------------------------------
# Pydantic models
# ---------------------------------------------------------------------------

class PortfolioKey(BaseModel):
    """Maps to PORTFLIO.cpy  05 PORT-KEY."""

    # PIC X(8)
    portfolio_id: Annotated[str, Field(max_length=8)]
    # PIC X(10)
    account_no: Annotated[str, Field(max_length=10)]


class PortfolioClientInfo(BaseModel):
    """Maps to PORTFLIO.cpy  05 PORT-CLIENT-INFO."""

    # PIC X(30)
    client_name: Annotated[str, Field(max_length=30)]
    # PIC X(1) with level-88 values
    client_type: ClientType


class PortfolioInfo(BaseModel):
    """Maps to PORTFLIO.cpy  05 PORT-PORTFOLIO-INFO."""

    # PIC 9(8)  YYYYMMDD
    create_date: datetime.date
    # PIC 9(8)  YYYYMMDD
    last_maint: datetime.date
    # PIC X(1) with level-88 values
    status: PortfolioStatus

    @field_validator("create_date", "last_maint", mode="before")
    @classmethod
    def _parse_cobol_date(cls, value: object) -> object:
        """Accept COBOL YYYYMMDD strings and convert to date objects."""
        if isinstance(value, str) and len(value) == 8 and value.isdigit():
            return datetime.date(
                int(value[:4]), int(value[4:6]), int(value[6:8])
            )
        return value


class PortfolioFinancialInfo(BaseModel):
    """Maps to PORTFLIO.cpy  05 PORT-FINANCIAL-INFO.

    COBOL: PIC S9(13)V99 COMP-3  =>  13 integer + 2 decimal digits.
    """

    total_value: Annotated[
        Decimal,
        Field(decimal_places=2, max_digits=15),
    ]
    cash_balance: Annotated[
        Decimal,
        Field(decimal_places=2, max_digits=15),
    ]


class PortfolioAuditInfo(BaseModel):
    """Maps to PORTFLIO.cpy  05 PORT-AUDIT-INFO."""

    # PIC X(8)
    last_user: Annotated[str, Field(max_length=8)]
    # PIC 9(8)  YYYYMMDD
    last_trans: datetime.date

    @field_validator("last_trans", mode="before")
    @classmethod
    def _parse_cobol_date(cls, value: object) -> object:
        if isinstance(value, str) and len(value) == 8 and value.isdigit():
            return datetime.date(
                int(value[:4]), int(value[4:6]), int(value[6:8])
            )
        return value


class PortfolioRecord(BaseModel):
    """Complete portfolio master record.

    Maps to PORTFLIO.cpy  01 PORT-RECORD.
    """

    # PORT-KEY
    portfolio_id: Annotated[str, Field(max_length=8)]
    account_no: Annotated[str, Field(max_length=10)]

    # PORT-CLIENT-INFO
    client_name: Annotated[str, Field(max_length=30)]
    client_type: ClientType

    # PORT-PORTFOLIO-INFO
    create_date: datetime.date
    last_maint: datetime.date
    status: PortfolioStatus

    # PORT-FINANCIAL-INFO  (PIC S9(13)V99 COMP-3)
    total_value: Annotated[Decimal, Field(decimal_places=2, max_digits=15)]
    cash_balance: Annotated[Decimal, Field(decimal_places=2, max_digits=15)]

    # PORT-AUDIT-INFO
    last_user: Annotated[str, Field(max_length=8)]
    last_trans: datetime.date

    # PORT-FILLER  PIC X(50) - reserved space, not modelled

    @field_validator("create_date", "last_maint", "last_trans", mode="before")
    @classmethod
    def _parse_cobol_date(cls, value: object) -> object:
        """Accept COBOL YYYYMMDD strings and convert to date objects."""
        if isinstance(value, str) and len(value) == 8 and value.isdigit():
            return datetime.date(
                int(value[:4]), int(value[4:6]), int(value[6:8])
            )
        return value


# ---------------------------------------------------------------------------
# Validation models  (from PORTVAL.cpy)
# ---------------------------------------------------------------------------

class PortfolioValidationResult(BaseModel):
    """Captures the outcome of portfolio validation per PORTVAL.cpy."""

    error_code: int = 0
    error_message: Annotated[str, Field(max_length=50)] = ""
    is_valid: bool = True

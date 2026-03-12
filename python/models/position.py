"""Position record model translated from src/copybook/common/POSREC.cpy."""

from datetime import datetime
from decimal import Decimal

from pydantic import BaseModel, field_validator

from models.enums import PositionStatus


class PositionRecord(BaseModel):
    """Position record (POSREC.cpy POSITION-RECORD).

    - quantity:     PIC S9(11)V9(4) COMP-3 -> Decimal
    - cost_basis:   PIC S9(13)V9(2) COMP-3 -> Decimal
    - market_value: PIC S9(13)V9(2) COMP-3 -> Decimal
    """

    portfolio_id: str
    date: str
    investment_id: str
    quantity: Decimal
    cost_basis: Decimal
    market_value: Decimal
    currency: str
    status: PositionStatus
    last_maint_date: datetime
    last_maint_user: str

    @field_validator("portfolio_id")
    @classmethod
    def validate_portfolio_id(cls, v: str) -> str:
        """Portfolio ID max 8 characters (COBOL PIC X(08))."""
        if len(v) > 8:
            raise ValueError("Portfolio ID must not exceed 8 characters")
        return v

    @field_validator("date")
    @classmethod
    def validate_date(cls, v: str) -> str:
        """Position date must be YYYYMMDD (COBOL PIC X(08))."""
        if len(v) != 8 or not v.isdigit():
            raise ValueError("Position date must be 8 numeric digits (YYYYMMDD)")
        return v

    @field_validator("investment_id")
    @classmethod
    def validate_investment_id(cls, v: str) -> str:
        """Investment ID max 10 characters (COBOL PIC X(10))."""
        if len(v) > 10:
            raise ValueError("Investment ID must not exceed 10 characters")
        return v

    @field_validator("currency")
    @classmethod
    def validate_currency(cls, v: str) -> str:
        """Currency code max 3 characters (COBOL PIC X(03))."""
        if len(v) > 3:
            raise ValueError("Currency code must not exceed 3 characters")
        return v

    @field_validator("last_maint_user")
    @classmethod
    def validate_last_maint_user(cls, v: str) -> str:
        """Last maintenance user max 8 characters (COBOL PIC X(08))."""
        if len(v) > 8:
            raise ValueError("Last maintenance user must not exceed 8 characters")
        return v

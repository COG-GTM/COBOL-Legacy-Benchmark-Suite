"""
Position data models translated from COBOL copybook POSREC.cpy.

COBOL PIC clauses:
    POS-PORTFOLIO-ID   PIC X(08)
    POS-DATE           PIC X(08)
    POS-INVESTMENT-ID  PIC X(10)
    POS-QUANTITY       PIC S9(11)V9(4) COMP-3
    POS-COST-BASIS     PIC S9(13)V9(2) COMP-3
    POS-MARKET-VALUE   PIC S9(13)V9(2) COMP-3
"""

from datetime import date, datetime
from decimal import Decimal
from typing import Optional

from pydantic import BaseModel, Field, field_validator

from src.common.constants import PositionStatus


class PositionRecord(BaseModel):
    """Translates COBOL 01 POSITION-RECORD from POSREC.cpy."""

    # --- Key fields (from POS-KEY) ---
    portfolio_id: str = Field(max_length=8, description="PIC X(08)")
    position_date: date = Field(description="PIC X(08) YYYYMMDD")
    investment_id: str = Field(max_length=10, description="PIC X(10)")

    # --- Data fields (from POS-DATA) ---
    quantity: Decimal = Field(
        default=Decimal("0.0000"),
        description="PIC S9(11)V9(4) COMP-3",
    )
    cost_basis: Decimal = Field(
        default=Decimal("0.00"),
        description="PIC S9(13)V9(2) COMP-3",
    )
    market_value: Decimal = Field(
        default=Decimal("0.00"),
        description="PIC S9(13)V9(2) COMP-3",
    )
    currency: str = Field(max_length=3, default="USD", description="PIC X(03)")
    status: PositionStatus = Field(
        default=PositionStatus.ACTIVE,
        description="PIC X(01), level-88: A/C/P",
    )

    # --- Audit fields ---
    last_maint_date: Optional[datetime] = Field(default=None)
    last_maint_user: str = Field(max_length=8, default="", description="VARCHAR(8)")

    @field_validator("quantity")
    @classmethod
    def validate_quantity(cls, v: Decimal) -> Decimal:
        """PIC S9(11)V9(4) COMP-3 — 4 decimal places."""
        if v is not None:
            return Decimal(str(v)).quantize(Decimal("0.0001"))
        return v

    @field_validator("cost_basis", "market_value")
    @classmethod
    def validate_monetary(cls, v: Decimal) -> Decimal:
        """PIC S9(13)V9(2) COMP-3 — 2 decimal places."""
        if v is not None:
            return Decimal(str(v)).quantize(Decimal("0.01"))
        return v

    model_config = {"from_attributes": True}

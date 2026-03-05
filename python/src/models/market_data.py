"""
Market data models translated from COBOL copybook MKTDATA.cpy.
"""

from datetime import date, datetime
from decimal import Decimal
from typing import Optional

from pydantic import BaseModel, Field, field_validator


class MarketDataRecord(BaseModel):
    """Translates COBOL market data record from MKTDATA.cpy."""

    investment_id: str = Field(max_length=10, description="PIC X(10)")
    price_date: date = Field(description="PIC X(08) YYYYMMDD")
    current_price: Decimal = Field(
        default=Decimal("0.0000"),
        description="PIC S9(11)V9(4) COMP-3",
    )
    previous_price: Decimal = Field(
        default=Decimal("0.0000"),
        description="PIC S9(11)V9(4) COMP-3",
    )
    high_price: Decimal = Field(
        default=Decimal("0.0000"),
        description="PIC S9(11)V9(4) COMP-3",
    )
    low_price: Decimal = Field(
        default=Decimal("0.0000"),
        description="PIC S9(11)V9(4) COMP-3",
    )
    volume: Decimal = Field(
        default=Decimal("0"),
        description="PIC S9(13) COMP-3",
    )
    currency: str = Field(max_length=3, default="USD", description="PIC X(03)")
    source: str = Field(max_length=10, default="", description="PIC X(10)")
    last_update: Optional[datetime] = Field(default=None)

    @field_validator("current_price", "previous_price", "high_price", "low_price")
    @classmethod
    def validate_price(cls, v: Decimal) -> Decimal:
        if v is not None:
            return Decimal(str(v)).quantize(Decimal("0.0001"))
        return v

    model_config = {"from_attributes": True}

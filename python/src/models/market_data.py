"""
Market data models translated from COBOL copybook MKTDATA.cpy concepts.

Provides pricing and market information for investment positions.
"""

from datetime import date, datetime
from decimal import Decimal

from pydantic import BaseModel, Field

from src.common.constants import CurrencyCode


class MarketDataRecord(BaseModel):
    """Market data record for investment pricing."""

    investment_id: str = Field(max_length=10, description="Investment identifier")
    price_date: date = Field(default_factory=date.today, description="Price date")
    current_price: Decimal = Field(default=Decimal("0.0000"), max_digits=15, decimal_places=4)
    previous_close: Decimal = Field(default=Decimal("0.0000"), max_digits=15, decimal_places=4)
    day_high: Decimal = Field(default=Decimal("0.0000"), max_digits=15, decimal_places=4)
    day_low: Decimal = Field(default=Decimal("0.0000"), max_digits=15, decimal_places=4)
    volume: Decimal = Field(default=Decimal("0"), max_digits=15, decimal_places=0)
    currency: CurrencyCode = Field(default=CurrencyCode.USD, description="Currency code")
    last_update: datetime = Field(default_factory=datetime.now, description="Last update timestamp")

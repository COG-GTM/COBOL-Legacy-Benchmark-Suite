"""Position Record model - translated from POSREC.cpy copybook.

Mirrors the COBOL POSITION-RECORD structure with composite key
(portfolio_id, date, investment_id) and position data fields.
"""

from decimal import Decimal
from enum import StrEnum

from pydantic import BaseModel, Field


class PositionStatus(StrEnum):
    """Position status codes from 88-level values in POSREC."""

    ACTIVE = "A"
    CLOSED = "C"
    PENDING = "P"


class PositionRecord(BaseModel):
    """Full position record translated from COBOL POSITION-RECORD.

    Maps to POSREC.cpy copybook fields:
    - POS-KEY (composite key: portfolio_id + date + investment_id)
    - POS-DATA (position details)
    """

    # Key fields (POS-KEY)
    portfolio_id: str = Field(max_length=8, description="POS-PORTFOLIO-ID")
    date: str = Field(max_length=8, description="POS-DATE: YYYYMMDD")
    investment_id: str = Field(max_length=10, description="POS-INVESTMENT-ID")

    # Data fields (POS-DATA)
    quantity: Decimal = Field(max_digits=15, decimal_places=4, description="POS-QUANTITY S9(11)V9(4)")
    cost_basis: Decimal = Field(max_digits=15, decimal_places=2, description="POS-COST-BASIS S9(13)V9(2)")
    market_value: Decimal = Field(max_digits=15, decimal_places=2, description="POS-MARKET-VALUE S9(13)V9(2)")
    currency: str = Field(max_length=3, description="POS-CURRENCY")
    status: PositionStatus = Field(default=PositionStatus.ACTIVE, description="POS-STATUS: A/C/P")

    @property
    def composite_key(self) -> str:
        """Build the composite key matching VSAM KSDS key structure (18 bytes)."""
        return f"{self.portfolio_id}{self.date}{self.investment_id}"

    @property
    def unrealized_gain_loss(self) -> Decimal:
        """Calculate unrealized gain/loss."""
        return self.market_value - self.cost_basis

    @property
    def gain_loss_percent(self) -> Decimal:
        """Calculate gain/loss percentage."""
        if self.cost_basis == 0:
            return Decimal("0")
        return ((self.market_value - self.cost_basis) / self.cost_basis) * 100

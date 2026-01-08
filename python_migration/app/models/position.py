"""Position Record model - converted from POSREC.cpy.

COBOL Original:
01  POSITION-RECORD.
    05  POS-KEY.
        10  POS-PORTFOLIO-ID   PIC X(08).
        10  POS-DATE           PIC X(08).
        10  POS-INVESTMENT-ID  PIC X(10).
    05  POS-DATA.
        10  POS-QUANTITY       PIC S9(11)V9(4) COMP-3.
        10  POS-COST-BASIS     PIC S9(13)V9(2) COMP-3.
        10  POS-MARKET-VALUE   PIC S9(13)V9(2) COMP-3.
        10  POS-CURRENCY       PIC X(03).
        10  POS-STATUS         PIC X(01).
    05  POS-AUDIT.
        10  POS-LAST-MAINT-DATE   PIC X(26).
        10  POS-LAST-MAINT-USER   PIC X(08).
"""

from datetime import datetime
from decimal import Decimal
from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field, field_validator


class PositionStatus(str, Enum):
    """Position status codes - maps to 88-level conditions in COBOL."""

    ACTIVE = "A"  # POS-STATUS-ACTIVE
    CLOSED = "C"  # POS-STATUS-CLOSED
    PENDING = "P"  # POS-STATUS-PEND


class PositionKey(BaseModel):
    """Position key structure - maps to POS-KEY in COBOL."""

    portfolio_id: str = Field(max_length=8, description="Portfolio identifier")
    date: str = Field(max_length=8, description="Position date (YYYYMMDD)")
    investment_id: str = Field(max_length=10, description="Investment identifier")

    @field_validator("portfolio_id", "investment_id")
    @classmethod
    def strip_and_upper(cls, v: str) -> str:
        """Strip whitespace and convert to uppercase."""
        return v.strip().upper()

    @field_validator("date")
    @classmethod
    def validate_date_format(cls, v: str) -> str:
        """Validate date is in YYYYMMDD format."""
        if len(v) != 8 or not v.isdigit():
            raise ValueError("Date must be in YYYYMMDD format")
        return v


class PositionData(BaseModel):
    """Position data structure - maps to POS-DATA in COBOL."""

    quantity: Decimal = Field(
        default=Decimal("0"),
        decimal_places=4,
        description="Holding quantity - PIC S9(11)V9(4)",
    )
    cost_basis: Decimal = Field(
        default=Decimal("0"),
        decimal_places=2,
        description="Total cost basis - PIC S9(13)V9(2)",
    )
    market_value: Decimal = Field(
        default=Decimal("0"),
        decimal_places=2,
        description="Current market value - PIC S9(13)V9(2)",
    )
    currency: str = Field(default="USD", max_length=3, description="Currency code")
    status: PositionStatus = Field(
        default=PositionStatus.ACTIVE, description="Position status"
    )


class PositionAudit(BaseModel):
    """Position audit structure - maps to POS-AUDIT in COBOL."""

    last_maint_date: Optional[datetime] = Field(
        default=None, description="Last maintenance date/time"
    )
    last_maint_user: str = Field(
        default="", max_length=8, description="Last maintenance user ID"
    )


class PositionRecord(BaseModel):
    """Complete position record - maps to POSITION-RECORD in COBOL.

    This model represents a portfolio position at a specific point in time,
    tracking the quantity, cost basis, and market value of an investment.
    """

    key: PositionKey
    data: PositionData = Field(default_factory=PositionData)
    audit: PositionAudit = Field(default_factory=PositionAudit)

    @property
    def portfolio_id(self) -> str:
        """Convenience accessor for portfolio ID."""
        return self.key.portfolio_id

    @property
    def investment_id(self) -> str:
        """Convenience accessor for investment ID."""
        return self.key.investment_id

    @property
    def unrealized_gain_loss(self) -> Decimal:
        """Calculate unrealized gain/loss."""
        return self.data.market_value - self.data.cost_basis

    @property
    def is_active(self) -> bool:
        """Check if position is active."""
        return self.data.status == PositionStatus.ACTIVE

    def to_flat_dict(self) -> dict:
        """Convert to flat dictionary for database operations."""
        return {
            "portfolio_id": self.key.portfolio_id,
            "position_date": self.key.date,
            "investment_id": self.key.investment_id,
            "quantity": self.data.quantity,
            "cost_basis": self.data.cost_basis,
            "market_value": self.data.market_value,
            "currency": self.data.currency,
            "status": self.data.status.value,
            "last_maint_date": self.audit.last_maint_date,
            "last_maint_user": self.audit.last_maint_user,
        }

    class Config:
        """Pydantic configuration."""

        json_encoders = {Decimal: str}

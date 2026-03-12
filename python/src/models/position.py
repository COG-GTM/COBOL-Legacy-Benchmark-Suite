"""
Position models translated from COBOL copybook POSREC.cpy and DB2 INVESTMENT_POSITIONS table.

COBOL PIC clauses mapped:
  POS-PORTFOLIO-ID   PIC X(08)              -> str, max_length=8
  POS-DATE           PIC X(08)              -> date (YYYYMMDD)
  POS-INVESTMENT-ID  PIC X(10)              -> str, max_length=10
  POS-QUANTITY       PIC S9(11)V9(4) COMP-3 -> Decimal(15,4)
  POS-COST-BASIS     PIC S9(13)V9(2) COMP-3 -> Decimal(15,2)
  POS-MARKET-VALUE   PIC S9(13)V9(2) COMP-3 -> Decimal(15,2)
  POS-CURRENCY       PIC X(03)              -> str, max_length=3
  POS-STATUS         PIC X(01)              -> PositionStatus enum
"""

from datetime import date, datetime
from decimal import Decimal

from pydantic import BaseModel, Field

from src.common.constants import CurrencyCode, PositionStatus


class PositionKey(BaseModel):
    """
    Composite key from POSREC.cpy POS-KEY.

    VSAM POSHIST key: portfolio(8) + date(8) + investment(10) = 26 bytes.
    """

    portfolio_id: str = Field(max_length=8, description="Portfolio identifier")
    position_date: date = Field(description="Position date")
    investment_id: str = Field(max_length=10, description="Investment identifier")


class PositionRecord(BaseModel):
    """Full position record from POSREC.cpy and DB2 INVESTMENT_POSITIONS."""

    # Key fields
    portfolio_id: str = Field(max_length=8, description="Portfolio identifier")
    position_date: date = Field(default_factory=date.today, description="Position date")
    investment_id: str = Field(max_length=10, description="Investment identifier")

    # Position data - Decimal for COBOL COMP-3 precision
    quantity: Decimal = Field(default=Decimal("0.0000"), max_digits=15, decimal_places=4)
    cost_basis: Decimal = Field(default=Decimal("0.00"), max_digits=15, decimal_places=2)
    market_value: Decimal = Field(default=Decimal("0.00"), max_digits=15, decimal_places=2)
    currency: CurrencyCode = Field(default=CurrencyCode.USD, description="Currency code")
    status: PositionStatus = Field(default=PositionStatus.ACTIVE, description="Position status")

    # Audit fields
    last_maint_date: datetime = Field(default_factory=datetime.now, description="Last maintenance timestamp")
    last_maint_user: str = Field(default="", max_length=8, description="Last maintenance user")

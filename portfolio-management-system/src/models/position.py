"""Position Record Model - migrated from COBOL copybook POSREC.cpy

Source: src/copybook/common/POSREC.cpy
COBOL Record: POSITION-RECORD

COBOL Data Type Mapping:
    PIC X(n)              -> str (fixed-length character)
    PIC S9(11)V9(4) COMP-3 -> Decimal (packed decimal, 11 integer + 4 decimal)
    PIC S9(13)V9(2) COMP-3 -> Decimal (packed decimal, 13 integer + 2 decimal)
    88-level conditions   -> Enum or validated string constants
"""
from decimal import Decimal
from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field
from sqlalchemy import Column, String, Numeric, Index
from sqlalchemy.orm import declarative_base

Base = declarative_base()


class PositionStatus(str, Enum):
    ACTIVE = "A"
    CLOSED = "C"
    PENDING = "P"


class PositionRecordORM(Base):
    """SQLAlchemy ORM model for VSAM position history records."""

    __tablename__ = "vsam_position_history"

    portfolio_id = Column(String(8), primary_key=True, nullable=False)
    position_date = Column(String(8), primary_key=True, nullable=False)
    investment_id = Column(String(10), primary_key=True, nullable=False)
    quantity = Column(Numeric(15, 4), nullable=False)
    cost_basis = Column(Numeric(15, 2), nullable=False)
    market_value = Column(Numeric(15, 2), nullable=False)
    currency_code = Column(String(3), nullable=False, default="USD")
    status = Column(String(1), nullable=False, default="A")
    last_maint_date = Column(String(26))
    last_maint_user = Column(String(8))

    __table_args__ = (
        Index("idx_vsam_pos_date", "position_date", "portfolio_id"),
        Index("idx_vsam_pos_investment", "investment_id", "position_date"),
    )


class PositionRecord(BaseModel):
    """Pydantic model for position record validation.

    Mapped from COBOL copybook POSREC.cpy:
        01  POSITION-RECORD.
            05  POS-KEY.
                10  POS-PORTFOLIO-ID   PIC X(08).     -> portfolio_id
                10  POS-DATE           PIC X(08).     -> position_date
                10  POS-INVESTMENT-ID  PIC X(10).     -> investment_id
            05  POS-DATA.
                10  POS-QUANTITY       PIC S9(11)V9(4) COMP-3. -> quantity
                10  POS-COST-BASIS     PIC S9(13)V9(2) COMP-3. -> cost_basis
                10  POS-MARKET-VALUE   PIC S9(13)V9(2) COMP-3. -> market_value
                10  POS-CURRENCY       PIC X(03).     -> currency_code
                10  POS-STATUS         PIC X(01).     -> status
            05  POS-AUDIT.
                10  POS-LAST-MAINT-DATE PIC X(26).    -> last_maint_date
                10  POS-LAST-MAINT-USER PIC X(08).    -> last_maint_user
    """

    portfolio_id: str = Field(
        ..., min_length=1, max_length=8, description="Portfolio identifier"
    )
    position_date: str = Field(
        ..., min_length=8, max_length=8, description="Position date (YYYYMMDD)"
    )
    investment_id: str = Field(
        ..., min_length=1, max_length=10, description="Investment identifier"
    )
    quantity: Decimal = Field(
        ...,
        max_digits=15,
        decimal_places=4,
        description="Holding quantity (COBOL: S9(11)V9(4) COMP-3)",
    )
    cost_basis: Decimal = Field(
        ...,
        max_digits=15,
        decimal_places=2,
        description="Total cost basis (COBOL: S9(13)V9(2) COMP-3)",
    )
    market_value: Decimal = Field(
        ...,
        max_digits=15,
        decimal_places=2,
        description="Current market value (COBOL: S9(13)V9(2) COMP-3)",
    )
    currency_code: str = Field(
        default="USD", min_length=3, max_length=3, description="ISO currency code"
    )
    status: PositionStatus = Field(
        default=PositionStatus.ACTIVE,
        description="A=Active, C=Closed, P=Pending",
    )
    last_maint_date: Optional[str] = Field(
        default=None, max_length=26, description="Last maintenance timestamp"
    )
    last_maint_user: Optional[str] = Field(
        default=None, max_length=8, description="Last maintenance user ID"
    )

    @property
    def unrealized_gain_loss(self) -> Decimal:
        """Calculate unrealized gain/loss (market value minus cost basis)."""
        return self.market_value - self.cost_basis

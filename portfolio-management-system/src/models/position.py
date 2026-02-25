"""
Position Data Model

Migrated from COBOL copybook: src/copybook/common/POSREC.cpy

Original COBOL structure:
- POS-KEY: Composite key (PORTFOLIO-ID + DATE + INVESTMENT-ID)
- POS-DATA: Position details (quantity, cost basis, market value, currency, status)
- POS-AUDIT: Audit trail (last maintenance date, user)

COBOL Data Types Mapping:
- PIC X(n) -> str
- PIC S9(11)V9(4) COMP-3 -> Decimal (packed decimal)
- PIC S9(13)V9(2) COMP-3 -> Decimal (packed decimal)
"""

from datetime import datetime, date
from decimal import Decimal
from enum import Enum
from typing import Optional
from pydantic import BaseModel, Field, field_validator
from sqlalchemy import Column, String, Date, Numeric, DateTime, Index

from src.models.base import Base


class PositionStatus(str, Enum):
    """
    Position status codes.
    
    Migrated from COBOL 88-level conditions:
    - POS-STATUS-ACTIVE VALUE 'A'
    - POS-STATUS-CLOSED VALUE 'C'
    - POS-STATUS-PEND   VALUE 'P'
    """
    ACTIVE = "A"
    CLOSED = "C"
    PENDING = "P"


class PositionRecord(BaseModel):
    """
    Pydantic model for position record validation.
    
    Preserves all field definitions from POSREC.cpy with Python type mappings.
    """
    
    # POS-KEY fields
    pos_portfolio_id: str = Field(
        ...,
        max_length=8,
        description="Portfolio identifier"
    )
    pos_date: date = Field(
        ...,
        description="Position date (YYYYMMDD format in COBOL)"
    )
    pos_investment_id: str = Field(
        ...,
        max_length=10,
        description="Investment identifier"
    )
    
    # POS-DATA fields
    pos_quantity: Decimal = Field(
        ...,
        max_digits=15,
        decimal_places=4,
        description="Holding quantity (PIC S9(11)V9(4) COMP-3)"
    )
    pos_cost_basis: Decimal = Field(
        ...,
        max_digits=15,
        decimal_places=2,
        description="Total cost basis (PIC S9(13)V9(2) COMP-3)"
    )
    pos_market_value: Decimal = Field(
        ...,
        max_digits=15,
        decimal_places=2,
        description="Current market value (PIC S9(13)V9(2) COMP-3)"
    )
    pos_currency: str = Field(
        ...,
        max_length=3,
        description="Currency code (ISO 4217)"
    )
    pos_status: PositionStatus = Field(
        ...,
        description="Position status: A=Active, C=Closed, P=Pending"
    )
    
    # POS-AUDIT fields
    pos_last_maint_date: Optional[datetime] = Field(
        None,
        description="Last maintenance timestamp"
    )
    pos_last_maint_user: Optional[str] = Field(
        None,
        max_length=8,
        description="Last maintenance user ID"
    )

    @field_validator("pos_portfolio_id", "pos_investment_id", "pos_currency")
    @classmethod
    def strip_and_upper(cls, v: str) -> str:
        """Strip whitespace and convert to uppercase for consistency with COBOL."""
        if v:
            return v.strip().upper()
        return v

    @field_validator("pos_quantity", "pos_cost_basis", "pos_market_value", mode="before")
    @classmethod
    def convert_to_decimal(cls, v) -> Decimal:
        """Convert numeric values to Decimal for precision."""
        if v is None:
            return Decimal("0")
        return Decimal(str(v))

    @property
    def unrealized_gain_loss(self) -> Decimal:
        """Calculate unrealized gain/loss."""
        return self.pos_market_value - self.pos_cost_basis

    @property
    def gain_loss_percentage(self) -> Decimal:
        """Calculate gain/loss percentage."""
        if self.pos_cost_basis == 0:
            return Decimal("0")
        return ((self.pos_market_value - self.pos_cost_basis) / self.pos_cost_basis) * 100

    class Config:
        """Pydantic configuration."""
        json_encoders = {
            Decimal: str,
            date: lambda v: v.strftime("%Y%m%d"),
            datetime: lambda v: v.isoformat(),
        }


class Position(Base):
    """
    SQLAlchemy ORM model for position records.
    
    Maps to PostgreSQL table: positions
    Replaces VSAM POSFILE and POSHIST files.
    """
    __tablename__ = "positions"

    # Primary key fields (composite key from POS-KEY)
    pos_portfolio_id = Column(String(8), primary_key=True, nullable=False)
    pos_date = Column(Date, primary_key=True, nullable=False)
    pos_investment_id = Column(String(10), primary_key=True, nullable=False)

    # Position data fields
    pos_quantity = Column(Numeric(15, 4), nullable=False)
    pos_cost_basis = Column(Numeric(15, 2), nullable=False)
    pos_market_value = Column(Numeric(15, 2), nullable=False)
    pos_currency = Column(String(3), nullable=False, default="USD")
    pos_status = Column(String(1), nullable=False, default="A")

    # Audit fields
    pos_last_maint_date = Column(DateTime, nullable=True)
    pos_last_maint_user = Column(String(8), nullable=True)

    # Indexes for common access patterns
    __table_args__ = (
        Index("idx_pos_portfolio", "pos_portfolio_id", "pos_date"),
        Index("idx_pos_investment", "pos_investment_id", "pos_date"),
        Index("idx_pos_status", "pos_status"),
    )

    def __repr__(self) -> str:
        return (
            f"<Position(portfolio={self.pos_portfolio_id}, "
            f"investment={self.pos_investment_id}, "
            f"date={self.pos_date}, quantity={self.pos_quantity})>"
        )

    def to_pydantic(self) -> PositionRecord:
        """Convert SQLAlchemy model to Pydantic model for validation/serialization."""
        return PositionRecord(
            pos_portfolio_id=self.pos_portfolio_id,
            pos_date=self.pos_date,
            pos_investment_id=self.pos_investment_id,
            pos_quantity=self.pos_quantity,
            pos_cost_basis=self.pos_cost_basis,
            pos_market_value=self.pos_market_value,
            pos_currency=self.pos_currency,
            pos_status=PositionStatus(self.pos_status),
            pos_last_maint_date=self.pos_last_maint_date,
            pos_last_maint_user=self.pos_last_maint_user,
        )

    @classmethod
    def from_pydantic(cls, record: PositionRecord) -> "Position":
        """Create SQLAlchemy model from Pydantic model."""
        return cls(
            pos_portfolio_id=record.pos_portfolio_id,
            pos_date=record.pos_date,
            pos_investment_id=record.pos_investment_id,
            pos_quantity=record.pos_quantity,
            pos_cost_basis=record.pos_cost_basis,
            pos_market_value=record.pos_market_value,
            pos_currency=record.pos_currency,
            pos_status=record.pos_status.value,
            pos_last_maint_date=record.pos_last_maint_date,
            pos_last_maint_user=record.pos_last_maint_user,
        )

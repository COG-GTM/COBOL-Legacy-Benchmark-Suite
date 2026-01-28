"""
Position data models.
Migrated from COBOL copybook: src/copybook/common/POSREC.cpy

Original COBOL structure:
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

from pydantic import BaseModel, Field, field_validator, computed_field
from sqlalchemy import Numeric, String
from sqlalchemy.orm import Mapped, mapped_column

from src.database.base import AuditMixin, Base


class PositionStatus(str, Enum):
    """
    Position status codes.
    Migrated from COBOL: POS-STATUS values.
    """
    ACTIVE = "A"
    CLOSED = "C"
    PENDING = "P"


class PositionRecord(Base, AuditMixin):
    """
    SQLAlchemy ORM model for position records.
    Migrated from COBOL POSREC copybook and VSAM POSFILE.
    """
    __tablename__ = "position_file"
    
    # Key fields (POS-KEY)
    portfolio_id: Mapped[str] = mapped_column(String(8), primary_key=True)
    position_date: Mapped[str] = mapped_column(String(8), primary_key=True)
    investment_id: Mapped[str] = mapped_column(String(10), primary_key=True)
    
    # Data fields (POS-DATA)
    quantity: Mapped[Decimal] = mapped_column(Numeric(15, 4), nullable=False, default=0)
    cost_basis: Mapped[Decimal] = mapped_column(Numeric(15, 2), nullable=False, default=0)
    market_value: Mapped[Decimal] = mapped_column(Numeric(15, 2), nullable=False, default=0)
    currency_code: Mapped[str] = mapped_column(String(3), nullable=False, default="USD")
    status: Mapped[str] = mapped_column(String(1), nullable=False, default="A")
    
    def __repr__(self) -> str:
        return (
            f"PositionRecord(portfolio={self.portfolio_id}, date={self.position_date}, "
            f"investment={self.investment_id}, quantity={self.quantity}, "
            f"market_value={self.market_value})"
        )
    
    @property
    def unrealized_gain_loss(self) -> Decimal:
        """Calculate unrealized gain/loss."""
        return self.market_value - self.cost_basis
    
    @property
    def gain_loss_percent(self) -> Decimal:
        """Calculate gain/loss percentage."""
        if self.cost_basis == 0:
            return Decimal(0)
        return ((self.market_value - self.cost_basis) / self.cost_basis) * 100


class PositionKey(BaseModel):
    """
    Pydantic model for position key (POS-KEY).
    Used for lookups and validation.
    """
    portfolio_id: str = Field(..., min_length=1, max_length=8, description="Portfolio identifier")
    position_date: str = Field(..., min_length=8, max_length=8, description="Position date YYYYMMDD")
    investment_id: str = Field(..., min_length=1, max_length=10, description="Investment identifier")
    
    @field_validator("position_date")
    @classmethod
    def validate_date(cls, v: str) -> str:
        """Validate date format YYYYMMDD."""
        if not v.isdigit():
            raise ValueError("Date must be numeric YYYYMMDD")
        try:
            datetime.strptime(v, "%Y%m%d")
        except ValueError:
            raise ValueError("Invalid date format, expected YYYYMMDD")
        return v


class PositionData(BaseModel):
    """
    Pydantic model for position data (POS-DATA).
    Used for API requests and validation.
    """
    quantity: Decimal = Field(..., decimal_places=4, description="Position quantity")
    cost_basis: Decimal = Field(..., ge=0, decimal_places=2, description="Cost basis")
    market_value: Decimal = Field(..., ge=0, decimal_places=2, description="Current market value")
    currency_code: str = Field(default="USD", min_length=3, max_length=3, description="Currency code")
    status: PositionStatus = Field(default=PositionStatus.ACTIVE, description="Position status")


class PositionCreate(PositionKey, PositionData):
    """
    Pydantic model for creating a new position.
    Combines key and data fields.
    """
    pass


class PositionResponse(PositionCreate):
    """
    Pydantic model for position API responses.
    Includes audit fields and calculated values.
    """
    last_maint_date: Optional[str] = None
    last_maint_user: Optional[str] = None
    
    @computed_field
    @property
    def unrealized_gain_loss(self) -> Decimal:
        """Calculate unrealized gain/loss."""
        return self.market_value - self.cost_basis
    
    @computed_field
    @property
    def gain_loss_percent(self) -> Decimal:
        """Calculate gain/loss percentage."""
        if self.cost_basis == 0:
            return Decimal(0)
        return ((self.market_value - self.cost_basis) / self.cost_basis) * 100
    
    class Config:
        from_attributes = True


class PositionSummary(BaseModel):
    """
    Pydantic model for position summary.
    Used in portfolio reports.
    """
    portfolio_id: str
    position_date: str
    total_positions: int
    total_cost_basis: Decimal
    total_market_value: Decimal
    total_unrealized_gain_loss: Decimal
    positions_by_status: dict[str, int]


class PortfolioPositions(BaseModel):
    """
    Pydantic model for portfolio positions response.
    Used in online inquiry (INQPORT equivalent).
    """
    portfolio_id: str
    as_of_date: str
    positions: list[PositionResponse]
    summary: PositionSummary

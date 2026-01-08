"""
Position model - Maps to COBOL POSREC.cpy
Represents portfolio position records.
"""

from dataclasses import dataclass, field
from decimal import Decimal
from datetime import datetime
from typing import Optional
from enum import Enum


class PositionStatus(str, Enum):
    """Position status codes - maps to POS-STATUS 88 levels"""
    ACTIVE = 'A'
    CLOSED = 'C'
    PENDING = 'P'


@dataclass
class PositionRecord:
    """
    Position record dataclass - maps to COBOL POSREC.cpy
    
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
    portfolio_id: str
    date: str
    investment_id: str
    quantity: Decimal = field(default_factory=lambda: Decimal('0'))
    cost_basis: Decimal = field(default_factory=lambda: Decimal('0'))
    market_value: Decimal = field(default_factory=lambda: Decimal('0'))
    currency: str = 'USD'
    status: PositionStatus = PositionStatus.ACTIVE
    last_maint_date: Optional[datetime] = None
    last_maint_user: Optional[str] = None

    def __post_init__(self):
        """Ensure proper types after initialization"""
        if isinstance(self.quantity, (int, float, str)):
            self.quantity = Decimal(str(self.quantity))
        if isinstance(self.cost_basis, (int, float, str)):
            self.cost_basis = Decimal(str(self.cost_basis))
        if isinstance(self.market_value, (int, float, str)):
            self.market_value = Decimal(str(self.market_value))
        if isinstance(self.status, str):
            self.status = PositionStatus(self.status)

    @property
    def key(self) -> str:
        """Return composite key matching COBOL POS-KEY"""
        return f"{self.portfolio_id}{self.date}{self.investment_id}"

    @property
    def unrealized_gain_loss(self) -> Decimal:
        """Calculate unrealized gain/loss"""
        return self.market_value - self.cost_basis

    @property
    def average_cost(self) -> Decimal:
        """Calculate average cost per unit"""
        if self.quantity == 0:
            return Decimal('0')
        return self.cost_basis / self.quantity

    def validate(self) -> tuple[bool, list[str]]:
        """
        Validate position record fields.
        Returns (is_valid, list of error messages)
        """
        errors = []
        
        if not self.portfolio_id or len(self.portfolio_id) > 8:
            errors.append("E001: Invalid portfolio ID (must be 1-8 characters)")
        
        if not self.date or len(self.date) != 8 or not self.date.isdigit():
            errors.append("E002: Invalid date format (must be YYYYMMDD)")
        
        if not self.investment_id or len(self.investment_id) > 10:
            errors.append("E003: Invalid investment ID (must be 1-10 characters)")
        
        if len(self.currency) != 3:
            errors.append("E004: Invalid currency code (must be 3 characters)")
        
        if self.status == PositionStatus.ACTIVE and self.quantity < 0:
            errors.append("E005: Active position cannot have negative quantity")
        
        return len(errors) == 0, errors

    def to_dict(self) -> dict:
        """Convert to dictionary for serialization"""
        return {
            'portfolio_id': self.portfolio_id,
            'date': self.date,
            'investment_id': self.investment_id,
            'quantity': str(self.quantity),
            'cost_basis': str(self.cost_basis),
            'market_value': str(self.market_value),
            'currency': self.currency,
            'status': self.status.value,
            'last_maint_date': self.last_maint_date.isoformat() if self.last_maint_date else None,
            'last_maint_user': self.last_maint_user,
        }

    @classmethod
    def from_dict(cls, data: dict) -> 'PositionRecord':
        """Create from dictionary"""
        return cls(
            portfolio_id=data['portfolio_id'],
            date=data['date'],
            investment_id=data['investment_id'],
            quantity=Decimal(data.get('quantity', '0')),
            cost_basis=Decimal(data.get('cost_basis', '0')),
            market_value=Decimal(data.get('market_value', '0')),
            currency=data.get('currency', 'USD'),
            status=PositionStatus(data.get('status', 'A')),
            last_maint_date=datetime.fromisoformat(data['last_maint_date']) if data.get('last_maint_date') else None,
            last_maint_user=data.get('last_maint_user'),
        )


# SQLAlchemy model for database persistence
from sqlalchemy import Column, Integer, String, Numeric, DateTime, Index, UniqueConstraint
from sqlalchemy.ext.declarative import declarative_base

Base = declarative_base()


class Position(Base):
    """SQLAlchemy model for positions table - replaces VSAM POSMSTRE"""
    __tablename__ = 'positions'

    id = Column(Integer, primary_key=True, autoincrement=True)
    portfolio_id = Column(String(8), nullable=False, index=True)
    date = Column(String(8), nullable=False)
    investment_id = Column(String(10), nullable=False)
    quantity = Column(Numeric(15, 4), nullable=False, default=0)
    cost_basis = Column(Numeric(15, 2), nullable=False, default=0)
    market_value = Column(Numeric(15, 2), nullable=False, default=0)
    currency = Column(String(3), nullable=False, default='USD')
    status = Column(String(1), nullable=False, default='A')
    last_maint_date = Column(DateTime, nullable=True)
    last_maint_user = Column(String(8), nullable=True)

    __table_args__ = (
        UniqueConstraint('portfolio_id', 'date', 'investment_id', name='uix_position_key'),
        Index('ix_position_portfolio', 'portfolio_id'),
        Index('ix_position_date', 'date'),
    )

    def to_record(self) -> PositionRecord:
        """Convert to PositionRecord dataclass"""
        return PositionRecord(
            portfolio_id=self.portfolio_id,
            date=self.date,
            investment_id=self.investment_id,
            quantity=Decimal(str(self.quantity)) if self.quantity else Decimal('0'),
            cost_basis=Decimal(str(self.cost_basis)) if self.cost_basis else Decimal('0'),
            market_value=Decimal(str(self.market_value)) if self.market_value else Decimal('0'),
            currency=self.currency,
            status=PositionStatus(self.status),
            last_maint_date=self.last_maint_date,
            last_maint_user=self.last_maint_user,
        )

    @classmethod
    def from_record(cls, record: PositionRecord) -> 'Position':
        """Create from PositionRecord dataclass"""
        return cls(
            portfolio_id=record.portfolio_id,
            date=record.date,
            investment_id=record.investment_id,
            quantity=record.quantity,
            cost_basis=record.cost_basis,
            market_value=record.market_value,
            currency=record.currency,
            status=record.status.value,
            last_maint_date=record.last_maint_date,
            last_maint_user=record.last_maint_user,
        )

    def __repr__(self):
        return f"<Position(portfolio={self.portfolio_id}, investment={self.investment_id}, qty={self.quantity})>"

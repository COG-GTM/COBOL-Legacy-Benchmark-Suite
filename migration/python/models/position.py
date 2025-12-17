"""
Position data models - Python translation of POSREC.cpy

This module contains dataclasses that correspond to the COBOL copybook
POSREC.cpy, which defines the position record structure used for
portfolio holdings management.

Original COBOL Structure:
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

from dataclasses import dataclass, field
from decimal import Decimal, ROUND_HALF_UP
from datetime import datetime
from typing import Optional
from enum import Enum


class PositionStatus(str, Enum):
    """
    Position status codes corresponding to POS-STATUS 88-level conditions.
    
    88  POS-STATUS-ACTIVE  VALUE 'A'.
    88  POS-STATUS-CLOSED  VALUE 'C'.
    88  POS-STATUS-PEND    VALUE 'P'.
    """
    ACTIVE = 'A'
    CLOSED = 'C'
    PENDING = 'P'


@dataclass
class PositionKey:
    """
    Position key structure corresponding to POS-KEY in POSREC.cpy.
    
    This composite key uniquely identifies each position and is used
    for VSAM KSDS file access.
    
    Attributes:
        portfolio_id: Portfolio identifier (8 bytes)
        date: Position date in YYYYMMDD format (8 bytes)
        investment_id: Investment/security identifier (10 bytes)
    """
    portfolio_id: str
    date: str  # YYYYMMDD format
    investment_id: str
    
    def __post_init__(self):
        """Validate key field lengths matching COBOL PIC definitions."""
        if len(self.portfolio_id) > 8:
            raise ValueError(f"Portfolio ID max 8 characters, got {len(self.portfolio_id)}")
        if len(self.date) != 8:
            raise ValueError(f"Date must be 8 characters (YYYYMMDD), got {len(self.date)}")
        if len(self.investment_id) > 10:
            raise ValueError(f"Investment ID max 10 characters, got {len(self.investment_id)}")
        
        # Pad fields to match COBOL fixed-length format
        self.portfolio_id = self.portfolio_id.ljust(8)
        self.investment_id = self.investment_id.ljust(10)
    
    @property
    def composite_key(self) -> str:
        """Return the full composite key as a single string."""
        return f"{self.portfolio_id}{self.date}{self.investment_id}"
    
    @property
    def as_date(self) -> datetime:
        """Convert date string to Python datetime object."""
        return datetime.strptime(self.date, "%Y%m%d")


@dataclass
class PositionData:
    """
    Position data structure corresponding to POS-DATA in POSREC.cpy.
    
    Contains the core position information including quantities,
    cost basis, and market value.
    
    Attributes:
        quantity: Number of units held (S9(11)V9(4) COMP-3)
        cost_basis: Total cost basis (S9(13)V9(2) COMP-3)
        market_value: Current market value (S9(13)V9(2) COMP-3)
        currency: Currency code (3 bytes, default USD)
        status: Position status (A, C, P)
    """
    quantity: Decimal
    cost_basis: Decimal
    market_value: Decimal
    currency: str = 'USD'
    status: PositionStatus = PositionStatus.ACTIVE
    
    def __post_init__(self):
        """Validate and normalize data fields."""
        if len(self.currency) != 3:
            raise ValueError(f"Currency must be 3 characters, got {len(self.currency)}")
        
        # Ensure Decimal types for financial precision
        if not isinstance(self.quantity, Decimal):
            self.quantity = Decimal(str(self.quantity))
        if not isinstance(self.cost_basis, Decimal):
            self.cost_basis = Decimal(str(self.cost_basis))
        if not isinstance(self.market_value, Decimal):
            self.market_value = Decimal(str(self.market_value))


@dataclass
class PositionAudit:
    """
    Position audit structure corresponding to POS-AUDIT in POSREC.cpy.
    
    Contains audit trail information for tracking when and by whom
    the position was last modified.
    
    Attributes:
        last_maint_date: Last maintenance timestamp (26 bytes in COBOL)
        last_maint_user: User ID who last modified the position (8 bytes)
    """
    last_maint_date: datetime
    last_maint_user: str
    
    def __post_init__(self):
        """Validate audit fields."""
        if len(self.last_maint_user) > 8:
            raise ValueError(f"Last maint user max 8 characters, got {len(self.last_maint_user)}")
        self.last_maint_user = self.last_maint_user.ljust(8)


@dataclass
class PositionRecord:
    """
    Complete position record corresponding to POSITION-RECORD in POSREC.cpy.
    
    This is the main data structure used for position management throughout
    the system. It combines the key, data, and audit sections.
    
    Provides methods for calculating derived values like unrealized gain/loss
    and average cost, which are common operations in the COBOL programs.
    """
    key: PositionKey
    data: PositionData
    audit: Optional[PositionAudit] = None
    
    @property
    def unrealized_gain_loss(self) -> Decimal:
        """
        Calculate unrealized gain/loss.
        
        This is the difference between current market value and cost basis.
        """
        return self.data.market_value - self.data.cost_basis
    
    @property
    def unrealized_gain_loss_percent(self) -> Decimal:
        """
        Calculate unrealized gain/loss as a percentage.
        
        Returns 0 if cost basis is 0 to avoid division by zero.
        """
        if self.data.cost_basis == 0:
            return Decimal('0')
        return ((self.data.market_value - self.data.cost_basis) / 
                self.data.cost_basis * Decimal('100')).quantize(
                    Decimal('0.01'), rounding=ROUND_HALF_UP)
    
    @property
    def average_cost(self) -> Decimal:
        """
        Calculate average cost per unit.
        
        Returns 0 if quantity is 0 to avoid division by zero.
        """
        if self.data.quantity == 0:
            return Decimal('0')
        return (self.data.cost_basis / self.data.quantity).quantize(
            Decimal('0.0001'), rounding=ROUND_HALF_UP)
    
    @property
    def current_price(self) -> Decimal:
        """
        Calculate current price per unit based on market value.
        
        Returns 0 if quantity is 0 to avoid division by zero.
        """
        if self.data.quantity == 0:
            return Decimal('0')
        return (self.data.market_value / self.data.quantity).quantize(
            Decimal('0.0001'), rounding=ROUND_HALF_UP)
    
    @property
    def is_active(self) -> bool:
        """Check if position is active."""
        return self.data.status == PositionStatus.ACTIVE
    
    @property
    def is_closed(self) -> bool:
        """Check if position is closed."""
        return self.data.status == PositionStatus.CLOSED
    
    def apply_buy(self, quantity: Decimal, price: Decimal, 
                  user: str, timestamp: Optional[datetime] = None) -> 'PositionRecord':
        """
        Apply a buy transaction to this position.
        
        Updates quantity and cost basis according to the business rules
        from POSUPD00.
        
        Args:
            quantity: Number of units to buy
            price: Price per unit
            user: User ID performing the transaction
            timestamp: Transaction timestamp (defaults to now)
            
        Returns:
            Updated PositionRecord
        """
        if timestamp is None:
            timestamp = datetime.now()
        
        # Calculate new values
        new_quantity = self.data.quantity + quantity
        additional_cost = quantity * price
        new_cost_basis = self.data.cost_basis + additional_cost
        
        # Update position data
        self.data.quantity = new_quantity
        self.data.cost_basis = new_cost_basis
        
        # Update audit
        self.audit = PositionAudit(
            last_maint_date=timestamp,
            last_maint_user=user
        )
        
        return self
    
    def apply_sell(self, quantity: Decimal, price: Decimal,
                   user: str, timestamp: Optional[datetime] = None) -> tuple['PositionRecord', Decimal]:
        """
        Apply a sell transaction to this position.
        
        Updates quantity and cost basis proportionally according to
        the business rules from POSUPD00.
        
        Args:
            quantity: Number of units to sell
            price: Price per unit
            user: User ID performing the transaction
            timestamp: Transaction timestamp (defaults to now)
            
        Returns:
            Tuple of (updated PositionRecord, realized gain/loss)
            
        Raises:
            ValueError: If selling more than available quantity
        """
        if timestamp is None:
            timestamp = datetime.now()
        
        if quantity > self.data.quantity:
            raise ValueError(
                f"Insufficient position balance: trying to sell {quantity} "
                f"but only {self.data.quantity} available"
            )
        
        # Calculate proportional cost basis reduction
        if self.data.quantity > 0:
            cost_per_unit = self.data.cost_basis / self.data.quantity
        else:
            cost_per_unit = Decimal('0')
        
        cost_basis_reduction = cost_per_unit * quantity
        sale_proceeds = quantity * price
        realized_gain_loss = sale_proceeds - cost_basis_reduction
        
        # Update position data
        self.data.quantity = self.data.quantity - quantity
        self.data.cost_basis = self.data.cost_basis - cost_basis_reduction
        
        # Close position if quantity is zero
        if self.data.quantity == 0:
            self.data.status = PositionStatus.CLOSED
        
        # Update audit
        self.audit = PositionAudit(
            last_maint_date=timestamp,
            last_maint_user=user
        )
        
        return self, realized_gain_loss.quantize(Decimal('0.01'), rounding=ROUND_HALF_UP)
    
    def update_market_value(self, new_price: Decimal, 
                            user: str, timestamp: Optional[datetime] = None) -> 'PositionRecord':
        """
        Update the market value based on a new price.
        
        Args:
            new_price: New price per unit
            user: User ID performing the update
            timestamp: Update timestamp (defaults to now)
            
        Returns:
            Updated PositionRecord
        """
        if timestamp is None:
            timestamp = datetime.now()
        
        self.data.market_value = (self.data.quantity * new_price).quantize(
            Decimal('0.01'), rounding=ROUND_HALF_UP)
        
        self.audit = PositionAudit(
            last_maint_date=timestamp,
            last_maint_user=user
        )
        
        return self
    
    @classmethod
    def from_dict(cls, d: dict) -> 'PositionRecord':
        """Create PositionRecord from dictionary."""
        key = PositionKey(
            portfolio_id=d['portfolio_id'],
            date=d['date'],
            investment_id=d['investment_id']
        )
        data = PositionData(
            quantity=Decimal(str(d['quantity'])),
            cost_basis=Decimal(str(d['cost_basis'])),
            market_value=Decimal(str(d['market_value'])),
            currency=d.get('currency', 'USD'),
            status=PositionStatus(d.get('status', 'A'))
        )
        audit = None
        if 'last_maint_date' in d and d['last_maint_date']:
            audit = PositionAudit(
                last_maint_date=datetime.fromisoformat(d['last_maint_date'])
                    if isinstance(d['last_maint_date'], str) else d['last_maint_date'],
                last_maint_user=d.get('last_maint_user', '')
            )
        return cls(key=key, data=data, audit=audit)
    
    def to_dict(self) -> dict:
        """Convert PositionRecord to dictionary for serialization."""
        result = {
            'portfolio_id': self.key.portfolio_id.strip(),
            'date': self.key.date,
            'investment_id': self.key.investment_id.strip(),
            'quantity': str(self.data.quantity),
            'cost_basis': str(self.data.cost_basis),
            'market_value': str(self.data.market_value),
            'currency': self.data.currency,
            'status': self.data.status.value,
        }
        if self.audit:
            result['last_maint_date'] = self.audit.last_maint_date.isoformat()
            result['last_maint_user'] = self.audit.last_maint_user.strip()
        return result
    
    @classmethod
    def create_new(cls, portfolio_id: str, investment_id: str,
                   quantity: Decimal, price: Decimal, user: str,
                   date: Optional[str] = None) -> 'PositionRecord':
        """
        Create a new position record.
        
        Args:
            portfolio_id: Portfolio identifier
            investment_id: Investment/security identifier
            quantity: Initial quantity
            price: Purchase price per unit
            user: User creating the position
            date: Position date (defaults to today)
            
        Returns:
            New PositionRecord
        """
        if date is None:
            date = datetime.now().strftime("%Y%m%d")
        
        cost_basis = (quantity * price).quantize(Decimal('0.01'), rounding=ROUND_HALF_UP)
        market_value = cost_basis  # Initially, market value equals cost basis
        
        key = PositionKey(
            portfolio_id=portfolio_id,
            date=date,
            investment_id=investment_id
        )
        data = PositionData(
            quantity=quantity,
            cost_basis=cost_basis,
            market_value=market_value,
            currency='USD',
            status=PositionStatus.ACTIVE
        )
        audit = PositionAudit(
            last_maint_date=datetime.now(),
            last_maint_user=user
        )
        
        return cls(key=key, data=data, audit=audit)

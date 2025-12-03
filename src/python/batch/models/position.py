"""
Position Record Model

Corresponds to COBOL copybook: POSREC.cpy
Defines the structure for portfolio positions in the portfolio management system.
"""

from dataclasses import dataclass, field
from datetime import datetime
from decimal import Decimal
from enum import Enum
from typing import Optional


class PositionStatus(Enum):
    """Position status codes matching COBOL 88-level conditions."""
    ACTIVE = "A"
    CLOSED = "C"
    PENDING = "P"


@dataclass
class PositionKey:
    """
    Position key structure.
    
    Corresponds to POS-KEY in POSREC.cpy:
    - POS-PORTFOLIO-ID: PIC X(08)
    - POS-DATE: PIC X(08) - YYYYMMDD
    - POS-INVESTMENT-ID: PIC X(10)
    """
    portfolio_id: str
    date: str  # YYYYMMDD format
    investment_id: str

    def __post_init__(self) -> None:
        self.portfolio_id = str(self.portfolio_id).ljust(8)[:8]
        self.date = str(self.date).ljust(8)[:8]
        self.investment_id = str(self.investment_id).ljust(10)[:10]

    def to_string(self) -> str:
        """Convert key to string for comparison and storage."""
        return f"{self.portfolio_id}{self.date}{self.investment_id}"

    @classmethod
    def from_string(cls, key_string: str) -> "PositionKey":
        """Parse key from string representation."""
        return cls(
            portfolio_id=key_string[0:8],
            date=key_string[8:16],
            investment_id=key_string[16:26],
        )


@dataclass
class PositionData:
    """
    Position data structure.
    
    Corresponds to POS-DATA in POSREC.cpy:
    - POS-QUANTITY: PIC S9(11)V9(4) COMP-3
    - POS-COST-BASIS: PIC S9(13)V9(2) COMP-3
    - POS-MARKET-VALUE: PIC S9(13)V9(2) COMP-3
    - POS-CURRENCY: PIC X(03)
    - POS-STATUS: PIC X(01)
    """
    quantity: Decimal
    cost_basis: Decimal
    market_value: Decimal
    currency: str = "USD"
    status: PositionStatus = PositionStatus.ACTIVE

    def __post_init__(self) -> None:
        self.currency = str(self.currency).ljust(3)[:3]
        if isinstance(self.status, str):
            self.status = PositionStatus(self.status)
        self.quantity = Decimal(str(self.quantity))
        self.cost_basis = Decimal(str(self.cost_basis))
        self.market_value = Decimal(str(self.market_value))


@dataclass
class PositionAudit:
    """
    Position audit structure.
    
    Corresponds to POS-AUDIT in POSREC.cpy:
    - POS-LAST-MAINT-DATE: PIC X(26)
    - POS-LAST-MAINT-USER: PIC X(08)
    """
    last_maint_date: str = ""
    last_maint_user: str = ""

    def __post_init__(self) -> None:
        if not self.last_maint_date:
            self.last_maint_date = datetime.now().isoformat()
        self.last_maint_date = str(self.last_maint_date).ljust(26)[:26]
        self.last_maint_user = str(self.last_maint_user).ljust(8)[:8]


@dataclass
class PositionRecord:
    """
    Complete position record structure.
    
    Corresponds to POSITION-RECORD in POSREC.cpy.
    Total record length matches COBOL definition.
    """
    key: PositionKey
    data: PositionData
    audit: PositionAudit = field(default_factory=PositionAudit)
    filler: str = ""

    def __post_init__(self) -> None:
        self.filler = " " * 50

    @property
    def portfolio_id(self) -> str:
        return self.key.portfolio_id

    @property
    def date(self) -> str:
        return self.key.date

    @property
    def investment_id(self) -> str:
        return self.key.investment_id

    @property
    def quantity(self) -> Decimal:
        return self.data.quantity

    @quantity.setter
    def quantity(self, value: Decimal) -> None:
        self.data.quantity = Decimal(str(value))

    @property
    def cost_basis(self) -> Decimal:
        return self.data.cost_basis

    @cost_basis.setter
    def cost_basis(self, value: Decimal) -> None:
        self.data.cost_basis = Decimal(str(value))

    @property
    def market_value(self) -> Decimal:
        return self.data.market_value

    @market_value.setter
    def market_value(self, value: Decimal) -> None:
        self.data.market_value = Decimal(str(value))

    @property
    def currency(self) -> str:
        return self.data.currency

    @property
    def status(self) -> PositionStatus:
        return self.data.status

    @status.setter
    def status(self, value: PositionStatus) -> None:
        self.data.status = value

    def is_active(self) -> bool:
        return self.data.status == PositionStatus.ACTIVE

    def is_closed(self) -> bool:
        return self.data.status == PositionStatus.CLOSED

    def is_pending(self) -> bool:
        return self.data.status == PositionStatus.PENDING

    def update_for_buy(
        self, quantity: Decimal, price: Decimal, user: str = "SYSTEM"
    ) -> None:
        """Update position for a buy transaction."""
        new_cost = quantity * price
        self.data.quantity += quantity
        self.data.cost_basis += new_cost
        self.audit.last_maint_date = datetime.now().isoformat()
        self.audit.last_maint_user = user

    def update_for_sell(
        self, quantity: Decimal, price: Decimal, user: str = "SYSTEM"
    ) -> Decimal:
        """
        Update position for a sell transaction.
        Returns the gain/loss from the sale.
        """
        if quantity > self.data.quantity:
            raise ValueError("Insufficient position balance for sell")
        
        avg_cost = (
            self.data.cost_basis / self.data.quantity
            if self.data.quantity > 0
            else Decimal("0")
        )
        cost_of_sold = quantity * avg_cost
        proceeds = quantity * price
        gain_loss = proceeds - cost_of_sold
        
        self.data.quantity -= quantity
        self.data.cost_basis -= cost_of_sold
        
        if self.data.quantity == 0:
            self.data.status = PositionStatus.CLOSED
        
        self.audit.last_maint_date = datetime.now().isoformat()
        self.audit.last_maint_user = user
        
        return gain_loss

    def close_position(self, user: str = "SYSTEM") -> None:
        """Close the position."""
        self.data.status = PositionStatus.CLOSED
        self.audit.last_maint_date = datetime.now().isoformat()
        self.audit.last_maint_user = user

    def to_dict(self) -> dict:
        """Convert record to dictionary for serialization."""
        return {
            "key": {
                "portfolio_id": self.key.portfolio_id,
                "date": self.key.date,
                "investment_id": self.key.investment_id,
            },
            "data": {
                "quantity": str(self.data.quantity),
                "cost_basis": str(self.data.cost_basis),
                "market_value": str(self.data.market_value),
                "currency": self.data.currency,
                "status": self.data.status.value,
            },
            "audit": {
                "last_maint_date": self.audit.last_maint_date,
                "last_maint_user": self.audit.last_maint_user,
            },
        }

    @classmethod
    def from_dict(cls, data: dict) -> "PositionRecord":
        """Create record from dictionary."""
        return cls(
            key=PositionKey(
                portfolio_id=data["key"]["portfolio_id"],
                date=data["key"]["date"],
                investment_id=data["key"]["investment_id"],
            ),
            data=PositionData(
                quantity=Decimal(data["data"]["quantity"]),
                cost_basis=Decimal(data["data"]["cost_basis"]),
                market_value=Decimal(data["data"]["market_value"]),
                currency=data["data"]["currency"],
                status=PositionStatus(data["data"]["status"]),
            ),
            audit=PositionAudit(
                last_maint_date=data["audit"]["last_maint_date"],
                last_maint_user=data["audit"]["last_maint_user"],
            ),
        )

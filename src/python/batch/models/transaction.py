"""
Transaction Record Model

Corresponds to COBOL copybook: TRNREC.cpy
Defines the structure for financial transactions in the portfolio management system.
"""

from dataclasses import dataclass, field
from datetime import datetime
from decimal import Decimal
from enum import Enum
from typing import Optional


class TransactionType(Enum):
    """Transaction type codes matching COBOL 88-level conditions."""
    BUY = "BU"
    SELL = "SL"
    TRANSFER = "TR"
    FEE = "FE"


class TransactionStatus(Enum):
    """Transaction status codes matching COBOL 88-level conditions."""
    PENDING = "P"
    DONE = "D"
    FAILED = "F"
    REVERSED = "R"


@dataclass
class TransactionKey:
    """
    Transaction key structure.
    
    Corresponds to TRN-KEY in TRNREC.cpy:
    - TRN-DATE: PIC X(08) - YYYYMMDD
    - TRN-TIME: PIC X(06) - HHMMSS
    - TRN-PORTFOLIO-ID: PIC X(08)
    - TRN-SEQUENCE-NO: PIC X(06)
    """
    date: str  # YYYYMMDD format
    time: str  # HHMMSS format
    portfolio_id: str
    sequence_no: str

    def __post_init__(self) -> None:
        self.date = str(self.date).ljust(8)[:8]
        self.time = str(self.time).ljust(6)[:6]
        self.portfolio_id = str(self.portfolio_id).ljust(8)[:8]
        self.sequence_no = str(self.sequence_no).ljust(6)[:6]

    def to_string(self) -> str:
        """Convert key to string for comparison and storage."""
        return f"{self.date}{self.time}{self.portfolio_id}{self.sequence_no}"

    @classmethod
    def from_string(cls, key_string: str) -> "TransactionKey":
        """Parse key from string representation."""
        return cls(
            date=key_string[0:8],
            time=key_string[8:14],
            portfolio_id=key_string[14:22],
            sequence_no=key_string[22:28],
        )


@dataclass
class TransactionData:
    """
    Transaction data structure.
    
    Corresponds to TRN-DATA in TRNREC.cpy:
    - TRN-INVESTMENT-ID: PIC X(10)
    - TRN-TYPE: PIC X(02)
    - TRN-QUANTITY: PIC S9(11)V9(4) COMP-3
    - TRN-PRICE: PIC S9(11)V9(4) COMP-3
    - TRN-AMOUNT: PIC S9(13)V9(2) COMP-3
    - TRN-CURRENCY: PIC X(03)
    - TRN-STATUS: PIC X(01)
    """
    investment_id: str
    transaction_type: TransactionType
    quantity: Decimal
    price: Decimal
    amount: Decimal
    currency: str = "USD"
    status: TransactionStatus = TransactionStatus.PENDING

    def __post_init__(self) -> None:
        self.investment_id = str(self.investment_id).ljust(10)[:10]
        self.currency = str(self.currency).ljust(3)[:3]
        if isinstance(self.transaction_type, str):
            self.transaction_type = TransactionType(self.transaction_type)
        if isinstance(self.status, str):
            self.status = TransactionStatus(self.status)
        self.quantity = Decimal(str(self.quantity))
        self.price = Decimal(str(self.price))
        self.amount = Decimal(str(self.amount))


@dataclass
class TransactionAudit:
    """
    Transaction audit structure.
    
    Corresponds to TRN-AUDIT in TRNREC.cpy:
    - TRN-PROCESS-DATE: PIC X(26)
    - TRN-PROCESS-USER: PIC X(08)
    """
    process_date: str = ""
    process_user: str = ""

    def __post_init__(self) -> None:
        if not self.process_date:
            self.process_date = datetime.now().isoformat()
        self.process_date = str(self.process_date).ljust(26)[:26]
        self.process_user = str(self.process_user).ljust(8)[:8]


@dataclass
class TransactionRecord:
    """
    Complete transaction record structure.
    
    Corresponds to TRANSACTION-RECORD in TRNREC.cpy.
    Total record length matches COBOL definition.
    """
    key: TransactionKey
    data: TransactionData
    audit: TransactionAudit = field(default_factory=TransactionAudit)
    filler: str = ""

    def __post_init__(self) -> None:
        self.filler = " " * 50

    @property
    def date(self) -> str:
        return self.key.date

    @property
    def time(self) -> str:
        return self.key.time

    @property
    def portfolio_id(self) -> str:
        return self.key.portfolio_id

    @property
    def sequence_no(self) -> str:
        return self.key.sequence_no

    @property
    def investment_id(self) -> str:
        return self.data.investment_id

    @property
    def transaction_type(self) -> TransactionType:
        return self.data.transaction_type

    @property
    def quantity(self) -> Decimal:
        return self.data.quantity

    @property
    def price(self) -> Decimal:
        return self.data.price

    @property
    def amount(self) -> Decimal:
        return self.data.amount

    @property
    def currency(self) -> str:
        return self.data.currency

    @property
    def status(self) -> TransactionStatus:
        return self.data.status

    @status.setter
    def status(self, value: TransactionStatus) -> None:
        self.data.status = value

    def is_buy(self) -> bool:
        return self.data.transaction_type == TransactionType.BUY

    def is_sell(self) -> bool:
        return self.data.transaction_type == TransactionType.SELL

    def is_transfer(self) -> bool:
        return self.data.transaction_type == TransactionType.TRANSFER

    def is_fee(self) -> bool:
        return self.data.transaction_type == TransactionType.FEE

    def is_pending(self) -> bool:
        return self.data.status == TransactionStatus.PENDING

    def is_done(self) -> bool:
        return self.data.status == TransactionStatus.DONE

    def is_failed(self) -> bool:
        return self.data.status == TransactionStatus.FAILED

    def mark_done(self, user: str = "SYSTEM") -> None:
        self.data.status = TransactionStatus.DONE
        self.audit.process_date = datetime.now().isoformat()
        self.audit.process_user = user

    def mark_failed(self, user: str = "SYSTEM") -> None:
        self.data.status = TransactionStatus.FAILED
        self.audit.process_date = datetime.now().isoformat()
        self.audit.process_user = user

    def to_dict(self) -> dict:
        """Convert record to dictionary for serialization."""
        return {
            "key": {
                "date": self.key.date,
                "time": self.key.time,
                "portfolio_id": self.key.portfolio_id,
                "sequence_no": self.key.sequence_no,
            },
            "data": {
                "investment_id": self.data.investment_id,
                "transaction_type": self.data.transaction_type.value,
                "quantity": str(self.data.quantity),
                "price": str(self.data.price),
                "amount": str(self.data.amount),
                "currency": self.data.currency,
                "status": self.data.status.value,
            },
            "audit": {
                "process_date": self.audit.process_date,
                "process_user": self.audit.process_user,
            },
        }

    @classmethod
    def from_dict(cls, data: dict) -> "TransactionRecord":
        """Create record from dictionary."""
        return cls(
            key=TransactionKey(
                date=data["key"]["date"],
                time=data["key"]["time"],
                portfolio_id=data["key"]["portfolio_id"],
                sequence_no=data["key"]["sequence_no"],
            ),
            data=TransactionData(
                investment_id=data["data"]["investment_id"],
                transaction_type=TransactionType(data["data"]["transaction_type"]),
                quantity=Decimal(data["data"]["quantity"]),
                price=Decimal(data["data"]["price"]),
                amount=Decimal(data["data"]["amount"]),
                currency=data["data"]["currency"],
                status=TransactionStatus(data["data"]["status"]),
            ),
            audit=TransactionAudit(
                process_date=data["audit"]["process_date"],
                process_user=data["audit"]["process_user"],
            ),
        )

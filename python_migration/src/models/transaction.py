"""
Transaction model - Maps to COBOL TRNREC.cpy
Represents financial transaction records.
"""

from dataclasses import dataclass, field
from decimal import Decimal
from datetime import datetime
from typing import Optional
from enum import Enum

from sqlalchemy import Column, Integer, String, Numeric, DateTime, Index
from sqlalchemy.ext.declarative import declarative_base

Base = declarative_base()


class TransactionType(str, Enum):
    """Transaction type codes - maps to TRN-TYPE 88 levels"""
    BUY = 'BU'
    SELL = 'SL'
    TRANSFER = 'TR'
    FEE = 'FE'


class TransactionStatus(str, Enum):
    """Transaction status codes - maps to TRN-STATUS 88 levels"""
    PENDING = 'P'
    DONE = 'D'
    FAILED = 'F'
    REVERSED = 'R'


@dataclass
class TransactionRecord:
    """
    Transaction record dataclass - maps to COBOL TRNREC.cpy
    
    Original COBOL structure:
    01  TRANSACTION-RECORD.
        05  TRN-KEY.
            10  TRN-DATE           PIC X(08).
            10  TRN-TIME           PIC X(06).
            10  TRN-PORTFOLIO-ID   PIC X(08).
            10  TRN-SEQUENCE-NO    PIC X(06).
        05  TRN-DATA.
            10  TRN-INVESTMENT-ID  PIC X(10).
            10  TRN-TYPE           PIC X(02).
            10  TRN-QUANTITY       PIC S9(11)V9(4) COMP-3.
            10  TRN-PRICE          PIC S9(11)V9(4) COMP-3.
            10  TRN-AMOUNT         PIC S9(13)V9(2) COMP-3.
            10  TRN-CURRENCY       PIC X(03).
            10  TRN-STATUS         PIC X(01).
        05  TRN-AUDIT.
            10  TRN-PROCESS-DATE   PIC X(26).
            10  TRN-PROCESS-USER   PIC X(08).
    """
    date: str
    time: str
    portfolio_id: str
    sequence_no: str
    investment_id: str
    transaction_type: TransactionType
    quantity: Decimal = field(default_factory=lambda: Decimal('0'))
    price: Decimal = field(default_factory=lambda: Decimal('0'))
    amount: Decimal = field(default_factory=lambda: Decimal('0'))
    currency: str = 'USD'
    status: TransactionStatus = TransactionStatus.PENDING
    process_date: Optional[datetime] = None
    process_user: Optional[str] = None

    def __post_init__(self):
        """Ensure proper types after initialization"""
        if isinstance(self.quantity, (int, float, str)):
            self.quantity = Decimal(str(self.quantity))
        if isinstance(self.price, (int, float, str)):
            self.price = Decimal(str(self.price))
        if isinstance(self.amount, (int, float, str)):
            self.amount = Decimal(str(self.amount))
        if isinstance(self.transaction_type, str):
            self.transaction_type = TransactionType(self.transaction_type)
        if isinstance(self.status, str):
            self.status = TransactionStatus(self.status)

    @property
    def key(self) -> str:
        """Return composite key matching COBOL TRN-KEY"""
        return f"{self.date}{self.time}{self.portfolio_id}{self.sequence_no}"

    @property
    def transaction_id(self) -> str:
        """Generate unique transaction ID"""
        return f"{self.date}{self.sequence_no}"

    @property
    def calculated_amount(self) -> Decimal:
        """Calculate transaction amount from quantity and price"""
        return self.quantity * self.price

    def validate(self) -> tuple[bool, list[str]]:
        """
        Validate transaction record fields.
        Implements validation rules from COBOL TRNVAL00.
        Returns (is_valid, list of error messages)
        """
        errors = []
        
        # E001: Date validation
        if not self.date or len(self.date) != 8 or not self.date.isdigit():
            errors.append("E001: Invalid transaction date (must be YYYYMMDD)")
        else:
            # Check date is not in future
            try:
                trans_date = datetime.strptime(self.date, '%Y%m%d')
                if trans_date > datetime.now():
                    errors.append("E001: Transaction date cannot be in the future")
            except ValueError:
                errors.append("E001: Invalid date format")
        
        # E002: Time validation
        if not self.time or len(self.time) != 6 or not self.time.isdigit():
            errors.append("E002: Invalid transaction time (must be HHMMSS)")
        
        # E003: Portfolio ID validation
        if not self.portfolio_id or len(self.portfolio_id) > 8:
            errors.append("E003: Invalid portfolio ID (must be 1-8 characters)")
        
        # E004: Investment ID validation
        if not self.investment_id or len(self.investment_id) > 10:
            errors.append("E004: Invalid investment ID (must be 1-10 characters)")
        
        # E005: Quantity validation for BUY/SELL
        if self.transaction_type in [TransactionType.BUY, TransactionType.SELL]:
            if self.quantity == 0:
                errors.append("E005: Quantity cannot be zero for BUY/SELL transactions")
        
        # E006: Price validation for BUY/SELL
        if self.transaction_type in [TransactionType.BUY, TransactionType.SELL]:
            if self.price <= 0:
                errors.append("E006: Price must be greater than zero for BUY/SELL transactions")
        
        # E007: Amount validation for FEE
        if self.transaction_type == TransactionType.FEE:
            if self.amount == 0:
                errors.append("E007: Amount cannot be zero for FEE transactions")
        
        # E008: Currency validation
        if len(self.currency) != 3:
            errors.append("E008: Invalid currency code (must be 3 characters)")
        
        # W001: Zero dollar transaction warning (not an error)
        if self.amount == 0 and self.transaction_type != TransactionType.FEE:
            pass  # This is a warning, not an error
        
        return len(errors) == 0, errors

    def to_dict(self) -> dict:
        """Convert to dictionary for serialization"""
        return {
            'date': self.date,
            'time': self.time,
            'portfolio_id': self.portfolio_id,
            'sequence_no': self.sequence_no,
            'investment_id': self.investment_id,
            'transaction_type': self.transaction_type.value,
            'quantity': str(self.quantity),
            'price': str(self.price),
            'amount': str(self.amount),
            'currency': self.currency,
            'status': self.status.value,
            'process_date': self.process_date.isoformat() if self.process_date else None,
            'process_user': self.process_user,
        }

    @classmethod
    def from_dict(cls, data: dict) -> 'TransactionRecord':
        """Create from dictionary"""
        return cls(
            date=data['date'],
            time=data['time'],
            portfolio_id=data['portfolio_id'],
            sequence_no=data['sequence_no'],
            investment_id=data['investment_id'],
            transaction_type=TransactionType(data['transaction_type']),
            quantity=Decimal(data.get('quantity', '0')),
            price=Decimal(data.get('price', '0')),
            amount=Decimal(data.get('amount', '0')),
            currency=data.get('currency', 'USD'),
            status=TransactionStatus(data.get('status', 'P')),
            process_date=datetime.fromisoformat(data['process_date']) if data.get('process_date') else None,
            process_user=data.get('process_user'),
        )


class Transaction(Base):
    """SQLAlchemy model for transactions table - replaces VSAM TRANHIST"""
    __tablename__ = 'transactions'

    id = Column(Integer, primary_key=True, autoincrement=True)
    date = Column(String(8), nullable=False)
    time = Column(String(6), nullable=False)
    portfolio_id = Column(String(8), nullable=False, index=True)
    sequence_no = Column(String(6), nullable=False)
    investment_id = Column(String(10), nullable=False)
    transaction_type = Column(String(2), nullable=False)
    quantity = Column(Numeric(15, 4), nullable=False, default=0)
    price = Column(Numeric(15, 4), nullable=False, default=0)
    amount = Column(Numeric(15, 2), nullable=False, default=0)
    currency = Column(String(3), nullable=False, default='USD')
    status = Column(String(1), nullable=False, default='P')
    process_date = Column(DateTime, nullable=True)
    process_user = Column(String(8), nullable=True)

    __table_args__ = (
        Index('ix_transaction_date', 'date'),
        Index('ix_transaction_portfolio', 'portfolio_id'),
        Index('ix_transaction_status', 'status'),
    )

    def to_record(self) -> TransactionRecord:
        """Convert to TransactionRecord dataclass"""
        return TransactionRecord(
            date=self.date,
            time=self.time,
            portfolio_id=self.portfolio_id,
            sequence_no=self.sequence_no,
            investment_id=self.investment_id,
            transaction_type=TransactionType(self.transaction_type),
            quantity=Decimal(str(self.quantity)) if self.quantity else Decimal('0'),
            price=Decimal(str(self.price)) if self.price else Decimal('0'),
            amount=Decimal(str(self.amount)) if self.amount else Decimal('0'),
            currency=self.currency,
            status=TransactionStatus(self.status),
            process_date=self.process_date,
            process_user=self.process_user,
        )

    @classmethod
    def from_record(cls, record: TransactionRecord) -> 'Transaction':
        """Create from TransactionRecord dataclass"""
        return cls(
            date=record.date,
            time=record.time,
            portfolio_id=record.portfolio_id,
            sequence_no=record.sequence_no,
            investment_id=record.investment_id,
            transaction_type=record.transaction_type.value,
            quantity=record.quantity,
            price=record.price,
            amount=record.amount,
            currency=record.currency,
            status=record.status.value,
            process_date=record.process_date,
            process_user=record.process_user,
        )

    def __repr__(self):
        return f"<Transaction(date={self.date}, portfolio={self.portfolio_id}, type={self.transaction_type})>"

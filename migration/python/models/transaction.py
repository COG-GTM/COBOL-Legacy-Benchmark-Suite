"""
Transaction data models - Python translation of TRNREC.cpy

This module contains dataclasses that correspond to the COBOL copybook
TRNREC.cpy, which defines the transaction record structure used throughout
the Investment Portfolio Management System.

Original COBOL Structure:
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

from dataclasses import dataclass, field
from decimal import Decimal
from datetime import datetime, date
from typing import Optional, List
from enum import Enum


class TransactionType(str, Enum):
    """
    Transaction type codes corresponding to TRN-TYPE 88-level conditions.
    
    88  TRN-TYPE-BUY     VALUE 'BU'.
    88  TRN-TYPE-SELL    VALUE 'SL'.
    88  TRN-TYPE-TRANS   VALUE 'TR'.
    88  TRN-TYPE-FEE     VALUE 'FE'.
    """
    BUY = 'BU'
    SELL = 'SL'
    TRANSFER = 'TR'
    FEE = 'FE'
    
    @classmethod
    def is_valid(cls, value: str) -> bool:
        """Check if a value is a valid transaction type."""
        return value in [t.value for t in cls]


class TransactionStatus(str, Enum):
    """
    Transaction status codes corresponding to TRN-STATUS 88-level conditions.
    
    88  TRN-STATUS-PEND   VALUE 'P'.
    88  TRN-STATUS-DONE   VALUE 'D'.
    88  TRN-STATUS-FAIL   VALUE 'F'.
    88  TRN-STATUS-REV    VALUE 'R'.
    """
    PENDING = 'P'
    DONE = 'D'
    FAILED = 'F'
    REVERSED = 'R'


@dataclass
class TransactionKey:
    """
    Transaction key structure corresponding to TRN-KEY in TRNREC.cpy.
    
    This composite key uniquely identifies each transaction and is used
    for VSAM KSDS file access.
    
    Attributes:
        date: Transaction date in YYYYMMDD format (8 bytes)
        time: Transaction time in HHMMSS format (6 bytes)
        portfolio_id: Portfolio identifier (8 bytes)
        sequence_no: Sequence number for multiple transactions (6 bytes)
    """
    date: str  # YYYYMMDD format
    time: str  # HHMMSS format
    portfolio_id: str
    sequence_no: str
    
    def __post_init__(self):
        """Validate key field lengths matching COBOL PIC definitions."""
        if len(self.date) != 8:
            raise ValueError(f"Date must be 8 characters (YYYYMMDD), got {len(self.date)}")
        if len(self.time) != 6:
            raise ValueError(f"Time must be 6 characters (HHMMSS), got {len(self.time)}")
        if len(self.portfolio_id) > 8:
            raise ValueError(f"Portfolio ID max 8 characters, got {len(self.portfolio_id)}")
        if len(self.sequence_no) > 6:
            raise ValueError(f"Sequence number max 6 characters, got {len(self.sequence_no)}")
        
        # Pad fields to match COBOL fixed-length format
        self.portfolio_id = self.portfolio_id.ljust(8)
        self.sequence_no = self.sequence_no.ljust(6)
    
    @property
    def composite_key(self) -> str:
        """Return the full composite key as a single string."""
        return f"{self.date}{self.time}{self.portfolio_id}{self.sequence_no}"
    
    @property
    def as_datetime(self) -> datetime:
        """Convert date and time to Python datetime object."""
        return datetime.strptime(f"{self.date}{self.time}", "%Y%m%d%H%M%S")
    
    @classmethod
    def from_datetime(cls, dt: datetime, portfolio_id: str, sequence_no: str) -> 'TransactionKey':
        """Create a TransactionKey from a datetime object."""
        return cls(
            date=dt.strftime("%Y%m%d"),
            time=dt.strftime("%H%M%S"),
            portfolio_id=portfolio_id,
            sequence_no=sequence_no
        )


@dataclass
class TransactionData:
    """
    Transaction data structure corresponding to TRN-DATA in TRNREC.cpy.
    
    Contains the core transaction information including investment details,
    quantities, prices, and amounts.
    
    Attributes:
        investment_id: Investment/security identifier (10 bytes)
        trans_type: Transaction type (BU, SL, TR, FE)
        quantity: Number of units (S9(11)V9(4) COMP-3 -> Decimal with 4 decimal places)
        price: Price per unit (S9(11)V9(4) COMP-3 -> Decimal with 4 decimal places)
        amount: Total amount (S9(13)V9(2) COMP-3 -> Decimal with 2 decimal places)
        currency: Currency code (3 bytes, default USD)
        status: Transaction status (P, D, F, R)
    """
    investment_id: str
    trans_type: TransactionType
    quantity: Decimal
    price: Decimal
    amount: Decimal
    currency: str = 'USD'
    status: TransactionStatus = TransactionStatus.PENDING
    
    def __post_init__(self):
        """Validate and normalize data fields."""
        if len(self.investment_id) > 10:
            raise ValueError(f"Investment ID max 10 characters, got {len(self.investment_id)}")
        if len(self.currency) != 3:
            raise ValueError(f"Currency must be 3 characters, got {len(self.currency)}")
        
        # Ensure Decimal types for financial precision
        if not isinstance(self.quantity, Decimal):
            self.quantity = Decimal(str(self.quantity))
        if not isinstance(self.price, Decimal):
            self.price = Decimal(str(self.price))
        if not isinstance(self.amount, Decimal):
            self.amount = Decimal(str(self.amount))
        
        # Pad investment_id to match COBOL fixed-length format
        self.investment_id = self.investment_id.ljust(10)


@dataclass
class TransactionAudit:
    """
    Transaction audit structure corresponding to TRN-AUDIT in TRNREC.cpy.
    
    Contains audit trail information for tracking when and by whom
    the transaction was processed.
    
    Attributes:
        process_date: Processing timestamp (26 bytes in COBOL)
        process_user: User ID who processed the transaction (8 bytes)
    """
    process_date: datetime
    process_user: str
    
    def __post_init__(self):
        """Validate audit fields."""
        if len(self.process_user) > 8:
            raise ValueError(f"Process user max 8 characters, got {len(self.process_user)}")
        self.process_user = self.process_user.ljust(8)


@dataclass
class TransactionRecord:
    """
    Complete transaction record corresponding to TRANSACTION-RECORD in TRNREC.cpy.
    
    This is the main data structure used for transaction processing throughout
    the system. It combines the key, data, and audit sections.
    
    COBOL COMP-3 (packed decimal) fields are translated to Python Decimal
    for precise financial calculations.
    """
    key: TransactionKey
    data: TransactionData
    audit: Optional[TransactionAudit] = None
    
    @classmethod
    def from_dict(cls, d: dict) -> 'TransactionRecord':
        """
        Create TransactionRecord from dictionary.
        
        Useful for loading from JSON, database results, or API responses.
        """
        key = TransactionKey(
            date=d['date'],
            time=d['time'],
            portfolio_id=d['portfolio_id'],
            sequence_no=d['sequence_no']
        )
        data = TransactionData(
            investment_id=d['investment_id'],
            trans_type=TransactionType(d['trans_type']),
            quantity=Decimal(str(d['quantity'])),
            price=Decimal(str(d['price'])),
            amount=Decimal(str(d['amount'])),
            currency=d.get('currency', 'USD'),
            status=TransactionStatus(d.get('status', 'P'))
        )
        audit = None
        if 'process_date' in d and d['process_date']:
            audit = TransactionAudit(
                process_date=datetime.fromisoformat(d['process_date']) 
                    if isinstance(d['process_date'], str) else d['process_date'],
                process_user=d.get('process_user', '')
            )
        return cls(key=key, data=data, audit=audit)
    
    def to_dict(self) -> dict:
        """Convert TransactionRecord to dictionary for serialization."""
        result = {
            'date': self.key.date,
            'time': self.key.time,
            'portfolio_id': self.key.portfolio_id.strip(),
            'sequence_no': self.key.sequence_no.strip(),
            'investment_id': self.data.investment_id.strip(),
            'trans_type': self.data.trans_type.value,
            'quantity': str(self.data.quantity),
            'price': str(self.data.price),
            'amount': str(self.data.amount),
            'currency': self.data.currency,
            'status': self.data.status.value,
        }
        if self.audit:
            result['process_date'] = self.audit.process_date.isoformat()
            result['process_user'] = self.audit.process_user.strip()
        return result
    
    @property
    def calculated_amount(self) -> Decimal:
        """Calculate amount from quantity and price."""
        return self.data.quantity * self.data.price


@dataclass
class ValidationError:
    """
    Represents a validation error found during transaction validation.
    
    Corresponds to error codes defined in the data dictionary:
    - E001: Invalid Account Number
    - E002: Invalid Fund ID
    - E003: Invalid Transaction Type
    - E004: Insufficient Position Balance
    - W001: Zero Dollar Transaction (Warning)
    - W002: Duplicate Transaction ID (Warning)
    """
    code: str
    message: str
    field: str
    severity: str = 'ERROR'  # ERROR or WARNING
    
    @property
    def is_error(self) -> bool:
        """Check if this is an error (vs warning)."""
        return self.severity == 'ERROR'
    
    @property
    def is_warning(self) -> bool:
        """Check if this is a warning."""
        return self.severity == 'WARNING'


@dataclass
class ValidatedTransaction:
    """
    Result of transaction validation.
    
    Contains the original transaction record along with validation results.
    This is the output of the TransactionValidator class.
    """
    transaction: TransactionRecord
    is_valid: bool
    errors: List[ValidationError] = field(default_factory=list)
    warnings: List[ValidationError] = field(default_factory=list)
    
    @property
    def has_errors(self) -> bool:
        """Check if there are any validation errors."""
        return len(self.errors) > 0
    
    @property
    def has_warnings(self) -> bool:
        """Check if there are any validation warnings."""
        return len(self.warnings) > 0
    
    def add_error(self, code: str, message: str, field: str):
        """Add a validation error."""
        self.errors.append(ValidationError(code, message, field, 'ERROR'))
        self.is_valid = False
    
    def add_warning(self, code: str, message: str, field: str):
        """Add a validation warning."""
        self.warnings.append(ValidationError(code, message, field, 'WARNING'))

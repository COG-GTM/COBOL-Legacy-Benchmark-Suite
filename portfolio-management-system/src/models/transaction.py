"""
Transaction Data Model

Migrated from COBOL copybook: src/copybook/common/TRNREC.cpy

Original COBOL structure:
- TRN-KEY: Composite key (DATE + TIME + PORTFOLIO-ID + SEQUENCE-NO)
- TRN-DATA: Transaction details (investment, type, quantity, price, amount, currency, status)
- TRN-AUDIT: Audit trail (process date, user)

COBOL Data Types Mapping:
- PIC X(n) -> str
- PIC S9(11)V9(4) COMP-3 -> Decimal (packed decimal)
- PIC S9(13)V9(2) COMP-3 -> Decimal (packed decimal)
"""

from datetime import datetime, date, time
from decimal import Decimal
from enum import Enum
from typing import Optional
from pydantic import BaseModel, Field, field_validator
from sqlalchemy import Column, String, Date, Time, Numeric, DateTime, Index

from src.models.base import Base


class TransactionType(str, Enum):
    """
    Transaction type codes.
    
    Migrated from COBOL 88-level conditions:
    - TRN-TYPE-BUY   VALUE 'BU'
    - TRN-TYPE-SELL  VALUE 'SL'
    - TRN-TYPE-TRANS VALUE 'TR'
    - TRN-TYPE-FEE   VALUE 'FE'
    """
    BUY = "BU"
    SELL = "SL"
    TRANSFER = "TR"
    FEE = "FE"


class TransactionStatus(str, Enum):
    """
    Transaction status codes.
    
    Migrated from COBOL 88-level conditions:
    - TRN-STATUS-PEND VALUE 'P'
    - TRN-STATUS-DONE VALUE 'D'
    - TRN-STATUS-FAIL VALUE 'F'
    - TRN-STATUS-REV  VALUE 'R'
    """
    PENDING = "P"
    DONE = "D"
    FAILED = "F"
    REVERSED = "R"


class TransactionRecord(BaseModel):
    """
    Pydantic model for transaction record validation.
    
    Preserves all field definitions from TRNREC.cpy with Python type mappings.
    """
    
    # TRN-KEY fields
    trn_date: date = Field(
        ...,
        description="Transaction date (YYYYMMDD format in COBOL)"
    )
    trn_time: time = Field(
        ...,
        description="Transaction time (HHMMSS format in COBOL)"
    )
    trn_portfolio_id: str = Field(
        ...,
        max_length=8,
        description="Portfolio identifier"
    )
    trn_sequence_no: str = Field(
        ...,
        max_length=6,
        description="Sequence number for multiple transactions"
    )
    
    # TRN-DATA fields
    trn_investment_id: str = Field(
        ...,
        max_length=10,
        description="Investment identifier"
    )
    trn_type: TransactionType = Field(
        ...,
        description="Transaction type: BU=Buy, SL=Sell, TR=Transfer, FE=Fee"
    )
    trn_quantity: Decimal = Field(
        ...,
        max_digits=15,
        decimal_places=4,
        description="Transaction quantity (PIC S9(11)V9(4) COMP-3)"
    )
    trn_price: Decimal = Field(
        ...,
        max_digits=15,
        decimal_places=4,
        description="Transaction price (PIC S9(11)V9(4) COMP-3)"
    )
    trn_amount: Decimal = Field(
        ...,
        max_digits=15,
        decimal_places=2,
        description="Transaction amount (PIC S9(13)V9(2) COMP-3)"
    )
    trn_currency: str = Field(
        ...,
        max_length=3,
        description="Currency code (ISO 4217)"
    )
    trn_status: TransactionStatus = Field(
        ...,
        description="Transaction status: P=Pending, D=Done, F=Failed, R=Reversed"
    )
    
    # TRN-AUDIT fields
    trn_process_date: Optional[datetime] = Field(
        None,
        description="Processing timestamp"
    )
    trn_process_user: Optional[str] = Field(
        None,
        max_length=8,
        description="Processing user ID"
    )

    @field_validator("trn_portfolio_id", "trn_sequence_no", "trn_investment_id", "trn_currency")
    @classmethod
    def strip_and_upper(cls, v: str) -> str:
        """Strip whitespace and convert to uppercase for consistency with COBOL."""
        if v:
            return v.strip().upper()
        return v

    @field_validator("trn_quantity", "trn_price", "trn_amount", mode="before")
    @classmethod
    def convert_to_decimal(cls, v) -> Decimal:
        """Convert numeric values to Decimal for precision."""
        if v is None:
            return Decimal("0")
        return Decimal(str(v))

    class Config:
        """Pydantic configuration."""
        json_encoders = {
            Decimal: str,
            date: lambda v: v.strftime("%Y%m%d"),
            time: lambda v: v.strftime("%H%M%S"),
            datetime: lambda v: v.isoformat(),
        }


class Transaction(Base):
    """
    SQLAlchemy ORM model for transaction records.
    
    Maps to PostgreSQL table: transactions
    Replaces VSAM TRANHIST file and DB2 TRANSACTION_HISTORY table.
    """
    __tablename__ = "transactions"

    # Primary key fields (composite key from TRN-KEY)
    trn_date = Column(Date, primary_key=True, nullable=False)
    trn_time = Column(Time, primary_key=True, nullable=False)
    trn_portfolio_id = Column(String(8), primary_key=True, nullable=False)
    trn_sequence_no = Column(String(6), primary_key=True, nullable=False)

    # Transaction data fields
    trn_investment_id = Column(String(10), nullable=False)
    trn_type = Column(String(2), nullable=False)
    trn_quantity = Column(Numeric(15, 4), nullable=False)
    trn_price = Column(Numeric(15, 4), nullable=False)
    trn_amount = Column(Numeric(15, 2), nullable=False)
    trn_currency = Column(String(3), nullable=False, default="USD")
    trn_status = Column(String(1), nullable=False, default="P")

    # Audit fields
    trn_process_date = Column(DateTime, nullable=True)
    trn_process_user = Column(String(8), nullable=True)

    # Indexes for common access patterns
    __table_args__ = (
        Index("idx_trn_portfolio_date", "trn_portfolio_id", "trn_date"),
        Index("idx_trn_investment", "trn_investment_id", "trn_date"),
        Index("idx_trn_status", "trn_status", "trn_date"),
    )

    def __repr__(self) -> str:
        return (
            f"<Transaction(portfolio={self.trn_portfolio_id}, "
            f"date={self.trn_date}, type={self.trn_type}, "
            f"amount={self.trn_amount})>"
        )

    def to_pydantic(self) -> TransactionRecord:
        """Convert SQLAlchemy model to Pydantic model for validation/serialization."""
        return TransactionRecord(
            trn_date=self.trn_date,
            trn_time=self.trn_time,
            trn_portfolio_id=self.trn_portfolio_id,
            trn_sequence_no=self.trn_sequence_no,
            trn_investment_id=self.trn_investment_id,
            trn_type=TransactionType(self.trn_type),
            trn_quantity=self.trn_quantity,
            trn_price=self.trn_price,
            trn_amount=self.trn_amount,
            trn_currency=self.trn_currency,
            trn_status=TransactionStatus(self.trn_status),
            trn_process_date=self.trn_process_date,
            trn_process_user=self.trn_process_user,
        )

    @classmethod
    def from_pydantic(cls, record: TransactionRecord) -> "Transaction":
        """Create SQLAlchemy model from Pydantic model."""
        return cls(
            trn_date=record.trn_date,
            trn_time=record.trn_time,
            trn_portfolio_id=record.trn_portfolio_id,
            trn_sequence_no=record.trn_sequence_no,
            trn_investment_id=record.trn_investment_id,
            trn_type=record.trn_type.value,
            trn_quantity=record.trn_quantity,
            trn_price=record.trn_price,
            trn_amount=record.trn_amount,
            trn_currency=record.trn_currency,
            trn_status=record.trn_status.value,
            trn_process_date=record.trn_process_date,
            trn_process_user=record.trn_process_user,
        )

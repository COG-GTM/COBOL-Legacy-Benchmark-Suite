"""
Transaction data models.
Migrated from COBOL copybook: src/copybook/common/TRNREC.cpy

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

from datetime import date, datetime, time
from decimal import Decimal
from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field, field_validator
from sqlalchemy import Column, Date, DateTime, Integer, Numeric, String, Time
from sqlalchemy.orm import Mapped, mapped_column

from src.database.base import AuditMixin, Base


class TransactionType(str, Enum):
    """
    Transaction type codes.
    Migrated from COBOL: TRN-TYPE values.
    """
    BUY = "BU"
    SELL = "SL"
    TRANSFER = "TR"
    FEE = "FE"
    DIVIDEND = "DV"
    INTEREST = "IN"
    DEPOSIT = "DP"
    WITHDRAWAL = "WD"


class TransactionStatus(str, Enum):
    """
    Transaction status codes.
    Migrated from COBOL: TRN-STATUS values.
    """
    PENDING = "P"
    DONE = "D"
    FAILED = "F"
    REVERSED = "R"


class TransactionRecord(Base, AuditMixin):
    """
    SQLAlchemy ORM model for transaction records.
    Migrated from COBOL TRNREC copybook and VSAM TRANHIST file.
    """
    __tablename__ = "transaction_file"
    
    # Key fields (TRN-KEY)
    trans_date: Mapped[str] = mapped_column(String(8), primary_key=True)
    trans_time: Mapped[str] = mapped_column(String(6), primary_key=True)
    portfolio_id: Mapped[str] = mapped_column(String(8), primary_key=True)
    sequence_no: Mapped[str] = mapped_column(String(6), primary_key=True)
    
    # Data fields (TRN-DATA)
    investment_id: Mapped[str] = mapped_column(String(10), nullable=False)
    trans_type: Mapped[str] = mapped_column(String(2), nullable=False)
    quantity: Mapped[Decimal] = mapped_column(Numeric(15, 4), nullable=False, default=0)
    price: Mapped[Decimal] = mapped_column(Numeric(15, 4), nullable=False, default=0)
    amount: Mapped[Decimal] = mapped_column(Numeric(15, 2), nullable=False, default=0)
    currency_code: Mapped[str] = mapped_column(String(3), nullable=False, default="USD")
    status: Mapped[str] = mapped_column(String(1), nullable=False, default="P")
    
    # Audit fields (TRN-AUDIT)
    process_date: Mapped[Optional[str]] = mapped_column(String(26), nullable=True)
    process_user: Mapped[Optional[str]] = mapped_column(String(8), nullable=True)
    
    def __repr__(self) -> str:
        return (
            f"TransactionRecord(date={self.trans_date}, time={self.trans_time}, "
            f"portfolio={self.portfolio_id}, seq={self.sequence_no}, "
            f"type={self.trans_type}, amount={self.amount})"
        )


class TransactionKey(BaseModel):
    """
    Pydantic model for transaction key (TRN-KEY).
    Used for lookups and validation.
    """
    trans_date: str = Field(..., min_length=8, max_length=8, description="Transaction date YYYYMMDD")
    trans_time: str = Field(..., min_length=6, max_length=6, description="Transaction time HHMMSS")
    portfolio_id: str = Field(..., min_length=1, max_length=8, description="Portfolio identifier")
    sequence_no: str = Field(..., min_length=1, max_length=6, description="Sequence number")
    
    @field_validator("trans_date")
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
    
    @field_validator("trans_time")
    @classmethod
    def validate_time(cls, v: str) -> str:
        """Validate time format HHMMSS."""
        if not v.isdigit():
            raise ValueError("Time must be numeric HHMMSS")
        if len(v) != 6:
            raise ValueError("Time must be 6 digits HHMMSS")
        hours, minutes, seconds = int(v[:2]), int(v[2:4]), int(v[4:6])
        if not (0 <= hours <= 23 and 0 <= minutes <= 59 and 0 <= seconds <= 59):
            raise ValueError("Invalid time values")
        return v


class TransactionData(BaseModel):
    """
    Pydantic model for transaction data (TRN-DATA).
    Used for API requests and validation.
    """
    investment_id: str = Field(..., min_length=1, max_length=10, description="Investment identifier")
    trans_type: TransactionType = Field(..., description="Transaction type code")
    quantity: Decimal = Field(..., ge=0, decimal_places=4, description="Transaction quantity")
    price: Decimal = Field(..., ge=0, decimal_places=4, description="Transaction price")
    amount: Decimal = Field(..., decimal_places=2, description="Transaction amount")
    currency_code: str = Field(default="USD", min_length=3, max_length=3, description="Currency code")
    status: TransactionStatus = Field(default=TransactionStatus.PENDING, description="Transaction status")
    
    @field_validator("amount", mode="before")
    @classmethod
    def calculate_amount(cls, v: Decimal, info) -> Decimal:
        """Calculate amount if not provided."""
        if v is None and "quantity" in info.data and "price" in info.data:
            return info.data["quantity"] * info.data["price"]
        return v


class TransactionCreate(TransactionKey, TransactionData):
    """
    Pydantic model for creating a new transaction.
    Combines key and data fields.
    """
    pass


class TransactionResponse(TransactionCreate):
    """
    Pydantic model for transaction API responses.
    Includes audit fields.
    """
    process_date: Optional[str] = None
    process_user: Optional[str] = None
    
    class Config:
        from_attributes = True


class TransactionSummary(BaseModel):
    """
    Pydantic model for transaction summary.
    Used in reports and aggregations.
    """
    portfolio_id: str
    trans_date: str
    total_transactions: int
    total_buys: int
    total_sells: int
    total_amount: Decimal
    net_amount: Decimal

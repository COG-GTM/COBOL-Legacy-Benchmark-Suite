"""Transaction Record model - converted from TRNREC.cpy.

COBOL Original:
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

from datetime import datetime
from decimal import Decimal
from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field, field_validator, model_validator


class TransactionType(str, Enum):
    """Transaction type codes - maps to 88-level conditions in COBOL."""

    BUY = "BU"  # TRN-TYPE-BUY
    SELL = "SL"  # TRN-TYPE-SELL
    TRANSFER = "TR"  # TRN-TYPE-TRANS
    FEE = "FE"  # TRN-TYPE-FEE


class TransactionStatus(str, Enum):
    """Transaction status codes - maps to 88-level conditions in COBOL."""

    PENDING = "P"  # TRN-STATUS-PEND
    DONE = "D"  # TRN-STATUS-DONE
    FAILED = "F"  # TRN-STATUS-FAIL
    REVERSED = "R"  # TRN-STATUS-REV


class TransactionKey(BaseModel):
    """Transaction key structure - maps to TRN-KEY in COBOL."""

    date: str = Field(max_length=8, description="Transaction date (YYYYMMDD)")
    time: str = Field(max_length=6, description="Transaction time (HHMMSS)")
    portfolio_id: str = Field(max_length=8, description="Portfolio identifier")
    sequence_no: str = Field(max_length=6, description="Sequence number")

    @field_validator("date")
    @classmethod
    def validate_date_format(cls, v: str) -> str:
        """Validate date is in YYYYMMDD format."""
        if len(v) != 8 or not v.isdigit():
            raise ValueError("Date must be in YYYYMMDD format")
        return v

    @field_validator("time")
    @classmethod
    def validate_time_format(cls, v: str) -> str:
        """Validate time is in HHMMSS format."""
        if len(v) != 6 or not v.isdigit():
            raise ValueError("Time must be in HHMMSS format")
        return v

    @property
    def transaction_id(self) -> str:
        """Generate unique transaction ID from key components."""
        return f"{self.date}{self.time}{self.portfolio_id}{self.sequence_no}"


class TransactionData(BaseModel):
    """Transaction data structure - maps to TRN-DATA in COBOL."""

    investment_id: str = Field(max_length=10, description="Investment identifier")
    type: TransactionType = Field(description="Transaction type")
    quantity: Decimal = Field(
        default=Decimal("0"),
        decimal_places=4,
        description="Transaction quantity - PIC S9(11)V9(4)",
    )
    price: Decimal = Field(
        default=Decimal("0"),
        decimal_places=4,
        description="Transaction price - PIC S9(11)V9(4)",
    )
    amount: Decimal = Field(
        default=Decimal("0"),
        decimal_places=2,
        description="Transaction amount - PIC S9(13)V9(2)",
    )
    currency: str = Field(default="USD", max_length=3, description="Currency code")
    status: TransactionStatus = Field(
        default=TransactionStatus.PENDING, description="Transaction status"
    )

    @field_validator("investment_id")
    @classmethod
    def strip_and_upper(cls, v: str) -> str:
        """Strip whitespace and convert to uppercase."""
        return v.strip().upper()


class TransactionAudit(BaseModel):
    """Transaction audit structure - maps to TRN-AUDIT in COBOL."""

    process_date: Optional[datetime] = Field(
        default=None, description="Processing date/time"
    )
    process_user: str = Field(
        default="", max_length=8, description="Processing user ID"
    )


class TransactionRecord(BaseModel):
    """Complete transaction record - maps to TRANSACTION-RECORD in COBOL.

    This model represents a financial transaction (buy, sell, transfer, fee)
    against a portfolio position.
    """

    key: TransactionKey
    data: TransactionData
    audit: TransactionAudit = Field(default_factory=TransactionAudit)

    @property
    def portfolio_id(self) -> str:
        """Convenience accessor for portfolio ID."""
        return self.key.portfolio_id

    @property
    def investment_id(self) -> str:
        """Convenience accessor for investment ID."""
        return self.data.investment_id

    @property
    def transaction_id(self) -> str:
        """Convenience accessor for transaction ID."""
        return self.key.transaction_id

    @property
    def is_buy(self) -> bool:
        """Check if transaction is a buy."""
        return self.data.type == TransactionType.BUY

    @property
    def is_sell(self) -> bool:
        """Check if transaction is a sell."""
        return self.data.type == TransactionType.SELL

    @property
    def is_pending(self) -> bool:
        """Check if transaction is pending."""
        return self.data.status == TransactionStatus.PENDING

    @property
    def is_completed(self) -> bool:
        """Check if transaction is completed."""
        return self.data.status == TransactionStatus.DONE

    @model_validator(mode="after")
    def calculate_amount_if_zero(self) -> "TransactionRecord":
        """Calculate amount from quantity and price if not provided."""
        if self.data.amount == Decimal("0") and self.data.quantity and self.data.price:
            self.data.amount = self.data.quantity * self.data.price
        return self

    def to_flat_dict(self) -> dict:
        """Convert to flat dictionary for database operations."""
        return {
            "transaction_id": self.key.transaction_id,
            "transaction_date": self.key.date,
            "transaction_time": self.key.time,
            "portfolio_id": self.key.portfolio_id,
            "sequence_no": self.key.sequence_no,
            "investment_id": self.data.investment_id,
            "transaction_type": self.data.type.value,
            "quantity": self.data.quantity,
            "price": self.data.price,
            "amount": self.data.amount,
            "currency": self.data.currency,
            "status": self.data.status.value,
            "process_date": self.audit.process_date,
            "process_user": self.audit.process_user,
        }

    class Config:
        """Pydantic configuration."""

        json_encoders = {Decimal: str}

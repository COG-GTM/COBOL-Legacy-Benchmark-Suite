"""Transaction Record model - translated from TRNREC.cpy copybook.

Mirrors the COBOL TRANSACTION-RECORD structure with composite key
(date, time, portfolio_id, sequence_no) and transaction data fields.
"""

from datetime import datetime
from decimal import Decimal
from enum import StrEnum

from pydantic import BaseModel, Field


class TransactionType(StrEnum):
    """Transaction type codes from 88-level values in TRNREC."""

    BUY = "BU"
    SELL = "SL"
    TRANSFER = "TR"
    FEE = "FE"


class TransactionStatus(StrEnum):
    """Transaction status codes from 88-level values in TRNREC."""

    PENDING = "P"
    DONE = "D"
    FAILED = "F"
    REVERSED = "R"


class TransactionKey(BaseModel):
    """Composite key for transaction record (TRN-KEY)."""

    date: str = Field(max_length=8, description="Transaction date YYYYMMDD")
    time: str = Field(max_length=6, description="Transaction time HHMMSS")
    portfolio_id: str = Field(max_length=8, description="Portfolio identifier")
    sequence_no: str = Field(max_length=6, description="Sequence number")


class TransactionRecord(BaseModel):
    """Full transaction record translated from COBOL TRANSACTION-RECORD.

    Maps to TRNREC.cpy copybook fields:
    - TRN-KEY (composite key)
    - TRN-DATA (transaction details)
    - TRN-AUDIT (audit trail)
    """

    # Key fields (TRN-KEY)
    date: str = Field(max_length=8, description="TRN-DATE: YYYYMMDD")
    time: str = Field(max_length=6, description="TRN-TIME: HHMMSS")
    portfolio_id: str = Field(max_length=8, description="TRN-PORTFOLIO-ID")
    sequence_no: str = Field(max_length=6, description="TRN-SEQUENCE-NO")

    # Data fields (TRN-DATA)
    investment_id: str = Field(max_length=10, description="TRN-INVESTMENT-ID")
    type: TransactionType = Field(description="TRN-TYPE: BU/SL/TR/FE")
    quantity: Decimal = Field(max_digits=15, decimal_places=4, description="TRN-QUANTITY S9(11)V9(4)")
    price: Decimal = Field(max_digits=15, decimal_places=4, description="TRN-PRICE S9(11)V9(4)")
    amount: Decimal = Field(max_digits=15, decimal_places=2, description="TRN-AMOUNT S9(13)V9(2)")
    currency: str = Field(max_length=3, description="TRN-CURRENCY")
    status: TransactionStatus = Field(description="TRN-STATUS: P/D/F/R")

    # Audit fields (TRN-AUDIT)
    process_date: str = Field(default="", max_length=26, description="TRN-PROCESS-DATE")
    process_user: str = Field(default="", max_length=8, description="TRN-PROCESS-USER")

    @property
    def composite_key(self) -> str:
        """Build the composite key matching VSAM KSDS key structure."""
        return f"{self.date}{self.time}{self.portfolio_id}{self.sequence_no}"

    def mark_done(self, user: str) -> None:
        """Mark transaction as done with audit trail."""
        self.status = TransactionStatus.DONE
        self.process_date = datetime.now().isoformat()
        self.process_user = user

    def mark_failed(self, user: str) -> None:
        """Mark transaction as failed with audit trail."""
        self.status = TransactionStatus.FAILED
        self.process_date = datetime.now().isoformat()
        self.process_user = user

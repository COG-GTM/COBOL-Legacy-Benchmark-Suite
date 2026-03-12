"""
Transaction models translated from COBOL copybook TRNREC.cpy and DB2 TRANSACTION_HISTORY table.

COBOL PIC clauses mapped:
  TRN-DATE           PIC X(08)              -> date (YYYYMMDD)
  TRN-TIME           PIC X(06)              -> time (HHMMSS)
  TRN-PORTFOLIO-ID   PIC X(08)              -> str, max_length=8
  TRN-SEQUENCE-NO    PIC X(06)              -> str, max_length=6
  TRN-INVESTMENT-ID  PIC X(10)              -> str, max_length=10
  TRN-TYPE           PIC X(02)              -> TransactionType enum
  TRN-QUANTITY       PIC S9(11)V9(4) COMP-3 -> Decimal(15,4)
  TRN-PRICE          PIC S9(11)V9(4) COMP-3 -> Decimal(15,4)
  TRN-AMOUNT         PIC S9(13)V9(2) COMP-3 -> Decimal(15,2)
  TRN-CURRENCY       PIC X(03)              -> str, max_length=3
  TRN-STATUS         PIC X(01)              -> TransactionStatus enum
  TRN-PROCESS-DATE   PIC X(26)              -> datetime
  TRN-PROCESS-USER   PIC X(08)              -> str, max_length=8
"""

from datetime import date, datetime, time
from decimal import Decimal

from pydantic import BaseModel, Field, field_validator

from src.common.constants import CurrencyCode, TransactionStatus, TransactionType


class TransactionKey(BaseModel):
    """
    Composite key from TRNREC.cpy TRN-KEY.

    VSAM TRANHIST key: date(8) + time(6) + portfolio(8) + seq(6) = 28 bytes.
    """

    trn_date: date = Field(description="Transaction date")
    trn_time: time = Field(description="Transaction time")
    portfolio_id: str = Field(max_length=8, description="Portfolio identifier")
    sequence_no: str = Field(max_length=6, description="Sequence number")


class TransactionRecord(BaseModel):
    """
    Full transaction record from TRNREC.cpy and DB2 TRANSACTION_HISTORY.

    Transaction ID format: YYYYMMDDHHMMSS + 6-digit sequence (from db2-definitions.sql notes).
    """

    # Key fields
    transaction_id: str = Field(default="", max_length=20, description="Unique transaction ID")
    trn_date: date = Field(default_factory=date.today, description="Transaction date")
    trn_time: time = Field(default_factory=lambda: datetime.now().time(), description="Transaction time")
    portfolio_id: str = Field(max_length=8, description="Portfolio identifier")
    sequence_no: str = Field(default="000001", max_length=6, description="Sequence number")

    # Transaction data
    investment_id: str = Field(default="", max_length=10, description="Investment identifier")
    trn_type: TransactionType = Field(description="Transaction type: BU/SL/TR/FE")
    quantity: Decimal = Field(gt=0, max_digits=15, decimal_places=4)
    price: Decimal = Field(default=Decimal("0.0000"), max_digits=15, decimal_places=4)
    amount: Decimal = Field(default=Decimal("0.00"), max_digits=15, decimal_places=2)
    currency: CurrencyCode = Field(default=CurrencyCode.USD, description="Currency code")
    status: TransactionStatus = Field(default=TransactionStatus.PENDING, description="Transaction status")

    # Audit fields
    process_date: datetime = Field(default_factory=datetime.now, description="Processing timestamp")
    process_user: str = Field(default="", max_length=8, description="Processing user")

    @field_validator("portfolio_id")
    @classmethod
    def validate_portfolio_id(cls, v: str) -> str:
        if not v or not v.strip():
            raise ValueError("Portfolio ID is required")
        return v.strip()

"""Transaction Record Model - migrated from COBOL copybook TRNREC.cpy

Source: src/copybook/common/TRNREC.cpy
COBOL Record: TRANSACTION-RECORD (variable length, COMP-3 packed decimal fields)

COBOL Data Type Mapping:
    PIC X(n)              -> str (fixed-length character)
    PIC S9(11)V9(4) COMP-3 -> Decimal (packed decimal, 11 integer + 4 decimal digits)
    PIC S9(13)V9(2) COMP-3 -> Decimal (packed decimal, 13 integer + 2 decimal digits)
    88-level conditions   -> Enum or validated string constants
"""
from datetime import datetime
from decimal import Decimal
from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field, field_validator
from sqlalchemy import Column, String, Numeric, DateTime, Index
from sqlalchemy.orm import declarative_base

Base = declarative_base()


class TransactionType(str, Enum):
    BUY = "BU"
    SELL = "SL"
    TRANSFER = "TR"
    FEE = "FE"


class TransactionStatus(str, Enum):
    PENDING = "P"
    DONE = "D"
    FAILED = "F"
    REVERSED = "R"


class TransactionRecordORM(Base):
    """SQLAlchemy ORM model for VSAM transaction history records."""

    __tablename__ = "vsam_transaction_history"

    trans_date = Column(String(8), primary_key=True, nullable=False)
    trans_time = Column(String(6), primary_key=True, nullable=False)
    portfolio_id = Column(String(8), primary_key=True, nullable=False)
    sequence_no = Column(String(6), primary_key=True, nullable=False)
    investment_id = Column(String(10), nullable=False)
    trans_type = Column(String(2), nullable=False)
    quantity = Column(Numeric(15, 4), nullable=False)
    price = Column(Numeric(15, 4), nullable=False)
    amount = Column(Numeric(15, 2), nullable=False)
    currency_code = Column(String(3), nullable=False, default="USD")
    status = Column(String(1), nullable=False, default="P")
    process_timestamp = Column(String(26))
    process_user = Column(String(8))

    __table_args__ = (
        Index("idx_vsam_trn_portfolio", "portfolio_id", "trans_date"),
        Index("idx_vsam_trn_investment", "investment_id", "trans_date"),
    )


class TransactionRecord(BaseModel):
    """Pydantic model for transaction record validation.

    Mapped from COBOL copybook TRNREC.cpy:
        01  TRANSACTION-RECORD.
            05  TRN-KEY.
                10  TRN-DATE           PIC X(08).     -> trans_date
                10  TRN-TIME           PIC X(06).     -> trans_time
                10  TRN-PORTFOLIO-ID   PIC X(08).     -> portfolio_id
                10  TRN-SEQUENCE-NO    PIC X(06).     -> sequence_no
            05  TRN-DATA.
                10  TRN-INVESTMENT-ID  PIC X(10).     -> investment_id
                10  TRN-TYPE           PIC X(02).     -> trans_type
                10  TRN-QUANTITY       PIC S9(11)V9(4) COMP-3. -> quantity
                10  TRN-PRICE          PIC S9(11)V9(4) COMP-3. -> price
                10  TRN-AMOUNT         PIC S9(13)V9(2) COMP-3. -> amount
                10  TRN-CURRENCY       PIC X(03).     -> currency_code
                10  TRN-STATUS         PIC X(01).     -> status
            05  TRN-AUDIT.
                10  TRN-PROCESS-DATE   PIC X(26).     -> process_timestamp
                10  TRN-PROCESS-USER   PIC X(08).     -> process_user
    """

    trans_date: str = Field(
        ..., min_length=8, max_length=8, description="Transaction date (YYYYMMDD)"
    )
    trans_time: str = Field(
        ..., min_length=6, max_length=6, description="Transaction time (HHMMSS)"
    )
    portfolio_id: str = Field(
        ..., min_length=1, max_length=8, description="Portfolio identifier"
    )
    sequence_no: str = Field(
        ..., min_length=1, max_length=6, description="Sequence number"
    )
    investment_id: str = Field(
        ..., min_length=1, max_length=10, description="Investment identifier"
    )
    trans_type: TransactionType = Field(
        ..., description="BU=Buy, SL=Sell, TR=Transfer, FE=Fee"
    )
    quantity: Decimal = Field(
        ...,
        max_digits=15,
        decimal_places=4,
        description="Transaction quantity (COBOL: S9(11)V9(4) COMP-3)",
    )
    price: Decimal = Field(
        ...,
        max_digits=15,
        decimal_places=4,
        description="Transaction price (COBOL: S9(11)V9(4) COMP-3)",
    )
    amount: Decimal = Field(
        ...,
        max_digits=15,
        decimal_places=2,
        description="Transaction amount (COBOL: S9(13)V9(2) COMP-3)",
    )
    currency_code: str = Field(
        default="USD", min_length=3, max_length=3, description="ISO currency code"
    )
    status: TransactionStatus = Field(
        default=TransactionStatus.PENDING,
        description="P=Pending, D=Done, F=Failed, R=Reversed",
    )
    process_timestamp: Optional[str] = Field(
        default=None, max_length=26, description="Processing timestamp"
    )
    process_user: Optional[str] = Field(
        default=None, max_length=8, description="Processing user ID"
    )

    @field_validator("trans_date")
    @classmethod
    def validate_trans_date(cls, v: str) -> str:
        try:
            datetime.strptime(v, "%Y%m%d")
        except ValueError:
            raise ValueError("trans_date must be valid YYYYMMDD format")
        return v

    @field_validator("trans_time")
    @classmethod
    def validate_trans_time(cls, v: str) -> str:
        if not v.isdigit():
            raise ValueError("trans_time must be numeric HHMMSS")
        hour, minute, second = int(v[:2]), int(v[2:4]), int(v[4:6])
        if not (0 <= hour <= 23 and 0 <= minute <= 59 and 0 <= second <= 59):
            raise ValueError("trans_time must be valid HHMMSS")
        return v

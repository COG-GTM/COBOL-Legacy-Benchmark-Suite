"""
Transaction Record Data Model.

Translated from COBOL copybook: src/copybook/common/TRNREC.cpy

COBOL Source Structure:
    01 TRANSACTION-RECORD.
       05 TRN-KEY.
          10 TRN-DATE           PIC X(8).       -> str (YYYYMMDD)
          10 TRN-TIME           PIC X(6).       -> str (HHMMSS)
          10 TRN-PORTFOLIO-ID   PIC X(8).       -> str
          10 TRN-SEQUENCE-NO    PIC X(6).       -> str
       05 TRN-DATA.
          10 TRN-INVESTMENT-ID  PIC X(10).      -> str
          10 TRN-TYPE           PIC X(2).       -> TransactionType enum
             88 TRN-TYPE-BUY    VALUE 'BU'.
             88 TRN-TYPE-SELL   VALUE 'SL'.
             88 TRN-TYPE-XFER   VALUE 'TR'.
             88 TRN-TYPE-FEE    VALUE 'FE'.
          10 TRN-QUANTITY       PIC S9(11)V9(4) COMP-3. -> Decimal(15,4)
          10 TRN-PRICE          PIC S9(11)V9(4) COMP-3. -> Decimal(15,4)
          10 TRN-AMOUNT         PIC S9(13)V9(2) COMP-3. -> Decimal(15,2)
          10 TRN-CURRENCY       PIC X(3).       -> str
          10 TRN-STATUS         PIC X(1).       -> TransactionStatus enum
             88 TRN-PENDING     VALUE 'P'.
             88 TRN-DONE        VALUE 'D'.
             88 TRN-FAILED      VALUE 'F'.
             88 TRN-REVERSED    VALUE 'R'.
       05 TRN-AUDIT.
          10 TRN-PROCESS-DATE   PIC X(26).      -> str (IBM timestamp)
          10 TRN-PROCESS-USER   PIC X(8).       -> str

Data Type Mapping Notes:
    PIC S9(11)V9(4) COMP-3 -> Decimal with 4 decimal places
        Packed decimal, signed, 11 integer digits + 4 fractional digits.
        Python Decimal provides exact arithmetic matching COMP-3 behavior.
    PIC S9(13)V9(2) COMP-3 -> Decimal with 2 decimal places
        Packed decimal, signed, 13 integer digits + 2 fractional digits.
    PIC X(n) -> str with max_length=n
        Fixed-length alphanumeric field.
"""

from decimal import Decimal
from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field, field_validator


class TransactionType(str, Enum):
    """Transaction type codes.

    COBOL 88-level condition names from TRNREC copybook.
    """

    BUY = "BU"
    SELL = "SL"
    TRANSFER = "TR"
    FEE = "FE"


class TransactionStatus(str, Enum):
    """Transaction status codes.

    COBOL 88-level condition names from TRNREC copybook.
    """

    PENDING = "P"
    DONE = "D"
    FAILED = "F"
    REVERSED = "R"


class TransactionKey(BaseModel):
    """Transaction key structure (TRN-KEY group).

    Composite key: date + time + portfolio_id + sequence_no
    Total key length: 28 bytes (matches VSAM TRANHIST extended key concept)
    """

    date: str = Field(
        ...,
        max_length=8,
        description="Transaction date (YYYYMMDD). COBOL: TRN-DATE PIC X(8).",
    )
    time: str = Field(
        ...,
        max_length=6,
        description="Transaction time (HHMMSS). COBOL: TRN-TIME PIC X(6).",
    )
    portfolio_id: str = Field(
        ...,
        max_length=8,
        description="Portfolio identifier. COBOL: TRN-PORTFOLIO-ID PIC X(8).",
    )
    sequence_no: str = Field(
        ...,
        max_length=6,
        description="Sequence number. COBOL: TRN-SEQUENCE-NO PIC X(6).",
    )

    @field_validator("date")
    @classmethod
    def validate_date_format(cls, v: str) -> str:
        if len(v) != 8 or not v.isdigit():
            raise ValueError("Date must be 8 digits in YYYYMMDD format")
        year, month, day = int(v[:4]), int(v[4:6]), int(v[6:8])
        if not (1900 <= year <= 2099 and 1 <= month <= 12 and 1 <= day <= 31):
            raise ValueError("Invalid date values")
        return v

    @field_validator("time")
    @classmethod
    def validate_time_format(cls, v: str) -> str:
        if len(v) != 6 or not v.isdigit():
            raise ValueError("Time must be 6 digits in HHMMSS format")
        hour, minute, second = int(v[:2]), int(v[2:4]), int(v[4:6])
        if not (0 <= hour <= 23 and 0 <= minute <= 59 and 0 <= second <= 59):
            raise ValueError("Invalid time values")
        return v


class TransactionData(BaseModel):
    """Transaction data fields (TRN-DATA group)."""

    investment_id: str = Field(
        ...,
        max_length=10,
        description="Investment identifier. COBOL: TRN-INVESTMENT-ID PIC X(10).",
    )
    transaction_type: TransactionType = Field(
        ...,
        description="Transaction type code. COBOL: TRN-TYPE PIC X(2).",
    )
    quantity: Decimal = Field(
        ...,
        max_digits=15,
        decimal_places=4,
        description=(
            "Transaction quantity. "
            "COBOL: TRN-QUANTITY PIC S9(11)V9(4) COMP-3."
        ),
    )
    price: Decimal = Field(
        ...,
        max_digits=15,
        decimal_places=4,
        description=(
            "Transaction price per unit. "
            "COBOL: TRN-PRICE PIC S9(11)V9(4) COMP-3."
        ),
    )
    amount: Decimal = Field(
        ...,
        max_digits=15,
        decimal_places=2,
        description=(
            "Transaction total amount. "
            "COBOL: TRN-AMOUNT PIC S9(13)V9(2) COMP-3."
        ),
    )
    currency: str = Field(
        default="USD",
        max_length=3,
        description="Currency code. COBOL: TRN-CURRENCY PIC X(3).",
    )
    status: TransactionStatus = Field(
        default=TransactionStatus.PENDING,
        description="Transaction status. COBOL: TRN-STATUS PIC X(1).",
    )


class TransactionAudit(BaseModel):
    """Transaction audit fields (TRN-AUDIT group)."""

    process_date: Optional[str] = Field(
        default=None,
        max_length=26,
        description="Processing timestamp. COBOL: TRN-PROCESS-DATE PIC X(26). IBM format: YYYY-MM-DD-HH.MM.SS.FFFFFF.",
    )
    process_user: Optional[str] = Field(
        default=None,
        max_length=8,
        description="Processing user ID. COBOL: TRN-PROCESS-USER PIC X(8).",
    )


class TransactionRecord(BaseModel):
    """Complete transaction record (TRANSACTION-RECORD).

    Translated from COBOL copybook TRNREC.cpy.
    Represents a single investment transaction with key, data, and audit fields.

    Usage:
        record = TransactionRecord(
            key=TransactionKey(
                date="20240115", time="143022",
                portfolio_id="PORT0001", sequence_no="000001"
            ),
            data=TransactionData(
                investment_id="AAPL000001",
                transaction_type=TransactionType.BUY,
                quantity=Decimal("100.0000"),
                price=Decimal("150.2500"),
                amount=Decimal("15025.00"),
            ),
            audit=TransactionAudit(
                process_date="20240115", process_user="BATCH001"
            ),
        )
    """

    key: TransactionKey
    data: TransactionData
    audit: TransactionAudit = Field(default_factory=TransactionAudit)

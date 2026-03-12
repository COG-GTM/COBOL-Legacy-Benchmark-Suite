"""
Transaction data models translated from COBOL copybook.

Source copybook:
  - src/copybook/common/TRNREC.cpy  (Transaction Record Structure)
"""

from __future__ import annotations

import datetime
from decimal import Decimal
from enum import Enum
from typing import Annotated, Optional

from pydantic import BaseModel, Field, field_validator


# ---------------------------------------------------------------------------
# Enums derived from TRNREC.cpy level-88 condition values
# ---------------------------------------------------------------------------

class TransactionType(str, Enum):
    """TRN-TYPE level-88 values from TRNREC.cpy.

    BU=Buy, SL=Sell, TR=Transfer, FE=Fee
    """

    BUY = "BU"
    SELL = "SL"
    TRANSFER = "TR"
    FEE = "FE"


class TransactionStatus(str, Enum):
    """TRN-STATUS level-88 values from TRNREC.cpy.

    P=Pending, D=Done, F=Failed, R=Reversed
    """

    PENDING = "P"
    DONE = "D"
    FAILED = "F"
    REVERSED = "R"


# ---------------------------------------------------------------------------
# Pydantic model
# ---------------------------------------------------------------------------

class TransactionRecord(BaseModel):
    """Complete transaction record.

    Maps to TRNREC.cpy  01 TRANSACTION-RECORD.

    Field sizes from PIC clauses:
      TRN-DATE          PIC X(08)             YYYYMMDD
      TRN-TIME          PIC X(06)             HHMMSS
      TRN-PORTFOLIO-ID  PIC X(08)
      TRN-SEQUENCE-NO   PIC X(06)
      TRN-INVESTMENT-ID PIC X(10)
      TRN-TYPE          PIC X(02)
      TRN-QUANTITY      PIC S9(11)V9(4) COMP-3   11 int + 4 dec
      TRN-PRICE         PIC S9(11)V9(4) COMP-3   11 int + 4 dec
      TRN-AMOUNT        PIC S9(13)V9(2) COMP-3   13 int + 2 dec
      TRN-CURRENCY      PIC X(03)
      TRN-STATUS        PIC X(01)
      TRN-PROCESS-DATE  PIC X(26)             timestamp
      TRN-PROCESS-USER  PIC X(08)
    """

    # TRN-KEY fields
    trn_date: datetime.date
    trn_time: Annotated[str, Field(max_length=6, pattern=r"^\d{6}$")]
    portfolio_id: Annotated[str, Field(max_length=8)]
    sequence_no: Annotated[str, Field(max_length=6)]

    # TRN-DATA fields
    investment_id: Annotated[str, Field(max_length=10)]
    trn_type: TransactionType
    # PIC S9(11)V9(4) COMP-3  =>  max 15 digits total, 4 decimal
    quantity: Annotated[Decimal, Field(decimal_places=4, max_digits=15)]
    # PIC S9(11)V9(4) COMP-3
    price: Annotated[Decimal, Field(decimal_places=4, max_digits=15)]
    # PIC S9(13)V9(2) COMP-3  =>  max 15 digits total, 2 decimal
    amount: Annotated[Decimal, Field(decimal_places=2, max_digits=15)]
    currency: Annotated[str, Field(max_length=3, min_length=3)]
    status: TransactionStatus

    # TRN-AUDIT fields
    process_date: Optional[datetime.datetime] = None
    process_user: Annotated[str, Field(max_length=8)] = ""

    @field_validator("trn_date", mode="before")
    @classmethod
    def _parse_cobol_date(cls, value: object) -> object:
        """Accept COBOL YYYYMMDD strings and convert to date objects."""
        if isinstance(value, str) and len(value) == 8 and value.isdigit():
            return datetime.date(
                int(value[:4]), int(value[4:6]), int(value[6:8])
            )
        return value

    @field_validator("process_date", mode="before")
    @classmethod
    def _parse_cobol_timestamp(cls, value: object) -> object:
        """Accept COBOL PIC X(26) timestamp strings."""
        if isinstance(value, str) and len(value) == 26:
            try:
                return datetime.datetime.fromisoformat(value.strip())
            except ValueError:
                pass
        return value

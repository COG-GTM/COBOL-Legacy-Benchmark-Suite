"""
Position data models translated from COBOL copybook.

Source copybook:
  - src/copybook/common/POSREC.cpy  (Position Record Structure)
"""

from __future__ import annotations

import datetime
from decimal import Decimal
from enum import Enum
from typing import Annotated, Optional

from pydantic import BaseModel, Field, field_validator


# ---------------------------------------------------------------------------
# Enums derived from POSREC.cpy level-88 condition values
# ---------------------------------------------------------------------------

class PositionStatus(str, Enum):
    """POS-STATUS level-88 values from POSREC.cpy.

    A=Active, C=Closed, P=Pending
    """

    ACTIVE = "A"
    CLOSED = "C"
    PENDING = "P"


# ---------------------------------------------------------------------------
# Pydantic model
# ---------------------------------------------------------------------------

class PositionRecord(BaseModel):
    """Complete position record.

    Maps to POSREC.cpy  01 POSITION-RECORD.

    Field sizes from PIC clauses:
      POS-PORTFOLIO-ID    PIC X(08)
      POS-DATE            PIC X(08)            YYYYMMDD
      POS-INVESTMENT-ID   PIC X(10)
      POS-QUANTITY        PIC S9(11)V9(4) COMP-3   11 int + 4 dec
      POS-COST-BASIS      PIC S9(13)V9(2) COMP-3   13 int + 2 dec
      POS-MARKET-VALUE    PIC S9(13)V9(2) COMP-3   13 int + 2 dec
      POS-CURRENCY        PIC X(03)
      POS-STATUS          PIC X(01)
      POS-LAST-MAINT-DATE PIC X(26)            timestamp
      POS-LAST-MAINT-USER PIC X(08)
    """

    # POS-KEY fields
    portfolio_id: Annotated[str, Field(max_length=8)]
    date: datetime.date
    investment_id: Annotated[str, Field(max_length=10)]

    # POS-DATA fields
    # PIC S9(11)V9(4) COMP-3  =>  max 15 digits total, 4 decimal
    quantity: Annotated[Decimal, Field(decimal_places=4, max_digits=15)]
    # PIC S9(13)V9(2) COMP-3  =>  max 15 digits total, 2 decimal
    cost_basis: Annotated[Decimal, Field(decimal_places=2, max_digits=15)]
    # PIC S9(13)V9(2) COMP-3
    market_value: Annotated[Decimal, Field(decimal_places=2, max_digits=15)]
    currency: Annotated[str, Field(max_length=3, min_length=3)]
    status: PositionStatus

    # POS-AUDIT fields
    last_maint_date: Optional[datetime.datetime] = None
    last_maint_user: Annotated[str, Field(max_length=8)] = ""

    @field_validator("date", mode="before")
    @classmethod
    def _parse_cobol_date(cls, value: object) -> object:
        """Accept COBOL YYYYMMDD strings and convert to date objects."""
        if isinstance(value, str) and len(value) == 8 and value.isdigit():
            return datetime.date(
                int(value[:4]), int(value[4:6]), int(value[6:8])
            )
        return value

    @field_validator("last_maint_date", mode="before")
    @classmethod
    def _parse_cobol_timestamp(cls, value: object) -> object:
        """Accept COBOL PIC X(26) timestamp strings."""
        if isinstance(value, str) and len(value) == 26:
            try:
                return datetime.datetime.fromisoformat(value.strip())
            except ValueError:
                pass
        return value

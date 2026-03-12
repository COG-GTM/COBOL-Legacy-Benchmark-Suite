"""
Market / history data models translated from COBOL copybook.

Source copybook:
  - src/copybook/common/HISTREC.cpy  (History Record Structure)

The HISTREC copybook stores change-history snapshots (before/after images)
for portfolios, positions, and transactions.  There is no dedicated
market-price copybook in the COBOL system; pricing information is embedded
in position records.  This module therefore models the history record as
``MarketDataRecord`` (the closest analogue to market-data tracking) and
adds a lightweight ``PriceSnapshot`` derived from position fields.
"""

from __future__ import annotations

import datetime
from decimal import Decimal
from enum import Enum
from typing import Annotated, Optional

from pydantic import BaseModel, Field, field_validator


# ---------------------------------------------------------------------------
# Enums derived from HISTREC.cpy level-88 condition values
# ---------------------------------------------------------------------------

class HistoryRecordType(str, Enum):
    """HIST-RECORD-TYPE level-88 values from HISTREC.cpy.

    PT=Portfolio, PS=Position, TR=Transaction
    """

    PORTFOLIO = "PT"
    POSITION = "PS"
    TRANSACTION = "TR"


class HistoryActionCode(str, Enum):
    """HIST-ACTION-CODE level-88 values from HISTREC.cpy.

    A=Add, C=Change, D=Delete
    """

    ADD = "A"
    CHANGE = "C"
    DELETE = "D"


# ---------------------------------------------------------------------------
# Pydantic models
# ---------------------------------------------------------------------------

class MarketDataRecord(BaseModel):
    """History / market-data record.

    Maps to HISTREC.cpy  01 HISTORY-RECORD.

    Field sizes from PIC clauses:
      HIST-PORTFOLIO-ID  PIC X(08)
      HIST-DATE          PIC X(08)           YYYYMMDD
      HIST-TIME          PIC X(06)           HHMMSS
      HIST-SEQ-NO        PIC X(04)
      HIST-RECORD-TYPE   PIC X(02)
      HIST-ACTION-CODE   PIC X(01)
      HIST-BEFORE-IMAGE  PIC X(400)
      HIST-AFTER-IMAGE   PIC X(400)
      HIST-REASON-CODE   PIC X(04)
      HIST-PROCESS-DATE  PIC X(26)           timestamp
      HIST-PROCESS-USER  PIC X(08)
    """

    # HIST-KEY fields
    portfolio_id: Annotated[str, Field(max_length=8)]
    date: datetime.date
    time: Annotated[str, Field(max_length=6, pattern=r"^\d{6}$")]
    seq_no: Annotated[str, Field(max_length=4)]

    # HIST-DATA fields
    record_type: HistoryRecordType
    action_code: HistoryActionCode
    before_image: Annotated[str, Field(max_length=400)] = ""
    after_image: Annotated[str, Field(max_length=400)] = ""
    reason_code: Annotated[str, Field(max_length=4)] = ""

    # HIST-AUDIT fields
    process_date: Optional[datetime.datetime] = None
    process_user: Annotated[str, Field(max_length=8)] = ""

    @field_validator("date", mode="before")
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


class PriceSnapshot(BaseModel):
    """Lightweight price snapshot derived from position data.

    Not directly from a copybook but synthesised from POS-MARKET-VALUE
    and POS-COST-BASIS fields in POSREC.cpy for market-data tracking.
    """

    investment_id: Annotated[str, Field(max_length=10)]
    date: datetime.date
    market_value: Annotated[Decimal, Field(decimal_places=2, max_digits=15)]
    currency: Annotated[str, Field(max_length=3, min_length=3)]

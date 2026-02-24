"""
Position Record Data Model.

Translated from COBOL copybook: src/copybook/common/POSREC.cpy

COBOL Source Structure:
    01 POSITION-RECORD.
       05 POS-KEY.
          10 POS-PORTFOLIO-ID   PIC X(8).       -> str
          10 POS-DATE           PIC X(8).       -> str (YYYYMMDD)
          10 POS-INVESTMENT-ID  PIC X(10).      -> str
       05 POS-DATA.
          10 POS-QUANTITY       PIC S9(11)V9(4) COMP-3. -> Decimal(15,4)
          10 POS-COST-BASIS     PIC S9(13)V9(2) COMP-3. -> Decimal(15,2)
          10 POS-MARKET-VALUE   PIC S9(13)V9(2) COMP-3. -> Decimal(15,2)
          10 POS-CURRENCY       PIC X(3).       -> str
          10 POS-STATUS         PIC X(1).       -> PositionStatus enum
             88 POS-ACTIVE      VALUE 'A'.
             88 POS-CLOSED      VALUE 'C'.
             88 POS-PENDING     VALUE 'P'.
       05 POS-AUDIT.
          10 POS-LAST-MAINT-DATE PIC X(8).      -> str (YYYYMMDD)
          10 POS-LAST-MAINT-USER PIC X(8).      -> str

Data Type Mapping Notes:
    PIC S9(11)V9(4) COMP-3 -> Decimal with 4 decimal places
        Packed decimal for share quantities. Allows fractional shares.
    PIC S9(13)V9(2) COMP-3 -> Decimal with 2 decimal places
        Packed decimal for monetary values (cost basis, market value).
    PIC X(n) -> str with max_length=n
        Fixed-length alphanumeric. Trailing spaces are stripped in Python.
"""

from decimal import Decimal
from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field, field_validator


class PositionStatus(str, Enum):
    """Position status codes.

    COBOL 88-level condition names from POSREC copybook.
    """

    ACTIVE = "A"
    CLOSED = "C"
    PENDING = "P"


class PositionKey(BaseModel):
    """Position key structure (POS-KEY group).

    Composite key: portfolio_id + date + investment_id
    Total key length: 26 bytes (matches VSAM POSHIST key concept)
    """

    portfolio_id: str = Field(
        ...,
        max_length=8,
        description="Portfolio identifier. COBOL: POS-PORTFOLIO-ID PIC X(8).",
    )
    date: str = Field(
        ...,
        max_length=8,
        description="Position date (YYYYMMDD). COBOL: POS-DATE PIC X(8).",
    )
    investment_id: str = Field(
        ...,
        max_length=10,
        description="Investment identifier. COBOL: POS-INVESTMENT-ID PIC X(10).",
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


class PositionData(BaseModel):
    """Position data fields (POS-DATA group)."""

    quantity: Decimal = Field(
        ...,
        max_digits=15,
        decimal_places=4,
        description=(
            "Position quantity (shares/units). "
            "COBOL: POS-QUANTITY PIC S9(11)V9(4) COMP-3."
        ),
    )
    cost_basis: Decimal = Field(
        ...,
        max_digits=15,
        decimal_places=2,
        description=(
            "Total cost basis. "
            "COBOL: POS-COST-BASIS PIC S9(13)V9(2) COMP-3."
        ),
    )
    market_value: Decimal = Field(
        ...,
        max_digits=15,
        decimal_places=2,
        description=(
            "Current market value. "
            "COBOL: POS-MARKET-VALUE PIC S9(13)V9(2) COMP-3."
        ),
    )
    currency: str = Field(
        default="USD",
        max_length=3,
        description="Currency code. COBOL: POS-CURRENCY PIC X(3).",
    )
    status: PositionStatus = Field(
        default=PositionStatus.ACTIVE,
        description="Position status. COBOL: POS-STATUS PIC X(1).",
    )


class PositionAudit(BaseModel):
    """Position audit fields (POS-AUDIT group)."""

    last_maint_date: Optional[str] = Field(
        default=None,
        max_length=8,
        description=(
            "Last maintenance date (YYYYMMDD). "
            "COBOL: POS-LAST-MAINT-DATE PIC X(8)."
        ),
    )
    last_maint_user: Optional[str] = Field(
        default=None,
        max_length=8,
        description=(
            "Last maintenance user ID. "
            "COBOL: POS-LAST-MAINT-USER PIC X(8)."
        ),
    )


class PositionRecord(BaseModel):
    """Complete position record (POSITION-RECORD).

    Translated from COBOL copybook POSREC.cpy.
    Represents a single investment position within a portfolio.

    The position tracks the quantity of an investment held, its cost basis
    (original purchase cost), and current market value for unrealized
    gain/loss calculations.

    Usage:
        record = PositionRecord(
            key=PositionKey(
                portfolio_id="PORT0001",
                date="20240115",
                investment_id="AAPL000001",
            ),
            data=PositionData(
                quantity=Decimal("100.0000"),
                cost_basis=Decimal("15025.00"),
                market_value=Decimal("18500.00"),
            ),
            audit=PositionAudit(
                last_maint_date="20240115",
                last_maint_user="BATCH001",
            ),
        )
    """

    key: PositionKey
    data: PositionData
    audit: PositionAudit = Field(default_factory=PositionAudit)

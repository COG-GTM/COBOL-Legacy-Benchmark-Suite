"""
Transaction data models translated from COBOL copybook TRNREC.cpy.

COBOL PIC clauses:
    TRN-DATE           PIC X(08)
    TRN-TIME           PIC X(06)
    TRN-PORTFOLIO-ID   PIC X(08)
    TRN-SEQUENCE-NO    PIC X(06)
    TRN-INVESTMENT-ID  PIC X(10)
    TRN-TYPE           PIC X(02) — BU/SL/TR/FE
    TRN-QUANTITY       PIC S9(11)V9(4) COMP-3
    TRN-PRICE          PIC S9(11)V9(4) COMP-3
    TRN-AMOUNT         PIC S9(13)V9(2) COMP-3
"""

from datetime import date, datetime
from decimal import Decimal
from typing import Optional

from pydantic import BaseModel, Field, field_validator

from src.common.constants import TransactionStatus, TransactionType


class TransactionRecord(BaseModel):
    """Translates COBOL 01 TRANSACTION-RECORD from TRNREC.cpy."""

    # --- Key fields (from TRN-KEY) ---
    trn_date: date = Field(description="PIC X(08) YYYYMMDD")
    trn_time: str = Field(max_length=6, description="PIC X(06) HHMMSS")
    portfolio_id: str = Field(max_length=8, description="PIC X(08)")
    sequence_no: str = Field(max_length=6, description="PIC X(06)")

    # --- Data fields (from TRN-DATA) ---
    investment_id: str = Field(max_length=10, description="PIC X(10)")
    trn_type: TransactionType = Field(description="PIC X(02)")
    quantity: Decimal = Field(
        default=Decimal("0.0000"),
        description="PIC S9(11)V9(4) COMP-3",
    )
    price: Decimal = Field(
        default=Decimal("0.0000"),
        description="PIC S9(11)V9(4) COMP-3",
    )
    amount: Decimal = Field(
        default=Decimal("0.00"),
        description="PIC S9(13)V9(2) COMP-3",
    )

    # --- Additional fields (from TRN-STATUS-INFO) ---
    currency: str = Field(max_length=3, default="USD", description="PIC X(03)")
    status: TransactionStatus = Field(
        default=TransactionStatus.PENDING,
        description="PIC X(01)",
    )
    process_date: Optional[date] = Field(default=None, description="PIC X(08)")
    process_user: str = Field(max_length=8, default="", description="PIC X(08)")

    # --- Computed fields (from PORTTRAN.cbl processing) ---
    fees: Decimal = Field(default=Decimal("0.00"), description="PIC S9(13)V9(2) COMP-3")
    total_amount: Decimal = Field(default=Decimal("0.00"), description="PIC S9(13)V9(2) COMP-3")
    cost_basis: Decimal = Field(default=Decimal("0.00"), description="PIC S9(13)V9(2) COMP-3")
    gain_loss: Decimal = Field(default=Decimal("0.00"), description="PIC S9(13)V9(2) COMP-3")

    # --- DB primary key (from DB2 TRANSACTION_HISTORY) ---
    transaction_id: Optional[int] = Field(default=None)
    created_at: Optional[datetime] = Field(default=None)

    @field_validator("quantity", "price")
    @classmethod
    def validate_comp3_4dec(cls, v: Decimal) -> Decimal:
        """PIC S9(11)V9(4) COMP-3 — 4 decimal places."""
        if v is not None:
            return Decimal(str(v)).quantize(Decimal("0.0001"))
        return v

    @field_validator("amount", "fees", "total_amount", "cost_basis", "gain_loss")
    @classmethod
    def validate_comp3_2dec(cls, v: Decimal) -> Decimal:
        """PIC S9(13)V9(2) COMP-3 — 2 decimal places."""
        if v is not None:
            return Decimal(str(v)).quantize(Decimal("0.01"))
        return v

    @field_validator("portfolio_id")
    @classmethod
    def validate_portfolio_id(cls, v: str) -> str:
        if not v or not v.strip():
            raise ValueError("Portfolio ID is required")
        return v.strip()

    @field_validator("investment_id")
    @classmethod
    def validate_investment_id(cls, v: str) -> str:
        if not v or not v.strip():
            raise ValueError("Investment ID is required")
        return v.strip()

    model_config = {"from_attributes": True}

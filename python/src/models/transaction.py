"""
Pydantic v2 models for COBOL TRNREC copybook (Transaction Record).

Source: src/copybook/common/TRNREC.cpy
"""

from decimal import Decimal
from typing import Literal, Optional

from pydantic import BaseModel, Field, field_validator


class TransactionKey(BaseModel):
    """Transaction key fields from TRN-KEY (level 05)."""

    model_config = {"from_attributes": True}

    trn_date: str = Field(
        max_length=8,
        description="Transaction date YYYYMMDD. COBOL: TRN-DATE PIC X(08).",
    )
    trn_time: str = Field(
        max_length=6,
        description="Transaction time HHMMSS. COBOL: TRN-TIME PIC X(06).",
    )
    trn_portfolio_id: str = Field(
        max_length=8,
        description="Portfolio identifier. COBOL: TRN-PORTFOLIO-ID PIC X(08).",
    )
    trn_sequence_no: str = Field(
        max_length=6,
        description="Sequence number for multiple transactions. COBOL: TRN-SEQUENCE-NO PIC X(06).",
    )

    @field_validator("trn_date")
    @classmethod
    def validate_trn_date(cls, v: str) -> str:
        if v.strip() and not v.strip().isdigit():
            raise ValueError("trn_date must contain only digits (YYYYMMDD)")
        return v

    @field_validator("trn_time")
    @classmethod
    def validate_trn_time(cls, v: str) -> str:
        if v.strip() and not v.strip().isdigit():
            raise ValueError("trn_time must contain only digits (HHMMSS)")
        return v


class TransactionData(BaseModel):
    """Transaction data fields from TRN-DATA (level 05)."""

    model_config = {"from_attributes": True}

    trn_investment_id: str = Field(
        max_length=10,
        description="Investment identifier. COBOL: TRN-INVESTMENT-ID PIC X(10).",
    )
    trn_type: str = Field(
        max_length=2,
        description=(
            "Transaction type: BU=Buy, SL=Sell, TR=Transfer, FE=Fee. "
            "COBOL: TRN-TYPE PIC X(02). "
            "88-level values: BU, SL, TR, FE."
        ),
    )
    trn_quantity: Decimal = Field(
        max_digits=15,
        decimal_places=4,
        description="Transaction quantity. COBOL: TRN-QUANTITY PIC S9(11)V9(4) COMP-3.",
    )
    trn_price: Decimal = Field(
        max_digits=15,
        decimal_places=4,
        description="Transaction price. COBOL: TRN-PRICE PIC S9(11)V9(4) COMP-3.",
    )
    trn_amount: Decimal = Field(
        max_digits=15,
        decimal_places=2,
        description="Transaction amount. COBOL: TRN-AMOUNT PIC S9(13)V9(2) COMP-3.",
    )
    trn_currency: str = Field(
        max_length=3,
        description="Currency code. COBOL: TRN-CURRENCY PIC X(03).",
    )
    trn_status: str = Field(
        max_length=1,
        description=(
            "Transaction status: P=Pending, D=Done, F=Failed, R=Reversed. "
            "COBOL: TRN-STATUS PIC X(01). "
            "88-level values: P, D, F, R."
        ),
    )

    @field_validator("trn_type")
    @classmethod
    def validate_trn_type(cls, v: str) -> str:
        valid = {"BU", "SL", "TR", "FE"}
        if v.strip() and v.strip() not in valid:
            raise ValueError(f"trn_type must be one of {valid}")
        return v

    @field_validator("trn_status")
    @classmethod
    def validate_trn_status(cls, v: str) -> str:
        valid = {"P", "D", "F", "R"}
        if v.strip() and v.strip() not in valid:
            raise ValueError(f"trn_status must be one of {valid}")
        return v


class TransactionAudit(BaseModel):
    """Transaction audit fields from TRN-AUDIT (level 05)."""

    model_config = {"from_attributes": True}

    trn_process_date: str = Field(
        max_length=26,
        description="Process timestamp. COBOL: TRN-PROCESS-DATE PIC X(26).",
    )
    trn_process_user: str = Field(
        max_length=8,
        description="Processing user ID. COBOL: TRN-PROCESS-USER PIC X(08).",
    )


class TransactionRecord(BaseModel):
    """
    Transaction Record — maps to COBOL 01-level TRANSACTION-RECORD.

    Source: src/copybook/common/TRNREC.cpy
    """

    model_config = {"from_attributes": True}

    trn_key: TransactionKey = Field(description="Transaction key (TRN-KEY).")
    trn_data: TransactionData = Field(description="Transaction data (TRN-DATA).")
    trn_audit: TransactionAudit = Field(description="Audit trail (TRN-AUDIT).")
    trn_filler: str = Field(
        default="",
        max_length=50,
        description="Reserved filler. COBOL: TRN-FILLER PIC X(50).",
    )

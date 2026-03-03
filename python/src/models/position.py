"""
Pydantic v2 models for COBOL POSREC copybook (Position Record).

Source: src/copybook/common/POSREC.cpy
"""

from decimal import Decimal

from pydantic import BaseModel, Field, field_validator


class PositionKey(BaseModel):
    """Position key fields from POS-KEY (level 05)."""

    model_config = {"from_attributes": True}

    pos_portfolio_id: str = Field(
        max_length=8,
        description="Portfolio identifier. COBOL: POS-PORTFOLIO-ID PIC X(08).",
    )
    pos_date: str = Field(
        max_length=8,
        description="Position date YYYYMMDD. COBOL: POS-DATE PIC X(08).",
    )
    pos_investment_id: str = Field(
        max_length=10,
        description="Investment identifier. COBOL: POS-INVESTMENT-ID PIC X(10).",
    )

    @field_validator("pos_date")
    @classmethod
    def validate_pos_date(cls, v: str) -> str:
        if v.strip() and not v.strip().isdigit():
            raise ValueError("pos_date must contain only digits (YYYYMMDD)")
        return v


class PositionData(BaseModel):
    """Position data fields from POS-DATA (level 05)."""

    model_config = {"from_attributes": True}

    pos_quantity: Decimal = Field(
        max_digits=15,
        decimal_places=4,
        description="Holding quantity. COBOL: POS-QUANTITY PIC S9(11)V9(4) COMP-3.",
    )
    pos_cost_basis: Decimal = Field(
        max_digits=15,
        decimal_places=2,
        description="Total cost basis. COBOL: POS-COST-BASIS PIC S9(13)V9(2) COMP-3.",
    )
    pos_market_value: Decimal = Field(
        max_digits=15,
        decimal_places=2,
        description="Current market value. COBOL: POS-MARKET-VALUE PIC S9(13)V9(2) COMP-3.",
    )
    pos_currency: str = Field(
        max_length=3,
        description="Currency code. COBOL: POS-CURRENCY PIC X(03).",
    )
    pos_status: str = Field(
        max_length=1,
        description=(
            "Position status: A=Active, C=Closed, P=Pending. "
            "COBOL: POS-STATUS PIC X(01). "
            "88-level values: A, C, P."
        ),
    )

    @field_validator("pos_status")
    @classmethod
    def validate_pos_status(cls, v: str) -> str:
        valid = {"A", "C", "P"}
        if v.strip() and v.strip() not in valid:
            raise ValueError(f"pos_status must be one of {valid}")
        return v


class PositionAudit(BaseModel):
    """Position audit fields from POS-AUDIT (level 05)."""

    model_config = {"from_attributes": True}

    pos_last_maint_date: str = Field(
        max_length=26,
        description="Last maintenance timestamp. COBOL: POS-LAST-MAINT-DATE PIC X(26).",
    )
    pos_last_maint_user: str = Field(
        max_length=8,
        description="Last maintenance user ID. COBOL: POS-LAST-MAINT-USER PIC X(08).",
    )


class PositionRecord(BaseModel):
    """
    Position Record — maps to COBOL 01-level POSITION-RECORD.

    Source: src/copybook/common/POSREC.cpy
    """

    model_config = {"from_attributes": True}

    pos_key: PositionKey = Field(description="Position key (POS-KEY).")
    pos_data: PositionData = Field(description="Position data (POS-DATA).")
    pos_audit: PositionAudit = Field(description="Audit trail (POS-AUDIT).")
    pos_filler: str = Field(
        default="",
        max_length=50,
        description="Reserved filler. COBOL: POS-FILLER PIC X(50).",
    )

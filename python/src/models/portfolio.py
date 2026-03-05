"""
Portfolio data models translated from COBOL copybooks:
- PORTFLIO.cpy (Portfolio Master Record Layout)
- PORTKEY.cpy (Portfolio Key Structure)
- PORTDATA.cpy (Portfolio Data Fields)

COBOL PIC clauses mapped to Python types with strict validation.
"""

from datetime import date, datetime
from decimal import Decimal
from typing import Optional

from pydantic import BaseModel, Field, field_validator

from src.common.constants import (
    AccountType,
    ClientType,
    PortfolioStatus,
    RiskLevel,
)


class PortfolioRecord(BaseModel):
    """
    Translates COBOL 01 PORT-RECORD from PORTFLIO.cpy.

    Key structure (PORT-KEY):
        PORT-ID           PIC X(8)
        PORT-ACCOUNT-NO   PIC X(10)
    """

    # --- Key fields (from PORT-KEY) ---
    portfolio_id: str = Field(max_length=8, description="PIC X(8)")
    account_number: str = Field(max_length=10, description="PIC X(10)")

    # --- Client info (from PORT-CLIENT-INFO) ---
    client_name: str = Field(max_length=30, description="PIC X(30)")
    client_type: ClientType = Field(description="PIC X(1), level-88: I/C/T")

    # --- Portfolio info (from PORT-PORTFOLIO-INFO) ---
    create_date: date = Field(description="PIC 9(8) YYYYMMDD")
    last_maint_date: date = Field(description="PIC 9(8) YYYYMMDD")
    status: PortfolioStatus = Field(
        default=PortfolioStatus.ACTIVE,
        description="PIC X(1), level-88: A/C/S",
    )

    # --- Financial info (from PORT-FINANCIAL-INFO) ---
    total_value: Decimal = Field(
        default=Decimal("0.00"),
        description="PIC S9(13)V99 COMP-3",
        decimal_places=2,
    )
    cash_balance: Decimal = Field(
        default=Decimal("0.00"),
        description="PIC S9(13)V99 COMP-3",
        decimal_places=2,
    )

    # --- Audit info (from PORT-AUDIT-INFO) ---
    last_user: str = Field(max_length=8, default="", description="PIC X(8)")
    last_trans_date: Optional[date] = Field(
        default=None,
        description="PIC 9(8) YYYYMMDD",
    )

    # --- Extended fields (from DB2 PORTFOLIO_MASTER table) ---
    account_type: AccountType = Field(
        default=AccountType.INDIVIDUAL,
        description="CHAR(2)",
    )
    branch_id: str = Field(max_length=2, default="00", description="CHAR(2)")
    client_id: str = Field(max_length=10, default="", description="CHAR(10)")
    portfolio_name: str = Field(max_length=50, default="", description="VARCHAR(50)")
    currency_code: str = Field(max_length=3, default="USD", description="CHAR(3)")
    risk_level: RiskLevel = Field(
        default=RiskLevel.MEDIUM,
        description="CHAR(1)",
    )
    open_date: Optional[date] = Field(default=None)
    close_date: Optional[date] = Field(default=None)
    last_maint_timestamp: Optional[datetime] = Field(default=None)
    last_maint_user: str = Field(max_length=8, default="", description="VARCHAR(8)")

    @field_validator("portfolio_id")
    @classmethod
    def validate_portfolio_id(cls, v: str) -> str:
        if not v or not v.strip():
            raise ValueError("Portfolio ID is required")
        if len(v) > 8:
            raise ValueError("Portfolio ID must be at most 8 characters")
        return v.strip()

    @field_validator("currency_code")
    @classmethod
    def validate_currency_code(cls, v: str) -> str:
        from src.common.constants import VALID_CURRENCIES

        if v and v not in VALID_CURRENCIES:
            raise ValueError(f"Invalid currency code: {v}. Must be one of {VALID_CURRENCIES}")
        return v

    @field_validator("total_value", "cash_balance")
    @classmethod
    def validate_decimal_precision(cls, v: Decimal) -> Decimal:
        """Ensure COMP-3 PIC S9(13)V99 precision."""
        if v is not None:
            # Quantize to 2 decimal places matching COBOL COMP-3
            return Decimal(str(v)).quantize(Decimal("0.01"))
        return v

    model_config = {"from_attributes": True}

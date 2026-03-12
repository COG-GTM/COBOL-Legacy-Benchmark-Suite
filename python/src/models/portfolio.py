"""
Portfolio models translated from COBOL copybook PORTFLIO.cpy and DB2 PORTFOLIO_MASTER table.

COBOL PIC clauses mapped:
  PORT-ID             PIC X(8)          -> str, max_length=8
  PORT-ACCOUNT-NO     PIC X(10)         -> str, max_length=10
  PORT-CLIENT-NAME    PIC X(30)         -> str, max_length=30
  PORT-CLIENT-TYPE    PIC X(1)          -> ClientType enum
  PORT-CREATE-DATE    PIC 9(8)          -> date
  PORT-LAST-MAINT     PIC 9(8)          -> date
  PORT-STATUS         PIC X(1)          -> PortfolioStatus enum
  PORT-TOTAL-VALUE    PIC S9(13)V99     -> Decimal(15,2)
  PORT-CASH-BALANCE   PIC S9(13)V99     -> Decimal(15,2)
  PORT-LAST-USER      PIC X(8)          -> str, max_length=8
"""

from datetime import date, datetime
from decimal import Decimal

from pydantic import BaseModel, Field, field_validator

from src.common.constants import (
    AccountType,
    ClientType,
    CurrencyCode,
    PortfolioStatus,
    RiskLevel,
)


class PortfolioKey(BaseModel):
    """Composite key from PORTFLIO.cpy PORT-KEY: PORT-ID + PORT-ACCOUNT-NO."""

    portfolio_id: str = Field(max_length=8, description="Portfolio identifier")
    account_no: str = Field(max_length=10, description="Account number")


class PortfolioRecord(BaseModel):
    """
    Full portfolio record from PORTFLIO.cpy.

    Maps the VSAM PORTMSTR record and DB2 PORTFOLIO_MASTER table.
    """

    # Key fields
    portfolio_id: str = Field(max_length=8, description="Portfolio identifier")
    account_no: str = Field(default="", max_length=10, description="Account number")

    # DB2 fields
    account_type: AccountType = Field(default=AccountType.INDIVIDUAL, description="Account type code")
    branch_id: str = Field(default="", max_length=2, description="Branch identifier")
    client_id: str = Field(default="", max_length=10, description="Client identifier")
    portfolio_name: str = Field(default="", max_length=50, description="Portfolio display name")
    currency_code: CurrencyCode = Field(default=CurrencyCode.USD, description="Base currency")
    risk_level: RiskLevel = Field(default=RiskLevel.MEDIUM, description="Risk level")

    # Client info from copybook
    client_name: str = Field(default="", max_length=30, description="Client name")
    client_type: ClientType = Field(default=ClientType.INDIVIDUAL, description="Client type")

    # Portfolio info
    status: PortfolioStatus = Field(default=PortfolioStatus.ACTIVE, description="Portfolio status")
    open_date: date = Field(default_factory=date.today, description="Portfolio open date")
    close_date: date | None = Field(default=None, description="Portfolio close date")
    create_date: date = Field(default_factory=date.today, description="Record creation date")

    # Financial info - Decimal for COBOL COMP-3 precision
    total_value: Decimal = Field(default=Decimal("0.00"), max_digits=15, decimal_places=2)
    cash_balance: Decimal = Field(default=Decimal("0.00"), max_digits=15, decimal_places=2)

    # Audit info
    last_maint_date: datetime = Field(default_factory=datetime.now, description="Last maintenance timestamp")
    last_maint_user: str = Field(default="", max_length=8, description="Last maintenance user")
    last_trans_date: date | None = Field(default=None, description="Last transaction date")

    @field_validator("portfolio_id")
    @classmethod
    def validate_portfolio_id(cls, v: str) -> str:
        if not v or not v.strip():
            raise ValueError("Portfolio ID is required")
        return v.strip()

    @field_validator("client_name")
    @classmethod
    def validate_client_name(cls, v: str) -> str:
        return v.strip()

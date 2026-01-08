"""Portfolio Record model - converted from PORTFLIO.cpy.

COBOL Original:
01  PORT-RECORD.
    05  PORT-KEY.
        10  PORT-ID             PIC X(8).
        10  PORT-ACCOUNT-NO     PIC X(10).
    05  PORT-CLIENT-INFO.
        10  PORT-CLIENT-NAME    PIC X(30).
        10  PORT-CLIENT-TYPE    PIC X(1).
    05  PORT-PORTFOLIO-INFO.
        10  PORT-CREATE-DATE    PIC 9(8).
        10  PORT-LAST-MAINT     PIC 9(8).
        10  PORT-STATUS         PIC X(1).
    05  PORT-FINANCIAL-INFO.
        10  PORT-TOTAL-VALUE    PIC S9(13)V99 COMP-3.
        10  PORT-CASH-BALANCE   PIC S9(13)V99 COMP-3.
    05  PORT-AUDIT-INFO.
        10  PORT-LAST-USER      PIC X(8).
        10  PORT-LAST-TRANS     PIC 9(8).
"""

from decimal import Decimal
from enum import Enum

from pydantic import BaseModel, Field, field_validator


class ClientType(str, Enum):
    """Client type codes - maps to 88-level conditions in COBOL."""

    INDIVIDUAL = "I"  # PORT-INDIVIDUAL
    CORPORATE = "C"  # PORT-CORPORATE
    TRUST = "T"  # PORT-TRUST


class PortfolioStatus(str, Enum):
    """Portfolio status codes - maps to 88-level conditions in COBOL."""

    ACTIVE = "A"  # PORT-ACTIVE
    CLOSED = "C"  # PORT-CLOSED
    SUSPENDED = "S"  # PORT-SUSPENDED


class PortfolioKey(BaseModel):
    """Portfolio key structure - maps to PORT-KEY in COBOL."""

    portfolio_id: str = Field(max_length=8, description="Portfolio identifier")
    account_no: str = Field(max_length=10, description="Account number")

    @field_validator("portfolio_id", "account_no")
    @classmethod
    def strip_and_upper(cls, v: str) -> str:
        """Strip whitespace and convert to uppercase."""
        return v.strip().upper()


class ClientInfo(BaseModel):
    """Client information structure - maps to PORT-CLIENT-INFO in COBOL."""

    client_name: str = Field(max_length=30, description="Client name")
    client_type: ClientType = Field(
        default=ClientType.INDIVIDUAL, description="Client type"
    )


class PortfolioInfo(BaseModel):
    """Portfolio information structure - maps to PORT-PORTFOLIO-INFO in COBOL."""

    create_date: str = Field(max_length=8, description="Creation date (YYYYMMDD)")
    last_maint: str = Field(
        default="", max_length=8, description="Last maintenance date (YYYYMMDD)"
    )
    status: PortfolioStatus = Field(
        default=PortfolioStatus.ACTIVE, description="Portfolio status"
    )

    @field_validator("create_date", "last_maint")
    @classmethod
    def validate_date_format(cls, v: str) -> str:
        """Validate date is in YYYYMMDD format or empty."""
        if v and (len(v) != 8 or not v.isdigit()):
            raise ValueError("Date must be in YYYYMMDD format")
        return v


class FinancialInfo(BaseModel):
    """Financial information structure - maps to PORT-FINANCIAL-INFO in COBOL."""

    total_value: Decimal = Field(
        default=Decimal("0"),
        decimal_places=2,
        description="Total portfolio value - PIC S9(13)V99",
    )
    cash_balance: Decimal = Field(
        default=Decimal("0"),
        decimal_places=2,
        description="Cash balance - PIC S9(13)V99",
    )


class AuditInfo(BaseModel):
    """Audit information structure - maps to PORT-AUDIT-INFO in COBOL."""

    last_user: str = Field(default="", max_length=8, description="Last user ID")
    last_trans: str = Field(
        default="", max_length=8, description="Last transaction date (YYYYMMDD)"
    )


class PortfolioRecord(BaseModel):
    """Complete portfolio record - maps to PORT-RECORD in COBOL.

    This model represents a portfolio master record containing client
    information, portfolio status, and financial summary.
    """

    key: PortfolioKey
    client_info: ClientInfo
    portfolio_info: PortfolioInfo
    financial_info: FinancialInfo = Field(default_factory=FinancialInfo)
    audit_info: AuditInfo = Field(default_factory=AuditInfo)

    @property
    def portfolio_id(self) -> str:
        """Convenience accessor for portfolio ID."""
        return self.key.portfolio_id

    @property
    def account_no(self) -> str:
        """Convenience accessor for account number."""
        return self.key.account_no

    @property
    def is_active(self) -> bool:
        """Check if portfolio is active."""
        return self.portfolio_info.status == PortfolioStatus.ACTIVE

    @property
    def is_individual(self) -> bool:
        """Check if client is individual."""
        return self.client_info.client_type == ClientType.INDIVIDUAL

    @property
    def is_corporate(self) -> bool:
        """Check if client is corporate."""
        return self.client_info.client_type == ClientType.CORPORATE

    @property
    def invested_value(self) -> Decimal:
        """Calculate invested value (total minus cash)."""
        return self.financial_info.total_value - self.financial_info.cash_balance

    def to_flat_dict(self) -> dict:
        """Convert to flat dictionary for database operations."""
        return {
            "portfolio_id": self.key.portfolio_id,
            "account_no": self.key.account_no,
            "client_name": self.client_info.client_name,
            "client_type": self.client_info.client_type.value,
            "create_date": self.portfolio_info.create_date,
            "last_maint": self.portfolio_info.last_maint,
            "status": self.portfolio_info.status.value,
            "total_value": self.financial_info.total_value,
            "cash_balance": self.financial_info.cash_balance,
            "last_user": self.audit_info.last_user,
            "last_trans": self.audit_info.last_trans,
        }

    class Config:
        """Pydantic configuration."""

        json_encoders = {Decimal: str}

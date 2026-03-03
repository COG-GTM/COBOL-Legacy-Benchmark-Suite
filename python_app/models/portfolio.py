"""Portfolio Record model - translated from PORTFLIO.cpy copybook.

Mirrors the COBOL PORT-RECORD structure with composite key
(id, account_no) and client/portfolio/financial info.
"""

from decimal import Decimal
from enum import StrEnum

from pydantic import BaseModel, Field


class ClientType(StrEnum):
    """Client type codes from 88-level values in PORTFLIO."""

    INDIVIDUAL = "I"
    CORPORATE = "C"
    TRUST = "T"


class PortfolioStatus(StrEnum):
    """Portfolio status codes from 88-level values in PORTFLIO."""

    ACTIVE = "A"
    CLOSED = "C"
    SUSPENDED = "S"


class PortfolioRecord(BaseModel):
    """Full portfolio record translated from COBOL PORT-RECORD.

    Maps to PORTFLIO.cpy copybook fields:
    - PORT-KEY (id + account_no)
    - PORT-CLIENT-INFO
    - PORT-PORTFOLIO-INFO
    - PORT-FINANCIAL-INFO
    """

    # Key fields (PORT-KEY)
    id: str = Field(max_length=8, description="PORT-ID")
    account_no: str = Field(max_length=10, description="PORT-ACCOUNT-NO")

    # Client info (PORT-CLIENT-INFO)
    client_name: str = Field(max_length=30, description="PORT-CLIENT-NAME")
    client_type: ClientType = Field(description="PORT-CLIENT-TYPE: I/C/T")

    # Portfolio info (PORT-PORTFOLIO-INFO)
    create_date: str = Field(max_length=8, description="PORT-CREATE-DATE: YYYYMMDD")
    last_maint: str = Field(default="", max_length=8, description="PORT-LAST-MAINT: YYYYMMDD")
    status: PortfolioStatus = Field(default=PortfolioStatus.ACTIVE, description="PORT-STATUS: A/C/S")

    # Financial info (PORT-FINANCIAL-INFO)
    total_value: Decimal = Field(
        default=Decimal("0"), max_digits=15, decimal_places=2, description="PORT-TOTAL-VALUE S9(13)V99"
    )
    cash_balance: Decimal = Field(
        default=Decimal("0"), max_digits=15, decimal_places=2, description="PORT-CASH-BALANCE S9(13)V99"
    )

    @property
    def composite_key(self) -> str:
        """Build the composite key matching VSAM KSDS key structure (12 bytes)."""
        return f"{self.id}{self.account_no}"

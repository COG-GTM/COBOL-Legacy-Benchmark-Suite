"""Portfolio Master Record Layout - migrated from PORTFLIO.cpy."""

from dataclasses import dataclass
from decimal import Decimal


@dataclass
class PortfolioRecord:
    port_id: str = ""
    account_no: str = ""
    client_name: str = ""
    client_type: str = ""
    create_date: str = ""
    last_maint: str = ""
    status: str = ""
    total_value: Decimal = Decimal("0")
    cash_balance: Decimal = Decimal("0")
    audit_timestamp: str = ""
    audit_user: str = ""

    @property
    def port_key(self) -> str:
        return f"{self.port_id}{self.account_no}"

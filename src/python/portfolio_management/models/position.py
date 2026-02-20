"""Position Record Structure - migrated from POSREC.cpy."""

from dataclasses import dataclass
from decimal import Decimal


@dataclass
class PositionRecord:
    portfolio_id: str = ""
    position_date: str = ""
    investment_id: str = ""
    quantity: Decimal = Decimal("0")
    cost_basis: Decimal = Decimal("0")
    market_value: Decimal = Decimal("0")
    audit_timestamp: str = ""
    audit_user: str = ""

    @property
    def position_key(self) -> str:
        return f"{self.portfolio_id}{self.position_date}{self.investment_id}"

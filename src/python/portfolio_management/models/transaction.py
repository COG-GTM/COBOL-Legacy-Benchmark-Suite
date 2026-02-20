"""Transaction Record Structure - migrated from TRNREC.cpy."""

from dataclasses import dataclass
from decimal import Decimal


@dataclass
class TransactionRecord:
    trans_date: str = ""
    trans_time: str = ""
    portfolio_id: str = ""
    sequence_no: str = ""
    investment_id: str = ""
    trans_type: str = ""
    quantity: Decimal = Decimal("0")
    price: Decimal = Decimal("0")
    amount: Decimal = Decimal("0")
    audit_timestamp: str = ""
    audit_user: str = ""

    @property
    def transaction_key(self) -> str:
        return f"{self.trans_date}{self.trans_time}{self.portfolio_id}{self.sequence_no}"

"""History Record Structure - migrated from HISTREC.cpy."""

from dataclasses import dataclass
from decimal import Decimal
from enum import Enum


class HistoryRecordType(str, Enum):
    PORTFOLIO = "PT"
    POSITION = "PS"
    TRANSACTION = "TR"


class HistoryActionCode(str, Enum):
    ADD = "A"
    CHANGE = "C"
    DELETE = "D"


@dataclass
class HistoryRecord:
    account_no: str = ""
    portfolio_id: str = ""
    trans_date: str = ""
    trans_time: str = ""
    trans_type: str = ""
    security_id: str = ""
    quantity: Decimal = Decimal("0")
    price: Decimal = Decimal("0")
    amount: Decimal = Decimal("0")
    seq_no: str = ""
    record_type: str = ""
    action_code: str = ""
    before_image: str = ""
    after_image: str = ""
    reason_code: str = ""
    process_date: str = ""
    process_user: str = ""

    @property
    def history_key(self) -> str:
        return f"{self.portfolio_id}{self.trans_date}{self.trans_time}{self.seq_no}"

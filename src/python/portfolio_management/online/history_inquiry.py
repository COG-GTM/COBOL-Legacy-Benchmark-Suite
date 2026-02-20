"""Transaction History Inquiry Handler - migrated from INQHIST.cbl.

Retrieves transaction history from database, formats history data
for display, supports scrolling through history.
"""

import logging
from typing import Optional

from portfolio_management.models.db2_tables import PosHistRecord
from portfolio_management.models.common import ReturnCode

logger = logging.getLogger(__name__)

PROGRAM_ID = "INQHIST"
DEFAULT_PAGE_SIZE = 20


class HistoryInquiryHandler:
    def __init__(self):
        self._history_records: list[PosHistRecord] = []
        self._current_page = 0
        self._page_size = DEFAULT_PAGE_SIZE

    def initialize(self, history_file_path: Optional[str] = None) -> int:
        if history_file_path is not None:
            return self._load_history(history_file_path)
        return ReturnCode.SUCCESS

    def get_history(
        self,
        account_no: str,
        page: int = 0,
        page_size: int = DEFAULT_PAGE_SIZE,
    ) -> list[dict]:
        matching = [
            r for r in self._history_records if r.account_no == account_no
        ]

        matching.sort(key=lambda r: (r.trans_date, r.trans_time), reverse=True)

        start = page * page_size
        end = start + page_size
        page_records = matching[start:end]

        return [
            {
                "account_no": r.account_no,
                "portfolio_id": r.portfolio_id,
                "trans_date": r.trans_date,
                "trans_time": r.trans_time,
                "trans_type": r.trans_type,
                "security_id": r.security_id,
                "quantity": str(r.quantity),
                "price": str(r.price),
                "amount": str(r.amount),
                "fees": str(r.fees),
                "total_amount": str(r.total_amount),
                "cost_basis": str(r.cost_basis),
                "gain_loss": str(r.gain_loss),
            }
            for r in page_records
        ]

    def get_history_count(self, account_no: str) -> int:
        return sum(1 for r in self._history_records if r.account_no == account_no)

    def add_record(self, record: PosHistRecord) -> int:
        self._history_records.append(record)
        return ReturnCode.SUCCESS

    def _load_history(self, file_path: str) -> int:
        try:
            from decimal import Decimal
            with open(file_path, "r") as f:
                for line in f:
                    line = line.strip()
                    if not line:
                        continue
                    parts = line.split("|")
                    if len(parts) >= 13:
                        record = PosHistRecord(
                            account_no=parts[0].strip(),
                            portfolio_id=parts[1].strip(),
                            trans_date=parts[2].strip(),
                            trans_time=parts[3].strip(),
                            trans_type=parts[4].strip(),
                            security_id=parts[5].strip(),
                            quantity=Decimal(parts[6].strip()),
                            price=Decimal(parts[7].strip()),
                            amount=Decimal(parts[8].strip()),
                            fees=Decimal(parts[9].strip()),
                            total_amount=Decimal(parts[10].strip()),
                            cost_basis=Decimal(parts[11].strip()),
                            gain_loss=Decimal(parts[12].strip()),
                        )
                        self._history_records.append(record)
            logger.info("Loaded %d history records", len(self._history_records))
            return ReturnCode.SUCCESS
        except FileNotFoundError:
            logger.warning("History file not found: %s", file_path)
            return ReturnCode.WARNING
        except Exception as e:
            logger.error("Error loading history: %s", e)
            return ReturnCode.ERROR

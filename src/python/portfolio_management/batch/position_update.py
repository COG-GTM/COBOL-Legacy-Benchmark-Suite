"""Position Update Processor - migrated from POSUPDT.cbl.

Processes position updates from transaction records, updating the
position master file with new quantities, values, and audit information.
"""

import logging
from datetime import datetime
from decimal import Decimal
from typing import Optional

from portfolio_management.models.position import PositionRecord
from portfolio_management.models.transaction import TransactionRecord
from portfolio_management.models.common import ReturnCode, TransactionType

logger = logging.getLogger(__name__)

PROGRAM_ID = "POSUPDT"


class PositionUpdateProcessor:
    def __init__(self):
        self._positions: dict[str, PositionRecord] = {}
        self._records_read = 0
        self._records_updated = 0
        self._records_added = 0
        self._error_count = 0

    def initialize(self, position_file_path: Optional[str] = None) -> int:
        if position_file_path is not None:
            return self._load_positions(position_file_path)
        return ReturnCode.SUCCESS

    def process_transaction(self, transaction: TransactionRecord) -> int:
        self._records_read += 1

        key = f"{transaction.portfolio_id}{transaction.trans_date}{transaction.investment_id}"
        position = self._positions.get(key)

        if position is None:
            position = PositionRecord(
                portfolio_id=transaction.portfolio_id,
                position_date=transaction.trans_date,
                investment_id=transaction.investment_id,
            )
            self._positions[key] = position
            self._records_added += 1

        try:
            self._apply_transaction(position, transaction)
            position.audit_timestamp = datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f")
            position.audit_user = "BATCH"
            self._records_updated += 1
            return ReturnCode.SUCCESS
        except Exception as e:
            logger.error("Error processing transaction: %s", e)
            self._error_count += 1
            return ReturnCode.ERROR

    def _apply_transaction(
        self, position: PositionRecord, transaction: TransactionRecord
    ) -> None:
        if transaction.trans_type == TransactionType.BUY:
            position.quantity += transaction.quantity
            position.cost_basis += transaction.amount
            position.market_value += transaction.amount
        elif transaction.trans_type == TransactionType.SELL:
            position.quantity -= transaction.quantity
            position.cost_basis -= transaction.amount
            position.market_value -= transaction.amount
        elif transaction.trans_type == TransactionType.TRANSFER:
            position.quantity += transaction.quantity
        elif transaction.trans_type == TransactionType.FEE:
            position.cost_basis += transaction.amount

    def save_positions(self, output_file_path: str) -> int:
        try:
            with open(output_file_path, "w") as f:
                for position in self._positions.values():
                    f.write(
                        f"{position.portfolio_id}|{position.position_date}|"
                        f"{position.investment_id}|{position.quantity}|"
                        f"{position.cost_basis}|{position.market_value}|"
                        f"{position.audit_timestamp}|{position.audit_user}\n"
                    )
            return ReturnCode.SUCCESS
        except Exception as e:
            logger.error("Error saving positions: %s", e)
            return ReturnCode.ERROR

    def _load_positions(self, file_path: str) -> int:
        try:
            with open(file_path, "r") as f:
                for line in f:
                    line = line.strip()
                    if not line:
                        continue
                    parts = line.split("|")
                    if len(parts) >= 6:
                        record = PositionRecord(
                            portfolio_id=parts[0].strip(),
                            position_date=parts[1].strip(),
                            investment_id=parts[2].strip(),
                            quantity=Decimal(parts[3].strip()),
                            cost_basis=Decimal(parts[4].strip()),
                            market_value=Decimal(parts[5].strip()),
                            audit_timestamp=parts[6].strip() if len(parts) > 6 else "",
                            audit_user=parts[7].strip() if len(parts) > 7 else "",
                        )
                        self._positions[record.position_key] = record
            return ReturnCode.SUCCESS
        except FileNotFoundError:
            return ReturnCode.SUCCESS
        except Exception as e:
            logger.error("Error loading positions: %s", e)
            return ReturnCode.ERROR

    def display_statistics(self) -> None:
        logger.info(
            "Position Update Statistics:\n"
            "  Records Read:    %d\n"
            "  Records Updated: %d\n"
            "  Records Added:   %d\n"
            "  Errors:          %d",
            self._records_read,
            self._records_updated,
            self._records_added,
            self._error_count,
        )

    def get_position(self, key: str) -> Optional[PositionRecord]:
        return self._positions.get(key)

    @property
    def position_count(self) -> int:
        return len(self._positions)

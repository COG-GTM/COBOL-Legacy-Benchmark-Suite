"""Portfolio Position Inquiry Handler - migrated from INQPORT.cbl.

Retrieves current portfolio positions from file storage and database,
formats position data for display.
"""

import logging
from typing import Optional

from portfolio_management.models.position import PositionRecord
from portfolio_management.models.common import ReturnCode

logger = logging.getLogger(__name__)

PROGRAM_ID = "INQPORT"


class PortfolioInquiryHandler:
    def __init__(self):
        self._positions: dict[str, PositionRecord] = {}

    def initialize(self, position_file_path: Optional[str] = None) -> int:
        if position_file_path is not None:
            return self._load_positions(position_file_path)
        return ReturnCode.SUCCESS

    def get_position(self, account_no: str) -> Optional[dict]:
        for key, position in self._positions.items():
            if position.portfolio_id == account_no or key.startswith(account_no):
                return {
                    "portfolio_id": position.portfolio_id,
                    "position_date": position.position_date,
                    "investment_id": position.investment_id,
                    "quantity": str(position.quantity),
                    "cost_basis": str(position.cost_basis),
                    "market_value": str(position.market_value),
                    "audit_timestamp": position.audit_timestamp,
                }

        logger.info("No position found for account: %s", account_no)
        return None

    def get_all_positions(self, portfolio_id: str) -> list[dict]:
        results = []
        for position in self._positions.values():
            if position.portfolio_id == portfolio_id:
                results.append({
                    "portfolio_id": position.portfolio_id,
                    "position_date": position.position_date,
                    "investment_id": position.investment_id,
                    "quantity": str(position.quantity),
                    "cost_basis": str(position.cost_basis),
                    "market_value": str(position.market_value),
                })
        return results

    def add_position(self, position: PositionRecord) -> int:
        key = position.position_key
        self._positions[key] = position
        return ReturnCode.SUCCESS

    def _load_positions(self, file_path: str) -> int:
        try:
            with open(file_path, "r") as f:
                for line in f:
                    line = line.strip()
                    if not line:
                        continue
                    parts = line.split("|")
                    if len(parts) >= 6:
                        from decimal import Decimal
                        record = PositionRecord(
                            portfolio_id=parts[0].strip(),
                            position_date=parts[1].strip(),
                            investment_id=parts[2].strip(),
                            quantity=Decimal(parts[3].strip()),
                            cost_basis=Decimal(parts[4].strip()),
                            market_value=Decimal(parts[5].strip()),
                        )
                        self._positions[record.position_key] = record
            logger.info("Loaded %d positions", len(self._positions))
            return ReturnCode.SUCCESS
        except FileNotFoundError:
            logger.warning("Position file not found: %s", file_path)
            return ReturnCode.WARNING
        except Exception as e:
            logger.error("Error loading positions: %s", e)
            return ReturnCode.ERROR

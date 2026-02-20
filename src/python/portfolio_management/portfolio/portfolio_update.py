"""Portfolio Update Program - migrated from PORTUPDT.cbl.

Updates existing portfolio records from an update file with support
for status, value, and name changes.
"""

import logging
from datetime import datetime
from decimal import Decimal
from typing import Optional

from portfolio_management.models.portfolio import PortfolioRecord
from portfolio_management.models.common import ReturnCode

logger = logging.getLogger(__name__)

PROGRAM_ID = "PORTUPDT"


class UpdateAction:
    STATUS = "S"
    VALUE = "V"
    NAME = "N"


class PortfolioUpdateProcessor:
    def __init__(self):
        self._portfolios: dict[str, PortfolioRecord] = {}
        self._update_count = 0
        self._error_count = 0

    def initialize(self, portfolio_store: Optional[dict[str, PortfolioRecord]] = None) -> int:
        if portfolio_store is not None:
            self._portfolios = portfolio_store
        return ReturnCode.SUCCESS

    def process_file(self, update_file_path: str) -> int:
        self._update_count = 0
        self._error_count = 0

        try:
            with open(update_file_path, "r") as f:
                for line in f:
                    line = line.strip()
                    if not line:
                        continue

                    parts = line.split("|")
                    if len(parts) < 4:
                        self._error_count += 1
                        continue

                    port_id = parts[0].strip()
                    account_no = parts[1].strip()
                    action = parts[2].strip()
                    new_value = parts[3].strip()

                    self._process_update(port_id, account_no, action, new_value)

        except FileNotFoundError:
            logger.error("Update file not found: %s", update_file_path)
            return ReturnCode.ERROR
        except Exception as e:
            logger.error("Error processing update file: %s", e)
            return ReturnCode.ERROR

        logger.info("Updates processed: %d", self._update_count)
        logger.info("Errors occurred:  %d", self._error_count)

        if self._error_count > 0:
            return ReturnCode.WARNING
        return ReturnCode.SUCCESS

    def _process_update(
        self, port_id: str, account_no: str, action: str, new_value: str
    ) -> int:
        key = f"{port_id}{account_no}"

        portfolio = self._portfolios.get(key)
        if portfolio is None:
            self._error_count += 1
            logger.warning("Record not found: %s", key)
            return ReturnCode.ERROR

        return self._apply_update(portfolio, action, new_value)

    def _apply_update(
        self, portfolio: PortfolioRecord, action: str, new_value: str
    ) -> int:
        if action == UpdateAction.STATUS:
            portfolio.status = new_value
        elif action == UpdateAction.NAME:
            portfolio.client_name = new_value
        elif action == UpdateAction.VALUE:
            try:
                portfolio.total_value = Decimal(new_value)
            except Exception:
                self._error_count += 1
                logger.warning("Invalid numeric value: %s", new_value)
                return ReturnCode.ERROR
        else:
            self._error_count += 1
            logger.warning("Unknown update action: %s", action)
            return ReturnCode.ERROR

        portfolio.last_maint = datetime.now().strftime("%Y%m%d")
        portfolio.audit_timestamp = datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f")
        portfolio.audit_user = PROGRAM_ID
        self._update_count += 1
        return ReturnCode.SUCCESS

    def update_single(
        self, port_id: str, account_no: str, action: str, new_value: str
    ) -> int:
        key = f"{port_id}{account_no}"
        portfolio = self._portfolios.get(key)
        if portfolio is None:
            return ReturnCode.ERROR

        rc = self._apply_update(portfolio, action, new_value)
        return rc

    def get_statistics(self) -> dict:
        return {
            "update_count": self._update_count,
            "error_count": self._error_count,
        }

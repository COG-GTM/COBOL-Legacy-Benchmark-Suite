"""Portfolio Addition Program - migrated from PORTADD.cbl.

Creates new portfolio records from input file with validation
and duplicate detection.
"""

import logging
from datetime import datetime
from decimal import Decimal
from typing import Optional

from portfolio_management.models.portfolio import PortfolioRecord
from portfolio_management.models.common import ReturnCode

logger = logging.getLogger(__name__)

PROGRAM_ID = "PORTADD"


class PortfolioAddProcessor:
    def __init__(self):
        self._portfolios: dict[str, PortfolioRecord] = {}
        self._records_read = 0
        self._records_added = 0
        self._records_rejected = 0
        self._error_count = 0

    def initialize(self, portfolio_store: Optional[dict[str, PortfolioRecord]] = None) -> int:
        if portfolio_store is not None:
            self._portfolios = portfolio_store
        return ReturnCode.SUCCESS

    def process_file(self, input_file_path: str) -> int:
        self._records_read = 0
        self._records_added = 0
        self._records_rejected = 0
        self._error_count = 0

        try:
            with open(input_file_path, "r") as f:
                for line in f:
                    line = line.strip()
                    if not line:
                        continue

                    self._records_read += 1
                    record = self._parse_record(line)
                    if record is None:
                        self._error_count += 1
                        continue

                    rc = self.add_portfolio(record)
                    if rc != ReturnCode.SUCCESS:
                        self._records_rejected += 1

        except FileNotFoundError:
            logger.error("Input file not found: %s", input_file_path)
            return ReturnCode.ERROR
        except Exception as e:
            logger.error("Error processing input file: %s", e)
            return ReturnCode.ERROR

        self._display_statistics()

        if self._error_count > 0 or self._records_rejected > 0:
            return ReturnCode.WARNING
        return ReturnCode.SUCCESS

    def add_portfolio(self, record: PortfolioRecord) -> int:
        key = record.port_key

        if key in self._portfolios:
            logger.warning("Duplicate portfolio: %s", key)
            return ReturnCode.ERROR

        rc = self._validate_record(record)
        if rc != ReturnCode.SUCCESS:
            return rc

        record.create_date = datetime.now().strftime("%Y%m%d")
        record.last_maint = datetime.now().strftime("%Y%m%d")
        record.status = record.status or "A"
        record.audit_timestamp = datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f")
        record.audit_user = "BATCH"

        self._portfolios[key] = record
        self._records_added += 1
        logger.debug("Portfolio added: %s", key)
        return ReturnCode.SUCCESS

    def _validate_record(self, record: PortfolioRecord) -> int:
        if not record.port_id:
            logger.error("Portfolio ID is required")
            return ReturnCode.ERROR
        if not record.account_no:
            logger.error("Account number is required")
            return ReturnCode.ERROR
        if not record.client_name:
            logger.error("Client name is required")
            return ReturnCode.ERROR
        return ReturnCode.SUCCESS

    def _parse_record(self, line: str) -> Optional[PortfolioRecord]:
        parts = line.split("|")
        if len(parts) < 4:
            logger.warning("Invalid record format: %s", line[:50])
            return None

        try:
            return PortfolioRecord(
                port_id=parts[0].strip(),
                account_no=parts[1].strip(),
                client_name=parts[2].strip(),
                client_type=parts[3].strip() if len(parts) > 3 else "",
                status=parts[4].strip() if len(parts) > 4 else "A",
                total_value=Decimal(parts[5].strip()) if len(parts) > 5 else Decimal("0"),
                cash_balance=Decimal(parts[6].strip()) if len(parts) > 6 else Decimal("0"),
            )
        except (ValueError, IndexError) as e:
            logger.warning("Error parsing record: %s", e)
            return None

    def _display_statistics(self) -> None:
        logger.info(
            "Portfolio Addition Statistics:\n"
            "  Records Read:     %d\n"
            "  Records Added:    %d\n"
            "  Records Rejected: %d\n"
            "  Errors:           %d",
            self._records_read,
            self._records_added,
            self._records_rejected,
            self._error_count,
        )

    def get_statistics(self) -> dict:
        return {
            "records_read": self._records_read,
            "records_added": self._records_added,
            "records_rejected": self._records_rejected,
            "error_count": self._error_count,
        }

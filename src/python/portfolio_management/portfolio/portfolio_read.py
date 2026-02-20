"""Portfolio Record Reading Program - migrated from PORTREAD.cbl.

Demonstrates reading capabilities of portfolio file with sequential
and random access modes.
"""

import logging
from decimal import Decimal
from typing import Optional

from portfolio_management.models.portfolio import PortfolioRecord
from portfolio_management.models.common import ReturnCode

logger = logging.getLogger(__name__)

PROGRAM_ID = "PORTREAD"


class PortfolioReader:
    def __init__(self):
        self._portfolios: list[PortfolioRecord] = []
        self._record_count = 0

    def read_all(self, file_path: str) -> int:
        self._portfolios = []
        self._record_count = 0

        try:
            with open(file_path, "r") as f:
                for line in f:
                    line = line.strip()
                    if not line:
                        continue

                    record = self._parse_record(line)
                    if record is not None:
                        self._portfolios.append(record)
                        self._record_count += 1
                        self._display_record(record)

        except FileNotFoundError:
            logger.error("Error opening file: %s", file_path)
            return ReturnCode.ERROR
        except Exception as e:
            logger.error("Error reading file: %s", e)
            return ReturnCode.ERROR

        logger.info("Total Records Read: %d", self._record_count)
        return ReturnCode.SUCCESS

    def read_by_key(
        self, file_path: str, port_id: str, account_no: str
    ) -> Optional[PortfolioRecord]:
        target_key = f"{port_id}{account_no}"

        try:
            with open(file_path, "r") as f:
                for line in f:
                    line = line.strip()
                    if not line:
                        continue

                    record = self._parse_record(line)
                    if record is not None and record.port_key == target_key:
                        return record

        except FileNotFoundError:
            logger.error("File not found: %s", file_path)
        except Exception as e:
            logger.error("Error reading file: %s", e)

        return None

    def _parse_record(self, line: str) -> Optional[PortfolioRecord]:
        parts = line.split("|")
        if len(parts) < 7:
            return None

        try:
            return PortfolioRecord(
                port_id=parts[0].strip(),
                account_no=parts[1].strip(),
                client_name=parts[2].strip(),
                client_type=parts[3].strip(),
                create_date=parts[4].strip(),
                last_maint=parts[5].strip(),
                status=parts[6].strip(),
                total_value=Decimal(parts[7].strip()) if len(parts) > 7 else Decimal("0"),
                cash_balance=Decimal(parts[8].strip()) if len(parts) > 8 else Decimal("0"),
                audit_timestamp=parts[9].strip() if len(parts) > 9 else "",
                audit_user=parts[10].strip() if len(parts) > 10 else "",
            )
        except (ValueError, IndexError):
            return None

    def _display_record(self, record: PortfolioRecord) -> None:
        logger.info(
            "Portfolio Record: %d\n"
            "  ID: %s\n"
            "  Account: %s\n"
            "  Client: %s\n"
            "  Status: %s\n"
            "  Total Value: %s",
            self._record_count,
            record.port_id,
            record.account_no,
            record.client_name,
            record.status,
            record.total_value,
        )

    def get_records(self) -> list[PortfolioRecord]:
        return list(self._portfolios)

    @property
    def record_count(self) -> int:
        return self._record_count

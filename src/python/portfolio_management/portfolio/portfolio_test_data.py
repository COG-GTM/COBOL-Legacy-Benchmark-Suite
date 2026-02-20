"""Portfolio Test Data Generator - migrated from PORTTEST.cbl.

Generates test portfolio data for system testing.
"""

import logging
import random
from datetime import datetime
from decimal import Decimal
from typing import Optional

from portfolio_management.models.portfolio import PortfolioRecord
from portfolio_management.models.common import ReturnCode

logger = logging.getLogger(__name__)

PROGRAM_ID = "PORTTEST"

CLIENT_TYPES = ["I", "C", "T"]
STATUS_TYPES = ["A", "C", "S"]
NAME_PREFIX = "TEST"


class PortfolioTestDataGenerator:
    def __init__(self, seed: Optional[int] = None):
        self._record_count = 0
        self._max_records = 100
        if seed is not None:
            random.seed(seed)

    def generate(
        self,
        output_file_path: str,
        max_records: int = 100,
    ) -> int:
        self._record_count = 0
        self._max_records = max_records
        current_date = datetime.now().strftime("%Y%m%d")

        try:
            with open(output_file_path, "w") as f:
                while self._record_count < self._max_records:
                    record = self._generate_record(current_date)
                    f.write(
                        f"{record.port_id}|{record.account_no}|"
                        f"{record.client_name}|{record.client_type}|"
                        f"{record.create_date}|{record.last_maint}|"
                        f"{record.status}|{record.total_value}|"
                        f"{record.cash_balance}|{record.audit_timestamp}|"
                        f"{record.audit_user}\n"
                    )
                    self._record_count += 1

        except Exception as e:
            logger.error("Error generating test data: %s", e)
            return ReturnCode.ERROR

        logger.info("Records generated: %d", self._record_count)
        return ReturnCode.SUCCESS

    def generate_records(self, count: int = 100) -> list[PortfolioRecord]:
        records = []
        current_date = datetime.now().strftime("%Y%m%d")
        for _ in range(count):
            records.append(self._generate_record(current_date))
            self._record_count += 1
        return records

    def _generate_record(self, current_date: str) -> PortfolioRecord:
        port_id = f"PORT{self._record_count:04d}"
        account_no = str(self._record_count + 1000000000)
        client_name = f"{NAME_PREFIX}{self._record_count:05d}"
        client_type = random.choice(CLIENT_TYPES)
        status = random.choice(STATUS_TYPES)
        total_value = Decimal(str(round(random.random() * 1000000, 2)))
        cash_balance = Decimal(str(round(float(total_value) * 0.10, 2)))

        return PortfolioRecord(
            port_id=port_id,
            account_no=account_no,
            client_name=client_name,
            client_type=client_type,
            create_date=current_date,
            last_maint=current_date,
            status=status,
            total_value=total_value,
            cash_balance=cash_balance,
            audit_timestamp=datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f"),
            audit_user="PORTTEST",
        )

    @property
    def record_count(self) -> int:
        return self._record_count

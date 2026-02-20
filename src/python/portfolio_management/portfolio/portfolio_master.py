"""Portfolio Master File Maintenance Program - migrated from PORTMSTR.cbl.

Handles CRUD operations for Portfolio records with validation and error handling.
"""

import logging
from datetime import datetime
from typing import Optional

from portfolio_management.models.portfolio import PortfolioRecord
from portfolio_management.models.common import ReturnCode

logger = logging.getLogger(__name__)

PROGRAM_ID = "PORTMSTR"


class PortfolioMasterManager:
    def __init__(self):
        self._portfolios: dict[str, PortfolioRecord] = {}
        self._file_path: Optional[str] = None

    def initialize(self, file_path: Optional[str] = None) -> int:
        self._file_path = file_path
        if file_path is not None:
            return self._load_from_file(file_path)
        return ReturnCode.SUCCESS

    def process_command(self, command: str, record: PortfolioRecord) -> int:
        command = command.upper().strip()
        if command == "CREATE":
            return self.create_portfolio(record)
        elif command == "READ":
            found = self.read_portfolio(record)
            return ReturnCode.SUCCESS if found is not None else ReturnCode.ERROR
        elif command == "UPDATE":
            return self.update_portfolio(record)
        elif command == "DELETE":
            return self.delete_portfolio(record)
        else:
            logger.error("Invalid command: %s", command)
            return ReturnCode.ERROR

    def create_portfolio(self, record: PortfolioRecord) -> int:
        key = record.port_key
        if key in self._portfolios:
            logger.error("Portfolio already exists: %s", key)
            return ReturnCode.ERROR

        rc = self._validate_portfolio(record)
        if rc != ReturnCode.SUCCESS:
            return rc

        record.create_date = datetime.now().strftime("%Y%m%d")
        record.last_maint = datetime.now().strftime("%Y%m%d")
        record.audit_timestamp = datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f")
        record.audit_user = PROGRAM_ID

        self._portfolios[key] = record
        logger.info("Portfolio created: %s", key)
        return ReturnCode.SUCCESS

    def read_portfolio(self, record: PortfolioRecord) -> Optional[PortfolioRecord]:
        key = record.port_key
        found = self._portfolios.get(key)
        if found is None:
            logger.info("Portfolio not found: %s", key)
            return None
        return found

    def update_portfolio(self, record: PortfolioRecord) -> int:
        key = record.port_key
        if key not in self._portfolios:
            logger.error("Portfolio not found for update: %s", key)
            return ReturnCode.ERROR

        rc = self._validate_portfolio(record)
        if rc != ReturnCode.SUCCESS:
            return rc

        record.last_maint = datetime.now().strftime("%Y%m%d")
        record.audit_timestamp = datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f")
        record.audit_user = PROGRAM_ID

        self._portfolios[key] = record
        logger.info("Portfolio updated: %s", key)
        return ReturnCode.SUCCESS

    def delete_portfolio(self, record: PortfolioRecord) -> int:
        key = record.port_key
        if key not in self._portfolios:
            logger.error("Portfolio not found for deletion: %s", key)
            return ReturnCode.ERROR

        del self._portfolios[key]
        logger.info("Portfolio deleted: %s", key)
        return ReturnCode.SUCCESS

    def _validate_portfolio(self, record: PortfolioRecord) -> int:
        if not record.port_id:
            logger.error("Portfolio ID is required")
            return ReturnCode.ERROR
        if not record.account_no:
            logger.error("Account number is required")
            return ReturnCode.ERROR
        return ReturnCode.SUCCESS

    def save_to_file(self, file_path: Optional[str] = None) -> int:
        path = file_path or self._file_path
        if path is None:
            logger.error("No file path specified")
            return ReturnCode.ERROR

        try:
            with open(path, "w") as f:
                for record in self._portfolios.values():
                    f.write(
                        f"{record.port_id}|{record.account_no}|"
                        f"{record.client_name}|{record.client_type}|"
                        f"{record.create_date}|{record.last_maint}|"
                        f"{record.status}|{record.total_value}|"
                        f"{record.cash_balance}|{record.audit_timestamp}|"
                        f"{record.audit_user}\n"
                    )
            return ReturnCode.SUCCESS
        except Exception as e:
            logger.error("Error saving portfolio file: %s", e)
            return ReturnCode.ERROR

    def _load_from_file(self, file_path: str) -> int:
        try:
            from decimal import Decimal
            with open(file_path, "r") as f:
                for line in f:
                    line = line.strip()
                    if not line:
                        continue
                    parts = line.split("|")
                    if len(parts) >= 9:
                        record = PortfolioRecord(
                            port_id=parts[0].strip(),
                            account_no=parts[1].strip(),
                            client_name=parts[2].strip(),
                            client_type=parts[3].strip(),
                            create_date=parts[4].strip(),
                            last_maint=parts[5].strip(),
                            status=parts[6].strip(),
                            total_value=Decimal(parts[7].strip()),
                            cash_balance=Decimal(parts[8].strip()),
                            audit_timestamp=parts[9].strip() if len(parts) > 9 else "",
                            audit_user=parts[10].strip() if len(parts) > 10 else "",
                        )
                        self._portfolios[record.port_key] = record
            logger.info("Loaded %d portfolios", len(self._portfolios))
            return ReturnCode.SUCCESS
        except FileNotFoundError:
            return ReturnCode.SUCCESS
        except Exception as e:
            logger.error("Error loading portfolio file: %s", e)
            return ReturnCode.ERROR

    def get_all_portfolios(self) -> list[PortfolioRecord]:
        return list(self._portfolios.values())

    @property
    def portfolio_count(self) -> int:
        return len(self._portfolios)

"""Portfolio Deletion Program - migrated from PORTDEL.cbl.

Processes portfolio deletion requests with audit trail recording.
"""

import logging
from datetime import datetime
from typing import Optional

from portfolio_management.models.portfolio import PortfolioRecord
from portfolio_management.models.audit import AuditLogRecord
from portfolio_management.models.common import ReturnCode

logger = logging.getLogger(__name__)

PROGRAM_ID = "PORTDEL"


class PortfolioDeleteProcessor:
    def __init__(self):
        self._portfolios: dict[str, PortfolioRecord] = {}
        self._audit_records: list[AuditLogRecord] = []
        self._records_deleted = 0
        self._records_not_found = 0
        self._error_count = 0

    def initialize(self, portfolio_store: Optional[dict[str, PortfolioRecord]] = None) -> int:
        if portfolio_store is not None:
            self._portfolios = portfolio_store
        return ReturnCode.SUCCESS

    def process_file(self, input_file_path: str) -> int:
        self._records_deleted = 0
        self._records_not_found = 0
        self._error_count = 0

        try:
            with open(input_file_path, "r") as f:
                for line in f:
                    line = line.strip()
                    if not line:
                        continue

                    parts = line.split("|")
                    if len(parts) >= 2:
                        port_id = parts[0].strip()
                        account_no = parts[1].strip()
                        self.delete_portfolio(port_id, account_no)
                    else:
                        self._error_count += 1
                        logger.warning("Invalid deletion record: %s", line[:50])

        except FileNotFoundError:
            logger.error("Input file not found: %s", input_file_path)
            return ReturnCode.ERROR
        except Exception as e:
            logger.error("Error processing deletion file: %s", e)
            return ReturnCode.ERROR

        self._display_statistics()

        if self._error_count > 0:
            return ReturnCode.WARNING
        return ReturnCode.SUCCESS

    def delete_portfolio(self, port_id: str, account_no: str) -> int:
        key = f"{port_id}{account_no}"

        if key not in self._portfolios:
            logger.warning("Portfolio not found for deletion: %s", key)
            self._records_not_found += 1
            return ReturnCode.WARNING

        portfolio = self._portfolios[key]

        self._record_audit(portfolio)

        del self._portfolios[key]
        self._records_deleted += 1
        logger.info("Portfolio deleted: %s", key)
        return ReturnCode.SUCCESS

    def _record_audit(self, portfolio: PortfolioRecord) -> None:
        audit = AuditLogRecord(
            timestamp=datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f"),
            system_id="BATCH",
            user_id="SYSTEM",
            program=PROGRAM_ID,
            audit_type="D",
            audit_action="DELETE",
            audit_status="C",
            key_info=portfolio.port_key,
            before_image=(
                f"{portfolio.port_id}|{portfolio.account_no}|"
                f"{portfolio.client_name}|{portfolio.status}|"
                f"{portfolio.total_value}"
            ),
            after_image="",
        )
        self._audit_records.append(audit)

    def _display_statistics(self) -> None:
        logger.info(
            "Portfolio Deletion Statistics:\n"
            "  Records Deleted:   %d\n"
            "  Records Not Found: %d\n"
            "  Errors:            %d",
            self._records_deleted,
            self._records_not_found,
            self._error_count,
        )

    def get_statistics(self) -> dict:
        return {
            "records_deleted": self._records_deleted,
            "records_not_found": self._records_not_found,
            "error_count": self._error_count,
        }

    def get_audit_records(self) -> list[AuditLogRecord]:
        return list(self._audit_records)

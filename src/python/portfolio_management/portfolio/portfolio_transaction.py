"""Portfolio Transaction Processing - migrated from PORTTRAN.cbl.

Processes buy/sell/transfer/fee transactions with portfolio updates
and audit trail.
"""

import logging
from datetime import datetime

from portfolio_management.models.portfolio import PortfolioRecord
from portfolio_management.models.transaction import TransactionRecord
from portfolio_management.models.audit import AuditLogRecord
from portfolio_management.models.common import ReturnCode, TransactionType

logger = logging.getLogger(__name__)

PROGRAM_ID = "PORTTRAN"


class PortfolioTransactionProcessor:
    def __init__(self):
        self._portfolios: dict[str, PortfolioRecord] = {}
        self._transactions_processed = 0
        self._transactions_failed = 0
        self._audit_records: list[AuditLogRecord] = []

    def initialize(self, portfolios: dict[str, PortfolioRecord]) -> int:
        self._portfolios = portfolios
        self._transactions_processed = 0
        self._transactions_failed = 0
        self._audit_records = []
        return ReturnCode.SUCCESS

    def process_transaction(self, transaction: TransactionRecord) -> int:
        portfolio = self._portfolios.get(transaction.portfolio_id)
        if portfolio is None:
            logger.error("Portfolio not found: %s", transaction.portfolio_id)
            self._transactions_failed += 1
            return ReturnCode.ERROR

        rc = self._validate_transaction(transaction, portfolio)
        if rc != ReturnCode.SUCCESS:
            self._transactions_failed += 1
            return rc

        before_value = portfolio.total_value

        if transaction.trans_type == TransactionType.BUY:
            rc = self._process_buy(portfolio, transaction)
        elif transaction.trans_type == TransactionType.SELL:
            rc = self._process_sell(portfolio, transaction)
        elif transaction.trans_type == TransactionType.TRANSFER:
            rc = self._process_transfer(portfolio, transaction)
        elif transaction.trans_type == TransactionType.FEE:
            rc = self._process_fee(portfolio, transaction)
        else:
            logger.error("Unknown transaction type: %s", transaction.trans_type)
            self._transactions_failed += 1
            return ReturnCode.ERROR

        if rc == ReturnCode.SUCCESS:
            self._transactions_processed += 1
            self._record_audit(
                portfolio, transaction, str(before_value), str(portfolio.total_value)
            )
            portfolio.last_maint = datetime.now().strftime("%Y%m%d")
        else:
            self._transactions_failed += 1

        return rc

    def _validate_transaction(
        self, transaction: TransactionRecord, portfolio: PortfolioRecord
    ) -> int:
        if portfolio.status != "A":
            logger.error("Portfolio %s is not active (status: %s)", portfolio.port_id, portfolio.status)
            return ReturnCode.ERROR

        if transaction.amount < 0:
            logger.error("Negative transaction amount: %s", transaction.amount)
            return ReturnCode.ERROR

        return ReturnCode.SUCCESS

    def _process_buy(
        self, portfolio: PortfolioRecord, transaction: TransactionRecord
    ) -> int:
        if portfolio.cash_balance < transaction.amount:
            logger.error(
                "Insufficient cash balance for buy: %s < %s",
                portfolio.cash_balance,
                transaction.amount,
            )
            return ReturnCode.ERROR

        portfolio.cash_balance -= transaction.amount
        portfolio.total_value = portfolio.total_value
        logger.info(
            "Buy processed: %s qty=%s amt=%s",
            portfolio.port_id,
            transaction.quantity,
            transaction.amount,
        )
        return ReturnCode.SUCCESS

    def _process_sell(
        self, portfolio: PortfolioRecord, transaction: TransactionRecord
    ) -> int:
        portfolio.cash_balance += transaction.amount
        logger.info(
            "Sell processed: %s qty=%s amt=%s",
            portfolio.port_id,
            transaction.quantity,
            transaction.amount,
        )
        return ReturnCode.SUCCESS

    def _process_transfer(
        self, portfolio: PortfolioRecord, transaction: TransactionRecord
    ) -> int:
        logger.info(
            "Transfer processed: %s qty=%s",
            portfolio.port_id,
            transaction.quantity,
        )
        return ReturnCode.SUCCESS

    def _process_fee(
        self, portfolio: PortfolioRecord, transaction: TransactionRecord
    ) -> int:
        portfolio.cash_balance -= transaction.amount
        portfolio.total_value -= transaction.amount
        logger.info(
            "Fee processed: %s amt=%s",
            portfolio.port_id,
            transaction.amount,
        )
        return ReturnCode.SUCCESS

    def _record_audit(
        self,
        portfolio: PortfolioRecord,
        transaction: TransactionRecord,
        before_image: str,
        after_image: str,
    ) -> None:
        audit = AuditLogRecord(
            timestamp=datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f"),
            system_id="BATCH",
            user_id=transaction.audit_user or "SYSTEM",
            program=PROGRAM_ID,
            audit_type="T",
            audit_action=transaction.trans_type,
            audit_status="C",
            key_info=f"{portfolio.port_id}|{transaction.transaction_key}",
            before_image=before_image,
            after_image=after_image,
        )
        self._audit_records.append(audit)

    def get_statistics(self) -> dict:
        return {
            "transactions_processed": self._transactions_processed,
            "transactions_failed": self._transactions_failed,
            "audit_records": len(self._audit_records),
        }

    def get_audit_records(self) -> list[AuditLogRecord]:
        return list(self._audit_records)

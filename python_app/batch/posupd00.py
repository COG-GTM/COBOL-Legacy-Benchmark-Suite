"""Position Update module - replaces POSUPD00.cbl.

Updates portfolio positions based on validated transactions.
Second step in the batch pipeline: TRNVAL00 -> POSUPD00 -> HISTLD00.

COBOL program flow:
- 0000-MAIN: Initialize -> Process -> Terminate
- 1000-INITIALIZE: Open files, connect DB2
- 2000-PROCESS: Read validated transactions, update positions
- 2100-UPDATE-POSITION: Apply transaction to position
- 2200-INSERT-POSITION: Create new position if not found
- 3000-TERMINATE: Final commit, close files, set RC
"""

import logging
from datetime import datetime
from decimal import Decimal

from python_app.common.db2 import CommitController, StatisticsCollector
from python_app.common.errors import ErrorHandler, ErrorSeverity
from python_app.models.position import PositionRecord, PositionStatus
from python_app.models.return_code import RC_SUCCESS, RC_WARNING, RC_ERROR
from python_app.models.transaction import TransactionRecord, TransactionType

logger = logging.getLogger("portfolio.batch.posupd00")


class PositionUpdater:
    """Position update processor replacing POSUPD00.cbl.

    Applies validated transactions to portfolio positions.
    Creates new positions for new portfolio/investment combinations.
    """

    def __init__(self) -> None:
        self.error_handler = ErrorHandler("POSUPD00")
        self.stats = StatisticsCollector("POSUPD00")
        self.commit_ctrl = CommitController(commit_interval=1000)
        self.positions: dict[str, PositionRecord] = {}
        self.records_read = 0
        self.records_updated = 0
        self.records_inserted = 0
        self.records_error = 0

    def initialize(self, existing_positions: list[PositionRecord] | None = None) -> None:
        """Initialize processing - replaces 1000-INITIALIZE."""
        self.stats.initialize()
        self.commit_ctrl.initialize()
        self.positions.clear()
        self.records_read = 0
        self.records_updated = 0
        self.records_inserted = 0
        self.records_error = 0

        # Load existing positions into lookup dictionary
        if existing_positions:
            for pos in existing_positions:
                key = f"{pos.portfolio_id}:{pos.investment_id}"
                self.positions[key] = pos

        logger.info(
            "POSUPD00 initialized - %d existing positions loaded - %s",
            len(self.positions), datetime.now().isoformat(),
        )

    def update_position(self, transaction: TransactionRecord) -> bool:
        """Update or create a position - replaces 2100-UPDATE-POSITION / 2200-INSERT-POSITION.

        Applies transaction to the matching position:
        - BUY: Increase quantity, add to cost basis
        - SELL: Decrease quantity, reduce cost basis proportionally
        - TRANSFER: Adjust quantity (positive or negative)
        - FEE: Reduce market value
        """
        key = f"{transaction.portfolio_id}:{transaction.investment_id}"
        position = self.positions.get(key)

        try:
            if position is None:
                # 2200-INSERT-POSITION: Create new position
                position = PositionRecord(
                    portfolio_id=transaction.portfolio_id,
                    date=transaction.date,
                    investment_id=transaction.investment_id,
                    quantity=Decimal("0"),
                    cost_basis=Decimal("0"),
                    market_value=Decimal("0"),
                    currency=transaction.currency,
                    status=PositionStatus.ACTIVE,
                )
                self.positions[key] = position
                self.records_inserted += 1
                self.stats.update("inserts")

            # Apply transaction based on type
            if transaction.type == TransactionType.BUY:
                position.quantity += transaction.quantity
                position.cost_basis += transaction.amount
                position.market_value += transaction.amount

            elif transaction.type == TransactionType.SELL:
                if position.quantity < transaction.quantity:
                    self.error_handler.log_error(
                        f"Insufficient quantity for SELL: have {position.quantity}, "
                        f"need {transaction.quantity}",
                        severity=ErrorSeverity.WARNING,
                        error_code="PQTY",
                    )
                    return False

                # Proportional cost basis reduction
                if position.quantity > 0:
                    ratio = transaction.quantity / position.quantity
                    cost_reduction = position.cost_basis * ratio
                    position.cost_basis -= cost_reduction
                position.quantity -= transaction.quantity
                position.market_value -= transaction.amount

                # Close position if quantity is zero
                if position.quantity == 0:
                    position.status = PositionStatus.CLOSED

            elif transaction.type == TransactionType.TRANSFER:
                position.quantity += transaction.quantity
                position.market_value += transaction.amount

            elif transaction.type == TransactionType.FEE:
                position.market_value -= abs(transaction.amount)

            # Update position date
            position.date = transaction.date
            self.records_updated += 1
            self.stats.update("updates")
            return True

        except Exception as exc:
            self.error_handler.log_error(
                f"Position update failed: {exc}",
                severity=ErrorSeverity.ERROR,
                error_code="PUPD",
                details=f"Portfolio={transaction.portfolio_id}, Investment={transaction.investment_id}",
                exc=exc,
            )
            self.records_error += 1
            self.stats.update("errors")
            return False

    def process_batch(
        self,
        transactions: list[TransactionRecord],
        existing_positions: list[PositionRecord] | None = None,
    ) -> int:
        """Process a batch of validated transactions - replaces 0000-MAIN.

        Returns the return code (RC):
        - 0: All updates successful
        - 4: Some updates had errors (warnings)
        - 8: Too many errors or processing failure
        """
        self.initialize(existing_positions)

        try:
            for txn in transactions:
                self.records_read += 1
                self.stats.update("reads")
                self.update_position(txn)

            return self.terminate()
        except Exception as exc:
            self.error_handler.log_error(
                f"Batch processing failed: {exc}",
                severity=ErrorSeverity.FATAL,
                error_code="PBAT",
                exc=exc,
            )
            return RC_ERROR

    def terminate(self) -> int:
        """Terminate processing and determine return code - replaces 3000-TERMINATE."""
        final_stats = self.stats.terminate()

        logger.info(
            "POSUPD00 complete: read=%d, updated=%d, inserted=%d, errors=%d",
            self.records_read, self.records_updated, self.records_inserted, self.records_error,
        )
        logger.info("Statistics: %s", final_stats)

        if self.records_error == 0:
            rc = RC_SUCCESS
        elif self.records_error <= self.records_read * 0.1:
            rc = RC_WARNING
        else:
            rc = RC_ERROR

        logger.info("POSUPD00 return code: %d", rc)
        return rc

    def get_positions(self) -> list[PositionRecord]:
        """Get all current positions."""
        return list(self.positions.values())

"""
Position Update Batch Processor - migrated from POSUPD00.cbl.

Original COBOL Program: POSUPD00.cbl
Purpose: Updates portfolio positions based on validated transactions

Key Functions:
- P100-INIT: Initialize program, open files
- P200-PROCESS: Main processing loop
- P300-UPDATE-POSITION: Update position for transaction
- P400-CALCULATE-COST-BASIS: Calculate new cost basis
- P500-WRITE-HISTORY: Write position history
- P900-TERMINATE: Close files, write statistics
- 9000-ERROR-ROUTINE: Error handling
- 9100-CHECKPOINT: Checkpoint/restart

Position Update Logic:
- BUY: Add quantity, recalculate cost basis (weighted average)
- SELL: Subtract quantity, calculate gain/loss
- TRANSFER: Move position between portfolios
- FEE: Deduct from cash balance
"""

from datetime import date, datetime
from decimal import Decimal

from sqlalchemy import and_
from sqlalchemy.orm import Session

from app.batch.base import BatchProcessor
from app.models.database import (
    PortfolioMaster,
    PositionHistory,
    PositionMaster,
    TransactionHistory,
)
from app.models.domain import TransactionStatus
from app.utils.exceptions import BatchProcessingError


class PositionUpdater(BatchProcessor):
    """
    Position update batch processor.
    Replaces POSUPD00.cbl functionality.
    """

    def __init__(
        self,
        db: Session,
        process_date: date | None = None,
    ):
        super().__init__(
            db=db,
            job_name="POSUPD",
            program_name="POSUPD00",
            process_date=process_date,
        )

        self.positions_updated = 0
        self.positions_created = 0
        self.history_written = 0

    def initialize(self) -> None:
        """
        Initialize position update processing.
        Replaces P100-INIT in POSUPD00.cbl.
        """
        self.logger.info("Initializing position update processing")

    def process(self) -> None:
        """
        Main processing loop.
        Replaces P200-PROCESS in POSUPD00.cbl.

        Process pending transactions and update positions.
        """
        pending_transactions = self.db.query(TransactionHistory).filter(
            TransactionHistory.status == TransactionStatus.PENDING.value
        ).order_by(
            TransactionHistory.transaction_date,
            TransactionHistory.transaction_time,
        ).all()

        self.logger.info(
            "Processing pending transactions",
            count=len(pending_transactions),
        )

        for txn in pending_transactions:
            self.records_read += 1

            if self.restart_key and txn.transaction_id <= self.restart_key:
                continue

            try:
                self._update_position(txn)
                self._write_history(txn)

                txn.status = TransactionStatus.DONE.value
                txn.process_date = datetime.utcnow()
                txn.process_user = "POSUPD00"

                self.records_processed += 1
                self.records_written += 1

                if self.should_checkpoint():
                    self.checkpoint(txn.transaction_id)

            except Exception as e:
                self.error_logger.log_error(
                    message=f"Error processing transaction {txn.transaction_id}: {e}",
                    error_code="E007",
                    severity=8,
                    category="PR",
                )
                txn.status = TransactionStatus.FAILED.value
                if not self.increment_error_count():
                    raise BatchProcessingError(
                        f"Max errors exceeded processing transaction {txn.transaction_id}",
                        job_name=self.job_name,
                        program=self.program_name,
                    )

    def terminate(self) -> None:
        """
        Terminate position update processing.
        Replaces P900-TERMINATE in POSUPD00.cbl.
        """
        self.logger.info(
            "Position update complete",
            records_read=self.records_read,
            records_processed=self.records_processed,
            positions_updated=self.positions_updated,
            positions_created=self.positions_created,
            history_written=self.history_written,
        )

    def _update_position(self, txn: TransactionHistory) -> None:
        """
        Update position for a transaction.
        Replaces P300-UPDATE-POSITION in POSUPD00.cbl.
        """
        position = self.db.query(PositionMaster).filter(
            and_(
                PositionMaster.portfolio_id == txn.portfolio_id,
                PositionMaster.investment_id == txn.investment_id,
                PositionMaster.position_date == txn.transaction_date,
            )
        ).first()

        if txn.transaction_type == "BU":
            self._process_buy(txn, position)
        elif txn.transaction_type == "SL":
            self._process_sell(txn, position)
        elif txn.transaction_type == "TR":
            self._process_transfer(txn, position)
        elif txn.transaction_type == "FE":
            self._process_fee(txn)

    def _process_buy(
        self,
        txn: TransactionHistory,
        position: PositionMaster | None,
    ) -> None:
        """
        Process buy transaction.
        Replaces BUY logic in P300-UPDATE-POSITION.

        Cost basis calculation (weighted average):
        NEW-COST-BASIS = (OLD-COST-BASIS + TRANSACTION-AMOUNT) / NEW-QUANTITY
        """
        if position:
            old_quantity = position.quantity or Decimal("0")
            old_cost_basis = position.cost_basis or Decimal("0")

            new_quantity = old_quantity + txn.quantity
            new_cost_basis = old_cost_basis + txn.total_amount

            position.quantity = new_quantity
            position.cost_basis = new_cost_basis
            position.market_value = new_quantity * txn.price
            position.last_maint_date = datetime.utcnow()
            position.last_maint_user = "POSUPD00"

            self.positions_updated += 1
        else:
            position = PositionMaster(
                portfolio_id=txn.portfolio_id,
                investment_id=txn.investment_id,
                position_date=txn.transaction_date,
                quantity=txn.quantity,
                cost_basis=txn.total_amount,
                market_value=txn.quantity * txn.price,
                currency_code=txn.currency_code,
                status="A",
                last_maint_date=datetime.utcnow(),
                last_maint_user="POSUPD00",
            )
            self.db.add(position)
            self.positions_created += 1

    def _process_sell(
        self,
        txn: TransactionHistory,
        position: PositionMaster | None,
    ) -> None:
        """
        Process sell transaction.
        Replaces SELL logic in P300-UPDATE-POSITION.

        Gain/Loss calculation:
        GAIN-LOSS = SALE-AMOUNT - (COST-BASIS * QUANTITY-SOLD / TOTAL-QUANTITY)
        """
        if not position:
            raise BatchProcessingError(
                f"Cannot sell: no position found for {txn.portfolio_id}/{txn.investment_id}",
                job_name=self.job_name,
                program=self.program_name,
            )

        if position.quantity < txn.quantity:
            raise BatchProcessingError(
                f"Cannot sell: insufficient quantity ({position.quantity} < {txn.quantity})",
                job_name=self.job_name,
                program=self.program_name,
            )

        old_quantity = position.quantity
        cost_per_unit = position.cost_basis / old_quantity if old_quantity > 0 else Decimal("0")
        cost_of_sold = cost_per_unit * txn.quantity

        new_quantity = old_quantity - txn.quantity
        new_cost_basis = position.cost_basis - cost_of_sold

        position.quantity = new_quantity
        position.cost_basis = new_cost_basis
        position.market_value = new_quantity * txn.price
        position.last_maint_date = datetime.utcnow()
        position.last_maint_user = "POSUPD00"

        if new_quantity == 0:
            position.status = "C"

        self.positions_updated += 1

    def _process_transfer(
        self,
        txn: TransactionHistory,
        position: PositionMaster | None,
    ) -> None:
        """
        Process transfer transaction.
        Replaces TRANSFER logic in P300-UPDATE-POSITION.
        """
        self._process_sell(txn, position)

    def _process_fee(self, txn: TransactionHistory) -> None:
        """
        Process fee transaction.
        Replaces FEE logic in P300-UPDATE-POSITION.
        Deducts fee from portfolio cash balance.
        """
        portfolio = self.db.query(PortfolioMaster).filter(
            PortfolioMaster.portfolio_id == txn.portfolio_id
        ).first()

        if portfolio:
            portfolio.cash_balance = (portfolio.cash_balance or Decimal("0")) - txn.total_amount
            portfolio.last_maint_date = datetime.utcnow()
            portfolio.last_maint_user = "POSUPD00"

    def _write_history(self, txn: TransactionHistory) -> None:
        """
        Write position history record.
        Replaces P500-WRITE-HISTORY in POSUPD00.cbl.
        """
        position = self.db.query(PositionMaster).filter(
            and_(
                PositionMaster.portfolio_id == txn.portfolio_id,
                PositionMaster.investment_id == txn.investment_id,
            )
        ).order_by(PositionMaster.position_date.desc()).first()

        cost_basis = position.cost_basis if position else Decimal("0")

        if txn.transaction_type == "SL":
            old_quantity = (position.quantity or Decimal("0")) + txn.quantity
            cost_per_unit = cost_basis / old_quantity if old_quantity > 0 else Decimal("0")
            cost_of_sold = cost_per_unit * txn.quantity
            gain_loss = txn.amount - cost_of_sold
        else:
            gain_loss = Decimal("0")

        history = PositionHistory(
            account_no=txn.portfolio_id,
            portfolio_id=txn.portfolio_id,
            trans_date=txn.transaction_date,
            trans_time=txn.transaction_time,
            trans_type=txn.transaction_type,
            security_id=txn.investment_id,
            quantity=txn.quantity,
            price=txn.price,
            amount=txn.amount,
            fees=txn.fees or Decimal("0"),
            total_amount=txn.total_amount,
            cost_basis=cost_basis,
            gain_loss=gain_loss,
            process_date=date.today(),
            process_time=datetime.utcnow().time(),
            program_id="POSUPD00",
            user_id="BATCH",
            audit_timestamp=datetime.utcnow(),
        )

        self.db.add(history)
        self.history_written += 1

    def get_results(self) -> dict:
        """Get processing results."""
        return {
            "records_read": self.records_read,
            "records_processed": self.records_processed,
            "positions_updated": self.positions_updated,
            "positions_created": self.positions_created,
            "history_written": self.history_written,
            "records_error": self.records_error,
            "return_code": self.return_code,
        }

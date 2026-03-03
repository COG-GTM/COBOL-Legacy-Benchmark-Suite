"""Position Updater — batch processing program.

Replaces: POSUPD00 (src/programs/batch/POSUPD00.cbl)

Updates the Investment Positions table based on validated transactions.
This is the second step in the batch pipeline (after TRNVAL00).

Original COBOL flow:
  0000-MAIN-PROCESS
    1000-INITIALIZE
    2000-PROCESS-TRANSACTIONS (loop)
      2100-READ-TRANSACTION
      2200-LOOKUP-POSITION
      2300-UPDATE-POSITION / 2400-CREATE-POSITION
    3000-FINALIZE

Return codes:
  RC 0 = success
  RC 4 = warnings
  RC 8 = error
  RC 12+ = severe
"""

from __future__ import annotations

import logging
from dataclasses import dataclass, field
from datetime import date, datetime
from decimal import Decimal

from sqlalchemy import select
from sqlalchemy.orm import Session

from portfolio_manager.models.copybook_models import (
    TransactionRecord,
    TransactionType,
)
from portfolio_manager.models.database import InvestmentPosition
from portfolio_manager.services.error_handler import ErrorProcessor

logger = logging.getLogger(__name__)


@dataclass
class UpdateResult:
    """Result of the position update batch run."""

    records_read: int = 0
    positions_updated: int = 0
    positions_created: int = 0
    records_error: int = 0
    return_code: int = 0
    error_messages: list[str] = field(default_factory=list)


class PositionUpdater:
    """Position update batch processor.

    Replaces POSUPD00 (src/programs/batch/POSUPD00.cbl).
    """

    PROGRAM_ID = "POSUPD00"
    COMMIT_THRESHOLD = 1000  # commit every N records

    def __init__(self, session: Session):
        self._session = session
        self._error_processor = ErrorProcessor(session)

    def run(
        self,
        transactions: list[TransactionRecord],
        process_date: date | None = None,
        user_id: str = "BATCH",
    ) -> UpdateResult:
        """Run the position update batch.

        Replaces 0000-MAIN-PROCESS flow from POSUPD00.cbl.

        Args:
            transactions: Validated transaction records from TRNVAL00.
            process_date: Processing date (defaults to today).
            user_id: Processing user ID.

        Returns:
            UpdateResult with processing statistics.
        """
        result = UpdateResult()
        proc_date = process_date or date.today()

        logger.info(
            "%s: Starting position updates for %d transactions",
            self.PROGRAM_ID,
            len(transactions),
        )

        for i, txn in enumerate(transactions, 1):
            result.records_read += 1

            try:
                self._process_transaction(txn, proc_date, user_id, result)
            except Exception as exc:
                result.records_error += 1
                error_msg = f"Error processing txn {txn.portfolio_id}/{txn.investment_id}: {exc}"
                result.error_messages.append(error_msg)
                logger.error("%s: %s", self.PROGRAM_ID, error_msg)

                self._error_processor.process_error(
                    program_id=self.PROGRAM_ID,
                    category="PR",
                    error_code="E007",
                    severity=3,
                    error_text=str(exc)[:200],
                    details=f"portfolio={txn.portfolio_id} investment={txn.investment_id}",
                )

            # Periodic commit (replaces COBOL COMMIT-THRESHOLD logic)
            if i % self.COMMIT_THRESHOLD == 0:
                self._session.flush()
                logger.info(
                    "%s: Checkpoint at record %d", self.PROGRAM_ID, i
                )

        # 3000-FINALIZE
        if result.records_error == 0:
            result.return_code = 0
        elif result.records_error < result.records_read:
            result.return_code = 4
        else:
            result.return_code = 8

        logger.info(
            "%s: Complete — read=%d updated=%d created=%d errors=%d RC=%d",
            self.PROGRAM_ID,
            result.records_read,
            result.positions_updated,
            result.positions_created,
            result.records_error,
            result.return_code,
        )

        return result

    def _process_transaction(
        self,
        txn: TransactionRecord,
        proc_date: date,
        user_id: str,
        result: UpdateResult,
    ) -> None:
        """Process a single transaction to update positions.

        Replaces POSUPD00 paragraphs:
          2200-LOOKUP-POSITION  -> query existing position
          2300-UPDATE-POSITION  -> update if exists
          2400-CREATE-POSITION  -> create if not exists
        """
        # 2200-LOOKUP-POSITION
        position = self._session.execute(
            select(InvestmentPosition).where(
                InvestmentPosition.portfolio_id == txn.portfolio_id,
                InvestmentPosition.investment_id == txn.investment_id,
                InvestmentPosition.position_date == proc_date,
            )
        ).scalar_one_or_none()

        if position is not None:
            # 2300-UPDATE-POSITION
            self._update_position(position, txn, user_id)
            result.positions_updated += 1
        else:
            # 2400-CREATE-POSITION
            self._create_position(txn, proc_date, user_id)
            result.positions_created += 1

    def _update_position(
        self,
        position: InvestmentPosition,
        txn: TransactionRecord,
        user_id: str,
    ) -> None:
        """Update an existing position based on the transaction.

        Replaces POSUPD00 paragraph 2300-UPDATE-POSITION.
        """
        if txn.transaction_type == TransactionType.BUY:
            position.quantity += txn.quantity
            position.cost_basis += txn.amount
        elif txn.transaction_type == TransactionType.SELL:
            position.quantity -= txn.quantity
            # Proportional cost basis reduction
            if position.quantity > 0:
                ratio = txn.quantity / (position.quantity + txn.quantity)
                position.cost_basis -= position.cost_basis * ratio
            else:
                position.cost_basis = Decimal("0")
        elif txn.transaction_type == TransactionType.FEE:
            position.cost_basis += txn.amount

        position.last_maint_date = datetime.now()
        position.last_maint_user = user_id

    def _create_position(
        self,
        txn: TransactionRecord,
        proc_date: date,
        user_id: str,
    ) -> None:
        """Create a new position from a transaction.

        Replaces POSUPD00 paragraph 2400-CREATE-POSITION.
        """
        position = InvestmentPosition(
            portfolio_id=txn.portfolio_id,
            investment_id=txn.investment_id,
            position_date=proc_date,
            quantity=txn.quantity,
            cost_basis=txn.amount,
            market_value=txn.amount,
            currency_code=txn.currency.value,
            last_maint_date=datetime.now(),
            last_maint_user=user_id,
        )
        self._session.add(position)

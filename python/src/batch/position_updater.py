"""
Position updater translated from COBOL program POSUPD00.cbl.

Replaces:
  - POSUPD00.cbl 1000-PROCESS-INITIALIZE: Initialize position update run
  - POSUPD00.cbl 2000-PROCESS-TRANSACTIONS: Main processing loop
  - POSUPD00.cbl 2100-UPDATE-POSITION: Update a single position
  - POSUPD00.cbl 2200-CALCULATE-VALUES: Calculate new values
  - POSUPD00.cbl 3000-PROCESS-TERMINATE: Finalize processing

All arithmetic uses Decimal - NEVER float.
COBOL COMP-3 PIC S9(11)V9(4) -> Decimal with 4 decimal places.
COBOL COMP-3 PIC S9(13)V9(2) -> Decimal with 2 decimal places.
"""

import logging
from datetime import date, datetime
from decimal import ROUND_HALF_UP, Decimal

from sqlalchemy.orm import Session

from src.common.constants import (
    COMMIT_THRESHOLD,
    PositionStatus,
    ReturnCode,
    TransactionStatus,
    TransactionType,
)
from src.db.repository import PortfolioRepository, PositionRepository, TransactionRepository
from src.db.tables import InvestmentPosition

logger = logging.getLogger(__name__)


class PositionUpdater:
    """
    Position update processor.

    Translates POSUPD00.cbl paragraph structure.
    Uses Decimal throughout for COBOL COMP-3 precision.
    """

    def __init__(self, session: Session) -> None:
        self._session = session
        self._portfolio_repo = PortfolioRepository(session)
        self._position_repo = PositionRepository(session)
        self._transaction_repo = TransactionRepository(session)
        # Counters from POSUPD00.cbl WS-COUNTERS
        self.records_read = 0
        self.records_updated = 0
        self.records_created = 0
        self.records_error = 0
        self.commit_count = 0

    def process(self, process_date: date) -> ReturnCode:
        """
        Process all pending transactions and update positions.

        Translates POSUPD00.cbl main flow:
          PERFORM 1000-PROCESS-INITIALIZE
          PERFORM 2000-PROCESS-TRANSACTIONS
          PERFORM 3000-PROCESS-TERMINATE
        """
        logger.info("Starting position update for date %s", process_date)

        # 1000-PROCESS-INITIALIZE
        self._initialize()

        # 2000-PROCESS-TRANSACTIONS
        rc = self._process_transactions(process_date)

        # 3000-PROCESS-TERMINATE
        self._terminate()

        return rc

    def _initialize(self) -> None:
        """
        Initialize position update run.

        Translates POSUPD00.cbl 1000-PROCESS-INITIALIZE.
        """
        self.records_read = 0
        self.records_updated = 0
        self.records_created = 0
        self.records_error = 0
        self.commit_count = 0
        logger.info("Position update initialized")

    def _process_transactions(self, process_date: date) -> ReturnCode:
        """
        Process all pending transactions.

        Translates POSUPD00.cbl 2000-PROCESS-TRANSACTIONS.
        Reads pending transactions and updates positions accordingly.
        """
        pending = self._transaction_repo.list_pending()
        logger.info("Found %d pending transactions", len(pending))

        for txn in pending:
            self.records_read += 1
            # Use SAVEPOINT so a failure only rolls back this single
            # transaction, not all uncommitted successful work.
            nested = self._session.begin_nested()
            try:
                self._update_position(txn, process_date)
                # Mark transaction as done
                txn.status = TransactionStatus.DONE.value
                txn.process_date = datetime.now()
                self._transaction_repo.update(txn)
                nested.commit()
                self.records_updated += 1

                # Periodic commit (from POSUPD00.cbl commit threshold)
                if self.records_updated % COMMIT_THRESHOLD == 0:
                    self._session.commit()
                    self.commit_count += 1
                    logger.info(
                        "Commit at record %d (commit #%d)",
                        self.records_updated,
                        self.commit_count,
                    )

            except Exception as exc:
                self.records_error += 1
                # Rollback only this SAVEPOINT; prior successful work is preserved
                nested.rollback()
                txn.status = TransactionStatus.FAILED.value
                self._transaction_repo.update(txn)
                self._session.commit()
                logger.error(
                    "Error processing transaction %s: %s",
                    txn.transaction_id,
                    exc,
                )

        # Final commit
        self._session.commit()
        self.commit_count += 1

        if self.records_error > 0:
            return ReturnCode.WARNING
        return ReturnCode.SUCCESS

    def _update_position(self, txn: object, process_date: date) -> None:
        """
        Update a single position based on a transaction.

        Translates POSUPD00.cbl 2100-UPDATE-POSITION.

        Transaction type dispatch:
          BUY  -> increase quantity and cost basis
          SELL -> decrease quantity and cost basis
          FEE  -> decrease cost basis only
        """
        portfolio_id = txn.portfolio_id  # type: ignore[attr-defined]
        investment_id = txn.investment_id  # type: ignore[attr-defined]
        trn_type = txn.trn_type  # type: ignore[attr-defined]
        quantity = Decimal(str(txn.quantity))  # type: ignore[attr-defined]
        amount = Decimal(str(txn.amount))  # type: ignore[attr-defined]

        # Get or create position
        position = self._position_repo.get(portfolio_id, investment_id, process_date)
        if position is None:
            # Carry forward from latest position
            latest = self._position_repo.get_latest_position(portfolio_id, investment_id)
            position = InvestmentPosition(
                portfolio_id=portfolio_id,
                investment_id=investment_id,
                position_date=process_date,
                quantity=latest.quantity if latest else Decimal("0.0000"),
                cost_basis=latest.cost_basis if latest else Decimal("0.00"),
                market_value=latest.market_value if latest else Decimal("0.00"),
                currency=txn.currency,  # type: ignore[attr-defined]
                status=PositionStatus.ACTIVE.value,
                last_maint_date=datetime.now(),
                last_maint_user="BATCH",
            )
            self._position_repo.create(position)
            self.records_created += 1

        # 2200-CALCULATE-VALUES: Apply transaction to position
        match trn_type:
            case TransactionType.BUY | "BU":
                position.quantity = (position.quantity + quantity).quantize(
                    Decimal("0.0001"), rounding=ROUND_HALF_UP
                )
                position.cost_basis = (position.cost_basis + amount).quantize(
                    Decimal("0.01"), rounding=ROUND_HALF_UP
                )
                position.market_value = (position.market_value + amount).quantize(
                    Decimal("0.01"), rounding=ROUND_HALF_UP
                )
            case TransactionType.SELL | "SL":
                position.quantity = (position.quantity - quantity).quantize(
                    Decimal("0.0001"), rounding=ROUND_HALF_UP
                )
                # Proportional cost basis reduction
                if position.quantity <= Decimal("0"):
                    position.quantity = Decimal("0.0000")
                    position.cost_basis = Decimal("0.00")
                    position.market_value = Decimal("0.00")
                    position.status = PositionStatus.CLOSED.value
                else:
                    orig_qty = position.quantity + quantity
                    ratio = quantity / orig_qty
                    cost_reduction = (position.cost_basis * ratio).quantize(
                        Decimal("0.01"), rounding=ROUND_HALF_UP
                    )
                    position.cost_basis = (position.cost_basis - cost_reduction).quantize(
                        Decimal("0.01"), rounding=ROUND_HALF_UP
                    )
                    position.market_value = (position.market_value - amount).quantize(
                        Decimal("0.01"), rounding=ROUND_HALF_UP
                    )
            case TransactionType.FEE | "FE":
                position.cost_basis = (position.cost_basis - amount).quantize(
                    Decimal("0.01"), rounding=ROUND_HALF_UP
                )
            case TransactionType.TRANSFER | "TR":
                pass  # Transfers don't affect individual positions
            case _:
                raise ValueError(f"Unknown transaction type: {trn_type}")

        position.last_maint_date = datetime.now()
        position.last_maint_user = "BATCH"
        self._position_repo.update(position)

    def _terminate(self) -> None:
        """
        Finalize processing.

        Translates POSUPD00.cbl 3000-PROCESS-TERMINATE.
        """
        logger.info(
            "Position update complete: read=%d, updated=%d, created=%d, errors=%d, commits=%d",
            self.records_read,
            self.records_updated,
            self.records_created,
            self.records_error,
            self.commit_count,
        )

"""
Position updater translated from COBOL program POSUPD00.cbl.

Processes validated transactions to update investment positions.
CRITICAL: Uses Decimal arithmetic throughout — never float.
"""

import logging
from datetime import date, datetime
from decimal import Decimal

from sqlalchemy.orm import Session

from src.common.constants import (
    PositionStatus,
    ReturnCode,
    TransactionStatus,
    TransactionType,
)
from src.common.error_handler import BatchError
from src.db.repository import PositionRepository, TransactionRepository
from src.db.tables import InvestmentPosition, TransactionHistory

logger = logging.getLogger(__name__)


class PositionUpdater:
    """
    Process validated transactions to update investment positions.
    Translates POSUPD00.cbl batch position update logic.

    CRITICAL: All monetary and quantity calculations use Decimal, never float.
    """

    def __init__(self, session: Session):
        self.session = session
        self.position_repo = PositionRepository(session)
        self.trn_repo = TransactionRepository(session)
        self.records_processed: int = 0
        self.records_updated: int = 0
        self.error_count: int = 0

    def process_pending_transactions(self, process_date: date | None = None) -> ReturnCode:
        """
        Process all pending transactions.
        Translates POSUPD00.cbl 2000-PROCESS main loop.
        """
        pending = self.trn_repo.list_pending()
        logger.info("Processing %d pending transactions", len(pending))

        for trn in pending:
            try:
                self._process_single(trn)
                self.records_processed += 1
            except Exception as e:
                self.error_count += 1
                logger.error("Error processing transaction %s: %s", trn.transaction_id, e)
                trn.status = TransactionStatus.FAILED.value
                self.session.flush()

        if self.error_count > 0:
            logger.warning(
                "Position update complete: %d processed, %d errors",
                self.records_processed, self.error_count,
            )
            return ReturnCode.WARNING if self.error_count < self.records_processed else ReturnCode.ERROR

        logger.info("Position update complete: %d processed successfully", self.records_processed)
        return ReturnCode.SUCCESS

    def _process_single(self, trn: TransactionHistory) -> None:
        """
        Process a single transaction.
        Translates POSUPD00.cbl EVALUATE TRN-TYPE.
        """
        match trn.trn_type:
            case TransactionType.BUY.value:
                self._process_buy(trn)
            case TransactionType.SELL.value:
                self._process_sell(trn)
            case TransactionType.TRANSFER.value:
                self._process_transfer(trn)
            case TransactionType.FEE.value:
                self._process_fee(trn)
            case _:
                raise BatchError(
                    f"Unknown transaction type: {trn.trn_type}",
                    job_name="POSUPD00",
                    error_code="PU01",
                )

        trn.status = TransactionStatus.DONE.value
        trn.process_date = date.today()
        self.session.flush()
        self.records_updated += 1

    def _process_buy(self, trn: TransactionHistory) -> None:
        """
        Translates POSUPD00.cbl 2100-PROCESS-BUY.
        Buy increases position quantity and cost basis.
        """
        quantity = Decimal(str(trn.quantity))
        price = Decimal(str(trn.price))
        amount = (quantity * price).quantize(Decimal("0.01"))

        position = self.position_repo.get_latest(trn.portfolio_id, trn.investment_id)

        if position is not None:
            old_qty = Decimal(str(position.quantity))
            old_cost = Decimal(str(position.cost_basis))

            new_qty = (old_qty + quantity).quantize(Decimal("0.0001"))
            new_cost = (old_cost + amount).quantize(Decimal("0.01"))
            new_mkt = (new_qty * price).quantize(Decimal("0.01"))

            position.quantity = new_qty
            position.cost_basis = new_cost
            position.market_value = new_mkt
            position.last_maint_date = datetime.now()
            position.last_maint_user = trn.process_user or "BATCH"
            self.position_repo.update(position)
        else:
            mkt_value = (quantity * price).quantize(Decimal("0.01"))
            new_position = InvestmentPosition(
                portfolio_id=trn.portfolio_id,
                investment_id=trn.investment_id,
                position_date=trn.trn_date,
                quantity=quantity,
                cost_basis=amount,
                market_value=mkt_value,
                currency_code=trn.currency_code,
                status=PositionStatus.ACTIVE.value,
                last_maint_date=datetime.now(),
                last_maint_user=trn.process_user or "BATCH",
            )
            self.position_repo.create(new_position)

    def _process_sell(self, trn: TransactionHistory) -> None:
        """
        Translates POSUPD00.cbl 2200-PROCESS-SELL.
        Sell decreases position quantity and updates cost basis.
        """
        position = self.position_repo.get_latest(trn.portfolio_id, trn.investment_id)
        if position is None:
            raise BatchError(
                f"No position for sell: {trn.portfolio_id}/{trn.investment_id}",
                job_name="POSUPD00",
                error_code="PU02",
            )

        quantity = Decimal(str(trn.quantity))
        price = Decimal(str(trn.price))
        old_qty = Decimal(str(position.quantity))
        old_cost = Decimal(str(position.cost_basis))

        if quantity > old_qty:
            raise BatchError(
                f"Insufficient quantity: have {old_qty}, selling {quantity}",
                job_name="POSUPD00",
                error_code="PU03",
            )

        # Calculate proportional cost basis
        avg_cost = (old_cost / old_qty).quantize(Decimal("0.0001")) if old_qty > 0 else Decimal("0.0000")
        cost_of_sold = (avg_cost * quantity).quantize(Decimal("0.01"))
        proceeds = (quantity * price).quantize(Decimal("0.01"))

        new_qty = (old_qty - quantity).quantize(Decimal("0.0001"))
        new_cost = (old_cost - cost_of_sold).quantize(Decimal("0.01"))
        new_mkt = (new_qty * price).quantize(Decimal("0.01"))

        position.quantity = new_qty
        position.cost_basis = new_cost
        position.market_value = new_mkt
        position.last_maint_date = datetime.now()
        position.last_maint_user = trn.process_user or "BATCH"

        if new_qty == Decimal("0.0000"):
            position.status = PositionStatus.CLOSED.value

        self.position_repo.update(position)

        # Store gain/loss on transaction
        trn.gain_loss = (proceeds - cost_of_sold).quantize(Decimal("0.01"))
        trn.cost_basis = cost_of_sold

    def _process_transfer(self, trn: TransactionHistory) -> None:
        """Translates POSUPD00.cbl 2300-PROCESS-TRANSFER."""
        # Transfer processing — amount already calculated
        pass

    def _process_fee(self, trn: TransactionHistory) -> None:
        """Translates POSUPD00.cbl 2400-PROCESS-FEE."""
        amount = Decimal(str(trn.amount))
        trn.fees = amount
        trn.total_amount = amount

    def get_summary(self) -> dict:
        """Get processing summary."""
        return {
            "records_processed": self.records_processed,
            "records_updated": self.records_updated,
            "error_count": self.error_count,
        }

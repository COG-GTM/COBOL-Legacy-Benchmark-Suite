"""Position Updater - converted from POSUPD00.cbl equivalent.

This module provides position update functionality similar to
the COBOL POSUPD00 batch program.

COBOL Program Reference:
- Updates portfolio positions based on validated transactions
- Calculates cost basis, market value, and gain/loss
- Maintains position history
"""

from datetime import datetime
from decimal import Decimal
from typing import Optional

from sqlalchemy.orm import Session

from app.database.models import InvestmentPosition, TransactionHistory
from app.models.error import ErrorCategory, ReturnCode
from app.models.transaction import TransactionRecord, TransactionStatus, TransactionType
from app.utils.error_handler import ErrorHandler
from app.utils.logging import get_logger, log_batch_end, log_batch_start, log_transaction

logger = get_logger(__name__)


class PositionUpdater:
    """Position Updater - replaces POSUPD00 batch program.

    This class updates portfolio positions based on validated transactions,
    calculating cost basis, market value, and realized gain/loss.
    """

    PROGRAM_NAME = "POSUPD00"

    def __init__(self, db: Session):
        self.db = db
        self.error_handler = ErrorHandler(db, self.PROGRAM_NAME)
        self.records_read = 0
        self.records_updated = 0
        self.records_created = 0
        self.records_error = 0
        self.return_code = ReturnCode.SUCCESS

    def process_transaction(
        self, transaction: TransactionRecord, user_id: str = "BATCH"
    ) -> tuple[bool, str]:
        """Process a single transaction and update position.

        Args:
            transaction: Validated transaction to process
            user_id: User ID for audit trail

        Returns:
            Tuple of (success, message)
        """
        self.records_read += 1

        try:
            if transaction.data.type == TransactionType.BUY:
                return self._process_buy(transaction, user_id)
            elif transaction.data.type == TransactionType.SELL:
                return self._process_sell(transaction, user_id)
            elif transaction.data.type == TransactionType.TRANSFER:
                return self._process_transfer(transaction, user_id)
            elif transaction.data.type == TransactionType.FEE:
                return self._process_fee(transaction, user_id)
            else:
                return False, f"Unknown transaction type: {transaction.data.type}"
        except Exception as e:
            self.records_error += 1
            self.return_code = max(self.return_code, ReturnCode.ERROR)
            self.error_handler.handle_error(
                e,
                code="PU01",
                category=ErrorCategory.PROCESSING,
                details=f"Transaction: {transaction.transaction_id}",
                user_id=user_id,
            )
            return False, str(e)

    def _process_buy(
        self, transaction: TransactionRecord, user_id: str
    ) -> tuple[bool, str]:
        """Process a buy transaction.

        Buy transactions increase position quantity and cost basis.
        """
        position = self._get_or_create_position(
            transaction.portfolio_id,
            transaction.investment_id,
            transaction.key.date,
        )

        position.quantity += transaction.data.quantity
        position.cost_basis += transaction.data.amount
        position.last_maint_date = datetime.now()
        position.last_maint_user = user_id

        self._save_position(position)
        self._record_transaction(transaction, user_id)

        log_transaction(
            logger,
            self.PROGRAM_NAME,
            transaction.transaction_id,
            "BUY",
            "SUCCESS",
            {
                "portfolio_id": transaction.portfolio_id,
                "investment_id": transaction.investment_id,
                "quantity_change": float(transaction.data.quantity),
                "new_quantity": float(position.quantity),
            },
        )

        return True, f"Buy processed: {transaction.data.quantity} units added"

    def _process_sell(
        self, transaction: TransactionRecord, user_id: str
    ) -> tuple[bool, str]:
        """Process a sell transaction.

        Sell transactions decrease position quantity and calculate gain/loss.
        """
        position = self._get_position(
            transaction.portfolio_id,
            transaction.investment_id,
        )

        if not position:
            self.records_error += 1
            return False, f"No position found for {transaction.investment_id}"

        if position.quantity < transaction.data.quantity:
            self.records_error += 1
            return False, f"Insufficient quantity: have {position.quantity}, selling {transaction.data.quantity}"

        avg_cost = position.cost_basis / position.quantity if position.quantity > 0 else Decimal("0")
        cost_of_sold = avg_cost * transaction.data.quantity
        gain_loss = transaction.data.amount - cost_of_sold

        position.quantity -= transaction.data.quantity
        position.cost_basis -= cost_of_sold
        position.last_maint_date = datetime.now()
        position.last_maint_user = user_id

        if position.quantity == 0:
            position.cost_basis = Decimal("0")

        self._save_position(position)
        self._record_transaction(transaction, user_id, gain_loss=gain_loss)

        log_transaction(
            logger,
            self.PROGRAM_NAME,
            transaction.transaction_id,
            "SELL",
            "SUCCESS",
            {
                "portfolio_id": transaction.portfolio_id,
                "investment_id": transaction.investment_id,
                "quantity_sold": float(transaction.data.quantity),
                "gain_loss": float(gain_loss),
            },
        )

        return True, f"Sell processed: {transaction.data.quantity} units sold, gain/loss: {gain_loss}"

    def _process_transfer(
        self, transaction: TransactionRecord, user_id: str
    ) -> tuple[bool, str]:
        """Process a transfer transaction.

        Transfer transactions move positions between portfolios.
        """
        self._record_transaction(transaction, user_id)
        self.records_updated += 1

        log_transaction(
            logger,
            self.PROGRAM_NAME,
            transaction.transaction_id,
            "TRANSFER",
            "SUCCESS",
        )

        return True, "Transfer recorded"

    def _process_fee(
        self, transaction: TransactionRecord, user_id: str
    ) -> tuple[bool, str]:
        """Process a fee transaction.

        Fee transactions are recorded but don't affect positions.
        """
        self._record_transaction(transaction, user_id)
        self.records_updated += 1

        log_transaction(
            logger,
            self.PROGRAM_NAME,
            transaction.transaction_id,
            "FEE",
            "SUCCESS",
        )

        return True, "Fee recorded"

    def _get_or_create_position(
        self, portfolio_id: str, investment_id: str, position_date: str
    ) -> InvestmentPosition:
        """Get existing position or create new one."""
        from datetime import datetime as dt

        date_obj = dt.strptime(position_date, "%Y%m%d").date()

        position = (
            self.db.query(InvestmentPosition)
            .filter(
                InvestmentPosition.portfolio_id == portfolio_id,
                InvestmentPosition.investment_id == investment_id,
                InvestmentPosition.position_date == date_obj,
            )
            .first()
        )

        if not position:
            position = InvestmentPosition(
                portfolio_id=portfolio_id,
                investment_id=investment_id,
                position_date=date_obj,
                quantity=Decimal("0"),
                cost_basis=Decimal("0"),
                market_value=Decimal("0"),
                currency_code="USD",
                last_maint_date=datetime.now(),
                last_maint_user="BATCH",
            )
            self.db.add(position)
            self.records_created += 1
        else:
            self.records_updated += 1

        return position

    def _get_position(
        self, portfolio_id: str, investment_id: str
    ) -> Optional[InvestmentPosition]:
        """Get most recent position for portfolio/investment."""
        return (
            self.db.query(InvestmentPosition)
            .filter(
                InvestmentPosition.portfolio_id == portfolio_id,
                InvestmentPosition.investment_id == investment_id,
            )
            .order_by(InvestmentPosition.position_date.desc())
            .first()
        )

    def _save_position(self, position: InvestmentPosition) -> None:
        """Save position to database."""
        self.db.add(position)
        self.db.flush()

    def _record_transaction(
        self,
        transaction: TransactionRecord,
        user_id: str,
        gain_loss: Decimal = Decimal("0"),
    ) -> None:
        """Record transaction in history."""
        from datetime import datetime as dt

        trans_date = dt.strptime(transaction.key.date, "%Y%m%d").date()
        trans_time = dt.strptime(transaction.key.time, "%H%M%S").time()

        history = TransactionHistory(
            transaction_id=transaction.transaction_id,
            portfolio_id=transaction.portfolio_id,
            transaction_date=trans_date,
            transaction_time=trans_time,
            investment_id=transaction.investment_id,
            transaction_type=transaction.data.type.value,
            quantity=transaction.data.quantity,
            price=transaction.data.price,
            amount=transaction.data.amount,
            currency_code=transaction.data.currency,
            status=TransactionStatus.DONE.value,
            process_date=datetime.now(),
            process_user=user_id,
        )
        self.db.add(history)

    def process_batch(
        self, transactions: list[TransactionRecord], user_id: str = "BATCH"
    ) -> dict:
        """Process a batch of transactions.

        Args:
            transactions: List of validated transactions
            user_id: User ID for audit trail

        Returns:
            Processing statistics
        """
        log_batch_start(
            logger, self.PROGRAM_NAME, "POSUPD", datetime.now().strftime("%Y%m%d")
        )

        results = []
        for transaction in transactions:
            success, message = self.process_transaction(transaction, user_id)
            results.append({"transaction_id": transaction.transaction_id, "success": success, "message": message})

        try:
            self.db.commit()
        except Exception as e:
            self.db.rollback()
            self.return_code = ReturnCode.SEVERE
            logger.error(f"Batch commit failed: {e}")

        log_batch_end(
            logger,
            self.PROGRAM_NAME,
            "POSUPD",
            self.return_code,
            self.records_read,
            self.records_updated + self.records_created,
            self.records_error,
        )

        return {
            "records_read": self.records_read,
            "records_updated": self.records_updated,
            "records_created": self.records_created,
            "records_error": self.records_error,
            "return_code": self.return_code,
            "results": results,
        }

    def get_statistics(self) -> dict:
        """Get processing statistics."""
        return {
            "records_read": self.records_read,
            "records_updated": self.records_updated,
            "records_created": self.records_created,
            "records_error": self.records_error,
            "return_code": self.return_code,
        }

"""
Transaction processing service translated from COBOL programs:
  - PORTTRAN.cbl: Portfolio transaction processing (BU, SL, TR, FE)
  - PORTHIST.cbl: Transaction history recording

Translates PORTTRAN.cbl dispatch:
  EVALUATE WS-TRN-TYPE
    WHEN 'BU' PERFORM 2100-PROCESS-BUY
    WHEN 'SL' PERFORM 2200-PROCESS-SELL
    WHEN 'TR' PERFORM 2300-PROCESS-TRANSFER
    WHEN 'FE' PERFORM 2400-PROCESS-FEE
  END-EVALUATE

All monetary arithmetic uses Decimal, never float.
"""

import logging
from datetime import date, datetime, time
from decimal import Decimal

from sqlalchemy.orm import Session

from src.common.audit import AuditService
from src.common.constants import (
    AuditAction,
    PositionStatus,
    TransactionStatus,
    TransactionType,
)
from src.common.error_handler import NotFoundError, ValidationError
from src.db.repository import PortfolioRepository, PositionRepository, TransactionRepository
from src.db.tables import InvestmentPosition, PortfolioMaster, TransactionHistory
from src.models.transaction import TransactionRecord

logger = logging.getLogger(__name__)


class TransactionService:
    """
    Transaction processing service.

    Each COBOL paragraph from PORTTRAN.cbl becomes a private method:
      1000-INIT-TRANSACTION    -> _init_transaction()
      2000-PROCESS-TRANSACTION -> process()
      2100-PROCESS-BUY         -> _process_buy()
      2200-PROCESS-SELL        -> _process_sell()
      2300-PROCESS-TRANSFER    -> _process_transfer()
      2400-PROCESS-FEE         -> _process_fee()
      3000-UPDATE-PORTFOLIO    -> _update_portfolio()
      4000-WRITE-HISTORY       -> _write_history()
    """

    def __init__(self, session: Session) -> None:
        self._session = session
        self._portfolio_repo = PortfolioRepository(session)
        self._position_repo = PositionRepository(session)
        self._transaction_repo = TransactionRepository(session)
        self._audit = AuditService(session)
        # Counters from PORTTRAN.cbl WS-COUNTERS
        self.read_count = 0
        self.process_count = 0
        self.error_count = 0

    def process(self, record: TransactionRecord, user_id: str = "SYSTEM") -> TransactionHistory:
        """
        Process a single transaction.

        Translates PORTTRAN.cbl 2000-PROCESS-TRANSACTION:
          EVALUATE WS-TRN-TYPE dispatch to buy/sell/transfer/fee handlers.

        Args:
            record: Transaction record to process.
            user_id: Processing user ID.

        Returns:
            Created TransactionHistory record.

        Raises:
            NotFoundError: If portfolio not found.
            ValidationError: If transaction fails validation.
        """
        self.read_count += 1

        # 1000-INIT-TRANSACTION: Verify portfolio exists
        portfolio = self._portfolio_repo.get_by_id(record.portfolio_id)
        if portfolio is None:
            self.error_count += 1
            raise NotFoundError(f"Portfolio not found: {record.portfolio_id}")

        # Dispatch based on transaction type (EVALUATE WS-TRN-TYPE)
        match record.trn_type:
            case TransactionType.BUY:
                self._process_buy(portfolio, record)
            case TransactionType.SELL:
                self._process_sell(portfolio, record)
            case TransactionType.TRANSFER:
                self._process_transfer(portfolio, record)
            case TransactionType.FEE:
                self._process_fee(portfolio, record)
            case _:
                self.error_count += 1
                raise ValidationError(
                    f"Unknown transaction type: {record.trn_type}",
                    field="trn_type",
                )

        # 3000-UPDATE-PORTFOLIO: Update portfolio totals
        self._update_portfolio(portfolio, user_id)

        # 4000-WRITE-HISTORY: Record transaction
        transaction = self._write_history(record, user_id)
        self.process_count += 1

        # Audit trail
        self._audit.log_portfolio_change(
            user_id=user_id,
            action=AuditAction.UPDATE,
            portfolio_id=record.portfolio_id,
            after_image=f"TXN {record.trn_type.value}: {record.amount}",
        )

        return transaction

    def _process_buy(self, portfolio: PortfolioMaster, record: TransactionRecord) -> None:
        """
        Process buy transaction.

        Translates PORTTRAN.cbl 2100-PROCESS-BUY:
          ADD WS-TRN-QUANTITY TO PORT-TOTAL-UNITS
          ADD WS-TRN-AMOUNT TO PORT-TOTAL-COST
        """
        position = self._get_or_create_position(
            portfolio.portfolio_id, record.investment_id, record.trn_date,
            record.currency.value,
        )
        # ADD quantity to position
        position.quantity += record.quantity
        # ADD amount to cost basis
        position.cost_basis += record.amount
        position.market_value += record.amount
        position.last_maint_date = datetime.now()
        self._position_repo.update(position)

        # Update portfolio totals
        portfolio.total_value += record.amount
        portfolio.cash_balance -= record.amount

    def _process_sell(self, portfolio: PortfolioMaster, record: TransactionRecord) -> None:
        """
        Process sell transaction.

        Translates PORTTRAN.cbl 2200-PROCESS-SELL:
          IF WS-TRN-QUANTITY > PORT-TOTAL-UNITS -> error
          SUBTRACT WS-TRN-QUANTITY FROM PORT-TOTAL-UNITS
        """
        # First check latest position exists and has sufficient units
        latest = self._position_repo.get_latest_position(
            portfolio.portfolio_id, record.investment_id
        )
        if latest is None:
            self.error_count += 1
            raise ValidationError(
                f"No position found for investment {record.investment_id}",
                field="investment_id",
            )

        # Validate sufficient units against latest known position
        if record.quantity > latest.quantity:
            self.error_count += 1
            raise ValidationError(
                f"Insufficient units: have {latest.quantity}, selling {record.quantity}",
                field="quantity",
            )

        # Create position for the transaction date (preserves historical snapshots)
        position = self._get_or_create_position(
            portfolio.portfolio_id, record.investment_id, record.trn_date,
            record.currency.value,
        )

        # SUBTRACT quantity from position
        position.quantity -= record.quantity
        # Proportional cost basis reduction
        if position.quantity == Decimal("0"):
            cost_reduction = position.cost_basis
            position.cost_basis = Decimal("0.00")
            position.market_value = Decimal("0.00")
            position.status = PositionStatus.CLOSED.value
        else:
            ratio = record.quantity / (position.quantity + record.quantity)
            cost_reduction = (position.cost_basis * ratio).quantize(Decimal("0.01"))
            position.cost_basis -= cost_reduction
            position.market_value -= record.amount

        position.last_maint_date = datetime.now()
        self._position_repo.update(position)

        # Update portfolio totals
        portfolio.total_value -= record.amount
        portfolio.cash_balance += record.amount

    def _process_transfer(self, portfolio: PortfolioMaster, record: TransactionRecord) -> None:
        """
        Process transfer transaction.

        Translates PORTTRAN.cbl 2300-PROCESS-TRANSFER.
        Adjusts cash balance for transfer in/out.
        """
        # Transfer affects cash balance only
        portfolio.cash_balance += record.amount

    def _process_fee(self, portfolio: PortfolioMaster, record: TransactionRecord) -> None:
        """
        Process fee transaction.

        Translates PORTTRAN.cbl 2400-PROCESS-FEE:
          SUBTRACT WS-TRN-AMOUNT FROM PORT-TOTAL-COST
        """
        # SUBTRACT fee from portfolio cost/cash
        portfolio.total_value -= record.amount
        portfolio.cash_balance -= record.amount

    def _update_portfolio(self, portfolio: PortfolioMaster, user_id: str) -> None:
        """
        Update portfolio after transaction processing.

        Translates PORTTRAN.cbl 3000-UPDATE-PORTFOLIO paragraph.
        """
        portfolio.last_maint_date = datetime.now()
        portfolio.last_maint_user = user_id
        portfolio.last_trans_date = date.today()
        self._portfolio_repo.update(portfolio)

    def _write_history(self, record: TransactionRecord, user_id: str) -> TransactionHistory:
        """
        Write transaction to history.

        Translates PORTTRAN.cbl 4000-WRITE-HISTORY / PORTHIST.cbl.
        """
        now = datetime.now()
        trn_time_str = record.trn_time.strftime("%H%M%S") if isinstance(record.trn_time, time) else "000000"
        transaction_id = record.transaction_id or (
            record.trn_date.strftime("%Y%m%d") + trn_time_str + record.sequence_no
        )

        transaction = TransactionHistory(
            transaction_id=transaction_id,
            trn_date=record.trn_date,
            trn_time=trn_time_str,
            portfolio_id=record.portfolio_id,
            sequence_no=record.sequence_no,
            investment_id=record.investment_id,
            trn_type=record.trn_type.value,
            quantity=record.quantity,
            price=record.price,
            amount=record.amount,
            currency=record.currency.value,
            status=TransactionStatus.DONE.value,
            process_date=now,
            process_user=user_id,
        )
        self._transaction_repo.create(transaction)
        return transaction

    def _get_or_create_position(
        self, portfolio_id: str, investment_id: str, position_date: date,
        currency: str = "USD",
    ) -> InvestmentPosition:
        """Get existing position or create a new one."""
        position = self._position_repo.get(portfolio_id, investment_id, position_date)
        if position is None:
            # Check for latest existing position to carry forward
            latest = self._position_repo.get_latest_position(portfolio_id, investment_id)
            position = InvestmentPosition(
                portfolio_id=portfolio_id,
                investment_id=investment_id,
                position_date=position_date,
                quantity=latest.quantity if latest else Decimal("0.0000"),
                cost_basis=latest.cost_basis if latest else Decimal("0.00"),
                market_value=latest.market_value if latest else Decimal("0.00"),
                currency=currency,
                status=PositionStatus.ACTIVE.value,
                last_maint_date=datetime.now(),
                last_maint_user="SYSTEM",
            )
            self._position_repo.create(position)
        return position

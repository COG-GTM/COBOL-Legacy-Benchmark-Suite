"""
Transaction service translated from COBOL programs:
- PORTTRAN.cbl (Process portfolio transactions)
- PORTHIST.cbl (Transaction history management)

Processes buy, sell, transfer, and fee transactions.
Updates positions based on transactions.
CRITICAL: Uses Decimal arithmetic throughout — never float.
"""

import logging
from datetime import date, datetime, time
from decimal import Decimal

from sqlalchemy.orm import Session

from src.common.audit import write_audit_record
from src.common.constants import (
    AuditAction,
    AuditType,
    PositionStatus,
    TransactionStatus,
    TransactionType,
)
from src.common.error_handler import ValidationError
from src.db.repository import (
    PortfolioRepository,
    PositionRepository,
    TransactionRepository,
)
from src.db.tables import InvestmentPosition, PortfolioMaster, TransactionHistory

logger = logging.getLogger(__name__)


class TransactionService:
    """
    Process portfolio transactions.
    Translates PORTTRAN.cbl EVALUATE TRN-TYPE dispatcher.
    """

    def __init__(self, session: Session):
        self.session = session
        self.portfolio_repo = PortfolioRepository(session)
        self.position_repo = PositionRepository(session)
        self.trn_repo = TransactionRepository(session)

    def process_transaction(
        self,
        portfolio_id: str,
        investment_id: str,
        trn_type: str,
        quantity: Decimal,
        price: Decimal,
        trn_date: date | None = None,
        trn_time: time | None = None,
        currency: str = "USD",
        user: str = "SYSTEM",
    ) -> TransactionHistory:
        """
        Process a single transaction.
        Translates PORTTRAN.cbl main processing logic.
        """
        trn_date = trn_date or date.today()
        trn_time = trn_time or datetime.now().time()

        # Validate portfolio exists
        portfolio = self.portfolio_repo.get_by_id(portfolio_id)
        if portfolio is None:
            raise ValidationError(
                f"Portfolio {portfolio_id} not found",
                field="portfolio_id",
                error_code="T001",
                program="PORTTRAN",
            )

        # CRITICAL: All arithmetic uses Decimal
        quantity = Decimal(str(quantity))
        price = Decimal(str(price))
        amount = (quantity * price).quantize(Decimal("0.01"))

        # Create transaction record
        time_str = trn_time.strftime("%H%M%S")
        seq = self._generate_sequence(portfolio_id, trn_date, time_str)

        transaction = TransactionHistory(
            portfolio_id=portfolio_id,
            investment_id=investment_id,
            trn_date=trn_date,
            trn_time=time_str,
            sequence_no=seq,
            trn_type=trn_type,
            quantity=quantity,
            price=price,
            amount=amount,
            currency_code=currency,
            status=TransactionStatus.PENDING.value,
            process_user=user,
        )

        # Dispatch by transaction type (EVALUATE TRN-TYPE)
        match trn_type:
            case TransactionType.BUY:
                self._process_buy(portfolio, transaction)
            case TransactionType.SELL:
                self._process_sell(portfolio, transaction)
            case TransactionType.TRANSFER:
                self._process_transfer(portfolio, transaction)
            case TransactionType.FEE:
                self._process_fee(portfolio, transaction)
            case _:
                raise ValidationError(
                    f"Invalid transaction type: {trn_type}",
                    field="trn_type",
                    error_code="T002",
                    program="PORTTRAN",
                )

        # Mark as done and persist
        transaction.status = TransactionStatus.DONE.value
        transaction.process_date = date.today()
        self.trn_repo.create(transaction)

        # Audit
        write_audit_record(
            session=self.session,
            audit_type=AuditType.TRANSACTION,
            action=AuditAction.CREATE,
            user_id=user,
            program="PORTTRAN",
            key_info=f"{portfolio_id}/{investment_id}",
            message=f"{trn_type} qty={quantity} price={price} amt={amount}",
        )

        logger.info(
            "Transaction processed: %s %s qty=%s price=%s for portfolio %s",
            trn_type, investment_id, quantity, price, portfolio_id,
        )
        return transaction

    def _process_buy(
        self, portfolio: PortfolioMaster, trn: TransactionHistory
    ) -> None:
        """
        Process buy transaction. Increases position quantity.
        Translates PORTTRAN.cbl 2100-PROCESS-BUY.
        """
        position = self.position_repo.get_latest(
            trn.portfolio_id, trn.investment_id
        )

        amount = Decimal(str(trn.amount))
        quantity = Decimal(str(trn.quantity))
        price = Decimal(str(trn.price))

        if position is not None:
            # Update existing position
            old_qty = Decimal(str(position.quantity))
            old_cost = Decimal(str(position.cost_basis))
            new_qty = (old_qty + quantity).quantize(Decimal("0.0001"))
            new_cost = (old_cost + amount).quantize(Decimal("0.01"))
            new_mkt = (new_qty * price).quantize(Decimal("0.01"))

            position.quantity = new_qty
            position.cost_basis = new_cost
            position.market_value = new_mkt
            position.last_maint_date = datetime.now()
            position.last_maint_user = trn.process_user
            self.position_repo.update(position)
        else:
            # Create new position
            mkt_value = (quantity * price).quantize(Decimal("0.01"))
            position = InvestmentPosition(
                portfolio_id=trn.portfolio_id,
                investment_id=trn.investment_id,
                position_date=trn.trn_date,
                quantity=quantity,
                cost_basis=amount,
                market_value=mkt_value,
                currency_code=trn.currency_code,
                status=PositionStatus.ACTIVE.value,
                last_maint_date=datetime.now(),
                last_maint_user=trn.process_user,
            )
            self.position_repo.create(position)

        trn.cost_basis = amount
        trn.total_amount = amount

    def _process_sell(
        self, portfolio: PortfolioMaster, trn: TransactionHistory
    ) -> None:
        """
        Process sell transaction. Decreases position quantity.
        Translates PORTTRAN.cbl 2200-PROCESS-SELL.
        """
        position = self.position_repo.get_latest(
            trn.portfolio_id, trn.investment_id
        )

        if position is None:
            raise ValidationError(
                f"No position found for {trn.investment_id} in portfolio {trn.portfolio_id}",
                field="investment_id",
                error_code="T003",
                program="PORTTRAN",
            )

        quantity = Decimal(str(trn.quantity))
        price = Decimal(str(trn.price))
        old_qty = Decimal(str(position.quantity))

        if quantity > old_qty:
            raise ValidationError(
                f"Insufficient quantity: have {old_qty}, selling {quantity}",
                field="quantity",
                error_code="T004",
                program="PORTTRAN",
            )

        # Calculate gain/loss
        old_cost = Decimal(str(position.cost_basis))
        avg_cost_per_unit = (old_cost / old_qty).quantize(Decimal("0.0001")) if old_qty > 0 else Decimal("0.0000")
        cost_of_sold = (avg_cost_per_unit * quantity).quantize(Decimal("0.01"))
        proceeds = (quantity * price).quantize(Decimal("0.01"))
        gain_loss = (proceeds - cost_of_sold).quantize(Decimal("0.01"))

        new_qty = (old_qty - quantity).quantize(Decimal("0.0001"))
        new_cost = (old_cost - cost_of_sold).quantize(Decimal("0.01"))
        new_mkt = (new_qty * price).quantize(Decimal("0.01"))

        position.quantity = new_qty
        position.cost_basis = new_cost
        position.market_value = new_mkt
        position.last_maint_date = datetime.now()
        position.last_maint_user = trn.process_user

        if new_qty == Decimal("0.0000"):
            position.status = PositionStatus.CLOSED.value

        self.position_repo.update(position)

        trn.cost_basis = cost_of_sold
        trn.gain_loss = gain_loss
        trn.total_amount = proceeds

    def _process_transfer(
        self, portfolio: PortfolioMaster, trn: TransactionHistory
    ) -> None:
        """
        Process transfer transaction.
        Translates PORTTRAN.cbl 2300-PROCESS-TRANSFER.
        """
        trn.total_amount = Decimal(str(trn.amount))

    def _process_fee(
        self, portfolio: PortfolioMaster, trn: TransactionHistory
    ) -> None:
        """
        Process fee transaction.
        Translates PORTTRAN.cbl 2400-PROCESS-FEE.
        """
        amount = Decimal(str(trn.amount))
        trn.fees = amount
        trn.total_amount = amount

    def _generate_sequence(
        self, portfolio_id: str, trn_date: date, trn_time: str
    ) -> str:
        """Generate unique sequence number for VSAM key.

        Uses MAX(sequence_no)+1 so the result is always one past the
        highest existing sequence, avoiding duplicates from deleted rows
        that a simple COUNT approach would produce.
        """
        from sqlalchemy import func, select

        from src.db.tables import TransactionHistory

        stmt = (
            select(func.max(TransactionHistory.sequence_no))
            .where(
                TransactionHistory.portfolio_id == portfolio_id,
                TransactionHistory.trn_date == trn_date,
            )
        )
        current_max = self.session.scalar(stmt)
        next_seq = int(current_max) + 1 if current_max else 1
        return str(next_seq).zfill(6)

"""
Transaction Service - migrated from INQHIST.cbl and transaction processing programs.
Handles transaction history inquiries and transaction processing.
"""

from datetime import date, datetime
from decimal import Decimal

from sqlalchemy import desc
from sqlalchemy.orm import Session

from app.models.database import (
    AuditLog,
    PortfolioMaster,
    PositionHistory,
    TransactionHistory,
)
from app.models.domain import (
    AuditAction,
    AuditStatus,
    AuditType,
    TransactionStatus,
    TransactionType,
)
from app.utils.exceptions import (
    PortfolioNotFoundError,
    TransactionNotFoundError,
)


class TransactionService:
    """
    Transaction service for managing transactions and history.
    Replaces INQHIST.cbl and transaction processing functionality.
    """

    def __init__(self, db: Session):
        self.db = db

    def get_transaction_history(
        self,
        portfolio_id: str,
        start_date: date | None = None,
        end_date: date | None = None,
        transaction_type: str | None = None,
        limit: int = 100,
        offset: int = 0,
    ) -> list[TransactionHistory]:
        """
        Get transaction history for a portfolio.
        Replaces P200-GET-HISTORY in INQHIST.cbl.

        The original COBOL query:
        SELECT TRANS_DATE, TRANS_TYPE, TRANS_UNITS, TRANS_PRICE, TRANS_AMOUNT
        FROM POSHIST
        WHERE ACCOUNT_NO = ?
        ORDER BY TRANS_DATE DESC
        """
        query = self.db.query(TransactionHistory).filter(
            TransactionHistory.portfolio_id == portfolio_id.upper()
        )

        if start_date:
            query = query.filter(TransactionHistory.transaction_date >= start_date)

        if end_date:
            query = query.filter(TransactionHistory.transaction_date <= end_date)

        if transaction_type:
            query = query.filter(TransactionHistory.transaction_type == transaction_type.upper())

        query = query.order_by(desc(TransactionHistory.transaction_date))

        return query.offset(offset).limit(limit).all()

    def get_transaction(self, transaction_id: str) -> TransactionHistory:
        """
        Get a specific transaction by ID.
        """
        transaction = self.db.query(TransactionHistory).filter(
            TransactionHistory.transaction_id == transaction_id
        ).first()

        if not transaction:
            raise TransactionNotFoundError(f"Transaction not found: {transaction_id}")

        return transaction

    def get_position_history(
        self,
        account_no: str,
        start_date: date | None = None,
        end_date: date | None = None,
        limit: int = 100,
    ) -> list[PositionHistory]:
        """
        Get position history from POSHIST table.
        Replaces DB2 query in INQHIST.cbl.
        """
        query = self.db.query(PositionHistory).filter(
            PositionHistory.account_no == account_no
        )

        if start_date:
            query = query.filter(PositionHistory.trans_date >= start_date)

        if end_date:
            query = query.filter(PositionHistory.trans_date <= end_date)

        query = query.order_by(desc(PositionHistory.trans_date))

        return query.limit(limit).all()

    def create_transaction(
        self,
        portfolio_id: str,
        investment_id: str,
        transaction_type: TransactionType,
        quantity: Decimal,
        price: Decimal,
        user_id: str,
        fees: Decimal = Decimal("0"),
    ) -> TransactionHistory:
        """
        Create a new transaction.
        """
        portfolio = self.db.query(PortfolioMaster).filter(
            PortfolioMaster.portfolio_id == portfolio_id.upper()
        ).first()

        if not portfolio:
            raise PortfolioNotFoundError(f"Portfolio not found: {portfolio_id}")

        amount = quantity * price
        total_amount = amount + fees

        transaction_id = self._generate_transaction_id()
        now = datetime.utcnow()

        transaction = TransactionHistory(
            transaction_id=transaction_id,
            portfolio_id=portfolio_id.upper(),
            transaction_date=now.date(),
            transaction_time=now.time(),
            investment_id=investment_id.upper(),
            transaction_type=transaction_type.value,
            quantity=quantity,
            price=price,
            amount=amount,
            fees=fees,
            total_amount=total_amount,
            currency_code="USD",
            status=TransactionStatus.PENDING.value,
            process_date=now,
            process_user=user_id,
        )

        self.db.add(transaction)

        self._log_audit(
            user_id=user_id,
            action=AuditAction.CREATE,
            portfolio_id=portfolio_id,
            message=f"Created transaction: {transaction_type.value} {quantity} {investment_id}",
        )

        return transaction

    def update_transaction_status(
        self,
        transaction_id: str,
        status: TransactionStatus,
        user_id: str,
    ) -> TransactionHistory:
        """
        Update transaction status.
        """
        transaction = self.get_transaction(transaction_id)
        before_status = transaction.status

        transaction.status = status.value
        transaction.process_date = datetime.utcnow()
        transaction.process_user = user_id

        self._log_audit(
            user_id=user_id,
            action=AuditAction.UPDATE,
            portfolio_id=transaction.portfolio_id,
            before_image=f"status={before_status}",
            after_image=f"status={status.value}",
            message=f"Updated transaction status: {transaction_id}",
        )

        return transaction

    def get_transaction_summary(
        self,
        portfolio_id: str,
        start_date: date | None = None,
        end_date: date | None = None,
    ) -> dict:
        """
        Get transaction summary for a portfolio.
        """
        transactions = self.get_transaction_history(
            portfolio_id=portfolio_id,
            start_date=start_date,
            end_date=end_date,
            limit=10000,
        )

        buy_count = sum(1 for t in transactions if t.transaction_type == "BU")
        sell_count = sum(1 for t in transactions if t.transaction_type == "SL")
        transfer_count = sum(1 for t in transactions if t.transaction_type == "TR")
        fee_count = sum(1 for t in transactions if t.transaction_type == "FE")

        total_buy_amount = sum(
            t.total_amount or Decimal("0")
            for t in transactions
            if t.transaction_type == "BU"
        )
        total_sell_amount = sum(
            t.total_amount or Decimal("0")
            for t in transactions
            if t.transaction_type == "SL"
        )
        total_fees = sum(t.fees or Decimal("0") for t in transactions)

        return {
            "portfolio_id": portfolio_id,
            "transaction_count": len(transactions),
            "buy_count": buy_count,
            "sell_count": sell_count,
            "transfer_count": transfer_count,
            "fee_count": fee_count,
            "total_buy_amount": float(total_buy_amount),
            "total_sell_amount": float(total_sell_amount),
            "total_fees": float(total_fees),
            "net_amount": float(total_sell_amount - total_buy_amount),
        }

    def _generate_transaction_id(self) -> str:
        """
        Generate a unique transaction ID.
        Format: YYYYMMDDHHMMSS + 6-digit sequence
        """
        now = datetime.utcnow()
        timestamp = now.strftime("%Y%m%d%H%M%S")

        count = self.db.query(TransactionHistory).filter(
            TransactionHistory.transaction_id.like(f"{timestamp}%")
        ).count()

        sequence = str(count + 1).zfill(6)
        return f"{timestamp}{sequence}"

    def _log_audit(
        self,
        user_id: str,
        action: AuditAction,
        portfolio_id: str | None = None,
        before_image: str | None = None,
        after_image: str | None = None,
        message: str | None = None,
    ) -> None:
        """Log an audit entry."""
        audit = AuditLog(
            timestamp=datetime.utcnow(),
            system_id="PORTMGMT",
            user_id=user_id,
            program="TRNSVC",
            audit_type=AuditType.TRANSACTION.value,
            action=action.value,
            status=AuditStatus.SUCCESS.value,
            portfolio_id=portfolio_id,
            before_image=before_image,
            after_image=after_image,
            message=message,
        )
        self.db.add(audit)

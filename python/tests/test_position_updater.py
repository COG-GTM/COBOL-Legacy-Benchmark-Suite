"""
Position updater tests.

Critical: verify decimal math matches COBOL COMP-3 precision.
Tests position calculations from POSUPD00.cbl.
"""

from datetime import date, datetime
from decimal import Decimal

from sqlalchemy.orm import Session

from src.batch.position_updater import PositionUpdater
from src.common.constants import (
    CurrencyCode,
    PositionStatus,
    TransactionStatus,
    TransactionType,
)
from src.db.tables import InvestmentPosition, TransactionHistory


class TestPositionUpdater:
    """Test PositionUpdater (POSUPD00.cbl)."""

    def _add_pending_transaction(
        self,
        session: Session,
        portfolio_id: str,
        investment_id: str,
        trn_type: str,
        quantity: Decimal,
        price: Decimal,
        amount: Decimal,
        txn_id: str = "TXN00001",
    ) -> TransactionHistory:
        txn = TransactionHistory(
            transaction_id=txn_id,
            trn_date=date(2024, 1, 15),
            trn_time="120000",
            portfolio_id=portfolio_id,
            sequence_no="000001",
            investment_id=investment_id,
            trn_type=trn_type,
            quantity=quantity,
            price=price,
            amount=amount,
            currency=CurrencyCode.USD.value,
            status=TransactionStatus.PENDING.value,
            process_date=datetime.now(),
            process_user="BATCH",
        )
        session.add(txn)
        session.commit()
        return txn

    def test_buy_creates_position(self, session: Session, sample_portfolio):
        """BUY transaction should create a new position."""
        self._add_pending_transaction(
            session,
            "PORT0001",
            "MSFT000001",
            TransactionType.BUY.value,
            Decimal("50.0000"),
            Decimal("300.0000"),
            Decimal("15000.00"),
        )

        updater = PositionUpdater(session)
        rc = updater.process(date(2024, 1, 15))

        assert rc.value <= 4  # SUCCESS or WARNING
        assert updater.records_read == 1
        assert updater.records_updated == 1

    def test_buy_increases_position(self, session: Session, sample_portfolio, sample_position):
        """BUY should increase existing position quantity and cost basis."""
        self._add_pending_transaction(
            session,
            "PORT0001",
            "AAPL000001",
            TransactionType.BUY.value,
            Decimal("50.0000"),
            Decimal("150.0000"),
            Decimal("7500.00"),
        )

        updater = PositionUpdater(session)
        updater.process(date(2024, 1, 15))

        # Verify position was updated
        pos = session.query(InvestmentPosition).filter_by(
            portfolio_id="PORT0001",
            investment_id="AAPL000001",
            position_date=date(2024, 1, 15),
        ).first()
        assert pos is not None
        # Original: 100 qty + 50 = 150
        assert pos.quantity == Decimal("150.0000")
        # Original cost: 15000 + 7500 = 22500
        assert pos.cost_basis == Decimal("22500.00")

    def test_sell_decreases_position(self, session: Session, sample_portfolio, sample_position):
        """SELL should decrease position quantity."""
        self._add_pending_transaction(
            session,
            "PORT0001",
            "AAPL000001",
            TransactionType.SELL.value,
            Decimal("30.0000"),
            Decimal("175.0000"),
            Decimal("5250.00"),
        )

        updater = PositionUpdater(session)
        updater.process(date(2024, 1, 15))

        pos = session.query(InvestmentPosition).filter_by(
            portfolio_id="PORT0001",
            investment_id="AAPL000001",
            position_date=date(2024, 1, 15),
        ).first()
        assert pos is not None
        # Original: 100 - 30 = 70
        assert pos.quantity == Decimal("70.0000")

    def test_sell_all_closes_position(self, session: Session, sample_portfolio, sample_position):
        """Selling all units should close the position."""
        self._add_pending_transaction(
            session,
            "PORT0001",
            "AAPL000001",
            TransactionType.SELL.value,
            Decimal("100.0000"),
            Decimal("175.0000"),
            Decimal("17500.00"),
        )

        updater = PositionUpdater(session)
        updater.process(date(2024, 1, 15))

        pos = session.query(InvestmentPosition).filter_by(
            portfolio_id="PORT0001",
            investment_id="AAPL000001",
            position_date=date(2024, 1, 15),
        ).first()
        assert pos is not None
        assert pos.quantity == Decimal("0.0000")
        assert pos.status == PositionStatus.CLOSED.value
        assert pos.cost_basis == Decimal("0.00")

    def test_decimal_precision_preserved(self, session: Session, sample_portfolio):
        """
        Critical: verify decimal math matches COBOL COMP-3 precision.
        COBOL PIC S9(11)V9(4) = 4 decimal places for quantity.
        COBOL PIC S9(13)V9(2) = 2 decimal places for amount.
        """
        self._add_pending_transaction(
            session,
            "PORT0001",
            "BOND00001",
            TransactionType.BUY.value,
            Decimal("33.3333"),
            Decimal("99.9999"),
            Decimal("3333.30"),
        )

        updater = PositionUpdater(session)
        updater.process(date(2024, 1, 15))

        pos = session.query(InvestmentPosition).filter_by(
            portfolio_id="PORT0001",
            investment_id="BOND00001",
        ).first()
        assert pos is not None
        # Quantity should maintain 4 decimal places
        assert pos.quantity == Decimal("33.3333")
        # Cost basis should maintain 2 decimal places
        assert pos.cost_basis == Decimal("3333.30")

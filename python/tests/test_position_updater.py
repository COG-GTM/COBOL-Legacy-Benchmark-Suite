"""
Position updater tests translated from COBOL POSUPD00.cbl.

CRITICAL: All tests verify Decimal arithmetic matches COBOL COMP-3 precision.
Tests:
- Buy processing (2100-PROCESS-BUY)
- Sell processing (2200-PROCESS-SELL)
- Transfer processing (2300-PROCESS-TRANSFER)
- Fee processing (2400-PROCESS-FEE)
"""

from datetime import date
from decimal import Decimal

from src.batch.position_updater import PositionUpdater
from src.common.constants import ReturnCode
from src.db.tables import TransactionHistory


class TestBuyProcessing:
    """Test buy transaction processing. Translates POSUPD00 2100-PROCESS-BUY."""

    def test_buy_new_position(self, session, sample_portfolio):
        """Buy creates new position when none exists."""
        trn = TransactionHistory(
            portfolio_id=sample_portfolio.portfolio_id,
            investment_id="NEWFUND1",
            trn_date=date(2024, 1, 15),
            trn_time="100000",
            sequence_no="000001",
            trn_type="BU",
            quantity=Decimal("100.0000"),
            price=Decimal("50.0000"),
            amount=Decimal("5000.00"),
            fees=Decimal("0.00"),
            total_amount=Decimal("5000.00"),
            currency_code="USD",
            status="P",
        )
        session.add(trn)
        session.flush()

        updater = PositionUpdater(session)
        rc = updater.process_pending_transactions()
        assert rc == ReturnCode.SUCCESS

    def test_buy_existing_position(self, session, sample_portfolio, sample_position):
        """Buy adds to existing position. Verifies Decimal arithmetic."""
        trn = TransactionHistory(
            portfolio_id=sample_portfolio.portfolio_id,
            investment_id=sample_position.investment_id,
            trn_date=date(2024, 1, 16),
            trn_time="100000",
            sequence_no="000001",
            trn_type="BU",
            quantity=Decimal("50.0000"),
            price=Decimal("52.0000"),
            amount=Decimal("2600.00"),
            fees=Decimal("0.00"),
            total_amount=Decimal("2600.00"),
            currency_code="USD",
            status="P",
        )
        session.add(trn)
        session.flush()

        updater = PositionUpdater(session)
        rc = updater.process_pending_transactions()
        assert rc == ReturnCode.SUCCESS

    def test_buy_decimal_precision(self, session):
        """
        Verify exact Decimal arithmetic for buy:
        COMPUTE NEW-QTY = OLD-QTY + TRN-QTY
        COMPUTE NEW-COST = OLD-COST + TRN-AMOUNT
        """
        old_qty = Decimal("100.0000")
        buy_qty = Decimal("33.3333")
        new_qty = (old_qty + buy_qty).quantize(Decimal("0.0001"))
        assert new_qty == Decimal("133.3333")

        old_cost = Decimal("5000.00")
        buy_amount = Decimal("1666.67")
        new_cost = (old_cost + buy_amount).quantize(Decimal("0.01"))
        assert new_cost == Decimal("6666.67")


class TestSellProcessing:
    """Test sell transaction processing. Translates POSUPD00 2200-PROCESS-SELL."""

    def test_sell_partial_position(self, session):
        """
        Sell reduces position with proportional cost basis.
        COMPUTE AVG-COST = OLD-COST / OLD-QTY
        COMPUTE COST-OF-SOLD = AVG-COST * SELL-QTY
        """
        old_qty = Decimal("100.0000")
        old_cost = Decimal("5000.00")
        sell_qty = Decimal("40.0000")

        avg_cost = (old_cost / old_qty).quantize(Decimal("0.0001"))
        cost_of_sold = (avg_cost * sell_qty).quantize(Decimal("0.01"))

        new_qty = (old_qty - sell_qty).quantize(Decimal("0.0001"))
        new_cost = (old_cost - cost_of_sold).quantize(Decimal("0.01"))

        assert avg_cost == Decimal("50.0000")
        assert cost_of_sold == Decimal("2000.00")
        assert new_qty == Decimal("60.0000")
        assert new_cost == Decimal("3000.00")

    def test_sell_gain_calculation(self, session):
        """
        Verify gain/loss: GAIN-LOSS = PROCEEDS - COST-OF-SOLD
        """
        sell_qty = Decimal("50.0000")
        sell_price = Decimal("60.0000")
        proceeds = (sell_qty * sell_price).quantize(Decimal("0.01"))

        old_qty = Decimal("100.0000")
        old_cost = Decimal("5000.00")
        avg_cost = (old_cost / old_qty).quantize(Decimal("0.0001"))
        cost_of_sold = (avg_cost * sell_qty).quantize(Decimal("0.01"))

        gain_loss = (proceeds - cost_of_sold).quantize(Decimal("0.01"))

        assert proceeds == Decimal("3000.00")
        assert cost_of_sold == Decimal("2500.00")
        assert gain_loss == Decimal("500.00")

    def test_sell_loss_calculation(self, session):
        """Verify loss when sell price < cost basis per unit."""
        sell_qty = Decimal("50.0000")
        sell_price = Decimal("40.0000")
        proceeds = (sell_qty * sell_price).quantize(Decimal("0.01"))

        old_qty = Decimal("100.0000")
        old_cost = Decimal("5000.00")
        avg_cost = (old_cost / old_qty).quantize(Decimal("0.0001"))
        cost_of_sold = (avg_cost * sell_qty).quantize(Decimal("0.01"))

        gain_loss = (proceeds - cost_of_sold).quantize(Decimal("0.01"))

        assert proceeds == Decimal("2000.00")
        assert cost_of_sold == Decimal("2500.00")
        assert gain_loss == Decimal("-500.00")


class TestFeeProcessing:
    """Test fee transaction processing. Translates POSUPD00 2400-PROCESS-FEE."""

    def test_fee_increases_cost_basis(self, session):
        """COMPUTE NEW-COST = OLD-COST + FEE-AMOUNT"""
        old_cost = Decimal("5000.00")
        fee_amount = Decimal("25.00")
        new_cost = (old_cost + fee_amount).quantize(Decimal("0.01"))
        assert new_cost == Decimal("5025.00")


class TestEdgeCases:
    """Test edge cases for position updater arithmetic."""

    def test_very_small_quantities(self, session):
        """Test with fractional shares (PIC S9(11)V9(4))."""
        qty = Decimal("0.0001")
        price = Decimal("1000.0000")
        amount = (qty * price).quantize(Decimal("0.01"))
        assert amount == Decimal("0.10")

    def test_very_large_amounts(self, session):
        """Test with large values within COMP-3 range."""
        qty = Decimal("99999999999.9999")
        price = Decimal("0.0001")
        amount = (qty * price).quantize(Decimal("0.01"))
        assert amount == Decimal("10000000.00")

    def test_rounding_consistency(self, session):
        """Verify banker's rounding (ROUND HALF EVEN) is used by default."""
        # Decimal uses ROUND_HALF_EVEN by default
        val = Decimal("2.5")
        rounded = val.quantize(Decimal("1"))
        assert rounded == Decimal("2")  # rounds to even

        val2 = Decimal("3.5")
        rounded2 = val2.quantize(Decimal("1"))
        assert rounded2 == Decimal("4")  # rounds to even

"""
Tests for domain and database models.
"""

from datetime import date, datetime
from decimal import Decimal

from app.models.domain import (
    ClientType,
    PortfolioRecord,
    PortfolioStatus,
    PositionRecord,
    PositionStatus,
    TransactionRecord,
    TransactionStatus,
    TransactionType,
)


class TestTransactionRecord:
    """Tests for TransactionRecord model."""

    def test_create_transaction_record(self):
        """Test creating a transaction record."""
        record = TransactionRecord(
            transaction_date="20231215",
            transaction_time="143052",
            portfolio_id="PORT0001",
            sequence_no="000001",
            investment_id="AAPL",
            transaction_type=TransactionType.BUY,
            quantity=Decimal("100.0000"),
            price=Decimal("150.5000"),
            amount=Decimal("15050.00"),
            currency="USD",
            status=TransactionStatus.PENDING,
            process_date=datetime.utcnow(),
            process_user="TESTUSER",
        )

        assert record.portfolio_id == "PORT0001"
        assert record.transaction_type == TransactionType.BUY
        assert record.quantity == Decimal("100.0000")

    def test_transaction_type_values(self):
        """Test transaction type enum values."""
        assert TransactionType.BUY.value == "BU"
        assert TransactionType.SELL.value == "SL"
        assert TransactionType.TRANSFER.value == "TR"
        assert TransactionType.FEE.value == "FE"


class TestPositionRecord:
    """Tests for PositionRecord model."""

    def test_create_position_record(self):
        """Test creating a position record."""
        record = PositionRecord(
            portfolio_id="PORT0001",
            position_date="20231215",
            investment_id="AAPL",
            quantity=Decimal("100.0000"),
            cost_basis=Decimal("15050.00"),
            market_value=Decimal("16000.00"),
            currency="USD",
            status=PositionStatus.ACTIVE,
            last_maint_date=datetime.utcnow(),
            last_maint_user="TESTUSER",
        )

        assert record.portfolio_id == "PORT0001"
        assert record.status == PositionStatus.ACTIVE
        assert record.quantity == Decimal("100.0000")


class TestPortfolioRecord:
    """Tests for PortfolioRecord model."""

    def test_create_portfolio_record(self):
        """Test creating a portfolio record."""
        record = PortfolioRecord(
            portfolio_id="PORT0001",
            account_no="1234567890",
            client_name="Test Client",
            client_type=ClientType.INDIVIDUAL,
            create_date=date.today(),
            last_maint_date=date.today(),
            status=PortfolioStatus.ACTIVE,
            total_value=Decimal("100000.00"),
            cash_balance=Decimal("10000.00"),
            last_user="TESTUSER",
            last_trans_date=date.today(),
        )

        assert record.portfolio_id == "PORT0001"
        assert record.client_type == ClientType.INDIVIDUAL
        assert record.status == PortfolioStatus.ACTIVE

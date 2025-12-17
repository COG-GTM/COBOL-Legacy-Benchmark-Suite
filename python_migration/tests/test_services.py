"""
Tests for service layer.
"""

from decimal import Decimal

import pytest
from sqlalchemy.orm import Session

from app.models.database import PortfolioMaster
from app.services.portfolio import PortfolioService
from app.services.transaction import TransactionService
from app.utils.exceptions import PortfolioNotFoundError


class TestPortfolioService:
    """Tests for PortfolioService."""

    def test_get_portfolio(self, db: Session, sample_portfolio: PortfolioMaster):
        """Test getting a portfolio by ID."""
        service = PortfolioService(db)
        portfolio = service.get_portfolio(sample_portfolio.portfolio_id)

        assert portfolio.portfolio_id == sample_portfolio.portfolio_id
        assert portfolio.portfolio_name == "Test Portfolio"

    def test_get_portfolio_not_found(self, db: Session):
        """Test getting a non-existent portfolio."""
        service = PortfolioService(db)

        with pytest.raises(PortfolioNotFoundError):
            service.get_portfolio("NOTEXIST")

    def test_get_positions_empty(self, db: Session, sample_portfolio: PortfolioMaster):
        """Test getting positions for portfolio with no positions."""
        service = PortfolioService(db)
        positions = service.get_positions(sample_portfolio.portfolio_id)

        assert len(positions) == 0

    def test_update_position_create_new(self, db: Session, sample_portfolio: PortfolioMaster):
        """Test creating a new position."""
        service = PortfolioService(db)

        position = service.update_position(
            portfolio_id=sample_portfolio.portfolio_id,
            investment_id="AAPL",
            quantity=Decimal("100"),
            cost_basis=Decimal("15000"),
            market_value=Decimal("16000"),
            user_id="TESTUSER",
        )
        db.commit()

        assert position.investment_id == "AAPL"
        assert position.quantity == Decimal("100")


class TestTransactionService:
    """Tests for TransactionService."""

    def test_get_transaction_history_empty(self, db: Session, sample_portfolio: PortfolioMaster):
        """Test getting transaction history for portfolio with no transactions."""
        service = TransactionService(db)
        transactions = service.get_transaction_history(sample_portfolio.portfolio_id)

        assert len(transactions) == 0

    def test_get_transaction_summary(self, db: Session, sample_portfolio: PortfolioMaster):
        """Test getting transaction summary."""
        service = TransactionService(db)
        summary = service.get_transaction_summary(sample_portfolio.portfolio_id)

        assert summary["portfolio_id"] == sample_portfolio.portfolio_id
        assert summary["transaction_count"] == 0

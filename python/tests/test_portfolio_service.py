"""
Portfolio service tests translated from TSTVAL00.cbl validation tests.

Tests CRUD operations and validation rules from:
  - PORTADD.cbl: Create portfolio
  - PORTUPDT.cbl: Update portfolio
  - PORTDEL.cbl: Delete/close portfolio
  - PORTINQ.cbl: Portfolio inquiry
  - PORTVALD.cbl: Validation rules
"""

from datetime import date
from decimal import Decimal

import pytest
from sqlalchemy.orm import Session

from src.common.constants import PortfolioStatus
from src.common.error_handler import DuplicateError, NotFoundError, ValidationError
from src.db.tables import PortfolioMaster
from src.models.portfolio import PortfolioRecord
from src.portfolio.service import PortfolioService


class TestPortfolioCreate:
    """Test portfolio creation (PORTADD.cbl)."""

    def test_create_portfolio(self, session: Session):
        service = PortfolioService(session)
        record = PortfolioRecord(
            portfolio_id="PORT0001",
            account_no="1234567890",
            portfolio_name="Test Portfolio",
            client_name="John Doe",
            cash_balance=Decimal("10000.00"),
        )
        portfolio = service.create(record, user_id="TESTER")
        assert portfolio.portfolio_id == "PORT0001"
        assert portfolio.status == PortfolioStatus.ACTIVE.value
        assert portfolio.cash_balance == Decimal("10000.00")
        assert portfolio.last_maint_user == "TESTER"

    def test_create_duplicate_raises_error(self, session: Session, sample_portfolio):
        """VSAM status 22 / SQLCODE -803: duplicate key."""
        service = PortfolioService(session)
        record = PortfolioRecord(
            portfolio_id="PORT0001",
            portfolio_name="Duplicate",
        )
        with pytest.raises(DuplicateError):
            service.create(record)

    def test_create_invalid_id_raises_error(self, session: Session):
        service = PortfolioService(session)
        record = PortfolioRecord(
            portfolio_id="BADID001",
            portfolio_name="Bad ID",
        )
        with pytest.raises(ValidationError):
            service.create(record)


class TestPortfolioRead:
    """Test portfolio inquiry (PORTINQ.cbl)."""

    def test_get_by_id(self, session: Session, sample_portfolio: PortfolioMaster):
        service = PortfolioService(session)
        result = service.get_by_id("PORT0001")
        assert result.portfolio_id == "PORT0001"
        assert result.portfolio_name == "Test Portfolio Alpha"

    def test_get_not_found(self, session: Session):
        """VSAM status 23: record not found."""
        service = PortfolioService(session)
        with pytest.raises(NotFoundError):
            service.get_by_id("PORT9999")

    def test_list_by_client(self, session: Session, multiple_portfolios):
        service = PortfolioService(session)
        results = service.list_by_client("CLIENT001")
        assert len(results) == 1
        assert results[0].portfolio_id == "PORT0001"

    def test_list_by_branch(self, session: Session, multiple_portfolios):
        service = PortfolioService(session)
        results = service.list_by_branch("02")
        assert len(results) > 0


class TestPortfolioUpdate:
    """Test portfolio update (PORTUPDT.cbl)."""

    def test_update_name(self, session: Session, sample_portfolio):
        service = PortfolioService(session)
        result = service.update(
            "PORT0001",
            {"portfolio_name": "Updated Name"},
            user_id="UPDATER",
        )
        assert result.portfolio_name == "Updated Name"
        assert result.last_maint_user == "UPDATER"

    def test_update_not_found(self, session: Session):
        service = PortfolioService(session)
        with pytest.raises(NotFoundError):
            service.update("PORT9999", {"portfolio_name": "X"})

    def test_update_closed_portfolio_raises_error(self, session: Session, sample_portfolio):
        """Cannot update a closed portfolio."""
        sample_portfolio.status = PortfolioStatus.CLOSED.value
        session.commit()

        service = PortfolioService(session)
        with pytest.raises(ValidationError):
            service.update("PORT0001", {"portfolio_name": "Updated"})


class TestPortfolioDelete:
    """Test portfolio closure (PORTDEL.cbl)."""

    def test_close_portfolio(self, session: Session, sample_portfolio):
        service = PortfolioService(session)
        result = service.delete("PORT0001", user_id="CLOSER")
        assert result.status == PortfolioStatus.CLOSED.value
        assert result.close_date == date.today()

    def test_close_not_found(self, session: Session):
        service = PortfolioService(session)
        with pytest.raises(NotFoundError):
            service.delete("PORT9999")

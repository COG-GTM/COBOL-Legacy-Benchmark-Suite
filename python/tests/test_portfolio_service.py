"""
Portfolio service tests translated from COBOL TSTVAL00.cbl.

Tests:
- Create portfolio (PORTADD.cbl flow)
- Update portfolio (PORTUPDT.cbl flow)
- Delete portfolio (PORTDEL.cbl flow)
- Inquiry (PORTINQ.cbl flow)
- Validation rules (PORTVALD.cbl)
"""


import pytest

from src.common.error_handler import ValidationError
from src.portfolio.service import PortfolioService


class TestPortfolioCreate:
    """Test portfolio creation. Translates PORTADD.cbl flow."""

    def test_create_portfolio(self, session):
        svc = PortfolioService(session)
        p = svc.create(
            portfolio_id="PORT0001",
            client_id="CLIENT001",
            client_name="Test Client",
            portfolio_name="Test Portfolio",
            account_type="IN",
            branch_id="01",
            currency_code="USD",
            risk_level="M",
            user="TESTUSER",
        )
        assert p.portfolio_id == "PORT0001"
        assert p.status == "A"
        assert p.client_id == "CLIENT001"

    def test_create_duplicate_portfolio(self, session, sample_portfolio):
        """Translates PORTADD.cbl 2000-CHECK-DUP error path."""
        svc = PortfolioService(session)
        with pytest.raises(ValidationError, match="already exists"):
            svc.create(
                portfolio_id=sample_portfolio.portfolio_id,
                client_id="CLIENT002",
                client_name="Another Client",
                portfolio_name="Another Portfolio",
                user="TESTUSER",
            )

    def test_create_portfolio_invalid_id(self, session):
        """Portfolio ID must be <= 8 chars (PIC X(08))."""
        svc = PortfolioService(session)
        with pytest.raises(ValidationError):
            svc.create(
                portfolio_id="TOOLONGID",
                client_id="CLIENT001",
                client_name="Test",
                portfolio_name="Test",
                user="TESTUSER",
            )


class TestPortfolioUpdate:
    """Test portfolio update. Translates PORTUPDT.cbl flow."""

    def test_update_portfolio_name(self, session, sample_portfolio):
        svc = PortfolioService(session)
        updated = svc.update(
            sample_portfolio.portfolio_id,
            user="TESTUSER",
            portfolio_name="Updated Name",
        )
        assert updated.portfolio_name == "Updated Name"

    def test_update_nonexistent_portfolio(self, session):
        svc = PortfolioService(session)
        with pytest.raises(ValidationError, match="not found"):
            svc.update("NOTEXIST", user="TESTUSER", portfolio_name="X")


class TestPortfolioDelete:
    """Test portfolio deletion. Translates PORTDEL.cbl flow."""

    def test_delete_portfolio(self, session, sample_portfolio):
        """Portfolio with no positions can be closed."""
        svc = PortfolioService(session)
        deleted = svc.delete(sample_portfolio.portfolio_id, user="TESTUSER")
        assert deleted.status == "C"

    def test_delete_portfolio_with_positions(self, session, sample_portfolio, sample_position):
        """Translates PORTDEL.cbl 2000-VERIFY-POSITIONS error path."""
        svc = PortfolioService(session)
        with pytest.raises(ValidationError, match="open positions"):
            svc.delete(sample_portfolio.portfolio_id, user="TESTUSER")


class TestPortfolioInquiry:
    """Test portfolio inquiry. Translates PORTINQ.cbl flow."""

    def test_get_by_id(self, session, sample_portfolio):
        svc = PortfolioService(session)
        p = svc.get_by_id(sample_portfolio.portfolio_id)
        assert p is not None
        assert p.portfolio_id == sample_portfolio.portfolio_id

    def test_get_nonexistent(self, session):
        svc = PortfolioService(session)
        p = svc.get_by_id("NOTEXIST")
        assert p is None

    def test_list_by_client(self, session, sample_portfolio):
        svc = PortfolioService(session)
        results = svc.list_by_client(sample_portfolio.client_id)
        assert len(results) >= 1
        assert results[0].client_id == sample_portfolio.client_id

    def test_list_by_branch(self, session, sample_portfolio):
        svc = PortfolioService(session)
        results = svc.list_by_branch(sample_portfolio.branch_id)
        assert len(results) >= 1

    def test_list_all(self, session, sample_portfolio):
        svc = PortfolioService(session)
        results = svc.list_all()
        assert len(results) >= 1

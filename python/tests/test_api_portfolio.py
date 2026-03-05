"""
Portfolio API endpoint tests translated from COBOL online programs.

Tests REST endpoints replacing CICS SEND MAP / RECEIVE MAP flows:
- GET /portfolios/{id} (INQPORT.cbl)
- POST /portfolios (PORTADD.cbl)
- PUT /portfolios/{id} (PORTUPDT.cbl)
- DELETE /portfolios/{id} (PORTDEL.cbl)
- GET /portfolios (list with filters)
"""

import os
from datetime import date
from decimal import Decimal

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, event
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from src.api.app import app
from src.db.session import get_session_dependency
from src.db.tables import Base, InvestmentPosition, PortfolioMaster

# Override API key for testing
os.environ["API_KEY"] = "test-api-key"

TEST_ENGINE = create_engine(
    "sqlite://",
    echo=False,
    connect_args={"check_same_thread": False},
    poolclass=StaticPool,
)


@event.listens_for(TEST_ENGINE, "connect")
def _set_sqlite_pragma(dbapi_conn, connection_record):
    cursor = dbapi_conn.cursor()
    cursor.execute("PRAGMA foreign_keys=ON")
    cursor.close()


TestSessionLocal = sessionmaker(bind=TEST_ENGINE)


def override_get_session():
    session = TestSessionLocal()
    try:
        yield session
        session.commit()
    except Exception:
        session.rollback()
        raise
    finally:
        session.close()


@pytest.fixture(autouse=True)
def setup_tables():
    """Recreate tables for each test and set dependency override."""
    app.dependency_overrides[get_session_dependency] = override_get_session
    Base.metadata.drop_all(TEST_ENGINE)
    Base.metadata.create_all(TEST_ENGINE)
    yield
    app.dependency_overrides.pop(get_session_dependency, None)


@pytest.fixture()
def client():
    return TestClient(app)


@pytest.fixture()
def auth_headers():
    return {"X-API-Key": "test-api-key"}


@pytest.fixture()
def seed_portfolio():
    """Seed a portfolio directly in the DB."""
    session = TestSessionLocal()
    p = PortfolioMaster(
        portfolio_id="PORT0001",
        account_type="IN",
        branch_id="01",
        client_id="CLIENT01",
        client_name="Test Client",
        client_type="I",
        portfolio_name="Test Portfolio",
        currency_code="USD",
        risk_level="M",
        status="A",
        total_value=Decimal("100000.00"),
        cash_balance=Decimal("25000.00"),
        open_date=date(2024, 1, 15),
    )
    session.add(p)
    session.commit()
    session.close()
    return p


class TestGetPortfolio:
    """Test GET /portfolios/{id}. Translates INQPORT.cbl."""

    def test_get_existing(self, client, auth_headers, seed_portfolio):
        resp = client.get("/portfolios/PORT0001", headers=auth_headers)
        assert resp.status_code == 200
        data = resp.json()
        assert data["portfolio_id"] == "PORT0001"
        assert data["client_id"] == "CLIENT01"

    def test_get_not_found(self, client, auth_headers):
        resp = client.get("/portfolios/NOTEXIST", headers=auth_headers)
        assert resp.status_code == 404

    def test_get_no_auth(self, client):
        resp = client.get("/portfolios/PORT0001")
        assert resp.status_code in (401, 403)


class TestCreatePortfolio:
    """Test POST /portfolios. Translates PORTADD.cbl."""

    def test_create_portfolio(self, client, auth_headers):
        payload = {
            "portfolio_id": "PORT0002",
            "client_id": "CLIENT02",
            "client_name": "New Client",
            "portfolio_name": "New Portfolio",
            "account_type": "IN",
            "branch_id": "01",
            "currency_code": "USD",
            "risk_level": "M",
        }
        resp = client.post("/portfolios", json=payload, headers=auth_headers)
        assert resp.status_code == 201
        data = resp.json()
        assert data["portfolio_id"] == "PORT0002"
        assert data["status"] == "A"

    def test_create_duplicate(self, client, auth_headers, seed_portfolio):
        payload = {
            "portfolio_id": "PORT0001",
            "client_id": "CLIENT01",
            "client_name": "Test",
            "portfolio_name": "Test",
        }
        resp = client.post("/portfolios", json=payload, headers=auth_headers)
        assert resp.status_code == 400


class TestUpdatePortfolio:
    """Test PUT /portfolios/{id}. Translates PORTUPDT.cbl."""

    def test_update_portfolio(self, client, auth_headers, seed_portfolio):
        payload = {"portfolio_name": "Updated Name"}
        resp = client.put("/portfolios/PORT0001", json=payload, headers=auth_headers)
        assert resp.status_code == 200
        assert resp.json()["portfolio_name"] == "Updated Name"

    def test_update_not_found(self, client, auth_headers):
        payload = {"portfolio_name": "X"}
        resp = client.put("/portfolios/NOTEXIST", json=payload, headers=auth_headers)
        assert resp.status_code in (400, 404, 500)


class TestDeletePortfolio:
    """Test DELETE /portfolios/{id}. Translates PORTDEL.cbl."""

    def test_delete_portfolio(self, client, auth_headers, seed_portfolio):
        resp = client.delete("/portfolios/PORT0001", headers=auth_headers)
        assert resp.status_code == 200
        assert resp.json()["status"] == "C"

    def test_delete_with_positions(self, client, auth_headers, seed_portfolio):
        """Cannot delete portfolio with active positions."""
        session = TestSessionLocal()
        pos = InvestmentPosition(
            portfolio_id="PORT0001",
            investment_id="FUND0001",
            position_date=date(2024, 1, 15),
            quantity=Decimal("100.0000"),
            cost_basis=Decimal("5000.00"),
            market_value=Decimal("5500.00"),
            status="A",
        )
        session.add(pos)
        session.commit()
        session.close()

        resp = client.delete("/portfolios/PORT0001", headers=auth_headers)
        assert resp.status_code == 400


class TestListPortfolios:
    """Test GET /portfolios. List with filters."""

    def test_list_all(self, client, auth_headers, seed_portfolio):
        resp = client.get("/portfolios", headers=auth_headers)
        assert resp.status_code == 200
        data = resp.json()
        assert data["total"] >= 1

    def test_list_by_client(self, client, auth_headers, seed_portfolio):
        resp = client.get("/portfolios?client_id=CLIENT01", headers=auth_headers)
        assert resp.status_code == 200
        assert resp.json()["total"] >= 1

"""
Inquiry API endpoint tests translated from COBOL online programs.

Tests REST endpoints replacing CICS inquiry flow:
- GET /positions/{portfolio_id} (INQONLN.cbl)
- GET /positions/{portfolio_id}/{investment_id}
- GET /transactions/{portfolio_id} (INQHIST.cbl)
- GET /health
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
from src.db.tables import (
    Base,
    InvestmentPosition,
    PortfolioMaster,
    TransactionHistory,
)

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
def seed_data():
    """Seed portfolio, position, and transaction data."""
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

    pos = InvestmentPosition(
        portfolio_id="PORT0001",
        investment_id="FUND0001",
        position_date=date(2024, 1, 15),
        quantity=Decimal("100.0000"),
        cost_basis=Decimal("5000.00"),
        market_value=Decimal("5500.00"),
        currency_code="USD",
        status="A",
    )
    session.add(pos)

    trn = TransactionHistory(
        portfolio_id="PORT0001",
        investment_id="FUND0001",
        trn_date=date(2024, 1, 15),
        trn_time="100000",
        sequence_no="000001",
        trn_type="BU",
        quantity=Decimal("100.0000"),
        price=Decimal("50.0000"),
        amount=Decimal("5000.00"),
        fees=Decimal("10.00"),
        total_amount=Decimal("5010.00"),
        currency_code="USD",
        status="D",
    )
    session.add(trn)
    session.commit()
    session.close()


class TestPositionInquiry:
    """Test GET /positions. Translates INQONLN.cbl."""

    def test_get_positions(self, client, auth_headers, seed_data):
        resp = client.get("/positions/PORT0001", headers=auth_headers)
        assert resp.status_code == 200
        data = resp.json()
        assert data["portfolio_id"] == "PORT0001"
        assert data["total"] >= 1

    def test_get_specific_position(self, client, auth_headers, seed_data):
        resp = client.get("/positions/PORT0001/FUND0001", headers=auth_headers)
        assert resp.status_code == 200
        data = resp.json()
        assert data["investment_id"] == "FUND0001"
        assert data["quantity"] == "100.0000"

    def test_position_not_found(self, client, auth_headers, seed_data):
        resp = client.get("/positions/PORT0001/NOTEXIST", headers=auth_headers)
        assert resp.status_code == 404


class TestTransactionHistory:
    """Test GET /transactions. Translates INQHIST.cbl."""

    def test_get_history(self, client, auth_headers, seed_data):
        resp = client.get("/transactions/PORT0001", headers=auth_headers)
        assert resp.status_code == 200
        data = resp.json()
        assert data["portfolio_id"] == "PORT0001"
        assert data["total"] >= 1

    def test_get_history_with_dates(self, client, auth_headers, seed_data):
        resp = client.get(
            "/transactions/PORT0001?start_date=2024-01-01&end_date=2024-12-31",
            headers=auth_headers,
        )
        assert resp.status_code == 200
        assert resp.json()["total"] >= 1


class TestHealthCheck:
    """Test GET /health."""

    def test_health_check(self, client):
        resp = client.get("/health")
        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] in ("ok", "degraded")
        assert data["database"] == "ok"

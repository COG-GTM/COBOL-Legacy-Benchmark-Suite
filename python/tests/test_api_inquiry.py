"""
API inquiry endpoint tests.

Tests the position and transaction inquiry endpoints
replacing CICS pseudo-conversational inquiry flow
from INQONLN.cbl and INQHIST.cbl.
"""

from decimal import Decimal

import pytest
from fastapi.testclient import TestClient

from src.api.app import app
from src.api.security import get_api_key
from src.db.session import get_session


@pytest.fixture()
def test_client(engine, session):
    """Create a test client with in-memory database."""

    def _override_session():
        yield session

    app.dependency_overrides[get_session] = _override_session
    yield TestClient(app)
    app.dependency_overrides.clear()


@pytest.fixture()
def auth_headers():
    return {"X-API-Key": get_api_key()}


class TestPositionInquiry:
    """Test position inquiry endpoints (INQONLN.cbl)."""

    def test_get_positions(
        self, test_client, auth_headers, sample_portfolio, sample_position
    ):
        response = test_client.get(
            "/positions/PORT0001", headers=auth_headers
        )
        assert response.status_code == 200
        data = response.json()
        assert data["portfolio_id"] == "PORT0001"
        assert len(data["positions"]) > 0
        assert Decimal(data["total_market_value"]) > 0

    def test_get_positions_not_found(self, test_client, auth_headers):
        response = test_client.get(
            "/positions/PORT9999", headers=auth_headers
        )
        assert response.status_code == 404

    def test_get_specific_position(
        self, test_client, auth_headers, sample_portfolio, sample_position
    ):
        response = test_client.get(
            "/positions/PORT0001/AAPL000001", headers=auth_headers
        )
        assert response.status_code == 200
        data = response.json()
        assert data["investment_id"] == "AAPL000001"
        assert Decimal(data["quantity"]) == Decimal("100.0000")


class TestTransactionHistory:
    """Test transaction history endpoints (INQHIST.cbl)."""

    def test_get_transaction_history(
        self, test_client, auth_headers, sample_portfolio, sample_transaction
    ):
        response = test_client.get(
            "/transactions/PORT0001", headers=auth_headers
        )
        assert response.status_code == 200
        data = response.json()
        assert data["total"] > 0

    def test_get_transaction_detail(
        self, test_client, auth_headers, sample_portfolio, sample_transaction
    ):
        response = test_client.get(
            "/transactions/detail/20240115103000000001",
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert data["transaction_id"] == "20240115103000000001"

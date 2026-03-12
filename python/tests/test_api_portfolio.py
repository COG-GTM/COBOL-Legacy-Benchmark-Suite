"""
API portfolio endpoint integration tests.

Tests the FastAPI endpoints that replace CICS PINQ transaction:
  - POST /portfolios: Create portfolio
  - GET /portfolios/{id}: Get portfolio
  - PUT /portfolios/{id}: Update portfolio
  - DELETE /portfolios/{id}: Close portfolio
  - GET /portfolios: List portfolios
"""


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
    """Headers with valid API key."""
    return {"X-API-Key": get_api_key()}


class TestPortfolioEndpoints:
    """Test portfolio API endpoints."""

    def test_create_portfolio(self, test_client, auth_headers):
        response = test_client.post(
            "/portfolios",
            json={
                "portfolio_id": "PORT0001",
                "account_no": "1234567890",
                "portfolio_name": "API Test Portfolio",
                "client_name": "API Tester",
                "cash_balance": "10000.00",
            },
            headers=auth_headers,
        )
        assert response.status_code == 201
        data = response.json()
        assert data["portfolio_id"] == "PORT0001"
        assert data["portfolio_name"] == "API Test Portfolio"

    def test_get_portfolio(self, test_client, auth_headers, sample_portfolio):
        response = test_client.get("/portfolios/PORT0001", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()
        assert data["portfolio_id"] == "PORT0001"

    def test_get_portfolio_not_found(self, test_client, auth_headers):
        response = test_client.get("/portfolios/PORT9999", headers=auth_headers)
        assert response.status_code == 404

    def test_update_portfolio(self, test_client, auth_headers, sample_portfolio):
        response = test_client.put(
            "/portfolios/PORT0001",
            json={"portfolio_name": "Updated Via API"},
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert data["portfolio_name"] == "Updated Via API"

    def test_delete_portfolio(self, test_client, auth_headers, sample_portfolio):
        response = test_client.delete("/portfolios/PORT0001", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "C"

    def test_list_portfolios(self, test_client, auth_headers, multiple_portfolios):
        response = test_client.get("/portfolios", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()
        assert data["total"] > 0

    def test_unauthorized_without_key(self, test_client):
        response = test_client.get("/portfolios/PORT0001")
        assert response.status_code == 401

    def test_unauthorized_wrong_key(self, test_client):
        response = test_client.get(
            "/portfolios/PORT0001",
            headers={"X-API-Key": "wrong-key"},
        )
        assert response.status_code == 401

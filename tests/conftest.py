"""
Shared pytest fixtures for the Investment Portfolio Management System tests.

These fixtures provide convenient access to valid default data structures
that can be used across all test modules.
"""

import pytest

from tests.business_rules.validators import Portfolio, Transaction


@pytest.fixture
def valid_portfolio() -> Portfolio:
    """Return a Portfolio populated with valid default values."""
    return Portfolio(
        portfolio_id="PORT0001",
        account_number="1234567890",
        name="My Portfolio",
        status="A",
        client_type="I",
        investment_type="STK",
        total_units=100.0,
        total_cost=1000.0,
    )


@pytest.fixture
def valid_transaction() -> Transaction:
    """Return a Transaction populated with valid default values."""
    return Transaction(
        transaction_type="BU",
        portfolio_id="PORT0001",
        quantity=50.0,
        price=10.0,
        amount=500.0,
        status="P",
    )


# ---------------------------------------------------------------------------
# Helper factory fixtures
# ---------------------------------------------------------------------------
@pytest.fixture
def make_portfolio():
    """Factory fixture: create a Portfolio with optional field overrides."""

    def _make(**overrides) -> Portfolio:
        defaults = dict(
            portfolio_id="PORT0001",
            account_number="1234567890",
            name="My Portfolio",
            status="A",
            client_type="I",
            investment_type="STK",
            total_units=100.0,
            total_cost=1000.0,
        )
        defaults.update(overrides)
        return Portfolio(**defaults)

    return _make


@pytest.fixture
def make_transaction():
    """Factory fixture: create a Transaction with optional field overrides."""

    def _make(**overrides) -> Transaction:
        defaults = dict(
            transaction_type="BU",
            portfolio_id="PORT0001",
            quantity=50.0,
            price=10.0,
            amount=500.0,
            status="P",
        )
        defaults.update(overrides)
        return Transaction(**defaults)

    return _make

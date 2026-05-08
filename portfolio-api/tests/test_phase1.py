import pytest
from pydantic import ValidationError
from sqlalchemy import inspect, text

from app.models import Base
from app.schemas.constants import (
    AccountType,
    CurrencyCode,
    ReturnCode,
    RiskLevel,
    StatusCode,
    TransactionType,
)
from app.schemas.portfolio import PortfolioCreate
from tests.conftest import engine


@pytest.mark.asyncio
async def test_health_check(client):
    response = await client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


@pytest.mark.asyncio
async def test_all_tables_exist():
    async with engine.connect() as conn:
        table_names = await conn.run_sync(
            lambda sync_conn: inspect(sync_conn).get_table_names()
        )
    expected = {
        "portfolio_master",
        "investment_positions",
        "transaction_history",
        "audit_log",
        "error_log",
        "batch_control",
    }
    assert expected.issubset(set(table_names)), (
        f"Missing tables: {expected - set(table_names)}"
    )


def test_valid_portfolio_accepted():
    portfolio = PortfolioCreate(
        portfolio_id="PORT1234",
        account_type="IN",
        branch_id="01",
        client_id="CLIENT001",
        portfolio_name="Test Portfolio",
        currency_code=CurrencyCode.USD,
        risk_level=RiskLevel.MEDIUM,
        status=StatusCode.ACTIVE,
    )
    assert portfolio.portfolio_id == "PORT1234"
    assert portfolio.portfolio_name == "Test Portfolio"


def test_invalid_portfolio_id_rejected():
    with pytest.raises(ValidationError) as exc_info:
        PortfolioCreate(
            portfolio_id="ABCD12345",
            account_type="IN",
            branch_id="01",
            client_id="CLIENT001",
            portfolio_name="Test",
            currency_code=CurrencyCode.USD,
            risk_level=RiskLevel.MEDIUM,
            status=StatusCode.ACTIVE,
        )
    assert "portfolio_id" in str(exc_info.value)


def test_blank_name_rejected():
    with pytest.raises(ValidationError) as exc_info:
        PortfolioCreate(
            portfolio_id="PORT1234",
            account_type="IN",
            branch_id="01",
            client_id="CLIENT001",
            portfolio_name="   ",
            currency_code=CurrencyCode.USD,
            risk_level=RiskLevel.MEDIUM,
            status=StatusCode.ACTIVE,
        )
    assert "portfolio_name" in str(exc_info.value).lower() or "blank" in str(
        exc_info.value
    ).lower()


def test_invalid_status_rejected():
    with pytest.raises(ValidationError):
        PortfolioCreate(
            portfolio_id="PORT1234",
            account_type="IN",
            branch_id="01",
            client_id="CLIENT001",
            portfolio_name="Test",
            currency_code=CurrencyCode.USD,
            risk_level=RiskLevel.MEDIUM,
            status=StatusCode.FAILED,
        )


def test_return_code_values():
    assert ReturnCode.SUCCESS == 0
    assert ReturnCode.WARNING == 4
    assert ReturnCode.ERROR == 8
    assert ReturnCode.SEVERE == 12
    assert ReturnCode.CRITICAL == 16


def test_status_code_values():
    assert StatusCode.ACTIVE.value == "A"
    assert StatusCode.CLOSED.value == "C"
    assert StatusCode.PENDING.value == "P"
    assert StatusCode.SUSPENDED.value == "S"
    assert StatusCode.FAILED.value == "F"
    assert StatusCode.REVERSED.value == "R"


def test_transaction_type_values():
    assert TransactionType.BUY.value == "BU"
    assert TransactionType.SELL.value == "SL"
    assert TransactionType.TRANSFER.value == "TR"
    assert TransactionType.FEE.value == "FE"


def test_currency_code_values():
    assert CurrencyCode.USD.value == "USD"
    assert CurrencyCode.EUR.value == "EUR"
    assert CurrencyCode.GBP.value == "GBP"
    assert CurrencyCode.JPY.value == "JPY"
    assert CurrencyCode.CAD.value == "CAD"


def test_account_type_values():
    assert AccountType.INDIVIDUAL.value == "IN"
    assert AccountType.JOINT.value == "JT"
    assert AccountType.CORPORATE.value == "CO"
    assert AccountType.TRUST.value == "TR"
    assert AccountType.RETIREMENT.value == "RT"


def test_risk_level_values():
    assert RiskLevel.LOW.value == "L"
    assert RiskLevel.MEDIUM.value == "M"
    assert RiskLevel.HIGH.value == "H"
    assert RiskLevel.AGGRESSIVE.value == "A"

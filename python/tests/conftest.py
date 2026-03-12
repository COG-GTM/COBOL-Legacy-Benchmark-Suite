"""
Test configuration and fixtures.

Translates test data generation concepts from TSTGEN00.cbl.
Uses SQLite in-memory database for fast, isolated tests.
"""

from datetime import date, datetime
from decimal import Decimal

import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import Session, sessionmaker

from src.common.constants import (
    AccountType,
    ClientType,
    CurrencyCode,
    PortfolioStatus,
    PositionStatus,
    RiskLevel,
    TransactionStatus,
    TransactionType,
)
from src.db.tables import (
    Base,
    InvestmentPosition,
    PortfolioMaster,
    TransactionHistory,
)


@pytest.fixture()
def engine():
    """Create an in-memory SQLite engine for testing."""
    engine = create_engine("sqlite:///:memory:", echo=False)
    Base.metadata.create_all(engine)
    yield engine
    engine.dispose()


@pytest.fixture()
def session(engine):
    """Create a database session for testing."""
    factory = sessionmaker(bind=engine, expire_on_commit=False)
    session = factory()
    try:
        yield session
    finally:
        session.rollback()
        session.close()


@pytest.fixture()
def sample_portfolio(session: Session) -> PortfolioMaster:
    """
    Create a sample portfolio.

    Translates TSTGEN00.cbl test data generation for portfolios.
    """
    portfolio = PortfolioMaster(
        portfolio_id="PORT0001",
        account_no="1234567890",
        account_type=AccountType.INDIVIDUAL.value,
        branch_id="01",
        client_id="CLIENT001",
        portfolio_name="Test Portfolio Alpha",
        currency_code=CurrencyCode.USD.value,
        risk_level=RiskLevel.MEDIUM.value,
        client_name="John Doe",
        client_type=ClientType.INDIVIDUAL.value,
        status=PortfolioStatus.ACTIVE.value,
        open_date=date(2024, 1, 1),
        close_date=None,
        create_date=date(2024, 1, 1),
        total_value=Decimal("100000.00"),
        cash_balance=Decimal("25000.00"),
        last_maint_date=datetime(2024, 1, 15, 10, 30, 0),
        last_maint_user="TESTUSER",
        last_trans_date=date(2024, 1, 15),
    )
    session.add(portfolio)
    session.commit()
    return portfolio


@pytest.fixture()
def sample_position(session: Session, sample_portfolio: PortfolioMaster) -> InvestmentPosition:
    """
    Create a sample investment position.

    Translates TSTGEN00.cbl test data for positions.
    """
    position = InvestmentPosition(
        portfolio_id=sample_portfolio.portfolio_id,
        investment_id="AAPL000001",
        position_date=date(2024, 1, 15),
        quantity=Decimal("100.0000"),
        cost_basis=Decimal("15000.00"),
        market_value=Decimal("17500.00"),
        currency=CurrencyCode.USD.value,
        status=PositionStatus.ACTIVE.value,
        last_maint_date=datetime(2024, 1, 15, 10, 30, 0),
        last_maint_user="TESTUSER",
    )
    session.add(position)
    session.commit()
    return position


@pytest.fixture()
def sample_transaction(session: Session, sample_portfolio: PortfolioMaster) -> TransactionHistory:
    """
    Create a sample transaction.

    Translates TSTGEN00.cbl test data for transactions.
    """
    transaction = TransactionHistory(
        transaction_id="20240115103000000001",
        trn_date=date(2024, 1, 15),
        trn_time="103000",
        portfolio_id=sample_portfolio.portfolio_id,
        sequence_no="000001",
        investment_id="AAPL000001",
        trn_type=TransactionType.BUY.value,
        quantity=Decimal("50.0000"),
        price=Decimal("150.0000"),
        amount=Decimal("7500.00"),
        currency=CurrencyCode.USD.value,
        status=TransactionStatus.DONE.value,
        process_date=datetime(2024, 1, 15, 10, 30, 0),
        process_user="TESTUSER",
    )
    session.add(transaction)
    session.commit()
    return transaction


@pytest.fixture()
def multiple_portfolios(session: Session) -> list[PortfolioMaster]:
    """Create multiple portfolios for list/filter testing."""
    portfolios = []
    for i in range(1, 6):
        portfolio = PortfolioMaster(
            portfolio_id=f"PORT{i:04d}",
            account_no=f"{i:010d}",
            account_type=AccountType.INDIVIDUAL.value,
            branch_id=f"{(i % 3) + 1:02d}",
            client_id=f"CLIENT{i:03d}",
            portfolio_name=f"Test Portfolio {i}",
            currency_code=CurrencyCode.USD.value,
            risk_level=RiskLevel.MEDIUM.value,
            client_name=f"Client {i}",
            client_type=ClientType.INDIVIDUAL.value,
            status=PortfolioStatus.ACTIVE.value if i <= 3 else PortfolioStatus.CLOSED.value,
            open_date=date(2024, 1, i),
            close_date=date(2024, 6, 1) if i > 3 else None,
            create_date=date(2024, 1, i),
            total_value=Decimal(str(i * 10000)),
            cash_balance=Decimal(str(i * 2500)),
            last_maint_date=datetime(2024, 1, 15, 10, 30, 0),
            last_maint_user="TESTUSER",
        )
        session.add(portfolio)
        portfolios.append(portfolio)
    session.commit()
    return portfolios

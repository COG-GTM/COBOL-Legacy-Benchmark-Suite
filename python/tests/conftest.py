"""
Test fixtures translated from COBOL test data generator TSTGEN00.cbl.

Provides:
- SQLite in-memory database for isolated testing
- Sample portfolio, position, and transaction data
- Session fixtures with automatic rollback
"""

from datetime import date
from decimal import Decimal

import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import Session

from src.db.tables import (
    AuditLog,
    Base,
    BatchControl,
    InvestmentPosition,
    PortfolioMaster,
    TransactionHistory,
)

# Use in-memory SQLite for tests
TEST_DATABASE_URL = "sqlite://"


@pytest.fixture(scope="session")
def engine():
    """Create test engine once per session."""
    eng = create_engine(TEST_DATABASE_URL, echo=False)
    Base.metadata.create_all(eng)
    return eng


@pytest.fixture()
def session(engine):
    """Create a new database session for each test with rollback."""
    connection = engine.connect()
    transaction = connection.begin()
    sess = Session(bind=connection)
    yield sess
    sess.close()
    transaction.rollback()
    connection.close()


@pytest.fixture()
def sample_portfolio(session) -> PortfolioMaster:
    """
    Create a sample portfolio record.
    Translates TSTGEN00.cbl 2100-GENERATE-PORTFOLIOS.
    """
    portfolio = PortfolioMaster(
        portfolio_id="PORT0001",
        account_type="IN",
        branch_id="01",
        client_id="CLIENT001",
        client_name="TEST CLIENT ONE",
        client_type="I",
        portfolio_name="Test Portfolio",
        currency_code="USD",
        risk_level="M",
        status="A",
        total_value=Decimal("100000.00"),
        cash_balance=Decimal("25000.00"),
        open_date=date(2024, 1, 15),
        last_maint_user="TSTGEN",
    )
    session.add(portfolio)
    session.flush()
    return portfolio


@pytest.fixture()
def sample_position(session, sample_portfolio) -> InvestmentPosition:
    """
    Create a sample position record.
    Translates TSTGEN00.cbl 2200-GENERATE-POSITIONS.
    """
    position = InvestmentPosition(
        portfolio_id=sample_portfolio.portfolio_id,
        investment_id="FUND0001",
        position_date=date(2024, 1, 15),
        quantity=Decimal("100.0000"),
        cost_basis=Decimal("5000.00"),
        market_value=Decimal("5500.00"),
        currency_code="USD",
        status="A",
        last_maint_user="TSTGEN",
    )
    session.add(position)
    session.flush()
    return position


@pytest.fixture()
def sample_transaction(session, sample_portfolio) -> TransactionHistory:
    """
    Create a sample transaction record.
    Translates TSTGEN00.cbl 2300-GENERATE-TRANSACTIONS.
    """
    trn = TransactionHistory(
        portfolio_id=sample_portfolio.portfolio_id,
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
        status="P",
    )
    session.add(trn)
    session.flush()
    return trn


@pytest.fixture()
def sample_batch_control(session) -> BatchControl:
    """Create a sample batch control record."""
    rec = BatchControl(
        job_name="TRNVAL",
        process_date="20240115",
        sequence_no=1,
        status="R",
        return_code=0,
    )
    session.add(rec)
    session.flush()
    return rec


@pytest.fixture()
def sample_audit(session) -> AuditLog:
    """Create a sample audit log record."""
    rec = AuditLog(
        user_id="TESTUSER",
        system_id="SYSTEM",
        program="TSTGEN",
        audit_type="TRAN",
        action="CREATE",
        status="SUCC",
        key_info="PORT0001",
        message="Test audit record",
    )
    session.add(rec)
    session.flush()
    return rec

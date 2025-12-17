"""
Pytest configuration and fixtures for the Portfolio Management System tests.
"""

from collections.abc import Generator
from datetime import date, datetime
from decimal import Decimal

import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import Session, sessionmaker

from app.models.database import Base, PortfolioMaster, User
from app.services.auth import AuthService


@pytest.fixture(scope="session")
def engine():
    """Create test database engine."""
    engine = create_engine(
        "sqlite:///:memory:",
        connect_args={"check_same_thread": False},
    )
    Base.metadata.create_all(bind=engine)
    return engine


@pytest.fixture(scope="function")
def db(engine) -> Generator[Session, None, None]:
    """Create database session for each test."""
    session_local = sessionmaker(autocommit=False, autoflush=False, bind=engine)
    session = session_local()

    try:
        yield session
    finally:
        session.rollback()
        session.close()


@pytest.fixture
def sample_portfolio(db: Session) -> PortfolioMaster:
    """Create a sample portfolio for testing."""
    portfolio = PortfolioMaster(
        portfolio_id="PORT0001",
        account_type="IN",
        branch_id="01",
        client_id="CLIENT001",
        portfolio_name="Test Portfolio",
        client_name="Test Client",
        client_type="I",
        currency_code="USD",
        status="A",
        total_value=Decimal("100000.00"),
        cash_balance=Decimal("10000.00"),
        open_date=date.today(),
        last_maint_date=datetime.utcnow(),
        last_maint_user="TESTUSER",
    )
    db.add(portfolio)
    db.commit()
    return portfolio


@pytest.fixture
def sample_user(db: Session) -> User:
    """Create a sample user for testing."""
    auth_service = AuthService(db)
    user = User(
        user_id="TESTUSER",
        username="testuser",
        email="test@example.com",
        hashed_password=auth_service.get_password_hash("password123"),
        full_name="Test User",
        is_active=True,
        is_superuser=False,
        created_at=datetime.utcnow(),
    )
    db.add(user)
    db.commit()
    return user

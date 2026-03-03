"""SQLAlchemy models for PORTFOLIO_MASTER and INVESTMENT_POSITIONS tables.

Migrated from: src/database/db2/db2-definitions.sql
"""

from datetime import date, datetime

from sqlalchemy import Date, DateTime, ForeignKey, Index, Numeric, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from .base import Base


class PortfolioMaster(Base):
    """Portfolio Master table.

    Status codes: 'A'=Active, 'C'=Closed, 'S'=Suspended
    """

    __tablename__ = "portfolio_master"

    portfolio_id: Mapped[str] = mapped_column(String(8), primary_key=True)
    account_type: Mapped[str] = mapped_column(String(2), nullable=False)
    branch_id: Mapped[str] = mapped_column(String(2), nullable=False)
    client_id: Mapped[str] = mapped_column(String(10), nullable=False)
    portfolio_name: Mapped[str] = mapped_column(String(50), nullable=False)
    currency_code: Mapped[str] = mapped_column(String(3), nullable=False)
    risk_level: Mapped[str] = mapped_column(String(1), nullable=False)
    status: Mapped[str] = mapped_column(String(1), nullable=False)
    open_date: Mapped[date] = mapped_column(Date, nullable=False)
    close_date: Mapped[date | None] = mapped_column(Date, nullable=True)
    last_maint_date: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    last_maint_user: Mapped[str] = mapped_column(String(8), nullable=False)

    # Relationships
    positions: Mapped[list["InvestmentPositions"]] = relationship(
        back_populates="portfolio"
    )
    transactions: Mapped[list["TransactionHistory"]] = relationship(
        back_populates="portfolio"
    )

    __table_args__ = (
        Index("idx_port_master_client", "client_id", "status"),
    )


class InvestmentPositions(Base):
    """Investment Positions table."""

    __tablename__ = "investment_positions"

    portfolio_id: Mapped[str] = mapped_column(
        String(8),
        ForeignKey("portfolio_master.portfolio_id"),
        primary_key=True,
    )
    investment_id: Mapped[str] = mapped_column(String(10), primary_key=True)
    position_date: Mapped[date] = mapped_column(Date, primary_key=True)
    quantity: Mapped[float] = mapped_column(
        Numeric(precision=18, scale=4), nullable=False
    )
    cost_basis: Mapped[float] = mapped_column(
        Numeric(precision=18, scale=2), nullable=False
    )
    market_value: Mapped[float] = mapped_column(
        Numeric(precision=18, scale=2), nullable=False
    )
    currency_code: Mapped[str] = mapped_column(String(3), nullable=False)
    last_maint_date: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    last_maint_user: Mapped[str] = mapped_column(String(8), nullable=False)

    # Relationships
    portfolio: Mapped["PortfolioMaster"] = relationship(
        back_populates="positions"
    )

    __table_args__ = (
        Index("idx_positions_date", "position_date", "portfolio_id"),
    )


# Avoid circular import for forward references used by relationship()
from .transaction_history import TransactionHistory  # noqa: E402, F401

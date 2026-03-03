"""SQLAlchemy model for TRANSACTION_HISTORY table.

Migrated from: src/database/db2/db2-definitions.sql

Transaction types: 'BU'=Buy, 'SL'=Sell, 'TR'=Transfer, 'FE'=Fee
Status codes: 'P'=Processed, 'F'=Failed, 'R'=Reversed
TRANSACTION_ID format: YYYYMMDDHHMMSS + 6-digit sequence
"""

from __future__ import annotations

from datetime import date, datetime, time

from sqlalchemy import Date, DateTime, ForeignKey, Index, Numeric, String, Time
from sqlalchemy.orm import Mapped, mapped_column, relationship

from .base import Base


class TransactionHistory(Base):
    """Transaction History table."""

    __tablename__ = "transaction_history"

    transaction_id: Mapped[str] = mapped_column(String(20), primary_key=True)
    portfolio_id: Mapped[str] = mapped_column(
        String(8),
        ForeignKey("portfolio_master.portfolio_id"),
        nullable=False,
    )
    transaction_date: Mapped[date] = mapped_column(Date, nullable=False)
    transaction_time: Mapped[time] = mapped_column(Time, nullable=False)
    investment_id: Mapped[str] = mapped_column(String(10), nullable=False)
    transaction_type: Mapped[str] = mapped_column(String(2), nullable=False)
    quantity: Mapped[float] = mapped_column(
        Numeric(precision=18, scale=4), nullable=False
    )
    price: Mapped[float] = mapped_column(
        Numeric(precision=18, scale=4), nullable=False
    )
    amount: Mapped[float] = mapped_column(
        Numeric(precision=18, scale=2), nullable=False
    )
    currency_code: Mapped[str] = mapped_column(String(3), nullable=False)
    status: Mapped[str] = mapped_column(String(1), nullable=False)
    process_date: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    process_user: Mapped[str] = mapped_column(String(8), nullable=False)

    # Relationships
    portfolio: Mapped["PortfolioMaster"] = relationship(
        back_populates="transactions"
    )

    __table_args__ = (
        Index("idx_trans_hist_port", "portfolio_id", "transaction_date"),
        Index("idx_trans_hist_date", "transaction_date", "portfolio_id"),
    )


from .portfolio import PortfolioMaster  # noqa: E402, F401

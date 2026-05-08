from datetime import date, datetime, time
from decimal import Decimal

from sqlalchemy import Date, DateTime, ForeignKey, Numeric, String, Time
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base


class TransactionHistory(Base):
    __tablename__ = "transaction_history"

    transaction_id: Mapped[str] = mapped_column(String(20), primary_key=True)
    portfolio_id: Mapped[str] = mapped_column(
        String(8), ForeignKey("portfolio_master.portfolio_id"), nullable=False
    )
    transaction_date: Mapped[date] = mapped_column(Date, nullable=False)
    transaction_time: Mapped[time] = mapped_column(Time, nullable=False)
    investment_id: Mapped[str] = mapped_column(String(10), nullable=False)
    transaction_type: Mapped[str] = mapped_column(String(2), nullable=False)
    quantity: Mapped[Decimal] = mapped_column(Numeric(18, 4), nullable=False)
    price: Mapped[Decimal] = mapped_column(Numeric(18, 4), nullable=False)
    amount: Mapped[Decimal] = mapped_column(Numeric(18, 2), nullable=False)
    currency_code: Mapped[str] = mapped_column(String(3), nullable=False)
    status: Mapped[str] = mapped_column(String(1), nullable=False)
    process_date: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    process_user: Mapped[str] = mapped_column(String(8), nullable=False)

    portfolio = relationship("PortfolioMaster", back_populates="transactions")

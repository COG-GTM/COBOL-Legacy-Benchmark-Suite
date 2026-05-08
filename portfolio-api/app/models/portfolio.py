from datetime import date, datetime

from sqlalchemy import Date, DateTime, String, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base


class PortfolioMaster(Base):
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
    last_maint_date: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, server_default=func.now()
    )
    last_maint_user: Mapped[str] = mapped_column(String(8), nullable=False)

    positions = relationship("InvestmentPosition", back_populates="portfolio")
    transactions = relationship("TransactionHistory", back_populates="portfolio")

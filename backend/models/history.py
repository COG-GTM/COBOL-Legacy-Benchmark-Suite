"""Position history model — translated from HISTREC.cpy and POSHIST DB2 table."""

from sqlalchemy import Column, String, Numeric, Date, DateTime, Index
from datetime import datetime
from .database import Base


class PositionHistory(Base):
    __tablename__ = "position_history"

    id = Column(String(36), primary_key=True)
    portfolio_id = Column(String(8), nullable=False)
    investment_id = Column(String(10), nullable=False)
    record_date = Column(Date, nullable=False)
    share_balance = Column(Numeric(14, 3), nullable=False, default=0)
    cost_basis = Column(Numeric(13, 2), nullable=False, default=0)
    market_value = Column(Numeric(13, 2), nullable=False, default=0)
    avg_cost = Column(Numeric(9, 4), nullable=False, default=0)
    event_type = Column(String(20), nullable=False, default="UPDATE")
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)

    __table_args__ = (
        Index("idx_hist_portfolio", "portfolio_id"),
        Index("idx_hist_date", "record_date"),
        Index("idx_hist_investment", "investment_id"),
    )

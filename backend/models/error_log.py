"""Error log model — translated from ERRLOG DB2 table."""

from sqlalchemy import Column, String, Integer, DateTime, Text, Index
from datetime import datetime
from .database import Base


class ErrorLog(Base):
    __tablename__ = "error_logs"

    id = Column(Integer, primary_key=True, autoincrement=True)
    timestamp = Column(DateTime, nullable=False, default=datetime.utcnow)
    program_id = Column(String(8), nullable=False)
    error_code = Column(String(4), nullable=False)
    account_number = Column(String(10), nullable=True)
    portfolio_id = Column(String(8), nullable=True)
    transaction_id = Column(String(20), nullable=True)
    error_description = Column(Text, nullable=False)
    severity = Column(String(10), nullable=False, default="ERROR")

    __table_args__ = (
        Index("idx_errlog_timestamp", "timestamp"),
        Index("idx_errlog_program", "program_id"),
        Index("idx_errlog_code", "error_code"),
    )

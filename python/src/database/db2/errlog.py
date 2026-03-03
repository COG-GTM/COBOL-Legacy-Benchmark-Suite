"""SQLAlchemy model for ERRLOG (Error Logging) table.

Migrated from: src/database/db2/ERRLOG.sql

Error types: S=System, A=Application, D=Data
Severity levels: 1=Info, 2=Warning, 3=Error, 4=Severe
"""

from datetime import date, datetime, time

from sqlalchemy import Date, DateTime, Index, Integer, String, Time
from sqlalchemy.orm import Mapped, mapped_column

from .base import Base


class ErrLog(Base):
    """Error Logging table - stores application errors and warnings."""

    __tablename__ = "errlog"

    error_timestamp: Mapped[datetime] = mapped_column(
        DateTime, primary_key=True
    )
    program_id: Mapped[str] = mapped_column(String(8), primary_key=True)
    error_type: Mapped[str] = mapped_column(String(1), nullable=False)
    error_severity: Mapped[int] = mapped_column(Integer, nullable=False)
    error_code: Mapped[str] = mapped_column(String(8), nullable=False)
    error_message: Mapped[str] = mapped_column(String(200), nullable=False)
    process_date: Mapped[date] = mapped_column(Date, nullable=False)
    process_time: Mapped[time] = mapped_column(Time, nullable=False)
    user_id: Mapped[str] = mapped_column(String(8), nullable=False)
    additional_info: Mapped[str | None] = mapped_column(
        String(500), nullable=True
    )

    __table_args__ = (
        Index("errlog_ix1", "process_date", "error_severity"),
    )

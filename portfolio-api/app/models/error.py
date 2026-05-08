from datetime import datetime

from sqlalchemy import DateTime, Integer, String, func
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class ErrorLog(Base):
    __tablename__ = "error_log"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    timestamp: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, server_default=func.now()
    )
    program_id: Mapped[str | None] = mapped_column(String(8), nullable=True)
    category: Mapped[str | None] = mapped_column(String(2), nullable=True)
    error_code: Mapped[str | None] = mapped_column(String(4), nullable=True)
    severity: Mapped[int | None] = mapped_column(Integer, nullable=True)
    error_text: Mapped[str | None] = mapped_column(String(80), nullable=True)
    error_details: Mapped[str | None] = mapped_column(String(256), nullable=True)
    return_code: Mapped[int | None] = mapped_column(Integer, nullable=True)

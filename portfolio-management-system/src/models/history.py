"""
History data models.
Migrated from COBOL copybook: src/copybook/common/HISTREC.cpy

Original COBOL structure:
01  HISTORY-RECORD.
    05  HIST-KEY.
        10  HIST-PORTFOLIO-ID  PIC X(08).
        10  HIST-DATE          PIC X(08).
        10  HIST-TIME          PIC X(06).
        10  HIST-SEQ-NO        PIC X(04).
    05  HIST-DATA.
        10  HIST-RECORD-TYPE   PIC X(02).
        10  HIST-ACTION-CODE   PIC X(01).
        10  HIST-BEFORE-IMAGE  PIC X(400).
        10  HIST-AFTER-IMAGE   PIC X(400).
        10  HIST-REASON-CODE   PIC X(04).
    05  HIST-AUDIT.
        10  HIST-PROCESS-DATE  PIC X(26).
        10  HIST-PROCESS-USER  PIC X(08).
"""

from datetime import datetime
from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field, field_validator
from sqlalchemy import String, Text
from sqlalchemy.orm import Mapped, mapped_column

from src.database.base import AuditMixin, Base


class HistoryRecordType(str, Enum):
    """
    History record type codes.
    Migrated from COBOL: HIST-RECORD-TYPE values.
    """
    PORTFOLIO = "PT"
    POSITION = "PS"
    TRANSACTION = "TR"


class HistoryActionCode(str, Enum):
    """
    History action codes.
    Migrated from COBOL: HIST-ACTION-CODE values.
    """
    ADD = "A"
    CHANGE = "C"
    DELETE = "D"


class HistoryRecord(Base, AuditMixin):
    """
    SQLAlchemy ORM model for history records.
    Migrated from COBOL HISTREC copybook.
    """
    __tablename__ = "history_record"
    
    # Key fields (HIST-KEY)
    portfolio_id: Mapped[str] = mapped_column(String(8), primary_key=True)
    hist_date: Mapped[str] = mapped_column(String(8), primary_key=True)
    hist_time: Mapped[str] = mapped_column(String(6), primary_key=True)
    seq_no: Mapped[str] = mapped_column(String(4), primary_key=True)
    
    # Data fields (HIST-DATA)
    record_type: Mapped[str] = mapped_column(String(2), nullable=False)
    action_code: Mapped[str] = mapped_column(String(1), nullable=False)
    before_image: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    after_image: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    reason_code: Mapped[Optional[str]] = mapped_column(String(4), nullable=True)
    
    # Audit fields (HIST-AUDIT)
    process_date: Mapped[Optional[str]] = mapped_column(String(26), nullable=True)
    process_user: Mapped[Optional[str]] = mapped_column(String(8), nullable=True)
    
    def __repr__(self) -> str:
        return (
            f"HistoryRecord(portfolio={self.portfolio_id}, date={self.hist_date}, "
            f"time={self.hist_time}, seq={self.seq_no}, "
            f"type={self.record_type}, action={self.action_code})"
        )


class HistoryKey(BaseModel):
    """
    Pydantic model for history key (HIST-KEY).
    Used for lookups and validation.
    """
    portfolio_id: str = Field(..., min_length=1, max_length=8, description="Portfolio identifier")
    hist_date: str = Field(..., min_length=8, max_length=8, description="History date YYYYMMDD")
    hist_time: str = Field(..., min_length=6, max_length=6, description="History time HHMMSS")
    seq_no: str = Field(..., min_length=1, max_length=4, description="Sequence number")
    
    @field_validator("hist_date")
    @classmethod
    def validate_date(cls, v: str) -> str:
        """Validate date format YYYYMMDD."""
        if not v.isdigit():
            raise ValueError("Date must be numeric YYYYMMDD")
        try:
            datetime.strptime(v, "%Y%m%d")
        except ValueError:
            raise ValueError("Invalid date format, expected YYYYMMDD")
        return v
    
    @field_validator("hist_time")
    @classmethod
    def validate_time(cls, v: str) -> str:
        """Validate time format HHMMSS."""
        if not v.isdigit():
            raise ValueError("Time must be numeric HHMMSS")
        if len(v) != 6:
            raise ValueError("Time must be 6 digits HHMMSS")
        hours, minutes, seconds = int(v[:2]), int(v[2:4]), int(v[4:6])
        if not (0 <= hours <= 23 and 0 <= minutes <= 59 and 0 <= seconds <= 59):
            raise ValueError("Invalid time values")
        return v


class HistoryData(BaseModel):
    """
    Pydantic model for history data (HIST-DATA).
    Used for API requests and validation.
    """
    record_type: HistoryRecordType = Field(..., description="Record type code")
    action_code: HistoryActionCode = Field(..., description="Action code")
    before_image: Optional[str] = Field(None, max_length=400, description="Before image data")
    after_image: Optional[str] = Field(None, max_length=400, description="After image data")
    reason_code: Optional[str] = Field(None, max_length=4, description="Reason code")


class HistoryCreate(HistoryKey, HistoryData):
    """
    Pydantic model for creating a new history record.
    Combines key and data fields.
    """
    pass


class HistoryResponse(HistoryCreate):
    """
    Pydantic model for history API responses.
    Includes audit fields.
    """
    process_date: Optional[str] = None
    process_user: Optional[str] = None
    
    class Config:
        from_attributes = True


class HistoryQuery(BaseModel):
    """
    Pydantic model for history query parameters.
    Used in INQHIST equivalent.
    """
    portfolio_id: str = Field(..., min_length=1, max_length=8)
    start_date: Optional[str] = Field(None, min_length=8, max_length=8)
    end_date: Optional[str] = Field(None, min_length=8, max_length=8)
    record_type: Optional[HistoryRecordType] = None
    action_code: Optional[HistoryActionCode] = None
    limit: int = Field(default=100, ge=1, le=1000)
    offset: int = Field(default=0, ge=0)


class HistoryListResponse(BaseModel):
    """
    Pydantic model for history list response.
    Used in online inquiry (INQHIST equivalent).
    """
    portfolio_id: str
    total_records: int
    records: list[HistoryResponse]
    has_more: bool

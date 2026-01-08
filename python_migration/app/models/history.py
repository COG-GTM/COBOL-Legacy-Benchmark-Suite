"""History Record model - converted from HISTREC.cpy.

COBOL Original:
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


class HistoryRecordType(str, Enum):
    """History record type codes - maps to 88-level conditions in COBOL."""

    PORTFOLIO = "PT"  # HIST-TYPE-PORT
    POSITION = "PS"  # HIST-TYPE-POS
    TRANSACTION = "TR"  # HIST-TYPE-TRN


class HistoryActionCode(str, Enum):
    """History action codes - maps to 88-level conditions in COBOL."""

    ADD = "A"  # HIST-ACTION-ADD
    CHANGE = "C"  # HIST-ACTION-CHG
    DELETE = "D"  # HIST-ACTION-DEL


class HistoryKey(BaseModel):
    """History key structure - maps to HIST-KEY in COBOL."""

    portfolio_id: str = Field(max_length=8, description="Portfolio identifier")
    date: str = Field(max_length=8, description="History date (YYYYMMDD)")
    time: str = Field(max_length=6, description="History time (HHMMSS)")
    seq_no: str = Field(max_length=4, description="Sequence number")

    @field_validator("date")
    @classmethod
    def validate_date_format(cls, v: str) -> str:
        """Validate date is in YYYYMMDD format."""
        if len(v) != 8 or not v.isdigit():
            raise ValueError("Date must be in YYYYMMDD format")
        return v

    @field_validator("time")
    @classmethod
    def validate_time_format(cls, v: str) -> str:
        """Validate time is in HHMMSS format."""
        if len(v) != 6 or not v.isdigit():
            raise ValueError("Time must be in HHMMSS format")
        return v

    @property
    def history_id(self) -> str:
        """Generate unique history ID from key components."""
        return f"{self.portfolio_id}{self.date}{self.time}{self.seq_no}"


class HistoryData(BaseModel):
    """History data structure - maps to HIST-DATA in COBOL."""

    record_type: HistoryRecordType = Field(description="Type of record being tracked")
    action_code: HistoryActionCode = Field(description="Action performed")
    before_image: str = Field(
        default="", max_length=400, description="Record image before change"
    )
    after_image: str = Field(
        default="", max_length=400, description="Record image after change"
    )
    reason_code: str = Field(
        default="", max_length=4, description="Reason for change"
    )


class HistoryAudit(BaseModel):
    """History audit structure - maps to HIST-AUDIT in COBOL."""

    process_date: Optional[datetime] = Field(
        default=None, description="Processing date/time"
    )
    process_user: str = Field(
        default="", max_length=8, description="Processing user ID"
    )


class HistoryRecord(BaseModel):
    """Complete history record - maps to HISTORY-RECORD in COBOL.

    This model represents an audit trail entry tracking changes to
    portfolios, positions, and transactions.
    """

    key: HistoryKey
    data: HistoryData
    audit: HistoryAudit = Field(default_factory=HistoryAudit)

    @property
    def portfolio_id(self) -> str:
        """Convenience accessor for portfolio ID."""
        return self.key.portfolio_id

    @property
    def history_id(self) -> str:
        """Convenience accessor for history ID."""
        return self.key.history_id

    @property
    def is_add(self) -> bool:
        """Check if this is an add action."""
        return self.data.action_code == HistoryActionCode.ADD

    @property
    def is_change(self) -> bool:
        """Check if this is a change action."""
        return self.data.action_code == HistoryActionCode.CHANGE

    @property
    def is_delete(self) -> bool:
        """Check if this is a delete action."""
        return self.data.action_code == HistoryActionCode.DELETE

    def to_flat_dict(self) -> dict:
        """Convert to flat dictionary for database operations."""
        return {
            "history_id": self.key.history_id,
            "portfolio_id": self.key.portfolio_id,
            "history_date": self.key.date,
            "history_time": self.key.time,
            "seq_no": self.key.seq_no,
            "record_type": self.data.record_type.value,
            "action_code": self.data.action_code.value,
            "before_image": self.data.before_image,
            "after_image": self.data.after_image,
            "reason_code": self.data.reason_code,
            "process_date": self.audit.process_date,
            "process_user": self.audit.process_user,
        }

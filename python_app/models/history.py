"""History Record model - translated from HISTREC.cpy copybook.

Mirrors the COBOL HISTORY-RECORD structure with composite key
(portfolio_id, date, time, seq_no) and history data fields.
"""

from enum import StrEnum

from pydantic import BaseModel, Field


class HistoryRecordType(StrEnum):
    """History record type codes from 88-level values in HISTREC."""

    PORTFOLIO = "PT"
    POSITION = "PS"
    TRANSACTION = "TR"


class HistoryActionCode(StrEnum):
    """History action codes from 88-level values in HISTREC."""

    ADD = "A"
    CHANGE = "C"
    DELETE = "D"


class HistoryRecord(BaseModel):
    """Full history record translated from COBOL HISTORY-RECORD.

    Maps to HISTREC.cpy copybook fields:
    - HIST-KEY (composite key)
    - HIST-DATA (history details with before/after images)
    """

    # Key fields (HIST-KEY)
    portfolio_id: str = Field(max_length=8, description="HIST-PORTFOLIO-ID")
    date: str = Field(max_length=8, description="HIST-DATE: YYYYMMDD")
    time: str = Field(max_length=6, description="HIST-TIME: HHMMSS")
    seq_no: str = Field(max_length=4, description="HIST-SEQ-NO")

    # Data fields (HIST-DATA)
    record_type: HistoryRecordType = Field(description="HIST-RECORD-TYPE: PT/PS/TR")
    action_code: HistoryActionCode = Field(description="HIST-ACTION-CODE: A/C/D")
    before_image: str = Field(default="", max_length=400, description="HIST-BEFORE-IMAGE")
    after_image: str = Field(default="", max_length=400, description="HIST-AFTER-IMAGE")
    reason_code: str = Field(default="", max_length=4, description="HIST-REASON-CODE")

    @property
    def composite_key(self) -> str:
        """Build the composite key matching VSAM KSDS key structure."""
        return f"{self.portfolio_id}{self.date}{self.time}{self.seq_no}"

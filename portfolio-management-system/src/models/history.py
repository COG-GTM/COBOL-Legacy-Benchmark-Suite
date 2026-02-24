"""
History Record Data Model.

Translated from COBOL copybook: src/copybook/common/HISTREC.cpy

COBOL Source Structure:
    01 HISTORY-RECORD.
       05 HIST-KEY.
          10 HIST-PORTFOLIO-ID  PIC X(8).       -> str
          10 HIST-DATE          PIC X(8).       -> str (YYYYMMDD)
          10 HIST-TIME          PIC X(6).       -> str (HHMMSS)
          10 HIST-SEQ-NO        PIC X(4).       -> str
       05 HIST-DATA.
          10 HIST-RECORD-TYPE   PIC X(2).       -> HistoryRecordType enum
             88 HIST-PORTFOLIO  VALUE 'PT'.
             88 HIST-POSITION   VALUE 'PS'.
             88 HIST-TRANSACT   VALUE 'TR'.
          10 HIST-ACTION-CODE   PIC X(1).       -> HistoryActionCode enum
             88 HIST-ADD        VALUE 'A'.
             88 HIST-CHANGE     VALUE 'C'.
             88 HIST-DELETE     VALUE 'D'.
          10 HIST-BEFORE-IMAGE  PIC X(400).     -> str (up to 400 chars)
          10 HIST-AFTER-IMAGE   PIC X(400).     -> str (up to 400 chars)
       05 HIST-AUDIT.
          10 HIST-REASON-CODE   PIC X(4).       -> str
          10 HIST-PROCESS-DATE  PIC X(26).      -> str (IBM timestamp)
          10 HIST-PROCESS-USER  PIC X(8).       -> str

Data Type Mapping Notes:
    PIC X(400) -> str with max_length=400
        Used for before/after images to capture full record state.
        In the original COBOL, these are fixed-length 400-byte fields
        that store the complete record image before and after a change.
        In Python, we use str (variable length) but preserve the max length
        constraint for validation.
    PIC X(n) -> str with max_length=n
        Standard fixed-length alphanumeric fields.
"""

from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field, field_validator


class HistoryRecordType(str, Enum):
    """History record type codes.

    COBOL 88-level condition names from HISTREC copybook.
    Identifies what type of record this history entry tracks.
    """

    PORTFOLIO = "PT"
    POSITION = "PS"
    TRANSACTION = "TR"


class HistoryActionCode(str, Enum):
    """History action codes.

    COBOL 88-level condition names from HISTREC copybook.
    Identifies the type of change that was made.
    """

    ADD = "A"
    CHANGE = "C"
    DELETE = "D"


class HistoryKey(BaseModel):
    """History key structure (HIST-KEY group).

    Composite key: portfolio_id + date + time + seq_no
    Total key length: 26 bytes
    """

    portfolio_id: str = Field(
        ...,
        max_length=8,
        description="Portfolio identifier. COBOL: HIST-PORTFOLIO-ID PIC X(8).",
    )
    date: str = Field(
        ...,
        max_length=8,
        description="History date (YYYYMMDD). COBOL: HIST-DATE PIC X(8).",
    )
    time: str = Field(
        ...,
        max_length=6,
        description="History time (HHMMSS). COBOL: HIST-TIME PIC X(6).",
    )
    seq_no: str = Field(
        ...,
        max_length=4,
        description="Sequence number. COBOL: HIST-SEQ-NO PIC X(4).",
    )

    @field_validator("date")
    @classmethod
    def validate_date_format(cls, v: str) -> str:
        if len(v) != 8 or not v.isdigit():
            raise ValueError("Date must be 8 digits in YYYYMMDD format")
        year, month, day = int(v[:4]), int(v[4:6]), int(v[6:8])
        if not (1900 <= year <= 2099 and 1 <= month <= 12 and 1 <= day <= 31):
            raise ValueError("Invalid date values")
        return v

    @field_validator("time")
    @classmethod
    def validate_time_format(cls, v: str) -> str:
        if len(v) != 6 or not v.isdigit():
            raise ValueError("Time must be 6 digits in HHMMSS format")
        hour, minute, second = int(v[:2]), int(v[2:4]), int(v[4:6])
        if not (0 <= hour <= 23 and 0 <= minute <= 59 and 0 <= second <= 59):
            raise ValueError("Invalid time values")
        return v


class HistoryData(BaseModel):
    """History data fields (HIST-DATA group)."""

    record_type: HistoryRecordType = Field(
        ...,
        description=(
            "Type of record being tracked. "
            "COBOL: HIST-RECORD-TYPE PIC X(2)."
        ),
    )
    action_code: HistoryActionCode = Field(
        ...,
        description=(
            "Type of change performed. "
            "COBOL: HIST-ACTION-CODE PIC X(1)."
        ),
    )
    before_image: Optional[str] = Field(
        default=None,
        max_length=400,
        description=(
            "Record state before the change. "
            "COBOL: HIST-BEFORE-IMAGE PIC X(400). "
            "None for ADD operations."
        ),
    )
    after_image: Optional[str] = Field(
        default=None,
        max_length=400,
        description=(
            "Record state after the change. "
            "COBOL: HIST-AFTER-IMAGE PIC X(400). "
            "None for DELETE operations."
        ),
    )
    reason_code: Optional[str] = Field(
        default=None,
        max_length=4,
        description="Reason code for the change. COBOL: HIST-REASON-CODE PIC X(4).",
    )


class HistoryAudit(BaseModel):
    """History audit fields (HIST-AUDIT group)."""

    process_date: Optional[str] = Field(
        default=None,
        max_length=26,
        description=(
            "Processing timestamp. "
            "COBOL: HIST-PROCESS-DATE PIC X(26). "
            "IBM format: YYYY-MM-DD-HH.MM.SS.FFFFFF."
        ),
    )
    process_user: Optional[str] = Field(
        default=None,
        max_length=8,
        description="Processing user ID. COBOL: HIST-PROCESS-USER PIC X(8).",
    )


class HistoryRecord(BaseModel):
    """Complete history record (HISTORY-RECORD).

    Translated from COBOL copybook HISTREC.cpy.
    Provides a complete audit trail of all changes to portfolios,
    positions, and transactions by capturing before and after images
    of modified records.

    Usage:
        record = HistoryRecord(
            key=HistoryKey(
                portfolio_id="PORT0001",
                date="20240115",
                time="143022",
                seq_no="0001",
            ),
            data=HistoryData(
                record_type=HistoryRecordType.POSITION,
                action_code=HistoryActionCode.CHANGE,
                before_image="... original record data ...",
                after_image="... modified record data ...",
                reason_code="UPDT",
            ),
            audit=HistoryAudit(
                process_date="20240115",
                process_user="BATCH001",
            ),
        )
    """

    key: HistoryKey
    data: HistoryData
    audit: HistoryAudit = Field(default_factory=HistoryAudit)

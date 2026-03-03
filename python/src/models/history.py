"""
Pydantic v2 models for COBOL HISTREC copybook (History Record).

Source: src/copybook/common/HISTREC.cpy
"""

from pydantic import BaseModel, Field, field_validator


class HistoryKey(BaseModel):
    """History key fields from HIST-KEY (level 05)."""

    model_config = {"from_attributes": True}

    hist_portfolio_id: str = Field(
        max_length=8,
        description="Portfolio identifier. COBOL: HIST-PORTFOLIO-ID PIC X(08).",
    )
    hist_date: str = Field(
        max_length=8,
        description="History date YYYYMMDD. COBOL: HIST-DATE PIC X(08).",
    )
    hist_time: str = Field(
        max_length=6,
        description="History time HHMMSS. COBOL: HIST-TIME PIC X(06).",
    )
    hist_seq_no: str = Field(
        max_length=4,
        description="Sequence number. COBOL: HIST-SEQ-NO PIC X(04).",
    )


class HistoryData(BaseModel):
    """History data fields from HIST-DATA (level 05)."""

    model_config = {"from_attributes": True}

    hist_record_type: str = Field(
        max_length=2,
        description=(
            "Record type: PT=Portfolio, PS=Position, TR=Transaction. "
            "COBOL: HIST-RECORD-TYPE PIC X(02). "
            "88-level values: PT, PS, TR."
        ),
    )
    hist_action_code: str = Field(
        max_length=1,
        description=(
            "Action code: A=Add, C=Change, D=Delete. "
            "COBOL: HIST-ACTION-CODE PIC X(01). "
            "88-level values: A, C, D."
        ),
    )
    hist_before_image: str = Field(
        max_length=400,
        description="Record image before change. COBOL: HIST-BEFORE-IMAGE PIC X(400).",
    )
    hist_after_image: str = Field(
        max_length=400,
        description="Record image after change. COBOL: HIST-AFTER-IMAGE PIC X(400).",
    )
    hist_reason_code: str = Field(
        max_length=4,
        description="Reason for change. COBOL: HIST-REASON-CODE PIC X(04).",
    )

    @field_validator("hist_record_type")
    @classmethod
    def validate_record_type(cls, v: str) -> str:
        valid = {"PT", "PS", "TR"}
        if v.strip() and v.strip() not in valid:
            raise ValueError(f"hist_record_type must be one of {valid}")
        return v

    @field_validator("hist_action_code")
    @classmethod
    def validate_action_code(cls, v: str) -> str:
        valid = {"A", "C", "D"}
        if v.strip() and v.strip() not in valid:
            raise ValueError(f"hist_action_code must be one of {valid}")
        return v


class HistoryAudit(BaseModel):
    """History audit fields from HIST-AUDIT (level 05)."""

    model_config = {"from_attributes": True}

    hist_process_date: str = Field(
        max_length=26,
        description="Process timestamp. COBOL: HIST-PROCESS-DATE PIC X(26).",
    )
    hist_process_user: str = Field(
        max_length=8,
        description="Processing user ID. COBOL: HIST-PROCESS-USER PIC X(08).",
    )


class HistoryRecord(BaseModel):
    """
    History Record — maps to COBOL 01-level HISTORY-RECORD.

    Source: src/copybook/common/HISTREC.cpy
    """

    model_config = {"from_attributes": True}

    hist_key: HistoryKey = Field(description="History key (HIST-KEY).")
    hist_data: HistoryData = Field(description="History data (HIST-DATA).")
    hist_audit: HistoryAudit = Field(description="Audit trail (HIST-AUDIT).")
    hist_filler: str = Field(
        default="",
        max_length=50,
        description="Reserved filler. COBOL: HIST-FILLER PIC X(50).",
    )

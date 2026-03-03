"""
Pydantic v2 models for COBOL CKPRST copybook (Checkpoint/Restart Control).

Source: src/copybook/batch/CKPRST.cpy
"""

from typing import List

from pydantic import BaseModel, Field, field_validator


class CheckpointHeader(BaseModel):
    """Checkpoint header from CK-HEADER (level 05)."""

    model_config = {"from_attributes": True}

    ck_program_id: str = Field(
        max_length=8,
        description="Program identifier. COBOL: CK-PROGRAM-ID PIC X(8).",
    )
    ck_run_date: str = Field(
        max_length=8,
        description="Run date YYYYMMDD. COBOL: CK-RUN-DATE PIC X(8).",
    )
    ck_run_time: str = Field(
        max_length=6,
        description="Run time HHMMSS. COBOL: CK-RUN-TIME PIC X(6).",
    )
    ck_status: str = Field(
        max_length=1,
        description=(
            "Checkpoint status: I=Initial, A=Active, C=Complete, F=Failed, R=Restarted. "
            "COBOL: CK-STATUS PIC X(1). "
            "88-level values: I, A, C, F, R."
        ),
    )

    @field_validator("ck_status")
    @classmethod
    def validate_ck_status(cls, v: str) -> str:
        valid = {"I", "A", "C", "F", "R"}
        if v.strip() and v.strip() not in valid:
            raise ValueError(f"ck_status must be one of {valid}")
        return v


class CheckpointCounters(BaseModel):
    """Checkpoint counters from CK-COUNTERS (level 05)."""

    model_config = {"from_attributes": True}

    ck_records_read: int = Field(
        description="Records read. COBOL: CK-RECORDS-READ PIC 9(9) COMP.",
    )
    ck_records_proc: int = Field(
        description="Records processed. COBOL: CK-RECORDS-PROC PIC 9(9) COMP.",
    )
    ck_records_error: int = Field(
        description="Records in error. COBOL: CK-RECORDS-ERROR PIC 9(9) COMP.",
    )
    ck_restart_count: int = Field(
        description="Restart count. COBOL: CK-RESTART-COUNT PIC 9(2) COMP.",
    )


class CheckpointPosition(BaseModel):
    """Checkpoint position from CK-POSITION (level 05)."""

    model_config = {"from_attributes": True}

    ck_last_key: str = Field(
        max_length=50,
        description="Last key processed. COBOL: CK-LAST-KEY PIC X(50).",
    )
    ck_last_time: str = Field(
        max_length=26,
        description="Last checkpoint time. COBOL: CK-LAST-TIME PIC X(26).",
    )
    ck_phase: str = Field(
        max_length=2,
        description=(
            "Processing phase: 00=Init, 10=Read, 20=Process, 30=Update, 40=Terminate. "
            "COBOL: CK-PHASE PIC X(2). "
            "88-level values: 00, 10, 20, 30, 40."
        ),
    )

    @field_validator("ck_phase")
    @classmethod
    def validate_ck_phase(cls, v: str) -> str:
        valid = {"00", "10", "20", "30", "40"}
        if v.strip() and v.strip() not in valid:
            raise ValueError(f"ck_phase must be one of {valid}")
        return v


class CheckpointFileStatus(BaseModel):
    """
    Single file status entry from CK-FILE-STATUS (OCCURS 5 TIMES).

    COBOL: CK-FILE-STATUS OCCURS 5 TIMES.
    """

    model_config = {"from_attributes": True}

    ck_file_name: str = Field(
        max_length=8,
        description="File name. COBOL: CK-FILE-NAME PIC X(8).",
    )
    ck_file_pos: str = Field(
        max_length=50,
        description="File position. COBOL: CK-FILE-POS PIC X(50).",
    )
    ck_file_status: str = Field(
        max_length=2,
        description="File status. COBOL: CK-FILE-STATUS PIC X(2).",
    )


class CheckpointResources(BaseModel):
    """Checkpoint resources from CK-RESOURCES (level 05)."""

    model_config = {"from_attributes": True}

    ck_file_statuses: List[CheckpointFileStatus] = Field(
        max_length=5,
        description="File statuses. COBOL: CK-FILE-STATUS OCCURS 5 TIMES.",
    )


class CheckpointControlInfo(BaseModel):
    """Control info from CK-CONTROL-INFO (level 05)."""

    model_config = {"from_attributes": True}

    ck_commit_freq: int = Field(
        default=1000,
        description="Commit frequency. COBOL: CK-COMMIT-FREQ PIC 9(5) COMP VALUE 1000.",
    )
    ck_max_errors: int = Field(
        default=100,
        description="Max errors. COBOL: CK-MAX-ERRORS PIC 9(3) COMP VALUE 100.",
    )
    ck_max_restarts: int = Field(
        default=3,
        description="Max restarts. COBOL: CK-MAX-RESTARTS PIC 9(2) COMP VALUE 3.",
    )
    ck_restart_mode: str = Field(
        max_length=1,
        description=(
            "Restart mode: N=Normal, R=Restart, C=Recover. "
            "COBOL: CK-RESTART-MODE PIC X(1). "
            "88-level values: N, R, C."
        ),
    )

    @field_validator("ck_restart_mode")
    @classmethod
    def validate_ck_restart_mode(cls, v: str) -> str:
        valid = {"N", "R", "C"}
        if v.strip() and v.strip() not in valid:
            raise ValueError(f"ck_restart_mode must be one of {valid}")
        return v


class CheckpointControl(BaseModel):
    """
    Checkpoint/Restart Control -- maps to COBOL 01-level CHECKPOINT-CONTROL.

    Source: src/copybook/batch/CKPRST.cpy
    """

    model_config = {"from_attributes": True}

    ck_header: CheckpointHeader = Field(description="Checkpoint header (CK-HEADER).")
    ck_counters: CheckpointCounters = Field(description="Checkpoint counters (CK-COUNTERS).")
    ck_position: CheckpointPosition = Field(description="Checkpoint position (CK-POSITION).")
    ck_resources: CheckpointResources = Field(description="Checkpoint resources (CK-RESOURCES).")
    ck_control_info: CheckpointControlInfo = Field(description="Control info (CK-CONTROL-INFO).")


class CheckpointRecord(BaseModel):
    """
    Checkpoint VSAM File Record -- maps to COBOL 01-level CHECKPOINT-RECORD.

    Source: src/copybook/batch/CKPRST.cpy
    """

    model_config = {"from_attributes": True}

    ckr_program_id: str = Field(
        max_length=8,
        description="Program identifier. COBOL: CKR-PROGRAM-ID PIC X(8).",
    )
    ckr_run_date: str = Field(
        max_length=8,
        description="Run date. COBOL: CKR-RUN-DATE PIC X(8).",
    )
    ckr_data: str = Field(
        max_length=400,
        description="Checkpoint data. COBOL: CKR-DATA PIC X(400).",
    )

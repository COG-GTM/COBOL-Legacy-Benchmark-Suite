"""
Pydantic v2 models for COBOL BCHCTL copybook (Batch Control File Record).

Source: src/copybook/batch/BCHCTL.cpy
"""

from typing import List

from pydantic import BaseModel, Field, field_validator


class BatchControlKey(BaseModel):
    """Batch control key from BCT-KEY (level 05)."""

    model_config = {"from_attributes": True}

    bct_job_name: str = Field(
        max_length=8,
        description="Job name. COBOL: BCT-JOB-NAME PIC X(8).",
    )
    bct_process_date: str = Field(
        max_length=8,
        description="Process date YYYYMMDD. COBOL: BCT-PROCESS-DATE PIC X(8).",
    )
    bct_sequence_no: int = Field(
        description="Sequence number. COBOL: BCT-SEQUENCE-NO PIC 9(4).",
    )


class PrerequisiteJob(BaseModel):
    """
    Single prerequisite job entry from BCT-PREREQ-JOBS (OCCURS 10 TIMES).

    COBOL: BCT-PREREQ-JOBS OCCURS 10 TIMES.
    """

    model_config = {"from_attributes": True}

    bct_prereq_name: str = Field(
        max_length=8,
        description="Prerequisite job name. COBOL: BCT-PREREQ-NAME PIC X(8).",
    )
    bct_prereq_seq: int = Field(
        description="Prerequisite sequence number. COBOL: BCT-PREREQ-SEQ PIC 9(4).",
    )
    bct_prereq_rc: int = Field(
        description="Prerequisite return code. COBOL: BCT-PREREQ-RC PIC S9(4) COMP.",
    )


class BatchProcessControl(BaseModel):
    """Process control from BCT-PROCESS-CONTROL (level 10)."""

    model_config = {"from_attributes": True}

    bct_step_name: str = Field(
        max_length=8,
        description="Step name. COBOL: BCT-STEP-NAME PIC X(8).",
    )
    bct_program_name: str = Field(
        max_length=8,
        description="Program name. COBOL: BCT-PROGRAM-NAME PIC X(8).",
    )
    bct_start_time: str = Field(
        max_length=8,
        description="Start time. COBOL: BCT-START-TIME PIC X(8).",
    )
    bct_end_time: str = Field(
        max_length=8,
        description="End time. COBOL: BCT-END-TIME PIC X(8).",
    )


class BatchDependencies(BaseModel):
    """Dependencies from BCT-DEPENDENCIES (level 10)."""

    model_config = {"from_attributes": True}

    bct_prereq_count: int = Field(
        description="Number of prerequisites. COBOL: BCT-PREREQ-COUNT PIC 9(2) COMP.",
    )
    bct_prereq_jobs: List[PrerequisiteJob] = Field(
        max_length=10,
        description="Prerequisite jobs. COBOL: BCT-PREREQ-JOBS OCCURS 10 TIMES.",
    )


class BatchReturnInfo(BaseModel):
    """Return info from BCT-RETURN-INFO (level 10)."""

    model_config = {"from_attributes": True}

    bct_return_code: int = Field(
        description="Return code. COBOL: BCT-RETURN-CODE PIC S9(4) COMP.",
    )
    bct_error_desc: str = Field(
        max_length=80,
        description="Error description. COBOL: BCT-ERROR-DESC PIC X(80).",
    )


class BatchControlData(BaseModel):
    """Batch control data from BCT-DATA (level 05)."""

    model_config = {"from_attributes": True}

    bct_status: str = Field(
        max_length=1,
        description=(
            "Batch status: R=Ready, A=Active, W=Waiting, D=Done, E=Error. "
            "COBOL: BCT-STATUS PIC X(1). "
            "88-level values: R, A, W, D, E."
        ),
    )
    bct_process_control: BatchProcessControl = Field(
        description="Process control (BCT-PROCESS-CONTROL).",
    )
    bct_dependencies: BatchDependencies = Field(
        description="Dependencies (BCT-DEPENDENCIES).",
    )
    bct_return_info: BatchReturnInfo = Field(
        description="Return info (BCT-RETURN-INFO).",
    )

    @field_validator("bct_status")
    @classmethod
    def validate_bct_status(cls, v: str) -> str:
        valid = {"R", "A", "W", "D", "E"}
        if v.strip() and v.strip() not in valid:
            raise ValueError(f"bct_status must be one of {valid}")
        return v


class BatchStatistics(BaseModel):
    """Batch statistics from BCT-STATISTICS (level 05)."""

    model_config = {"from_attributes": True}

    bct_restart_count: int = Field(
        description="Restart count. COBOL: BCT-RESTART-COUNT PIC 9(2) COMP.",
    )
    bct_attempt_ts: str = Field(
        max_length=26,
        description="Attempt timestamp. COBOL: BCT-ATTEMPT-TS PIC X(26).",
    )
    bct_complete_ts: str = Field(
        max_length=26,
        description="Completion timestamp. COBOL: BCT-COMPLETE-TS PIC X(26).",
    )


class BatchControlRecord(BaseModel):
    """
    Batch Control File Record -- maps to COBOL 01-level BATCH-CONTROL-RECORD.

    Source: src/copybook/batch/BCHCTL.cpy
    """

    model_config = {"from_attributes": True}

    bct_key: BatchControlKey = Field(description="Batch control key (BCT-KEY).")
    bct_data: BatchControlData = Field(description="Batch control data (BCT-DATA).")
    bct_statistics: BatchStatistics = Field(description="Batch statistics (BCT-STATISTICS).")
    bct_filler: str = Field(
        default="",
        max_length=50,
        description="Reserved filler. COBOL: BCT-FILLER PIC X(50).",
    )

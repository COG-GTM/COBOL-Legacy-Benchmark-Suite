"""Batch control models translated from src/copybook/batch/ copybooks.

Sources:
- BCHCTL.cpy  -> BatchControlRecord
- BCHCON.cpy  -> batch constants (folded into enums.py)
- CKPRST.cpy  -> CheckpointRecord
- PRCSEQ.cpy  -> ProcessSequenceRecord
"""

from datetime import datetime

from pydantic import BaseModel, field_validator

from models.enums import (
    BatchStatus,
    CheckpointPhase,
    CheckpointStatus,
    PrcseqDependencyType,
    ProcessSequenceType,
    RestartMode,
    ScheduleFrequency,
)

# ---------------------------------------------------------------------------
# Constants from BCHCON.cpy
# ---------------------------------------------------------------------------

BATCH_MAX_PREREQ: int = 10
BATCH_MAX_RESTARTS: int = 3
BATCH_WAIT_INTERVAL: int = 300  # seconds
BATCH_MAX_WAIT_TIME: int = 3600  # seconds


# ---------------------------------------------------------------------------
# Nested models
# ---------------------------------------------------------------------------


class PrerequisiteJob(BaseModel):
    """Single prerequisite entry from BCT-PREREQ-JOBS OCCURS 10 TIMES."""

    name: str
    sequence: int
    return_code: int

    @field_validator("name")
    @classmethod
    def validate_name(cls, v: str) -> str:
        if len(v) > 8:
            raise ValueError("Prerequisite job name must not exceed 8 characters")
        return v


class DependencyEntry(BaseModel):
    """Single dependency entry from PSR-DEP-ENTRY OCCURS 10 TIMES."""

    dep_id: str
    dep_type: PrcseqDependencyType
    dep_return_code: int

    @field_validator("dep_id")
    @classmethod
    def validate_dep_id(cls, v: str) -> str:
        if len(v) > 8:
            raise ValueError("Dependency ID must not exceed 8 characters")
        return v


class FileStatus(BaseModel):
    """Checkpoint file status from CK-FILE-STATUS OCCURS 5 TIMES."""

    file_name: str
    file_position: str
    file_status: str

    @field_validator("file_name")
    @classmethod
    def validate_file_name(cls, v: str) -> str:
        if len(v) > 8:
            raise ValueError("File name must not exceed 8 characters")
        return v


# ---------------------------------------------------------------------------
# Main batch models
# ---------------------------------------------------------------------------


class BatchControlRecord(BaseModel):
    """Batch control record (BCHCTL.cpy BATCH-CONTROL-RECORD).

    Manages job-level sequencing and dependencies.
    """

    job_name: str
    process_date: str
    sequence_no: int
    status: BatchStatus
    step_name: str
    program_name: str
    start_time: str
    end_time: str
    prereq_count: int
    prerequisites: list[PrerequisiteJob]
    return_code: int
    error_desc: str
    restart_count: int
    attempt_timestamp: datetime | None = None
    complete_timestamp: datetime | None = None

    @field_validator("job_name")
    @classmethod
    def validate_job_name(cls, v: str) -> str:
        if len(v) > 8:
            raise ValueError("Job name must not exceed 8 characters")
        return v

    @field_validator("process_date")
    @classmethod
    def validate_process_date(cls, v: str) -> str:
        if len(v) != 8 or not v.isdigit():
            raise ValueError("Process date must be 8 numeric digits (YYYYMMDD)")
        return v

    @field_validator("step_name", "program_name")
    @classmethod
    def validate_name_fields(cls, v: str) -> str:
        if len(v) > 8:
            raise ValueError("Field must not exceed 8 characters")
        return v

    @field_validator("error_desc")
    @classmethod
    def validate_error_desc(cls, v: str) -> str:
        if len(v) > 80:
            raise ValueError("Error description must not exceed 80 characters")
        return v


class CheckpointRecord(BaseModel):
    """Checkpoint/restart control record (CKPRST.cpy CHECKPOINT-CONTROL).

    Tracks program-level checkpointing during batch execution.
    """

    program_id: str
    run_date: str
    run_time: str
    status: CheckpointStatus
    records_read: int
    records_processed: int
    records_error: int
    restart_count: int
    last_key: str
    last_time: datetime | None = None
    phase: CheckpointPhase
    file_statuses: list[FileStatus]
    commit_frequency: int = 1000
    max_errors: int = 100
    max_restarts: int = 3
    restart_mode: RestartMode = RestartMode.NORMAL

    @field_validator("program_id")
    @classmethod
    def validate_program_id(cls, v: str) -> str:
        if len(v) > 8:
            raise ValueError("Program ID must not exceed 8 characters")
        return v

    @field_validator("run_date")
    @classmethod
    def validate_run_date(cls, v: str) -> str:
        if len(v) != 8 or not v.isdigit():
            raise ValueError("Run date must be 8 numeric digits (YYYYMMDD)")
        return v

    @field_validator("run_time")
    @classmethod
    def validate_run_time(cls, v: str) -> str:
        if len(v) != 6 or not v.isdigit():
            raise ValueError("Run time must be 6 numeric digits (HHMMSS)")
        return v

    @field_validator("last_key")
    @classmethod
    def validate_last_key(cls, v: str) -> str:
        if len(v) > 50:
            raise ValueError("Last key must not exceed 50 characters")
        return v


class ProcessSequenceRecord(BaseModel):
    """Process sequence definition (PRCSEQ.cpy PROCESS-SEQUENCE-RECORD).

    Defines batch job ordering, dependencies, and scheduling.
    """

    process_id: str
    version: int
    description: str
    process_type: ProcessSequenceType
    frequency: ScheduleFrequency
    start_time: int
    max_time: int
    dep_count: int
    dependencies: list[DependencyEntry]
    program: str
    parameter: str
    max_return_code: int
    restartable: bool
    active_days: str
    month_end: bool
    holiday_run: bool
    recovery_program: str
    recovery_parameter: str
    error_limit: int
    create_date: str
    create_user: str
    update_date: str
    update_user: str

    @field_validator("process_id")
    @classmethod
    def validate_process_id(cls, v: str) -> str:
        if len(v) > 8:
            raise ValueError("Process ID must not exceed 8 characters")
        return v

    @field_validator("description")
    @classmethod
    def validate_description(cls, v: str) -> str:
        if len(v) > 30:
            raise ValueError("Description must not exceed 30 characters")
        return v

    @field_validator("program", "recovery_program")
    @classmethod
    def validate_program_fields(cls, v: str) -> str:
        if len(v) > 8:
            raise ValueError("Program name must not exceed 8 characters")
        return v

    @field_validator("parameter", "recovery_parameter")
    @classmethod
    def validate_parameter_fields(cls, v: str) -> str:
        if len(v) > 50:
            raise ValueError("Parameter must not exceed 50 characters")
        return v

    @field_validator("active_days")
    @classmethod
    def validate_active_days(cls, v: str) -> str:
        if len(v) != 7:
            raise ValueError("Active days must be exactly 7 characters (YYYYYNN pattern)")
        return v

    @field_validator("create_user", "update_user")
    @classmethod
    def validate_user_fields(cls, v: str) -> str:
        if len(v) > 8:
            raise ValueError("User field must not exceed 8 characters")
        return v

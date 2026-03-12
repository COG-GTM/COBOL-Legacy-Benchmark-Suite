"""
Batch control models translated from COBOL copybooks BCHPARM.cpy, BCHSTAT.cpy, BCHCTL.cpy,
BCHCON.cpy, PRCSEQ.cpy, and CKPRST.cpy.

These models govern the batch job lifecycle, step sequencing, prerequisites,
and checkpoint/restart capability.
"""

from datetime import date, datetime
from decimal import Decimal

from pydantic import BaseModel, Field

from src.common.constants import (
    BatchProcessType,
    BatchStatus,
    CheckpointPhase,
    CheckpointStatus,
    DependencyType,
    ProcessFrequency,
)


class BatchParameters(BaseModel):
    """
    Batch job parameters from BCHPARM.cpy.

    Used to pass runtime parameters to batch programs.
    """

    batch_id: str = Field(max_length=8, description="Batch job identifier")
    process_date: date = Field(default_factory=date.today, description="Business processing date")
    process_type: BatchProcessType = Field(
        default=BatchProcessType.INITIAL, description="Processing type"
    )
    restart_flag: bool = Field(default=False, description="True if restarting from checkpoint")
    restart_step: str = Field(default="", max_length=8, description="Step to restart from")
    commit_frequency: int = Field(default=1000, ge=1, description="Commit after N records")
    max_errors: int = Field(default=100, ge=1, description="Maximum errors before abort")
    debug_mode: bool = Field(default=False, description="Enable debug output")


class BatchStatusRecord(BaseModel):
    """
    Batch job status from BCHSTAT.cpy.

    Tracks real-time statistics for a running batch job.
    """

    batch_id: str = Field(max_length=8, description="Batch job identifier")
    status: BatchStatus = Field(default=BatchStatus.READY, description="Current batch status")
    start_time: datetime | None = Field(default=None, description="Job start time")
    end_time: datetime | None = Field(default=None, description="Job end time")
    current_step: str = Field(default="", max_length=8, description="Current processing step")
    records_read: int = Field(default=0, ge=0, description="Records read counter")
    records_processed: int = Field(default=0, ge=0, description="Records processed counter")
    records_rejected: int = Field(default=0, ge=0, description="Records rejected counter")
    records_written: int = Field(default=0, ge=0, description="Records written counter")
    error_count: int = Field(default=0, ge=0, description="Total error count")
    last_error_msg: str = Field(default="", max_length=80, description="Last error message")
    return_code: int = Field(default=0, description="Final return code")


class BatchControlRecord(BaseModel):
    """
    Batch control record from BCHCTL.cpy / BCHCON.cpy.

    Manages batch job lifecycle, prerequisites, and scheduling.
    """

    batch_id: str = Field(max_length=8, description="Batch job identifier")
    batch_name: str = Field(default="", max_length=30, description="Batch job name")
    batch_status: BatchStatus = Field(default=BatchStatus.READY, description="Current status")
    schedule_date: date = Field(default_factory=date.today, description="Scheduled run date")
    process_type: BatchProcessType = Field(
        default=BatchProcessType.INITIAL, description="Processing type"
    )
    prerequisites: list[str] = Field(default_factory=list, description="Prerequisite batch IDs")
    max_restarts: int = Field(default=3, ge=0, description="Maximum restart attempts")
    restart_count: int = Field(default=0, ge=0, description="Current restart count")
    last_run_date: date | None = Field(default=None, description="Last successful run date")
    last_run_rc: int = Field(default=0, description="Last run return code")


class ProcessSequenceRecord(BaseModel):
    """
    Process sequence record from PRCSEQ.cpy.

    Defines ordered batch steps with dependency management.
    """

    sequence_id: str = Field(max_length=8, description="Sequence identifier")
    step_number: int = Field(ge=1, description="Step order number")
    program_name: str = Field(max_length=8, description="Program to execute")
    description: str = Field(default="", max_length=50, description="Step description")
    dependency_type: DependencyType = Field(
        default=DependencyType.REQUIRED, description="Dependency type"
    )
    frequency: ProcessFrequency = Field(
        default=ProcessFrequency.DAILY, description="Execution frequency"
    )
    skip_flag: bool = Field(default=False, description="Skip this step")
    estimated_time: int = Field(default=0, ge=0, description="Estimated time in seconds")


class CheckpointRecord(BaseModel):
    """
    Checkpoint/restart record from CKPRST.cpy.

    Enables resumable batch processing after failures.
    """

    checkpoint_id: str = Field(max_length=8, description="Checkpoint identifier")
    batch_id: str = Field(max_length=8, description="Parent batch job ID")
    status: CheckpointStatus = Field(
        default=CheckpointStatus.INITIAL, description="Checkpoint status"
    )
    phase: CheckpointPhase = Field(default=CheckpointPhase.INIT, description="Current phase")
    save_date: date = Field(default_factory=date.today, description="Checkpoint save date")
    save_time: datetime = Field(default_factory=datetime.now, description="Checkpoint save time")
    last_key: str = Field(default="", max_length=50, description="Last processed record key")
    records_at_checkpoint: int = Field(default=0, ge=0, description="Records processed at checkpoint")
    commit_count: int = Field(default=0, ge=0, description="Number of commits performed")
    restart_data: str = Field(default="", max_length=200, description="Restart context data")
    total_amount: Decimal = Field(
        default=Decimal("0.00"), max_digits=15, decimal_places=2,
        description="Running total at checkpoint"
    )

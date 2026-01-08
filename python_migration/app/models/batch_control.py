"""Batch Control Record model - converted from BCHCTL.cpy.

COBOL Original:
01  BATCH-CONTROL-RECORD.
    05  BCT-KEY.
        10  BCT-JOB-NAME      PIC X(8).
        10  BCT-PROCESS-DATE  PIC X(8).
        10  BCT-SEQUENCE-NO   PIC 9(4).
    05  BCT-DATA.
        10  BCT-STATUS        PIC X(1).
        10  BCT-PROCESS-CONTROL.
            15  BCT-STEP-NAME    PIC X(8).
            15  BCT-PROGRAM-NAME PIC X(8).
            15  BCT-START-TIME   PIC X(8).
            15  BCT-END-TIME     PIC X(8).
        10  BCT-DEPENDENCIES.
            15  BCT-PREREQ-COUNT PIC 9(2) COMP.
            15  BCT-PREREQ-JOBS  OCCURS 10 TIMES.
                20  BCT-PREREQ-NAME  PIC X(8).
                20  BCT-PREREQ-SEQ   PIC 9(4).
                20  BCT-PREREQ-RC    PIC S9(4) COMP.
        10  BCT-RETURN-INFO.
            15  BCT-RETURN-CODE  PIC S9(4) COMP.
            15  BCT-ERROR-DESC   PIC X(80).
    05  BCT-STATISTICS.
        10  BCT-RESTART-COUNT  PIC 9(2) COMP.
        10  BCT-ATTEMPT-TS     PIC X(26).
        10  BCT-COMPLETE-TS    PIC X(26).
"""

from datetime import datetime
from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field, field_validator

from app.models.error import ReturnCode


class BatchStatus(str, Enum):
    """Batch status codes - maps to 88-level conditions in COBOL."""

    READY = "R"  # BCT-STATUS-READY
    ACTIVE = "A"  # BCT-STATUS-ACTIVE
    WAITING = "W"  # BCT-STATUS-WAITING
    DONE = "D"  # BCT-STATUS-DONE
    ERROR = "E"  # BCT-STATUS-ERROR


class BatchControlKey(BaseModel):
    """Batch control key structure - maps to BCT-KEY in COBOL."""

    job_name: str = Field(max_length=8, description="Job name")
    process_date: str = Field(max_length=8, description="Process date (YYYYMMDD)")
    sequence_no: int = Field(ge=0, le=9999, description="Sequence number")

    @field_validator("job_name")
    @classmethod
    def strip_and_upper(cls, v: str) -> str:
        """Strip whitespace and convert to uppercase."""
        return v.strip().upper()

    @field_validator("process_date")
    @classmethod
    def validate_date_format(cls, v: str) -> str:
        """Validate date is in YYYYMMDD format."""
        if len(v) != 8 or not v.isdigit():
            raise ValueError("Date must be in YYYYMMDD format")
        return v


class ProcessControl(BaseModel):
    """Process control structure - maps to BCT-PROCESS-CONTROL in COBOL."""

    step_name: str = Field(default="", max_length=8, description="Step name")
    program_name: str = Field(default="", max_length=8, description="Program name")
    start_time: str = Field(default="", max_length=8, description="Start time")
    end_time: str = Field(default="", max_length=8, description="End time")


class PrerequisiteJob(BaseModel):
    """Prerequisite job structure - maps to BCT-PREREQ-JOBS in COBOL."""

    name: str = Field(max_length=8, description="Prerequisite job name")
    sequence: int = Field(ge=0, le=9999, description="Prerequisite sequence number")
    return_code: int = Field(default=0, description="Required return code")


class Dependencies(BaseModel):
    """Dependencies structure - maps to BCT-DEPENDENCIES in COBOL."""

    prereq_count: int = Field(default=0, ge=0, le=99, description="Prerequisite count")
    prereq_jobs: list[PrerequisiteJob] = Field(
        default_factory=list, max_length=10, description="Prerequisite jobs"
    )


class ReturnInfo(BaseModel):
    """Return information structure - maps to BCT-RETURN-INFO in COBOL."""

    return_code: int = Field(default=0, description="Return code")
    error_desc: str = Field(default="", max_length=80, description="Error description")


class BatchStatistics(BaseModel):
    """Batch statistics structure - maps to BCT-STATISTICS in COBOL."""

    restart_count: int = Field(default=0, ge=0, le=99, description="Restart count")
    attempt_ts: Optional[datetime] = Field(
        default=None, description="Last attempt timestamp"
    )
    complete_ts: Optional[datetime] = Field(
        default=None, description="Completion timestamp"
    )


class BatchControlRecord(BaseModel):
    """Complete batch control record - maps to BATCH-CONTROL-RECORD in COBOL.

    This model represents a batch job control record used for managing
    job-level sequencing, dependencies, and checkpoint/restart.
    """

    key: BatchControlKey
    status: BatchStatus = Field(default=BatchStatus.READY, description="Job status")
    process_control: ProcessControl = Field(default_factory=ProcessControl)
    dependencies: Dependencies = Field(default_factory=Dependencies)
    return_info: ReturnInfo = Field(default_factory=ReturnInfo)
    statistics: BatchStatistics = Field(default_factory=BatchStatistics)

    @property
    def job_name(self) -> str:
        """Convenience accessor for job name."""
        return self.key.job_name

    @property
    def process_date(self) -> str:
        """Convenience accessor for process date."""
        return self.key.process_date

    @property
    def is_ready(self) -> bool:
        """Check if job is ready to run."""
        return self.status == BatchStatus.READY

    @property
    def is_active(self) -> bool:
        """Check if job is currently running."""
        return self.status == BatchStatus.ACTIVE

    @property
    def is_waiting(self) -> bool:
        """Check if job is waiting for dependencies."""
        return self.status == BatchStatus.WAITING

    @property
    def is_done(self) -> bool:
        """Check if job completed successfully."""
        return self.status == BatchStatus.DONE

    @property
    def is_error(self) -> bool:
        """Check if job ended in error."""
        return self.status == BatchStatus.ERROR

    @property
    def has_prerequisites(self) -> bool:
        """Check if job has prerequisites."""
        return self.dependencies.prereq_count > 0

    def check_prerequisites_met(
        self, completed_jobs: dict[str, int]
    ) -> tuple[bool, str]:
        """Check if all prerequisites are met.

        Args:
            completed_jobs: Dictionary of job names to their return codes

        Returns:
            Tuple of (prerequisites_met, reason)
        """
        for prereq in self.dependencies.prereq_jobs:
            if prereq.name not in completed_jobs:
                return False, f"Prerequisite {prereq.name} not completed"
            if completed_jobs[prereq.name] > prereq.return_code:
                return (
                    False,
                    f"Prerequisite {prereq.name} failed with RC={completed_jobs[prereq.name]}",
                )
        return True, "All prerequisites met"

    def mark_active(self, step_name: str = "", program_name: str = "") -> None:
        """Mark job as active/running."""
        self.status = BatchStatus.ACTIVE
        self.process_control.step_name = step_name
        self.process_control.program_name = program_name
        self.process_control.start_time = datetime.now().strftime("%H:%M:%S")
        self.statistics.attempt_ts = datetime.now()

    def mark_complete(self, return_code: int = 0, error_desc: str = "") -> None:
        """Mark job as complete."""
        self.status = BatchStatus.DONE if return_code <= ReturnCode.WARNING else BatchStatus.ERROR
        self.process_control.end_time = datetime.now().strftime("%H:%M:%S")
        self.return_info.return_code = return_code
        self.return_info.error_desc = error_desc
        self.statistics.complete_ts = datetime.now()

    def mark_error(self, return_code: int, error_desc: str) -> None:
        """Mark job as error."""
        self.status = BatchStatus.ERROR
        self.process_control.end_time = datetime.now().strftime("%H:%M:%S")
        self.return_info.return_code = return_code
        self.return_info.error_desc = error_desc

    def increment_restart(self) -> None:
        """Increment restart count."""
        self.statistics.restart_count += 1
        self.statistics.attempt_ts = datetime.now()

    def to_flat_dict(self) -> dict:
        """Convert to flat dictionary for database operations."""
        return {
            "job_name": self.key.job_name,
            "process_date": self.key.process_date,
            "sequence_no": self.key.sequence_no,
            "status": self.status.value,
            "step_name": self.process_control.step_name,
            "program_name": self.process_control.program_name,
            "start_time": self.process_control.start_time,
            "end_time": self.process_control.end_time,
            "prereq_count": self.dependencies.prereq_count,
            "return_code": self.return_info.return_code,
            "error_desc": self.return_info.error_desc,
            "restart_count": self.statistics.restart_count,
            "attempt_ts": self.statistics.attempt_ts,
            "complete_ts": self.statistics.complete_ts,
        }

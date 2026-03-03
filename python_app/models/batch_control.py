"""Batch Control Record model - translated from BCHCTL.cpy copybook.

Mirrors the COBOL BATCH-CONTROL-RECORD structure used for
job-level sequencing, dependency management, and checkpoint tracking.
"""

from enum import StrEnum

from pydantic import BaseModel, Field


class BatchStatus(StrEnum):
    """Batch control status codes from 88-level values in BCHCTL."""

    READY = "R"
    ACTIVE = "A"
    WAITING = "W"
    DONE = "D"
    ERROR = "E"


class PrerequisiteJob(BaseModel):
    """Prerequisite job entry from BCT-PREREQ-JOBS OCCURS 10 TIMES."""

    name: str = Field(default="", max_length=8, description="BCT-PREREQ-NAME")
    sequence: int = Field(default=0, description="BCT-PREREQ-SEQ 9(4)")
    return_code: int = Field(default=0, description="BCT-PREREQ-RC S9(4)")


class BatchControlRecord(BaseModel):
    """Full batch control record translated from COBOL BATCH-CONTROL-RECORD.

    Maps to BCHCTL.cpy copybook fields:
    - BCT-KEY (job_name + process_date + sequence_no)
    - BCT-DATA (status, process control, dependencies, return info)
    """

    # Key fields (BCT-KEY)
    job_name: str = Field(max_length=8, description="BCT-JOB-NAME")
    process_date: str = Field(max_length=8, description="BCT-PROCESS-DATE: YYYYMMDD")
    sequence_no: int = Field(default=0, description="BCT-SEQUENCE-NO 9(4)")

    # Status
    status: BatchStatus = Field(default=BatchStatus.READY, description="BCT-STATUS: R/A/W/D/E")

    # Process control (BCT-PROCESS-CONTROL)
    step_name: str = Field(default="", max_length=8, description="BCT-STEP-NAME")
    program_name: str = Field(default="", max_length=8, description="BCT-PROGRAM-NAME")
    start_time: str = Field(default="", max_length=8, description="BCT-START-TIME")
    end_time: str = Field(default="", max_length=8, description="BCT-END-TIME")

    # Dependencies (BCT-DEPENDENCIES)
    prereq_count: int = Field(default=0, description="BCT-PREREQ-COUNT 9(2)")
    prereq_jobs: list[PrerequisiteJob] = Field(default_factory=list, description="BCT-PREREQ-JOBS")

    # Return info (BCT-RETURN-INFO)
    return_code: int = Field(default=0, description="BCT-RETURN-CODE S9(4)")

    # Extended fields from BCHCON constants
    restart_count: int = Field(default=0, description="Restart attempt counter")
    max_restarts: int = Field(default=3, description="Maximum restart attempts")
    error_desc: str = Field(default="", max_length=80, description="Error description")
    attempt_ts: str = Field(default="", max_length=26, description="Last attempt timestamp")

    @property
    def composite_key(self) -> str:
        """Build the composite key."""
        return f"{self.job_name}{self.process_date}{self.sequence_no:04d}"

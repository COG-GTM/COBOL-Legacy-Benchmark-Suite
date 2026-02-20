"""Batch Control File Record Definition - migrated from BCHCTL.cpy and BCHCON.cpy."""

from dataclasses import dataclass, field
from enum import Enum


class BatchStatus(str, Enum):
    READY = "R"
    ACTIVE = "A"
    WAITING = "W"
    DONE = "D"
    ERROR = "E"


class ProcessType(str, Enum):
    INITIAL = "INI"
    UPDATE = "UPD"
    REPORT = "RPT"
    CLEANUP = "CLN"


class DependencyType(str, Enum):
    REQUIRED = "R"
    OPTIONAL = "O"
    EXCLUSIVE = "X"


@dataclass
class PrerequisiteJob:
    name: str = ""
    sequence: int = 0
    return_code: int = 0


@dataclass
class BatchControlRecord:
    job_name: str = ""
    process_date: str = ""
    sequence_no: int = 0
    status: str = BatchStatus.READY
    step_name: str = ""
    program_name: str = ""
    start_time: str = ""
    end_time: str = ""
    prereq_count: int = 0
    prereq_jobs: list = field(default_factory=lambda: [PrerequisiteJob() for _ in range(10)])
    return_code: int = 0
    error_desc: str = ""
    restart_count: int = 0
    attempt_ts: str = ""
    complete_ts: str = ""

    @property
    def batch_key(self) -> str:
        return f"{self.job_name}{self.process_date}{self.sequence_no:04d}"


class BatchControlConstants:
    MAX_PREREQ = 10
    MAX_RESTARTS = 3
    WAIT_INTERVAL = 300
    MAX_WAIT_TIME = 3600

    PROC_START_OF_DAY = "STARTDAY"
    PROC_END_OF_DAY = "ENDDAY  "
    PROC_EMERGENCY = "EMERGENCY"

    MSG_STARTING = "Process starting..."
    MSG_COMPLETE = "Process completed successfully"
    MSG_FAILED = "Process failed - check errors"
    MSG_WAITING = "Waiting for prerequisites"

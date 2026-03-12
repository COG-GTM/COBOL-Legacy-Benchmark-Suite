"""
Batch control data models translated from COBOL copybooks.

Source copybooks:
  - src/copybook/batch/BCHCTL.cpy   (Batch Control File Record)
  - src/copybook/batch/BCHCON.cpy   (Batch Control Constants)
  - src/copybook/batch/PRCSEQ.cpy   (Process Sequence Definitions)
  - src/copybook/batch/CKPRST.cpy   (Checkpoint/Restart Control)
"""

from __future__ import annotations

import datetime
from enum import Enum
from typing import Annotated, Optional

from pydantic import BaseModel, Field, field_validator


# ---------------------------------------------------------------------------
# Enums from BCHCTL.cpy / BCHCON.cpy level-88 values
# ---------------------------------------------------------------------------

class BatchStatus(str, Enum):
    """BCT-STATUS level-88 values from BCHCTL.cpy."""

    READY = "R"
    ACTIVE = "A"
    WAITING = "W"
    DONE = "D"
    ERROR = "E"


class ProcessType(str, Enum):
    """BCT-PROC-TYPES from BCHCON.cpy."""

    INITIAL = "INI"
    UPDATE = "UPD"
    REPORT = "RPT"
    CLEANUP = "CLN"


class DependencyType(str, Enum):
    """BCT-DEP-TYPES from BCHCON.cpy."""

    REQUIRED = "R"
    OPTIONAL = "O"
    EXCLUSIVE = "X"


class RecordType(str, Enum):
    """BCT-REC-TYPES from BCHCON.cpy."""

    CONTROL = "C"
    PROCESS = "P"
    DEPENDENCY = "D"
    HISTORY = "H"


# ---------------------------------------------------------------------------
# Enums from PRCSEQ.cpy level-88 values
# ---------------------------------------------------------------------------

class ProcessSequenceType(str, Enum):
    """PSR-TYPE level-88 values from PRCSEQ.cpy."""

    INIT = "INI"
    PROCESS = "PRC"
    REPORT = "RPT"
    TERMINATE = "TRM"


class ProcessFrequency(str, Enum):
    """PSR-FREQ level-88 values from PRCSEQ.cpy."""

    DAILY = "D"
    WEEKLY = "W"
    MONTHLY = "M"


class ProcessDependencyType(str, Enum):
    """PSR-DEP-TYPE level-88 values from PRCSEQ.cpy."""

    HARD = "H"
    SOFT = "S"


# ---------------------------------------------------------------------------
# Enums from CKPRST.cpy level-88 values
# ---------------------------------------------------------------------------

class CheckpointStatus(str, Enum):
    """CK-STATUS level-88 values from CKPRST.cpy."""

    INITIAL = "I"
    ACTIVE = "A"
    COMPLETE = "C"
    FAILED = "F"
    RESTARTED = "R"


class CheckpointPhase(str, Enum):
    """CK-PHASE level-88 values from CKPRST.cpy."""

    INIT = "00"
    READ = "10"
    PROCESS = "20"
    UPDATE = "30"
    TERMINATE = "40"


class CheckpointRestartMode(str, Enum):
    """CK-RESTART-MODE level-88 values from CKPRST.cpy."""

    NORMAL = "N"
    RESTART = "R"
    RECOVER = "C"


# ---------------------------------------------------------------------------
# Sub-models
# ---------------------------------------------------------------------------

class PrerequisiteJob(BaseModel):
    """Single prerequisite entry from BCHCTL.cpy BCT-PREREQ-JOBS.

    OCCURS 10 TIMES:
      BCT-PREREQ-NAME  PIC X(8)
      BCT-PREREQ-SEQ   PIC 9(4)
      BCT-PREREQ-RC    PIC S9(4) COMP
    """

    prereq_name: Annotated[str, Field(max_length=8)]
    prereq_seq: int = 0
    prereq_rc: int = 0


class FileStatus(BaseModel):
    """Single file-status entry from CKPRST.cpy CK-FILE-STATUS.

    OCCURS 5 TIMES:
      CK-FILE-NAME    PIC X(8)
      CK-FILE-POS     PIC X(50)
      CK-FILE-STATUS  PIC X(2)
    """

    file_name: Annotated[str, Field(max_length=8)]
    file_pos: Annotated[str, Field(max_length=50)] = ""
    file_status: Annotated[str, Field(max_length=2)] = ""


class ProcessDependencyEntry(BaseModel):
    """Single dependency entry from PRCSEQ.cpy PSR-DEP-ENTRY.

    OCCURS 10 TIMES:
      PSR-DEP-ID    PIC X(8)
      PSR-DEP-TYPE  PIC X(1)
      PSR-DEP-RC    PIC S9(4) COMP
    """

    dep_id: Annotated[str, Field(max_length=8)]
    dep_type: ProcessDependencyType = ProcessDependencyType.HARD
    dep_rc: int = 0


# ---------------------------------------------------------------------------
# Main models
# ---------------------------------------------------------------------------

class BatchControl(BaseModel):
    """Batch control record from BCHCTL.cpy  01 BATCH-CONTROL-RECORD.

    Field sizes from PIC clauses:
      BCT-JOB-NAME      PIC X(8)
      BCT-PROCESS-DATE   PIC X(8)        YYYYMMDD
      BCT-SEQUENCE-NO    PIC 9(4)
      BCT-STATUS         PIC X(1)
      BCT-STEP-NAME      PIC X(8)
      BCT-PROGRAM-NAME   PIC X(8)
      BCT-START-TIME     PIC X(8)
      BCT-END-TIME       PIC X(8)
      BCT-PREREQ-COUNT   PIC 9(2) COMP
      BCT-PREREQ-JOBS    OCCURS 10 TIMES
      BCT-RETURN-CODE    PIC S9(4) COMP
      BCT-ERROR-DESC     PIC X(80)
      BCT-RESTART-COUNT  PIC 9(2) COMP
      BCT-ATTEMPT-TS     PIC X(26)
      BCT-COMPLETE-TS    PIC X(26)
    """

    # BCT-KEY
    job_name: Annotated[str, Field(max_length=8)]
    process_date: datetime.date
    sequence_no: int = 0

    # BCT-DATA
    status: BatchStatus = BatchStatus.READY
    step_name: Annotated[str, Field(max_length=8)] = ""
    program_name: Annotated[str, Field(max_length=8)] = ""
    start_time: Annotated[str, Field(max_length=8)] = ""
    end_time: Annotated[str, Field(max_length=8)] = ""

    # Dependencies (OCCURS 10 TIMES)
    prereq_count: int = 0
    prerequisites: list[PrerequisiteJob] = Field(default_factory=list, max_length=10)

    # Return info
    return_code: int = 0
    error_desc: Annotated[str, Field(max_length=80)] = ""

    # Statistics
    restart_count: int = 0
    attempt_ts: Optional[datetime.datetime] = None
    complete_ts: Optional[datetime.datetime] = None

    @field_validator("process_date", mode="before")
    @classmethod
    def _parse_cobol_date(cls, value: object) -> object:
        if isinstance(value, str) and len(value) == 8 and value.isdigit():
            return datetime.date(
                int(value[:4]), int(value[4:6]), int(value[6:8])
            )
        return value

    @field_validator("attempt_ts", "complete_ts", mode="before")
    @classmethod
    def _parse_cobol_timestamp(cls, value: object) -> object:
        if isinstance(value, str) and len(value) == 26:
            try:
                return datetime.datetime.fromisoformat(value.strip())
            except ValueError:
                pass
        return value


class BatchParameters(BaseModel):
    """Batch processing parameters from BCHCON.cpy constants.

    Captures the control constants defined in BATCH-CONTROL-CONSTANTS.
    """

    # BCT-RC-THRESHOLDS
    rc_success: int = 0
    rc_warning: int = 4
    rc_error: int = 8
    rc_severe: int = 12
    rc_critical: int = 16

    # BCT-CTRL-VALUES
    max_prerequisites: int = 10
    max_restarts: int = 3
    wait_interval_seconds: int = 300
    max_wait_time_seconds: int = 3600


class ProcessSequence(BaseModel):
    """Process sequence record from PRCSEQ.cpy  01 PROCESS-SEQUENCE-RECORD.

    Field sizes from PIC clauses:
      PSR-PROCESS-ID   PIC X(8)
      PSR-VERSION      PIC 9(2)
      PSR-DESCRIPTION  PIC X(30)
      PSR-TYPE         PIC X(3)
      PSR-FREQ         PIC X(1)
      PSR-START-TIME   PIC 9(4)
      PSR-MAX-TIME     PIC 9(4)
      PSR-DEP-COUNT    PIC 9(2) COMP
      PSR-DEP-ENTRY    OCCURS 10 TIMES
      PSR-PROGRAM      PIC X(8)
      PSR-PARM         PIC X(50)
      PSR-MAX-RC       PIC S9(4) COMP
      PSR-RESTART      PIC X(1)
      PSR-ACTIVE-DAYS  PIC X(7)
      PSR-MONTH-END    PIC X(1)
      PSR-HOLIDAY-RUN  PIC X(1)
      PSR-RECOVERY-PGM PIC X(8)
      PSR-RECOVERY-PARM PIC X(50)
      PSR-ERROR-LIMIT  PIC 9(4) COMP
      PSR-CREATE-DATE  PIC X(10)
      PSR-CREATE-USER  PIC X(8)
      PSR-UPDATE-DATE  PIC X(10)
      PSR-UPDATE-USER  PIC X(8)
    """

    # PSR-KEY
    process_id: Annotated[str, Field(max_length=8)]
    version: int = 1

    # PSR-DATA
    description: Annotated[str, Field(max_length=30)] = ""
    process_type: ProcessSequenceType = ProcessSequenceType.PROCESS
    frequency: ProcessFrequency = ProcessFrequency.DAILY
    start_time: int = 0
    max_time: int = 0

    # Dependencies (OCCURS 10 TIMES)
    dep_count: int = 0
    dependencies: list[ProcessDependencyEntry] = Field(
        default_factory=list, max_length=10
    )

    # Control
    program: Annotated[str, Field(max_length=8)] = ""
    parm: Annotated[str, Field(max_length=50)] = ""
    max_rc: int = 0
    restartable: bool = True

    # Schedule
    active_days: Annotated[str, Field(max_length=7)] = "YYYYYNN"
    month_end: bool = False
    holiday_run: bool = False

    # Recovery
    recovery_program: Annotated[str, Field(max_length=8)] = ""
    recovery_parm: Annotated[str, Field(max_length=50)] = ""
    error_limit: int = 0

    # Audit
    create_date: Annotated[str, Field(max_length=10)] = ""
    create_user: Annotated[str, Field(max_length=8)] = ""
    update_date: Annotated[str, Field(max_length=10)] = ""
    update_user: Annotated[str, Field(max_length=8)] = ""


class CheckpointControl(BaseModel):
    """Checkpoint/restart control from CKPRST.cpy  01 CHECKPOINT-CONTROL.

    Field sizes from PIC clauses:
      CK-PROGRAM-ID      PIC X(8)
      CK-RUN-DATE        PIC X(8)        YYYYMMDD
      CK-RUN-TIME        PIC X(6)        HHMMSS
      CK-STATUS          PIC X(1)
      CK-RECORDS-READ    PIC 9(9) COMP
      CK-RECORDS-PROC    PIC 9(9) COMP
      CK-RECORDS-ERROR   PIC 9(9) COMP
      CK-RESTART-COUNT   PIC 9(2) COMP
      CK-LAST-KEY        PIC X(50)
      CK-LAST-TIME       PIC X(26)
      CK-PHASE           PIC X(2)
      CK-FILE-STATUS     OCCURS 5 TIMES
      CK-COMMIT-FREQ     PIC 9(5) COMP  VALUE 1000
      CK-MAX-ERRORS      PIC 9(3) COMP  VALUE 100
      CK-MAX-RESTARTS    PIC 9(2) COMP  VALUE 3
      CK-RESTART-MODE    PIC X(1)
    """

    # CK-HEADER
    program_id: Annotated[str, Field(max_length=8)]
    run_date: datetime.date
    run_time: Annotated[str, Field(max_length=6, pattern=r"^\d{6}$")]
    status: CheckpointStatus = CheckpointStatus.INITIAL

    # CK-COUNTERS
    records_read: int = 0
    records_processed: int = 0
    records_error: int = 0
    restart_count: int = 0

    # CK-POSITION
    last_key: Annotated[str, Field(max_length=50)] = ""
    last_time: Optional[datetime.datetime] = None
    phase: CheckpointPhase = CheckpointPhase.INIT

    # CK-RESOURCES (OCCURS 5 TIMES)
    file_statuses: list[FileStatus] = Field(default_factory=list, max_length=5)

    # CK-CONTROL-INFO
    commit_frequency: int = 1000
    max_errors: int = 100
    max_restarts: int = 3
    restart_mode: CheckpointRestartMode = CheckpointRestartMode.NORMAL

    @field_validator("run_date", mode="before")
    @classmethod
    def _parse_cobol_date(cls, value: object) -> object:
        if isinstance(value, str) and len(value) == 8 and value.isdigit():
            return datetime.date(
                int(value[:4]), int(value[4:6]), int(value[6:8])
            )
        return value

    @field_validator("last_time", mode="before")
    @classmethod
    def _parse_cobol_timestamp(cls, value: object) -> object:
        if isinstance(value, str) and len(value) == 26:
            try:
                return datetime.datetime.fromisoformat(value.strip())
            except ValueError:
                pass
        return value

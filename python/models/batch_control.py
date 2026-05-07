"""BATCH-CONTROL-RECORD model from BCHCTL.cpy."""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import List, Optional

from python.models.batch_constants import MAX_PREREQ, ProcessStatus, ReturnCode


@dataclass
class PrerequisiteJob:
    """BCT-PREREQ-JOBS occurrence (one of up to 10 entries).

    Field lengths from BCHCTL.cpy:
        BCT-PREREQ-NAME  PIC X(8)
        BCT-PREREQ-SEQ   PIC 9(4)
        BCT-PREREQ-RC    PIC S9(4) COMP
    """

    name: str = ""
    sequence: int = 0
    return_code: int = 0

    def __post_init__(self) -> None:
        self.name = self.name[:8]


@dataclass
class BatchControlRecord:
    """Mirror of BATCH-CONTROL-RECORD from BCHCTL.cpy.

    Field lengths:
        BCT-JOB-NAME       PIC X(8)
        BCT-PROCESS-DATE   PIC X(8)   (YYYYMMDD)
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
        BCT-FILLER         PIC X(50)
        BCT-RECORDS-READ   (used by HISTLD00 2310-UPDATE-CHECKPOINT)
        BCT-RECORDS-WRITTEN
    """

    job_name: str = ""
    process_date: str = ""
    sequence_no: int = 0
    status: str = ProcessStatus.READY.value
    step_name: str = ""
    program_name: str = ""
    start_time: str = ""
    end_time: str = ""
    prereqs: List[PrerequisiteJob] = field(default_factory=list)
    return_code: int = ReturnCode.SUCCESS
    error_desc: str = ""
    restart_count: int = 0
    attempt_ts: str = ""
    complete_ts: str = ""
    records_read: int = 0
    records_written: int = 0

    def __post_init__(self) -> None:
        self.job_name = self.job_name[:8]
        self.process_date = self.process_date[:8]
        self.step_name = self.step_name[:8]
        self.program_name = self.program_name[:8]
        self.start_time = self.start_time[:8]
        self.end_time = self.end_time[:8]
        self.error_desc = self.error_desc[:80]
        self.attempt_ts = self.attempt_ts[:26]
        self.complete_ts = self.complete_ts[:26]
        if len(self.prereqs) > MAX_PREREQ:
            self.prereqs = self.prereqs[:MAX_PREREQ]

    @property
    def key(self) -> str:
        """Return the BCT-KEY composite (job + date + sequence)."""
        return f"{self.job_name:<8}{self.process_date:<8}{self.sequence_no:04d}"

    def mark_active(self, when: Optional[datetime] = None) -> None:
        """Transition status to ACTIVE and stamp BCT-ATTEMPT-TS."""
        when = when or datetime.now(timezone.utc)
        self.status = ProcessStatus.ACTIVE.value
        self.attempt_ts = _format_ts(when)
        self.start_time = when.strftime("%H:%M:%S")

    def mark_done(
        self,
        when: Optional[datetime] = None,
        return_code: int = ReturnCode.SUCCESS,
    ) -> None:
        """Transition status to DONE and stamp BCT-COMPLETE-TS."""
        when = when or datetime.now(timezone.utc)
        self.status = ProcessStatus.DONE.value
        self.complete_ts = _format_ts(when)
        self.end_time = when.strftime("%H:%M:%S")
        self.return_code = return_code

    def mark_error(
        self,
        error_desc: str,
        return_code: int = ReturnCode.ERROR,
        when: Optional[datetime] = None,
    ) -> None:
        """Transition status to ERROR and store the error description."""
        when = when or datetime.now(timezone.utc)
        self.status = ProcessStatus.ERROR.value
        self.complete_ts = _format_ts(when)
        self.end_time = when.strftime("%H:%M:%S")
        self.error_desc = error_desc[:80]
        self.return_code = return_code


def _format_ts(when: datetime) -> str:
    """Format a 26-character timestamp matching COBOL ``X(26)``."""
    base = when.strftime("%Y-%m-%d-%H.%M.%S.%f")
    return base[:26].ljust(26)

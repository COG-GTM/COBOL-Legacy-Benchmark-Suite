"""Checkpoint/Restart Control model - translated from CKPRST.cpy copybook.

Mirrors the COBOL CHECKPOINT-CONTROL structure used for
program-level state tracking and restart capability.
"""

from enum import StrEnum

from pydantic import BaseModel, Field


class CheckpointStatus(StrEnum):
    """Checkpoint status codes from 88-level values in CKPRST."""

    INITIAL = "I"
    ACTIVE = "A"
    COMPLETE = "C"
    FAILED = "F"
    RESTARTED = "R"


class CheckpointPhase(StrEnum):
    """Processing phase codes from 88-level values in CKPRST."""

    INIT = "00"
    READ = "10"
    PROCESS = "20"
    UPDATE = "30"
    TERMINATE = "40"


class CheckpointControl(BaseModel):
    """Checkpoint control record translated from COBOL CHECKPOINT-CONTROL.

    Maps to CKPRST.cpy copybook fields:
    - CK-HEADER (program identification and status)
    - CK-COUNTERS (processing counters)
    - CK-POSITION (restart position tracking)
    """

    # Header (CK-HEADER)
    program_id: str = Field(max_length=8, description="CK-PROGRAM-ID")
    run_date: str = Field(max_length=8, description="CK-RUN-DATE: YYYYMMDD")
    run_time: str = Field(max_length=6, description="CK-RUN-TIME: HHMMSS")
    status: CheckpointStatus = Field(default=CheckpointStatus.INITIAL, description="CK-STATUS")

    # Counters (CK-COUNTERS)
    records_read: int = Field(default=0, description="CK-RECORDS-READ 9(9)")
    records_processed: int = Field(default=0, description="CK-RECORDS-PROC 9(9)")
    records_error: int = Field(default=0, description="CK-RECORDS-ERROR 9(9)")
    restart_count: int = Field(default=0, description="CK-RESTART-COUNT 9(2)")

    # Position (CK-POSITION)
    last_key: str = Field(default="", max_length=50, description="CK-LAST-KEY")
    last_time: str = Field(default="", max_length=26, description="CK-LAST-TIME")
    phase: CheckpointPhase = Field(default=CheckpointPhase.INIT, description="CK-PHASE")

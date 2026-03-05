"""
Batch control models translated from COBOL copybooks:
- BCHCTL.cpy (Batch Control Record)
- BCHPARM.cpy (Batch Parameters)
- BCHSTAT.cpy (Batch Statistics)
- CKPRST.cpy (Checkpoint/Restart)
- PRCSEQ.cpy (Process Sequence)
"""

from datetime import datetime
from decimal import Decimal
from typing import Optional

from pydantic import BaseModel, Field

from src.common.constants import BatchStatus, ReturnCode


class BatchControlRecord(BaseModel):
    """Translates COBOL BATCH-CONTROL-RECORD from BCHCTL.cpy."""

    job_name: str = Field(max_length=8, description="PIC X(08)")
    process_date: str = Field(max_length=8, description="PIC X(08) YYYYMMDD")
    sequence_no: int = Field(default=0, description="PIC 9(4)")
    status: BatchStatus = Field(default=BatchStatus.READY, description="PIC X(01)")
    return_code: ReturnCode = Field(default=ReturnCode.SUCCESS, description="PIC S9(4) COMP")
    start_time: Optional[datetime] = Field(default=None)
    end_time: Optional[datetime] = Field(default=None)
    records_read: int = Field(default=0, description="PIC S9(9) COMP")
    records_written: int = Field(default=0, description="PIC S9(9) COMP")
    error_count: int = Field(default=0, description="PIC S9(9) COMP")
    restart_count: int = Field(default=0, description="PIC S9(4) COMP")
    max_restarts: int = Field(default=3, description="PIC S9(4) COMP")
    error_desc: str = Field(max_length=80, default="", description="PIC X(80)")
    attempt_ts: Optional[datetime] = Field(default=None)

    model_config = {"from_attributes": True}


class BatchParameters(BaseModel):
    """Translates COBOL batch parameter record from BCHPARM.cpy."""

    param_name: str = Field(max_length=20)
    param_value: str = Field(max_length=100)
    param_type: str = Field(max_length=10, default="STRING")
    description: str = Field(max_length=80, default="")

    model_config = {"from_attributes": True}


class BatchStatistics(BaseModel):
    """Translates COBOL batch statistics from BCHSTAT.cpy."""

    job_name: str = Field(max_length=8)
    process_date: str = Field(max_length=8)
    start_time: Optional[datetime] = None
    end_time: Optional[datetime] = None
    total_records: int = 0
    records_processed: int = 0
    records_rejected: int = 0
    elapsed_seconds: Decimal = Field(default=Decimal("0.00"))
    cpu_seconds: Decimal = Field(default=Decimal("0.00"))

    model_config = {"from_attributes": True}


class ProcessSequenceRecord(BaseModel):
    """Translates COBOL PROCESS-SEQUENCE-RECORD from PRCSEQ.cpy."""

    process_id: str = Field(max_length=8, description="PIC X(08)")
    sequence_type: str = Field(max_length=3, description="PIC X(03)")
    sequence_no: int = Field(default=0, description="PIC 9(4)")
    description: str = Field(max_length=40, default="")
    restartable: bool = Field(default=True)
    dependencies: list[str] = Field(default_factory=list)
    dep_hard_flags: list[bool] = Field(default_factory=list)
    dep_max_rc: list[int] = Field(default_factory=list)

    model_config = {"from_attributes": True}


class CheckpointRecord(BaseModel):
    """Translates COBOL CHECKPOINT-CONTROL from CKPRST.cpy."""

    checkpoint_id: str = Field(max_length=20)
    job_name: str = Field(max_length=8)
    step_name: str = Field(max_length=8, default="")
    records_processed: int = 0
    last_key: str = Field(max_length=50, default="")
    timestamp: Optional[datetime] = None
    status: str = Field(max_length=1, default="A")

    model_config = {"from_attributes": True}

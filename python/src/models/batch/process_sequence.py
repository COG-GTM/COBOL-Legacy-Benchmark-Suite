"""
Pydantic v2 models for COBOL PRCSEQ copybook (Process Sequence Definitions).

Source: src/copybook/batch/PRCSEQ.cpy
"""

from typing import List

from pydantic import BaseModel, Field, field_validator


class ProcessSequenceKey(BaseModel):
    """Process sequence key from PSR-KEY (level 05)."""

    model_config = {"from_attributes": True}

    psr_process_id: str = Field(
        max_length=8,
        description="Process identifier. COBOL: PSR-PROCESS-ID PIC X(8).",
    )
    psr_version: int = Field(
        description="Version number. COBOL: PSR-VERSION PIC 9(2).",
    )


class DependencyEntry(BaseModel):
    """
    Single dependency entry from PSR-DEP-ENTRY (OCCURS 10 TIMES).

    COBOL: PSR-DEP-ENTRY OCCURS 10 TIMES.
    """

    model_config = {"from_attributes": True}

    psr_dep_id: str = Field(
        max_length=8,
        description="Dependency process ID. COBOL: PSR-DEP-ID PIC X(8).",
    )
    psr_dep_type: str = Field(
        max_length=1,
        description=(
            "Dependency type: H=Hard, S=Soft. "
            "COBOL: PSR-DEP-TYPE PIC X(1). "
            "88-level values: H, S."
        ),
    )
    psr_dep_rc: int = Field(
        description="Dependency return code threshold. COBOL: PSR-DEP-RC PIC S9(4) COMP.",
    )

    @field_validator("psr_dep_type")
    @classmethod
    def validate_dep_type(cls, v: str) -> str:
        valid = {"H", "S"}
        if v.strip() and v.strip() not in valid:
            raise ValueError(f"psr_dep_type must be one of {valid}")
        return v


class ProcessTiming(BaseModel):
    """Process timing from PSR-TIMING (level 10)."""

    model_config = {"from_attributes": True}

    psr_freq: str = Field(
        max_length=1,
        description=(
            "Frequency: D=Daily, W=Weekly, M=Monthly. "
            "COBOL: PSR-FREQ PIC X(1). "
            "88-level values: D, W, M."
        ),
    )
    psr_start_time: int = Field(
        description="Start time (HHMM). COBOL: PSR-START-TIME PIC 9(4).",
    )
    psr_max_time: int = Field(
        description="Maximum execution time (minutes). COBOL: PSR-MAX-TIME PIC 9(4).",
    )

    @field_validator("psr_freq")
    @classmethod
    def validate_freq(cls, v: str) -> str:
        valid = {"D", "W", "M"}
        if v.strip() and v.strip() not in valid:
            raise ValueError(f"psr_freq must be one of {valid}")
        return v


class ProcessDependencies(BaseModel):
    """Process dependencies from PSR-DEPENDENCIES (level 10)."""

    model_config = {"from_attributes": True}

    psr_dep_count: int = Field(
        description="Number of dependencies. COBOL: PSR-DEP-COUNT PIC 9(2) COMP.",
    )
    psr_dep_entries: List[DependencyEntry] = Field(
        max_length=10,
        description="Dependency entries. COBOL: PSR-DEP-ENTRY OCCURS 10 TIMES.",
    )


class ProcessControl(BaseModel):
    """Process control from PSR-CONTROL (level 10)."""

    model_config = {"from_attributes": True}

    psr_program: str = Field(
        max_length=8,
        description="Program name. COBOL: PSR-PROGRAM PIC X(8).",
    )
    psr_parm: str = Field(
        max_length=50,
        description="Program parameters. COBOL: PSR-PARM PIC X(50).",
    )
    psr_max_rc: int = Field(
        description="Maximum acceptable return code. COBOL: PSR-MAX-RC PIC S9(4) COMP.",
    )
    psr_restart: str = Field(
        max_length=1,
        description=(
            "Restartable flag: Y=Yes, N=No. "
            "COBOL: PSR-RESTART PIC X(1). "
            "88-level values: Y, N."
        ),
    )

    @field_validator("psr_restart")
    @classmethod
    def validate_restart(cls, v: str) -> str:
        valid = {"Y", "N"}
        if v.strip() and v.strip() not in valid:
            raise ValueError(f"psr_restart must be one of {valid}")
        return v


class ProcessSequenceData(BaseModel):
    """Process sequence data from PSR-DATA (level 05)."""

    model_config = {"from_attributes": True}

    psr_description: str = Field(
        max_length=30,
        description="Process description. COBOL: PSR-DESCRIPTION PIC X(30).",
    )
    psr_type: str = Field(
        max_length=3,
        description=(
            "Process type: INI=Initial, PRC=Process, RPT=Report, TRM=Terminate. "
            "COBOL: PSR-TYPE PIC X(3). "
            "88-level values: INI, PRC, RPT, TRM."
        ),
    )
    psr_timing: ProcessTiming = Field(description="Process timing (PSR-TIMING).")
    psr_dependencies: ProcessDependencies = Field(
        description="Dependencies (PSR-DEPENDENCIES).",
    )
    psr_control: ProcessControl = Field(description="Process control (PSR-CONTROL).")

    @field_validator("psr_type")
    @classmethod
    def validate_psr_type(cls, v: str) -> str:
        valid = {"INI", "PRC", "RPT", "TRM"}
        if v.strip() and v.strip() not in valid:
            raise ValueError(f"psr_type must be one of {valid}")
        return v


class ProcessSchedule(BaseModel):
    """Process schedule from PSR-SCHEDULE (level 05)."""

    model_config = {"from_attributes": True}

    psr_active_days: str = Field(
        max_length=7,
        description=(
            "Active days (7 char, Y/N for each day Mon-Sun). "
            "COBOL: PSR-ACTIVE-DAYS PIC X(7). "
            "88-level values: YYYYYNN=Weekday, NNNNNYY=Weekend, YYYYYYY=All."
        ),
    )
    psr_month_end: str = Field(
        max_length=1,
        description=(
            "Run on month end: Y=Yes. "
            "COBOL: PSR-MONTH-END PIC X(1). "
            "88-level values: Y."
        ),
    )
    psr_holiday_run: str = Field(
        max_length=1,
        description=(
            "Holiday run flag: N=Skip, Y=Run. "
            "COBOL: PSR-HOLIDAY-RUN PIC X(1). "
            "88-level values: N, Y."
        ),
    )


class ProcessRecovery(BaseModel):
    """Process recovery from PSR-RECOVERY (level 05)."""

    model_config = {"from_attributes": True}

    psr_recovery_pgm: str = Field(
        max_length=8,
        description="Recovery program. COBOL: PSR-RECOVERY-PGM PIC X(8).",
    )
    psr_recovery_parm: str = Field(
        max_length=50,
        description="Recovery parameters. COBOL: PSR-RECOVERY-PARM PIC X(50).",
    )
    psr_error_limit: int = Field(
        description="Error limit. COBOL: PSR-ERROR-LIMIT PIC 9(4) COMP.",
    )


class ProcessAudit(BaseModel):
    """Process audit from PSR-AUDIT (level 05)."""

    model_config = {"from_attributes": True}

    psr_create_date: str = Field(
        max_length=10,
        description="Creation date. COBOL: PSR-CREATE-DATE PIC X(10).",
    )
    psr_create_user: str = Field(
        max_length=8,
        description="Creation user. COBOL: PSR-CREATE-USER PIC X(8).",
    )
    psr_update_date: str = Field(
        max_length=10,
        description="Last update date. COBOL: PSR-UPDATE-DATE PIC X(10).",
    )
    psr_update_user: str = Field(
        max_length=8,
        description="Last update user. COBOL: PSR-UPDATE-USER PIC X(8).",
    )


class ProcessSequenceRecord(BaseModel):
    """
    Process Sequence Record -- maps to COBOL 01-level PROCESS-SEQUENCE-RECORD.

    Source: src/copybook/batch/PRCSEQ.cpy
    """

    model_config = {"from_attributes": True}

    psr_key: ProcessSequenceKey = Field(description="Process sequence key (PSR-KEY).")
    psr_data: ProcessSequenceData = Field(description="Process sequence data (PSR-DATA).")
    psr_schedule: ProcessSchedule = Field(description="Process schedule (PSR-SCHEDULE).")
    psr_recovery: ProcessRecovery = Field(description="Process recovery (PSR-RECOVERY).")
    psr_audit: ProcessAudit = Field(description="Process audit (PSR-AUDIT).")
    psr_filler: str = Field(
        default="",
        max_length=50,
        description="Reserved filler. COBOL: PSR-FILLER PIC X(50).",
    )


class StandardSequences(BaseModel):
    """
    Standard process sequences -- maps to COBOL 01-level STANDARD-SEQUENCES.

    Source: src/copybook/batch/PRCSEQ.cpy
    """

    model_config = {"from_attributes": True}

    seq_start_of_day: List[str] = Field(
        default=["INITDAY", "CKPCLR", "DATEVAL"],
        description="Start of day sequence. COBOL: SEQ-START-OF-DAY (3 x PIC X(8) FILLERs).",
    )
    seq_main_process: List[str] = Field(
        default=["TRNVAL00", "POSUPD00", "HISTLD00"],
        description="Main process sequence. COBOL: SEQ-MAIN-PROCESS (3 x PIC X(8) FILLERs).",
    )
    seq_end_of_day: List[str] = Field(
        default=["RPTGEN00", "BCKLOD00", "ENDDAY"],
        description="End of day sequence. COBOL: SEQ-END-OF-DAY (3 x PIC X(8) FILLERs).",
    )

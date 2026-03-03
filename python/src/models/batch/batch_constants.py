"""
Pydantic v2 models for COBOL BCHCON copybook (Batch Control Constants).

Source: src/copybook/batch/BCHCON.cpy
"""

from pydantic import BaseModel, Field


class BatchStatusValues(BaseModel):
    """Process status values from BCT-STAT-VALUES (level 05)."""

    model_config = {"from_attributes": True}

    bct_stat_ready: str = Field(default="R", max_length=1, description="Ready status. COBOL: BCT-STAT-READY PIC X(1) VALUE 'R'.")
    bct_stat_active: str = Field(default="A", max_length=1, description="Active status. COBOL: BCT-STAT-ACTIVE PIC X(1) VALUE 'A'.")
    bct_stat_waiting: str = Field(default="W", max_length=1, description="Waiting status. COBOL: BCT-STAT-WAITING PIC X(1) VALUE 'W'.")
    bct_stat_done: str = Field(default="D", max_length=1, description="Done status. COBOL: BCT-STAT-DONE PIC X(1) VALUE 'D'.")
    bct_stat_error: str = Field(default="E", max_length=1, description="Error status. COBOL: BCT-STAT-ERROR PIC X(1) VALUE 'E'.")


class BatchRCThresholds(BaseModel):
    """Return code thresholds from BCT-RC-THRESHOLDS (level 05)."""

    model_config = {"from_attributes": True}

    bct_rc_success: int = Field(default=0, description="Success threshold. COBOL: BCT-RC-SUCCESS PIC S9(4) COMP VALUE +0.")
    bct_rc_warning: int = Field(default=4, description="Warning threshold. COBOL: BCT-RC-WARNING PIC S9(4) COMP VALUE +4.")
    bct_rc_error: int = Field(default=8, description="Error threshold. COBOL: BCT-RC-ERROR PIC S9(4) COMP VALUE +8.")
    bct_rc_severe: int = Field(default=12, description="Severe threshold. COBOL: BCT-RC-SEVERE PIC S9(4) COMP VALUE +12.")
    bct_rc_critical: int = Field(default=16, description="Critical threshold. COBOL: BCT-RC-CRITICAL PIC S9(4) COMP VALUE +16.")


class BatchControlValues(BaseModel):
    """Process control values from BCT-CTRL-VALUES (level 05)."""

    model_config = {"from_attributes": True}

    bct_max_prereq: int = Field(default=10, description="Max prerequisites. COBOL: BCT-MAX-PREREQ PIC 9(2) COMP VALUE 10.")
    bct_max_restarts: int = Field(default=3, description="Max restarts. COBOL: BCT-MAX-RESTARTS PIC 9(2) COMP VALUE 3.")
    bct_wait_interval: int = Field(default=300, description="Wait interval (seconds). COBOL: BCT-WAIT-INTERVAL PIC 9(4) COMP VALUE 300.")
    bct_max_wait_time: int = Field(default=3600, description="Max wait time (seconds). COBOL: BCT-MAX-WAIT-TIME PIC 9(4) COMP VALUE 3600.")


class BatchProcessTypes(BaseModel):
    """Process types from BCT-PROC-TYPES (level 05)."""

    model_config = {"from_attributes": True}

    bct_type_initial: str = Field(default="INI", max_length=3, description="Initial type. COBOL: BCT-TYPE-INITIAL PIC X(3) VALUE 'INI'.")
    bct_type_update: str = Field(default="UPD", max_length=3, description="Update type. COBOL: BCT-TYPE-UPDATE PIC X(3) VALUE 'UPD'.")
    bct_type_report: str = Field(default="RPT", max_length=3, description="Report type. COBOL: BCT-TYPE-REPORT PIC X(3) VALUE 'RPT'.")
    bct_type_cleanup: str = Field(default="CLN", max_length=3, description="Cleanup type. COBOL: BCT-TYPE-CLEANUP PIC X(3) VALUE 'CLN'.")


class BatchDependencyTypes(BaseModel):
    """Dependency types from BCT-DEP-TYPES (level 05)."""

    model_config = {"from_attributes": True}

    bct_dep_required: str = Field(default="R", max_length=1, description="Required dependency. COBOL: BCT-DEP-REQUIRED PIC X(1) VALUE 'R'.")
    bct_dep_optional: str = Field(default="O", max_length=1, description="Optional dependency. COBOL: BCT-DEP-OPTIONAL PIC X(1) VALUE 'O'.")
    bct_dep_exclusive: str = Field(default="X", max_length=1, description="Exclusive dependency. COBOL: BCT-DEP-EXCLUSIVE PIC X(1) VALUE 'X'.")


class BatchProcessNames(BaseModel):
    """Special process names from BCT-PROC-NAMES (level 05)."""

    model_config = {"from_attributes": True}

    bct_start_of_day: str = Field(default="STARTDAY", max_length=8, description="Start of day process. COBOL: BCT-START-OF-DAY PIC X(8) VALUE 'STARTDAY'.")
    bct_end_of_day: str = Field(default="ENDDAY", max_length=8, description="End of day process. COBOL: BCT-END-OF-DAY PIC X(8) VALUE 'ENDDAY  '.")
    bct_emergency: str = Field(default="EMERGENC", max_length=8, description="Emergency process. COBOL: BCT-EMERGENCY PIC X(8) VALUE 'EMERGENCY'.")


class BatchRecordTypes(BaseModel):
    """Control file record types from BCT-REC-TYPES (level 05)."""

    model_config = {"from_attributes": True}

    bct_rec_control: str = Field(default="C", max_length=1, description="Control record. COBOL: BCT-REC-CONTROL PIC X(1) VALUE 'C'.")
    bct_rec_process: str = Field(default="P", max_length=1, description="Process record. COBOL: BCT-REC-PROCESS PIC X(1) VALUE 'P'.")
    bct_rec_depend: str = Field(default="D", max_length=1, description="Dependency record. COBOL: BCT-REC-DEPEND PIC X(1) VALUE 'D'.")
    bct_rec_history: str = Field(default="H", max_length=1, description="History record. COBOL: BCT-REC-HISTORY PIC X(1) VALUE 'H'.")


class BatchMessages(BaseModel):
    """Standard messages from BCT-MESSAGES (level 05)."""

    model_config = {"from_attributes": True}

    bct_msg_starting: str = Field(default="Process starting...", max_length=30, description="Starting message. COBOL: BCT-MSG-STARTING PIC X(30).")
    bct_msg_complete: str = Field(default="Process completed successfully", max_length=30, description="Complete message. COBOL: BCT-MSG-COMPLETE PIC X(30).")
    bct_msg_failed: str = Field(default="Process failed - check errors", max_length=30, description="Failed message. COBOL: BCT-MSG-FAILED PIC X(30).")
    bct_msg_waiting: str = Field(default="Waiting for prerequisites", max_length=30, description="Waiting message. COBOL: BCT-MSG-WAITING PIC X(30).")


class BatchControlConstants(BaseModel):
    """
    Batch Control Constants -- maps to COBOL 01-level BATCH-CONTROL-CONSTANTS.

    Source: src/copybook/batch/BCHCON.cpy
    """

    model_config = {"from_attributes": True}

    bct_stat_values: BatchStatusValues = Field(default_factory=BatchStatusValues, description="Process status values (BCT-STAT-VALUES).")
    bct_rc_thresholds: BatchRCThresholds = Field(default_factory=BatchRCThresholds, description="Return code thresholds (BCT-RC-THRESHOLDS).")
    bct_ctrl_values: BatchControlValues = Field(default_factory=BatchControlValues, description="Process control values (BCT-CTRL-VALUES).")
    bct_proc_types: BatchProcessTypes = Field(default_factory=BatchProcessTypes, description="Process types (BCT-PROC-TYPES).")
    bct_dep_types: BatchDependencyTypes = Field(default_factory=BatchDependencyTypes, description="Dependency types (BCT-DEP-TYPES).")
    bct_proc_names: BatchProcessNames = Field(default_factory=BatchProcessNames, description="Special process names (BCT-PROC-NAMES).")
    bct_rec_types: BatchRecordTypes = Field(default_factory=BatchRecordTypes, description="Control file record types (BCT-REC-TYPES).")
    bct_messages: BatchMessages = Field(default_factory=BatchMessages, description="Standard messages (BCT-MESSAGES).")

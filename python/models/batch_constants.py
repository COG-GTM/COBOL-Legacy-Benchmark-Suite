"""Constants from BCHCON.cpy.

Mirrors the BATCH-CONTROL-CONSTANTS structure: status values, return-code
thresholds, process types, dependency types, and standard messages.
"""

from __future__ import annotations

from enum import Enum, IntEnum


class ProcessStatus(str, Enum):
    """BCT-STATUS values from BCHCON.cpy / BCHCTL.cpy."""

    READY = "R"
    ACTIVE = "A"
    WAITING = "W"
    DONE = "D"
    ERROR = "E"


class ReturnCode(IntEnum):
    """BCT-RC-* return-code thresholds from BCHCON.cpy."""

    SUCCESS = 0
    WARNING = 4
    ERROR = 8
    SEVERE = 12
    CRITICAL = 16


class ProcessType(str, Enum):
    """BCT-TYPE-* process types from BCHCON.cpy."""

    INITIAL = "INI"
    UPDATE = "UPD"
    REPORT = "RPT"
    CLEANUP = "CLN"


class DependencyType(str, Enum):
    """BCT-DEP-* dependency types from BCHCON.cpy."""

    REQUIRED = "R"
    OPTIONAL = "O"
    EXCLUSIVE = "X"


class RecordType(str, Enum):
    """BCT-REC-* control file record types from BCHCON.cpy."""

    CONTROL = "C"
    PROCESS = "P"
    DEPEND = "D"
    HISTORY = "H"


# Process control limits (BCT-CTRL-VALUES)
MAX_PREREQ = 10
MAX_RESTARTS = 3
WAIT_INTERVAL_SECONDS = 300
MAX_WAIT_SECONDS = 3600

# Standard messages (BCT-MESSAGES)
MSG_STARTING = "Process starting...           "
MSG_COMPLETE = "Process completed successfully"
MSG_FAILED = "Process failed - check errors "
MSG_WAITING = "Waiting for prerequisites     "

# Reserved process names (BCT-PROC-NAMES)
PROC_START_OF_DAY = "STARTDAY"
PROC_END_OF_DAY = "ENDDAY"
PROC_EMERGENCY = "EMERGENCY"

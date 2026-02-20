"""Process Sequence Definitions - migrated from PRCSEQ.cpy."""

from dataclasses import dataclass, field
from enum import Enum


class ProcessTypeCode(str, Enum):
    INIT = "INI"
    PROCESS = "PRC"
    REPORT = "RPT"
    TERMINATE = "TRM"


class Frequency(str, Enum):
    DAILY = "D"
    WEEKLY = "W"
    MONTHLY = "M"


class DependencyStrength(str, Enum):
    HARD = "H"
    SOFT = "S"


@dataclass
class DependencyEntry:
    dep_id: str = ""
    dep_type: str = DependencyStrength.HARD
    dep_rc: int = 0


@dataclass
class ProcessSequenceRecord:
    process_id: str = ""
    version: int = 0
    description: str = ""
    proc_type: str = ProcessTypeCode.PROCESS
    frequency: str = Frequency.DAILY
    start_time: int = 0
    max_time: int = 0
    dep_count: int = 0
    dependencies: list = field(default_factory=lambda: [DependencyEntry() for _ in range(10)])
    program: str = ""
    parm: str = ""
    max_rc: int = 0
    restartable: bool = True
    active_days: str = "YYYYYNN"
    month_end: bool = False
    holiday_run: bool = False
    recovery_pgm: str = ""
    recovery_parm: str = ""
    error_limit: int = 0
    create_date: str = ""
    create_user: str = ""
    update_date: str = ""
    update_user: str = ""

    @property
    def sequence_key(self) -> str:
        return f"{self.process_id}{self.version:02d}"


STANDARD_SEQUENCES = {
    "start_of_day": ["INITDAY ", "CKPCLR  ", "DATEVAL "],
    "main_process": ["TRNVAL00", "POSUPD00", "HISTLD00"],
    "end_of_day": ["RPTGEN00", "BCKLOD00", "ENDDAY  "],
}

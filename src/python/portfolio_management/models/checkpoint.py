"""Checkpoint/Restart Control Structure - migrated from CKPRST.cpy."""

from dataclasses import dataclass, field
from enum import Enum


class CheckpointStatus(str, Enum):
    INITIAL = "I"
    ACTIVE = "A"
    COMPLETE = "C"
    FAILED = "F"
    RESTARTED = "R"


class CheckpointPhase(str, Enum):
    INIT = "00"
    READ = "10"
    PROCESS = "20"
    UPDATE = "30"
    TERMINATE = "40"


class RestartMode(str, Enum):
    NORMAL = "N"
    RESTART = "R"
    RECOVER = "C"


@dataclass
class FileStatus:
    file_name: str = ""
    file_pos: str = ""
    status: str = ""


@dataclass
class CheckpointControl:
    program_id: str = ""
    run_date: str = ""
    run_time: str = ""
    status: str = CheckpointStatus.INITIAL
    records_read: int = 0
    records_processed: int = 0
    records_error: int = 0
    restart_count: int = 0
    last_key: str = ""
    last_time: str = ""
    phase: str = CheckpointPhase.INIT
    file_statuses: list = field(default_factory=lambda: [FileStatus() for _ in range(5)])
    commit_freq: int = 1000
    max_errors: int = 100
    max_restarts: int = 3
    restart_mode: str = RestartMode.NORMAL


@dataclass
class CheckpointRecord:
    program_id: str = ""
    run_date: str = ""
    data: str = ""

    @property
    def checkpoint_key(self) -> str:
        return f"{self.program_id}{self.run_date}"
